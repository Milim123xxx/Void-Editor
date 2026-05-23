package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.EditorSettings
import com.example.data.database.InstalledExtension
import com.example.data.database.WorkspaceFile
import com.example.data.database.WorkspaceProject
import com.example.data.repository.DiffLine
import com.example.data.repository.VSCodeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Models ---
data class ChatMessage(val text: String, val sender: String, val timestamp: Long = System.currentTimeMillis())
data class CommandLine(val text: String, val isError: Boolean = false, val isSuccess: Boolean = false, val isPrompt: Boolean = false)

class VSCodeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VSCodeRepository(application.applicationContext)

    // Sidebar Configuration
    enum class SidebarTab { EXPLORER, SEARCH, GIT, MARKETPLACE, SETTINGS, AI_CHAT }
    val activeSidebarTab = MutableStateFlow(SidebarTab.EXPLORER)

    // Data streams from Room
    val allProjects: StateFlow<List<WorkspaceProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExtensions: StateFlow<List<InstalledExtension>> = repository.allExtensions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<EditorSettings> = repository.editorSettings
        .map { it ?: EditorSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EditorSettings())

    // Tracks files in active project
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProjectFiles: StateFlow<List<WorkspaceFile>> = settings
        .flatMapLatest { s ->
            if (s.activeProjectId != -1) {
                repository.getFilesByProject(s.activeProjectId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tabs & Code Editor State
    val openTabs = MutableStateFlow<List<String>>(listOf("README.md"))
    val activeFilePath = MutableStateFlow<String?>("README.md")

    val activeFile: StateFlow<WorkspaceFile?> = combine(activeProjectFiles, activeFilePath) { files, path ->
        files.firstOrNull { it.filePath == path }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Editor live content (when editing, we keep memory copy before saving)
    val liveEditorContent = MutableStateFlow("")

    // Track if edited content is unsaved
    val isUnsaved = combine(activeFile, liveEditorContent) { file, live ->
        if (file == null) false else file.content != live
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Search & Replace Panel states
    val searchPattern = MutableStateFlow("")
    val replacePattern = MutableStateFlow("")
    val searchResults = combine(activeProjectFiles, searchPattern) { files, pat ->
        if (pat.isEmpty()) return@combine emptyList<SearchMatch>()
        val matches = mutableListOf<SearchMatch>()
        for (f in files) {
            val lines = f.content.lines()
            for ((idx, line) in lines.withIndex()) {
                if (line.contains(pat, ignoreCase = true)) {
                    matches.add(SearchMatch(f.filePath, line, idx + 1))
                }
            }
        }
        matches
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class SearchMatch(val filePath: String, val lineContent: String, val lineNumber: Int)

    // Git Status Helpers
    val activeProject = settings.map { s ->
        allProjects.value.firstOrNull { it.id == s.activeProjectId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Lists staged modified paths
    val stagedFilesList: StateFlow<List<String>> = activeProject.map { proj ->
        if (proj == null) return@map emptyList<String>()
        val json = JSONArray(proj.stagedFilesJson)
        val list = mutableListOf<String>()
        for (i in 0 until json.length()) {
            list.add(json.getString(i))
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lists modified files that aren't staged
    val modifiedUnstagedFiles: StateFlow<List<WorkspaceFile>> = combine(activeProjectFiles, stagedFilesList) { files, staged ->
        files.filter { it.hasUncommittedChanges && !staged.contains(it.filePath) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected file for git diff pane
    val diffFileAndLineResults = MutableStateFlow<Pair<WorkspaceFile, List<DiffLine>>?>(null)

    // AI Copilot State
    val chatHistory = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("สวัสดีครับ! ผมคือ **Gemini Copilot** ผู้ช่วยมืออาชีพของคุณ คุณสามารถ:\n- ให้ช่วยเขียนฟังก์ชั่นหรือบอยเลอร์เพลต\n- ถามอธิบายโค้ดจากแท็บที่เปิดอยู่\n- สั่งให้ช่วยแก้ไขบั๊กต่าง ๆ ได้เลยครับ", "assistant")
    ))
    val isAiLoading = MutableStateFlow(false)

    // Terminal History Status
    val terminalInput = MutableStateFlow("")
    val terminalHistory = MutableStateFlow<List<CommandLine>>(listOf(
        CommandLine("Windows PowerShell (VS Code Console v1.99)", isSuccess = true),
        CommandLine("Type 'help' to list available system console tasks.", isPrompt = true),
        CommandLine("")
    ))

    init {
        viewModelScope.launch {
            repository.initializeAndSeed()
            
            // Auto open first tab on successfully loaded
            activeProjectFiles.firstOrNull()?.firstOrNull()?.let { f ->
                openFile(f.filePath, f.content)
            }
        }
    }

    // --- Tab and file controls ---
    fun openFile(filePath: String, content: String) {
        viewModelScope.launch {
            // Unsaved edits cache backup check
            val currentActivePath = activeFilePath.value
            val currentFileObj = activeFile.value
            if (currentActivePath != null && currentFileObj != null) {
                if (liveEditorContent.value != currentFileObj.content) {
                    repository.saveFileContent(settings.value.activeProjectId, currentActivePath, liveEditorContent.value)
                }
            }

            val tabs = openTabs.value.toMutableList()
            if (!tabs.contains(filePath)) {
                tabs.add(filePath)
                openTabs.value = tabs
            }
            activeFilePath.value = filePath
            liveEditorContent.value = content
        }
    }

    fun closeTab(filePath: String) {
        viewModelScope.launch {
            val tabs = openTabs.value.toMutableList()
            tabs.remove(filePath)
            openTabs.value = tabs

            if (activeFilePath.value == filePath) {
                if (tabs.isNotEmpty()) {
                    val nextItemPath = tabs.last()
                    val nextFile = activeProjectFiles.value.firstOrNull { it.filePath == nextItemPath }
                    activeFilePath.value = nextItemPath
                    liveEditorContent.value = nextFile?.content ?: ""
                } else {
                    activeFilePath.value = null
                    liveEditorContent.value = ""
                }
            }
        }
    }

    fun updateLiveContent(newText: String) {
        liveEditorContent.value = newText
    }

    fun saveCurrentFile() {
        viewModelScope.launch {
            val projId = settings.value.activeProjectId
            val path = activeFilePath.value ?: return@launch
            repository.saveFileContent(projId, path, liveEditorContent.value)
            
            // Reload from Room to capture formatting updates if prettier applied
            val fileObj = repository.getFilesByProject(projId).firstOrNull()?.firstOrNull { it.filePath == path }
            if (fileObj != null) {
                liveEditorContent.value = fileObj.content
            }
        }
    }

    fun createNewFileInProject(filePath: String) {
        viewModelScope.launch {
            val projId = settings.value.activeProjectId
            if (projId != -1) {
                val contents = when {
                    filePath.endsWith(".html") -> "<!-- New HTML document -->\n<!DOCTYPE html>\n<html>\n<body>\n  \n</body>\n</html>"
                    filePath.endsWith(".py") -> "# Python Executable\n\nprint('Hello mobile programming!')"
                    filePath.endsWith(".js") -> "// Javascript workspace\nconsole.log('Online');"
                    else -> ""
                }
                val isDone = repository.createFile(projId, filePath, contents)
                if (isDone) {
                    openFile(filePath, contents)
                }
            }
        }
    }

    fun deleteFileInProject(filePath: String) {
        viewModelScope.launch {
            val projId = settings.value.activeProjectId
            if (projId != -1) {
                repository.deleteFile(projId, filePath)
                closeTab(filePath)
            }
        }
    }

    fun runGlobalReplace() {
        viewModelScope.launch {
            val projId = settings.value.activeProjectId
            val searchPat = searchPattern.value
            val replacePat = replacePattern.value
            if (searchPat.isNotEmpty() && projId != -1) {
                for (f in activeProjectFiles.value) {
                    if (f.content.contains(searchPat)) {
                        val modified = f.content.replace(searchPat, replacePat)
                        repository.saveFileContent(projId, f.filePath, modified)
                        if (activeFilePath.value == f.filePath) {
                            liveEditorContent.value = modified
                        }
                    }
                }
            }
        }
    }

    // --- Switch Project ---
    fun selectActiveProject(id: Int) {
        viewModelScope.launch {
            repository.updateActiveProject(id)
            openTabs.value = emptyList()
            activeFilePath.value = null
            liveEditorContent.value = ""

            // Load default file of that project if available
            val freshFiles = repository.getFilesByProject(id).firstOrNull() ?: emptyList()
            if (freshFiles.isNotEmpty()) {
                val lead = freshFiles.first()
                openFile(lead.filePath, lead.content)
            }
        }
    }

    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            val newId = repository.addProject(name, description)
            selectActiveProject(newId)
        }
    }

    fun deleteActiveProject(id: Int) {
        viewModelScope.launch {
            repository.deleteProject(id)
            val others = allProjects.value
            val nextBest = others.firstOrNull { it.id != id }
            if (nextBest != null) {
                selectActiveProject(nextBest.id)
            } else {
                repository.updateActiveProject(-1)
            }
        }
    }

    // --- Extension Management ---
    fun toggleExtensionIsEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleExtension(id, enabled)
        }
    }

    // --- Editor Preferences ---
    fun updateTheme(themeName: String) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(themeName = themeName))
        }
    }

    fun updateFontSize(newSize: Float) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(fontSize = newSize.coerceIn(10f, 24f)))
        }
    }

    fun toggleWordWrap(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(isWordWrapEnabled = enabled))
        }
    }

    fun updateWallpaper(wallpaperName: String) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(wallpaperName = wallpaperName))
        }
    }

    fun toggleTerminalPanel(open: Boolean) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(terminalOpen = open))
        }
    }

    // --- Git Actions Flow ---
    fun stageFile(filePath: String) {
        viewModelScope.launch {
            repository.gitStageFile(settings.value.activeProjectId, filePath)
        }
    }

    fun unstageFile(filePath: String) {
        viewModelScope.launch {
            repository.gitUnstageFile(settings.value.activeProjectId, filePath)
        }
    }

    fun stageAllChanges() {
        viewModelScope.launch {
            val pId = settings.value.activeProjectId
            val modifiedPaths = modifiedUnstagedFiles.value.map { it.filePath }
            val alreadyStaged = stagedFilesList.value
            val combo = (alreadyStaged + modifiedPaths).distinct()
            repository.gitStageAll(pId, combo)
        }
    }

    fun submitCommit(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            repository.gitCommit(settings.value.activeProjectId, message)
        }
    }

    fun showDiffViewer(file: WorkspaceFile) {
        viewModelScope.launch {
            val diffLines = repository.calculateLineDiffs(file.initialContent, file.content)
            diffFileAndLineResults.value = Pair(file, diffLines)
        }
    }

    fun closeDiffViewer() {
        diffFileAndLineResults.value = null
    }

    fun triggerGitClone(url: String) {
        viewModelScope.launch {
            val newId = repository.gitClone(url)
            selectActiveProject(newId)
        }
    }

    fun switchGitBranch(branchName: String) {
        viewModelScope.launch {
            repository.gitCreateBranch(settings.value.activeProjectId, branchName)
        }
    }

    // --- AI Assist Chat flows ---
    fun sendAiPromptMessage(userPromptMessage: String) {
        val messageText = userPromptMessage.trim()
        if (messageText.isEmpty()) return

        val userMsgObj = ChatMessage(messageText, "user")
        val currentHistory = chatHistory.value.toMutableList()
        currentHistory.add(userMsgObj)
        chatHistory.value = currentHistory

        viewModelScope.launch {
            isAiLoading.value = true
            
            // Grab selected file context to enrich help
            val currentFileContext = activeFile.value
            val snippet = currentFileContext?.let {
                "--- File Path: ${it.filePath} ---\n${liveEditorContent.value}"
            } ?: ""

            val promptCommand = if (snippet.isNotEmpty()) {
                "You are an expert IDE Assistant built directly inside VS Code Mobile as a premium extension.\n" +
                "The user is operating on the following code file context right now:\n$snippet\n\nQuestion or Request: $messageText"
            } else {
                "You are an expert IDE Assistant built directly inside VS Code Mobile as a premium extension.\n" +
                "Question or Request: $messageText"
            }

            val reply = repository.queryCopilotAssistant(promptCommand)
            val modelMsgObj = ChatMessage(reply, "assistant")
            val nextHistory = chatHistory.value.toMutableList()
            nextHistory.add(modelMsgObj)
            chatHistory.value = nextHistory
            
            isAiLoading.value = false
        }
    }

    // Auto-replace code using AI
    fun applyAiCodeEnhancement(instruction: String) {
        viewModelScope.launch {
            val currCode = liveEditorContent.value
            val path = activeFilePath.value ?: return@launch
            isAiLoading.value = true

            val systemInstruction = "Implement code modification request. You are a code generator. " +
                    "Return ONLY the updated file contents. Do NOT write markdown, code fences, or chat text. " +
                    "Generate plain unescaped source text output matching the requested edits."

            val prompt = "Current file: $path\nCode:\n$currCode\n\nInstruction to edit: $instruction"
            val updated = repository.queryCopilotAssistant(prompt, systemInstruction = systemInstruction)
            
            if (updated.isNotEmpty() && !updated.startsWith("Error:") && !updated.startsWith("⚠️")) {
                // Stripping out backtick formatting indicators if the AI included them despite instruction
                var cleanDoc = updated.trim()
                if (cleanDoc.startsWith("```")) {
                    cleanDoc = cleanDoc.substringAfter("\n")
                    if (cleanDoc.endsWith("```")) {
                        cleanDoc = cleanDoc.substringBeforeLast("```")
                    }
                }
                liveEditorContent.value = cleanDoc
            } else {
                // Return chat log error helper
                chatHistory.value = chatHistory.value + ChatMessage("⚠️ Failed to apply code modifications: $updated", "assistant")
            }
            isAiLoading.value = false
        }
    }

    // --- Mock Interactive Terminal Command Processor ---
    fun runCommandLine(rawCommand: String) {
        val cleanInput = rawCommand.trim()
        if (cleanInput.isEmpty()) return

        // Append user prompt tag
        val results = terminalHistory.value.toMutableList()
        results.add(CommandLine("> $cleanInput", isPrompt = true))

        val parts = cleanInput.split(" ")
        val command = parts[0].lowercase()

        when (command) {
            "help" -> {
                results.add(CommandLine("Available console tasks on VS Code Mobile:", isSuccess = true))
                results.add(CommandLine("  ls                      Lists current code files in project workspace"))
                results.add(CommandLine("  cat <file>              Outputs text content of target path in workspace"))
                results.add(CommandLine("  python <file>           Executes/analyzes Python scripts and outputs terminal print data."))
                results.add(CommandLine("  node <file>             Runs JavaScript script logs in mock container environment."))
                results.add(CommandLine("  npm install <pkg>       Simulates downloading and adding dependencies."))
                results.add(CommandLine("  git status              Shows working branch, staged items & uncommitted edits"))
                results.add(CommandLine("  git add <file>          Stages target file for git history commit"))
                results.add(CommandLine("  git add .               Stages all edited file blocks in workspace"))
                results.add(CommandLine("  git commit -m \"msg\"     Commits staged tracking files to local commit log"))
                results.add(CommandLine("  git log                 Lists commits history"))
                results.add(CommandLine("  prettier -run           Synthetically triggers Prettier format edits on open file"))
                results.add(CommandLine("  clear                   Clears the workspace terminal terminal display"))
            }
            "clear" -> {
                terminalHistory.value = listOf(CommandLine("Terminal cleared. Ready.", isSuccess = true))
                terminalInput.value = ""
                return
            }
            "ls" -> {
                val files = activeProjectFiles.value
                if (files.isEmpty()) {
                    results.add(CommandLine("Directory is currently empty. Create modern project codes to begin."))
                } else {
                    results.add(CommandLine("Worspace layout mapping metrics (Local):", isSuccess = true))
                    for (f in files) {
                        val size = f.content.length
                        val marker = if (f.hasUncommittedChanges) " [Modified]" else ""
                        results.add(CommandLine("  - ${f.filePath} ($size bytes)$marker"))
                    }
                }
            }
            "cat" -> {
                val targetFile = parts.getOrNull(1)
                if (targetFile == null) {
                    results.add(CommandLine("Error: Specify file path to display. E.g. cat index.html", isError = true))
                } else {
                    val fileObj = activeProjectFiles.value.firstOrNull { it.filePath == targetFile }
                    if (fileObj == null) {
                        results.add(CommandLine("Error: File '$targetFile' not found in workspace directories.", isError = true))
                    } else {
                        results.add(CommandLine("=== ${fileObj.filePath} ===", isSuccess = true))
                        val lines = fileObj.content.lines()
                        for (l in lines) {
                            results.add(CommandLine(l))
                        }
                    }
                }
            }
            "npm" -> {
                if (parts.getOrNull(1)?.lowercase() == "install") {
                    val pkg = parts.getOrNull(2) ?: "lodash"
                    results.add(CommandLine("npm WARN config global `--global`, `--local` are deprecated. Use `--location=global` instead."))
                    results.add(CommandLine("npm info run packages adding: $pkg..."))
                    results.add(CommandLine("✔ Installed dependency node_modules/$pkg successfully verified in background.", isSuccess = true))
                } else {
                    results.add(CommandLine("Error: npm commands supported: 'npm install <package>'", isError = true))
                }
            }
            "prettier" -> {
                if (parts.getOrNull(1)?.lowercase() == "-run") {
                    results.add(CommandLine("prettier: running formats verification on opened file..."))
                    saveCurrentFile()
                    results.add(CommandLine("✔ Prettier formatting successfully applied!", isSuccess = true))
                } else {
                    results.add(CommandLine("Prettier error: invalid syntax. Try 'prettier -run'", isError = true))
                }
            }
            "python" -> {
                val script = parts.getOrNull(1)
                if (script == null) {
                    results.add(CommandLine("Error: Specify python module. E.g. python main.py", isError = true))
                } else {
                    val fileObj = activeProjectFiles.value.firstOrNull { it.filePath == script }
                    if (fileObj == null) {
                        results.add(CommandLine("Error: Python script '$script' not found.", isError = true))
                    } else if (!script.endsWith(".py")) {
                        results.add(CommandLine("Error: '$script' is not a valid Python (.py) format file.", isError = true))
                    } else {
                        results.add(CommandLine("python interpreter active: $script", isPrompt = true))
                        results.add(CommandLine(">>> running compile lifecycle..."))
                        // Run simulated code blocks parser
                        if (fileObj.content.contains("calculate_analytics")) {
                            results.add(CommandLine("🛸 AI analysis computing online..."))
                            results.add(CommandLine("Mean: 10.24"))
                            results.add(CommandLine("Standard Deviation: 1.39"))
                        } else if (fileObj.content.contains("print")) {
                            // Extract basic prints
                            val prints = fileObj.content.lines()
                                .filter { it.trim().startsWith("print(") }
                                .map { it.substringAfter("print(").substringBeforeLast(")").replace("'", "").replace("\"", "") }
                            if (prints.isNotEmpty()) {
                                for (p in prints) {
                                    results.add(CommandLine(p))
                                }
                            } else {
                                results.add(CommandLine("Hello mobile programming!"))
                            }
                        } else {
                            results.add(CommandLine("Process finished with status exit code: 0", isSuccess = true))
                        }
                    }
                }
            }
            "node" -> {
                val script = parts.getOrNull(1)
                if (script == null) {
                    results.add(CommandLine("Error: Specify node script. E.g. node script.js", isError = true))
                } else {
                    val fileObj = activeProjectFiles.value.firstOrNull { it.filePath == script }
                    if (fileObj == null) {
                        results.add(CommandLine("Error: File '$script' not found.", isError = true))
                    } else {
                        results.add(CommandLine("node micro-engine: virtual execution of $script", isPrompt = true))
                        if (fileObj.content.contains("console.log")) {
                            val consoles = fileObj.content.lines()
                                .filter { it.contains("console.log(") }
                                .map { it.substringAfter("console.log(").substringBeforeLast(")").replace("'", "").replace("\"", "") }
                            for (c in consoles) {
                                results.add(CommandLine(c))
                            }
                        } else {
                            results.add(CommandLine("✔ Running verified. Returned with code 0"))
                        }
                    }
                }
            }
            "git" -> {
                val subCmd = parts.getOrNull(1)?.lowercase()
                when (subCmd) {
                    "status" -> {
                        val proj = activeProject.value
                        if (proj != null) {
                            results.add(CommandLine("On branch ${proj.gitBranch}", isSuccess = true))
                            results.add(CommandLine("Your branch is up to date with remote repository local logs."))
                            results.add(CommandLine(""))
                            val staged = stagedFilesList.value
                            val modified = modifiedUnstagedFiles.value
                            
                            if (staged.isNotEmpty()) {
                                results.add(CommandLine("Changes to be committed:", isSuccess = true))
                                results.add(CommandLine("  (use \"git restore --staged <file>...\" to unstage)"))
                                for (s in staged) {
                                    results.add(CommandLine("\tmodified:   $s", isSuccess = true))
                                }
                            }
                            if (modified.isNotEmpty()) {
                                results.add(CommandLine("Changes not staged for commit:", isError = true))
                                results.add(CommandLine("  (use \"git add <file>...\" to update what will be committed)"))
                                for (m in modified) {
                                    results.add(CommandLine("\tmodified:   ${m.filePath}", isError = true))
                                }
                            }
                            if (staged.isEmpty() && modified.isEmpty()) {
                                results.add(CommandLine("nothing to commit, working tree clean"))
                            }
                        }
                    }
                    "add" -> {
                        val target = parts.getOrNull(2)
                        if (target == null) {
                            results.add(CommandLine("Error: specify file. Try 'git add <file>' or 'git add .'", isError = true))
                        } else if (target == ".") {
                            val modifiedPaths = modifiedUnstagedFiles.value.map { it.filePath }
                            val alreadyStaged = stagedFilesList.value
                            viewModelScope.launch {
                                repository.gitStageAll(settings.value.activeProjectId, (alreadyStaged + modifiedPaths).distinct())
                            }
                            results.add(CommandLine("Staged all changed tracking structures successfully.", isSuccess = true))
                        } else {
                            val exists = activeProjectFiles.value.any { it.filePath == target }
                            if (exists) {
                                viewModelScope.launch {
                                    repository.gitStageFile(settings.value.activeProjectId, target)
                                }
                                results.add(CommandLine("Staged path $target successfully.", isSuccess = true))
                            } else {
                                results.add(CommandLine("Error: file path '$target' not matched.", isError = true))
                            }
                        }
                    }
                    "commit" -> {
                        val mIdx = parts.indexOf("-m")
                        if (mIdx == -1 || mIdx == parts.size - 1) {
                            results.add(CommandLine("Error: commit message requires '-m \"message\"'", isError = true))
                        } else {
                            val textStr = parts.drop(mIdx + 1).joinToString(" ").replace("\"", "").replace("'", "")
                            if (stagedFilesList.value.isEmpty()) {
                                results.add(CommandLine("nothing staged for commit (use 'git add' to stage)", isError = true))
                            } else {
                                val size = stagedFilesList.value.size
                                viewModelScope.launch {
                                    repository.gitCommit(settings.value.activeProjectId, textStr)
                                }
                                results.add(CommandLine("[$subCmd ${ (1000..9999).random() }] $textStr", isSuccess = true))
                                results.add(CommandLine(" $size files changed, staged files integrated successfully."))
                            }
                        }
                    }
                    "log" -> {
                        val historyArr = activeProject.value?.gitHistoryJson ?: "[]"
                        val jsonArr = JSONArray(historyArr)
                        if (jsonArr.length() == 0) {
                            results.add(CommandLine("No commits registered on branch yet."))
                        } else {
                            for (x in 0 until jsonArr.length()) {
                                val item = jsonArr.getJSONObject(jsonArr.length() - 1 - x) // reversed order
                                results.add(CommandLine("commit ${item.getString("id")}", isSuccess = true))
                                results.add(CommandLine("Author: ${item.getString("author")}"))
                                val date = SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.US).format(Date(item.getLong("timestamp")))
                                results.add(CommandLine("Date:   $date"))
                                results.add(CommandLine("\n    ${item.getString("message")}\n"))
                            }
                        }
                    }
                    else -> results.add(CommandLine("git: sub-command '$subCmd' option is not recognized. Try git status, git add, git commit", isError = true))
                }
            }
            else -> {
                results.add(CommandLine("Command '$command' not found on system path environment.", isError = true))
                results.add(CommandLine("Type 'help' to review console operations."))
            }
        }
        terminalHistory.value = results
        terminalInput.value = ""
    }
}
