package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.EditorSettings
import com.example.data.database.InstalledExtension
import com.example.data.database.WorkspaceFile
import com.example.data.database.WorkspaceProject
import com.example.data.repository.DiffLine
import com.example.data.repository.DiffType
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.CommandLine
import com.example.ui.viewmodel.VSCodeViewModel
import kotlinx.coroutines.launch

// --- Dynamic Theme Schemes ---
data class IDETheme(
    val sidebarBg: Color,
    val editorBg: Color,
    val activityBarBg: Color,
    val accentColor: Color,
    val textColor: Color,
    val keywordColor: Color,
    val stringColor: Color,
    val commentColor: Color,
    val numberColor: Color,
    val terminalBg: Color,
    val statusBarBg: Color
)

val ClassicDarkTheme = IDETheme(
    sidebarBg = Color(0xFF252526),
    editorBg = Color(0xFF1E1E1E),
    activityBarBg = Color(0xFF333333),
    accentColor = Color(0xFF007ACC),
    textColor = Color(0xFFCCCCCC),
    keywordColor = Color(0xFF569CD6),
    stringColor = Color(0xFFCE9178),
    commentColor = Color(0xFF6A9955),
    numberColor = Color(0xFFB5CEA8),
    terminalBg = Color(0xFF181818),
    statusBarBg = Color(0xFF007ACC)
)

val GitHubDarkTheme = IDETheme(
    sidebarBg = Color(0xFF161B22),
    editorBg = Color(0xFF0D1117),
    activityBarBg = Color(0xFF21262D),
    accentColor = Color(0xFF58A6FF),
    textColor = Color(0xFFC9D1D9),
    keywordColor = Color(0xFFFF7B72),
    stringColor = Color(0xFFA5D6FF),
    commentColor = Color(0xFF8B949E),
    numberColor = Color(0xFF79C0FF),
    terminalBg = Color(0xFF090C10),
    statusBarBg = Color(0xFF21262D)
)

val monokaiTheme = IDETheme(
    sidebarBg = Color(0xFF1E1E1E),
    editorBg = Color(0xFF272822),
    activityBarBg = Color(0xFF181915),
    accentColor = Color(0xFFF92672),
    textColor = Color(0xFFF8F8F2),
    keywordColor = Color(0xFFF92672),
    stringColor = Color(0xFFE6DB74),
    commentColor = Color(0xFF75715E),
    numberColor = Color(0xFFAE81FF),
    terminalBg = Color(0xFF1E1F1C),
    statusBarBg = Color(0xFF66D9EF)
)

val solarizedAmberTheme = IDETheme(
    sidebarBg = Color(0xFF002B36),
    editorBg = Color(0xFF073642),
    activityBarBg = Color(0xFF001F27),
    accentColor = Color(0xFFB58900),
    textColor = Color(0xFF93A1A1),
    keywordColor = Color(0xFF859900),
    stringColor = Color(0xFF2AA198),
    commentColor = Color(0xFF586E75),
    numberColor = Color(0xFFCB4B16),
    terminalBg = Color(0xFF00222B),
    statusBarBg = Color(0xFFB58900)
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VSCodeScreen(
    viewModel: VSCodeViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeSidebarTab.collectAsStateWithLifecycle()
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val extensions by viewModel.allExtensions.collectAsStateWithLifecycle()
    val projectFiles by viewModel.activeProjectFiles.collectAsStateWithLifecycle()
    val openTabs by viewModel.openTabs.collectAsStateWithLifecycle()
    val activePath by viewModel.activeFilePath.collectAsStateWithLifecycle()
    val activeFile by viewModel.activeFile.collectAsStateWithLifecycle()
    val liveContent by viewModel.liveEditorContent.collectAsStateWithLifecycle()
    val isUnsaved by viewModel.isUnsaved.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchPattern by viewModel.searchPattern.collectAsStateWithLifecycle()
    val replacePattern by viewModel.replacePattern.collectAsStateWithLifecycle()
    val activeProj by viewModel.activeProject.collectAsStateWithLifecycle()
    
    // Git State Flow
    val stagedFiles by viewModel.stagedFilesList.collectAsStateWithLifecycle()
    val modifiedFiles by viewModel.modifiedUnstagedFiles.collectAsStateWithLifecycle()
    val diffResult by viewModel.diffFileAndLineResults.collectAsStateWithLifecycle()

    // AI & Terminal
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val terminalHistory by viewModel.terminalHistory.collectAsStateWithLifecycle()
    val terminalInput by viewModel.terminalInput.collectAsStateWithLifecycle()

    // Screen State variables for show dialogue popups
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showCloneDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var showAiCompleteDialog by remember { mutableStateOf(false) }
    var isSidebarCollapsed by remember { mutableStateOf(false) }

    // Resolve current visual theme colors
    val theme = when (settings.themeName) {
        "GitHub Obsidian Dark" -> GitHubDarkTheme
        "Monokai Retro" -> monokaiTheme
        "Solarized Soft Amber" -> solarizedAmberTheme
        "Sleek Interface" -> ClassicDarkTheme
        "VS Code Slate Dark" -> ClassicDarkTheme
        else -> ClassicDarkTheme
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing // Prevent camera notch conflicts
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(theme.editorBg)
        ) {
            // Sleek Header Bar from Sleek Interface guidelines
            EditorHeaderBar(
                theme = theme,
                activePath = activePath,
                activeProjName = activeProj?.name ?: "No Workspace",
                onMenuClicked = { isSidebarCollapsed = !isSidebarCollapsed },
                onPlayClicked = {
                    viewModel.toggleTerminalPanel(true)
                    val path = activePath
                    if (path != null) {
                        if (path.endsWith(".py")) {
                            viewModel.runCommandLine("python $path")
                        } else {
                            viewModel.runCommandLine("node $path")
                        }
                    } else {
                        viewModel.runCommandLine("help")
                    }
                },
                onMoreClicked = {
                    viewModel.activeSidebarTab.value = VSCodeViewModel.SidebarTab.SETTINGS
                    isSidebarCollapsed = false
                }
            )

            // Main workspace content layout: consists of left activity column sidebar + collapsable project bar + central code view
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Back layer: Central Working Area (Tabs Row + Code Editor + Git Diff)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.editorBg)
                ) {
                    if (diffResult != null) {
                        // Display full Interactive Code Comparison Diff instead of classical editor
                        DiffViewerLayout(
                            diffResult = diffResult!!,
                            theme = theme,
                            onCloseDiff = { viewModel.closeDiffViewer() }
                        )
                    } else {
                        // Standard Tab Row
                        EditorTabsRow(
                            openTabs = openTabs,
                            activePath = activePath,
                            isUnsaved = isUnsaved,
                            theme = theme,
                            onTabSelected = { path -> 
                                val fileObj = projectFiles.firstOrNull { it.filePath == path }
                                if (fileObj != null) viewModel.openFile(path, fileObj.content)
                            },
                            onTabClose = { path -> viewModel.closeTab(path) }
                        )

                        // Code Editor frame
                        if (activePath != null && activeFile != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                val isMdPreviewEnabled = extensions.any { it.id == "markdown-preview" && it.isEnabled } && activePath!!.endsWith(".md")
                                val isHtmlLiveEnabled = extensions.any { it.id == "live-server" && it.isEnabled } && (activePath!!.endsWith(".html") || activePath!!.endsWith(".xml") || activePath!!.endsWith(".htm"))
                                
                                var editorViewMode by remember { mutableStateOf("edit") }
                                LaunchedEffect(activePath) {
                                    editorViewMode = "edit"
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    EditorToolbarActions(
                                        isUnsaved = isUnsaved,
                                        theme = theme,
                                        isMdPreviewEnabled = isMdPreviewEnabled,
                                        isHtmlLiveEnabled = isHtmlLiveEnabled,
                                        editorViewMode = editorViewMode,
                                        onViewModeChanged = { editorViewMode = it },
                                        onSaveClicked = { viewModel.saveCurrentFile() },
                                        onFormatClicked = { viewModel.saveCurrentFile() }, // save auto formats if prettier active
                                        onAiMagicClicked = { showAiCompleteDialog = true }
                                    )

                                    if (editorViewMode == "edit") {
                                        Column(modifier = Modifier.weight(1f)) {
                                            CodeTextBox(
                                                modifier = Modifier.weight(1f),
                                                liveContent = liveContent,
                                                settings = settings,
                                                theme = theme,
                                                onContentUpdated = { viewModel.updateLiveContent(it) }
                                            )

                                            // ⌨️ Touch Symbol Coding shortcut Ribbon (Crucial mobile utility tool)
                                            CodingKeyboardRibbon(
                                                onSymbolClicked = { symbol ->
                                                    viewModel.updateLiveContent(liveContent + symbol)
                                                },
                                                theme = theme
                                            )
                                        }
                                    } else {
                                        if (isMdPreviewEnabled) {
                                            MarkdownPreviewPane(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .background(theme.sidebarBg)
                                                    .padding(16.dp)
                                                    .verticalScroll(rememberScrollState()),
                                                markdownText = liveContent,
                                                theme = theme
                                            )
                                        } else if (isHtmlLiveEnabled) {
                                            LiveServerPreviewPane(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                htmlText = liveContent,
                                                theme = theme
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Friendly Empty State View
                            EditorEmptyState(
                                theme = theme,
                                onNewFileRequested = { showNewFileDialog = true },
                                onCloneRequested = { showCloneDialog = true }
                            )
                        }
                    }

                    // Bottom Drawer: Console terminal console outputs log
                    AnimatedVisibility(
                        visible = settings.terminalOpen,
                        enter = expandVertically(spring()),
                        exit = shrinkVertically(spring())
                    ) {
                        TerminalDrawerLayout(
                            theme = theme,
                            terminalHistory = terminalHistory,
                            terminalInput = terminalInput,
                            onInputChange = { viewModel.terminalInput.value = it },
                            onCommandSubmitted = { cmd -> viewModel.runCommandLine(cmd) },
                            onCloseTerminal = { viewModel.toggleTerminalPanel(false) }
                        )
                    }
                }

                // Translucent Backdrop Overlay (Dims Editor when Sidebar is open and lets you close by tapping anywhere outside)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isSidebarCollapsed,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isSidebarCollapsed = true
                            }
                    )
                }

                // Sliding Floating Sidebar Panel (Overlays smoothly on top)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isSidebarCollapsed,
                    enter = slideInHorizontally { -it } + fadeIn(),
                    exit = slideOutHorizontally { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(260.dp)
                            .shadow(16.dp, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                            .background(theme.sidebarBg)
                    ) {
                        SidebarPanel(
                            activeTab = activeTab,
                            theme = theme,
                            projects = projects,
                            activeProj = activeProj,
                            projectFiles = projectFiles,
                            activePath = activePath,
                            searchPattern = searchPattern,
                            replacePattern = replacePattern,
                            searchResults = searchResults,
                            stagedFiles = stagedFiles,
                            modifiedFiles = modifiedFiles,
                            extensions = extensions,
                            chatHistory = chatHistory,
                            isAiLoading = isAiLoading,
                            onProjectSelected = { id -> viewModel.selectActiveProject(id) },
                            onFileSelected = { file -> 
                                viewModel.openFile(file.filePath, file.content)
                                isSidebarCollapsed = true // Automatically close sidebar to give a clean screen
                            },
                            onNewProjectRequested = { showNewProjectDialog = true },
                            onNewFileRequested = { showNewFileDialog = true },
                            onDeleteFileRequested = { path -> viewModel.deleteFileInProject(path) },
                            onSearchChange = { viewModel.searchPattern.value = it },
                            onReplaceChange = { viewModel.replacePattern.value = it },
                            onReplaceAllRequested = { viewModel.runGlobalReplace() },
                            onStageRequested = { path -> viewModel.stageFile(path) },
                            onUnstageRequested = { path -> viewModel.unstageFile(path) },
                            onStageAllRequested = { viewModel.stageAllChanges() },
                            onGitCommitRequested = { msg -> viewModel.submitCommit(msg) },
                            onShowDiffRequested = { file -> viewModel.showDiffViewer(file) },
                            onGitCloneRequested = { showCloneDialog = true },
                            onGitBranchRequested = { showBranchDialog = true },
                            onExtensionToggle = { id, enabled -> viewModel.toggleExtensionIsEnabled(id, enabled) },
                            onAiPromptSubmitted = { prompt -> viewModel.sendAiPromptMessage(prompt) },
                            onDeleteProject = { id -> viewModel.deleteActiveProject(id) },
                            viewModel = viewModel
                        )
                    }
                }
            }

            // 4. Status Bar (Very bottom dark line details)
            StatusBar(
                theme = theme,
                activeProjName = activeProj?.name ?: "No Workspace",
                branchName = activeProj?.gitBranch ?: "no-git",
                isTerminalOpen = settings.terminalOpen,
                hasPrettier = extensions.any { it.id == "prettier-formatter" && it.isEnabled },
                onTerminalToggleRequested = { viewModel.toggleTerminalPanel(!settings.terminalOpen) }
            )
        }
    }

    // --- Dialouge Forms & Popups ---

    // New Project Dialog
    if (showNewProjectDialog) {
        var pName by remember { mutableStateOf("") }
        var pDesc by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showNewProjectDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.sidebarBg),
                border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Create New Code Workspace", fontWeight = FontWeight.Bold, color = theme.textColor, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pName,
                        onValueChange = { pName = it },
                        label = { Text("Workspace Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pDesc,
                        onValueChange = { pDesc = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.align(Alignment.End)) {
                        TextButton(onClick = { showNewProjectDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (pName.isNotBlank()) {
                                    viewModel.createProject(pName, pDesc)
                                    showNewProjectDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        var fPath by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showNewFileDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.sidebarBg),
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("New File", fontWeight = FontWeight.Bold, color = theme.textColor, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Enter relative file path (e.g. index.html, main.py, src/script.js)", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fPath,
                        onValueChange = { fPath = it },
                        placeholder = { Text("App.js") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.align(Alignment.End)) {
                        TextButton(onClick = { showNewFileDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (fPath.isNotBlank()) {
                                    viewModel.createNewFileInProject(fPath)
                                    showNewFileDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                        ) {
                            Text("Create File")
                        }
                    }
                }
            }
        }
    }

    // Git Clone Dialog
    if (showCloneDialog) {
        var cloneUrl by remember { mutableStateOf("https://github.com/facebook/react.git") }
        Dialog(onDismissRequest = { showCloneDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.sidebarBg),
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = theme.accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Git Clone Remote Repo", fontWeight = FontWeight.Bold, color = theme.textColor, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Simulate downloading a complete remote git directory workspace into your mobile environment.", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cloneUrl,
                        onValueChange = { cloneUrl = it },
                        label = { Text("Remote Git Endpoint") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.align(Alignment.End)) {
                        TextButton(onClick = { showCloneDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (cloneUrl.isNotBlank()) {
                                    viewModel.triggerGitClone(cloneUrl)
                                    showCloneDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                        ) {
                            Text("Clone")
                        }
                    }
                }
            }
        }
    }

    // Git Branch creation Dialog
    if (showBranchDialog) {
        var newBName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showBranchDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.sidebarBg),
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Create Git Branch", fontWeight = FontWeight.Bold, color = theme.textColor, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newBName,
                        onValueChange = { newBName = it },
                        label = { Text("Branch Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.align(Alignment.End)) {
                        TextButton(onClick = { showBranchDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newBName.isNotBlank()) {
                                    viewModel.switchGitBranch(newBName)
                                    showBranchDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                        ) {
                            Text("Switch Branch")
                        }
                    }
                }
            }
        }
    }

    // AI Generation complete prompt dialog
    if (showAiCompleteDialog) {
        var aiCommandText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAiCompleteDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.sidebarBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, theme.accentColor.copy(0.4f)),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SmartToy, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Gemini AI Code Customizer", fontWeight = FontWeight.SemiBold, color = theme.textColor, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Describe modifications to apply automatically directly into active file tab ${activePath ?: ""}:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = aiCommandText,
                        onValueChange = { aiCommandText = it },
                        placeholder = { Text("e.g., 'Add a countdown function to sparkle-btn click listener' or 'Fix any potential bugs'") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.align(Alignment.End)) {
                        TextButton(onClick = { showAiCompleteDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (aiCommandText.isNotBlank()) {
                                    viewModel.applyAiCodeEnhancement(aiCommandText)
                                    showAiCompleteDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                        ) {
                            Text("Ask Gemini")
                        }
                    }
                }
            }
        }
    }
}

// --- Activity Bar Layout (Left Narrow Rail Icons) ---
@Composable
fun ActivityBar(
    activeTab: VSCodeViewModel.SidebarTab,
    stagedAndModifiedCount: Int,
    onTabSelected: (VSCodeViewModel.SidebarTab) -> Unit,
    theme: IDETheme
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(52.dp)
            .background(theme.activityBarBg)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo Indicator
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(theme.accentColor)
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Code, contentDescription = "VS Code Mobile Logo", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            HorizontalDivider(color = Color.White.copy(0.1f), thickness = 1.dp, modifier = Modifier.fillMaxWidth(0.6f))

            // File Explorer Icon
            ActivityItem(
                icon = Icons.Filled.Folder,
                description = "File Explorer",
                isSelected = activeTab == VSCodeViewModel.SidebarTab.EXPLORER,
                accentColor = theme.accentColor,
                onClick = { onTabSelected(VSCodeViewModel.SidebarTab.EXPLORER) }
            )

            // Search Icon
            ActivityItem(
                icon = Icons.Filled.Search,
                description = "Search & Replace",
                isSelected = activeTab == VSCodeViewModel.SidebarTab.SEARCH,
                accentColor = theme.accentColor,
                onClick = { onTabSelected(VSCodeViewModel.SidebarTab.SEARCH) }
            )

            // Source Control (Git) WITH notification badge
            Box {
                ActivityItem(
                    icon = Icons.AutoMirrored.Filled.CallSplit,
                    description = "Git Source Control",
                    isSelected = activeTab == VSCodeViewModel.SidebarTab.GIT,
                    accentColor = theme.accentColor,
                    onClick = { onTabSelected(VSCodeViewModel.SidebarTab.GIT) }
                )
                if (stagedAndModifiedCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 10.dp, y = ((-2).dp))
                            .align(Alignment.TopEnd)
                            .background(Color.Red, CircleShape)
                            .size(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$stagedAndModifiedCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Extensions block Icon
            ActivityItem(
                icon = Icons.Filled.Widgets,
                description = "Extensions Marketplace",
                isSelected = activeTab == VSCodeViewModel.SidebarTab.MARKETPLACE,
                accentColor = theme.accentColor,
                onClick = { onTabSelected(VSCodeViewModel.SidebarTab.MARKETPLACE) }
            )

            // Gemini chatbot Icon
            ActivityItem(
                icon = Icons.Filled.SmartToy,
                description = "Gemini Copilot Assistant",
                isSelected = activeTab == VSCodeViewModel.SidebarTab.AI_CHAT,
                accentColor = theme.accentColor,
                onClick = { onTabSelected(VSCodeViewModel.SidebarTab.AI_CHAT) }
            )
        }

        // Settings Selector (Placed safely at bottom)
        ActivityItem(
            icon = Icons.Filled.Settings,
            description = "Settings Preferences",
            isSelected = activeTab == VSCodeViewModel.SidebarTab.SETTINGS,
            accentColor = theme.accentColor,
            onClick = { onTabSelected(VSCodeViewModel.SidebarTab.SETTINGS) }
        )
    }
}

@Composable
fun ActivityItem(
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection left pill highlighter
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (isSelected) accentColor else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

// --- Side Drawer Panel (Toggle Options) ---
@Composable
fun SidebarPanel(
    activeTab: VSCodeViewModel.SidebarTab,
    theme: IDETheme,
    projects: List<WorkspaceProject>,
    activeProj: WorkspaceProject?,
    projectFiles: List<WorkspaceFile>,
    activePath: String?,
    searchPattern: String,
    replacePattern: String,
    searchResults: List<VSCodeViewModel.SearchMatch>,
    stagedFiles: List<String>,
    modifiedFiles: List<WorkspaceFile>,
    extensions: List<InstalledExtension>,
    chatHistory: List<ChatMessage>,
    isAiLoading: Boolean,
    onProjectSelected: (Int) -> Unit,
    onFileSelected: (WorkspaceFile) -> Unit,
    onNewProjectRequested: () -> Unit,
    onNewFileRequested: () -> Unit,
    onDeleteFileRequested: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onReplaceAllRequested: () -> Unit,
    onStageRequested: (String) -> Unit,
    onUnstageRequested: (String) -> Unit,
    onStageAllRequested: () -> Unit,
    onGitCommitRequested: (String) -> Unit,
    onShowDiffRequested: (WorkspaceFile) -> Unit,
    onGitCloneRequested: () -> Unit,
    onGitBranchRequested: () -> Unit,
    onExtensionToggle: (String, Boolean) -> Unit,
    onAiPromptSubmitted: (String) -> Unit,
    onDeleteProject: (Int) -> Unit,
    viewModel: VSCodeViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(theme.sidebarBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161616))
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Pair(VSCodeViewModel.SidebarTab.EXPLORER, Icons.Filled.Folder),
                Pair(VSCodeViewModel.SidebarTab.SEARCH, Icons.Filled.Search),
                Pair(VSCodeViewModel.SidebarTab.GIT, Icons.AutoMirrored.Filled.CallSplit),
                Pair(VSCodeViewModel.SidebarTab.MARKETPLACE, Icons.Filled.Widgets),
                Pair(VSCodeViewModel.SidebarTab.AI_CHAT, Icons.Filled.SmartToy),
                Pair(VSCodeViewModel.SidebarTab.SETTINGS, Icons.Filled.Settings)
            )

            tabs.forEach { (tab, icon) ->
                val isSelected = activeTab == tab
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { viewModel.activeSidebarTab.value = tab },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) theme.accentColor.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.name,
                            tint = if (isSelected) theme.accentColor else theme.textColor.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (tab == VSCodeViewModel.SidebarTab.GIT) {
                        val stagedAndModifiedCount = stagedFiles.size + modifiedFiles.size
                        if (stagedAndModifiedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .background(Color.Red, CircleShape)
                                    .size(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$stagedAndModifiedCount",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(0.08f), thickness = 1.dp)

        // Header Category Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (activeTab) {
                    VSCodeViewModel.SidebarTab.EXPLORER -> "EXPLORER"
                    VSCodeViewModel.SidebarTab.SEARCH -> "SEARCH"
                    VSCodeViewModel.SidebarTab.GIT -> "SOURCE CONTROL"
                    VSCodeViewModel.SidebarTab.MARKETPLACE -> "EXTENSIONS"
                    VSCodeViewModel.SidebarTab.AI_CHAT -> "GEMINI AI CHAT"
                    VSCodeViewModel.SidebarTab.SETTINGS -> "PREFERENCES"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textColor.copy(0.7f),
                letterSpacing = 1.2.sp
            )
            
            // Context header Quick commands
            if (activeTab == VSCodeViewModel.SidebarTab.EXPLORER) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = "New File",
                        tint = theme.textColor.copy(0.6f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onNewFileRequested() }
                    )
                    Icon(
                        Icons.Filled.CreateNewFolder,
                        contentDescription = "New Project",
                        tint = theme.textColor.copy(0.6f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onNewProjectRequested() }
                    )
                }
            } else if (activeTab == VSCodeViewModel.SidebarTab.GIT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Filled.CloudDownload,
                        contentDescription = "Git Clone",
                        tint = theme.textColor.copy(0.6f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onGitCloneRequested() }
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = "Switch Branch",
                        tint = theme.textColor.copy(0.6f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onGitBranchRequested() }
                    )
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(0.08f), thickness = 1.dp)

        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                VSCodeViewModel.SidebarTab.EXPLORER -> {
                    ExplorerSideBar(
                        theme = theme,
                        projects = projects,
                        activeProj = activeProj,
                        projectFiles = projectFiles,
                        activePath = activePath,
                        onProjectSelected = onProjectSelected,
                        onFileSelected = onFileSelected,
                        onDeleteFileRequested = onDeleteFileRequested,
                        onDeleteProject = onDeleteProject
                    )
                }
                VSCodeViewModel.SidebarTab.SEARCH -> {
                    SearchSideBar(
                        theme = theme,
                        searchPattern = searchPattern,
                        replacePattern = replacePattern,
                        searchResults = searchResults,
                        onSearchChange = onSearchChange,
                        onReplaceChange = onReplaceChange,
                        onReplaceAllRequested = onReplaceAllRequested
                    )
                }
                VSCodeViewModel.SidebarTab.GIT -> {
                    GitSideBar(
                        theme = theme,
                        activeBranch = activeProj?.gitBranch ?: "main",
                        stagedFiles = stagedFiles,
                        modifiedFiles = modifiedFiles,
                        onStageRequested = onStageRequested,
                        onUnstageRequested = onUnstageRequested,
                        onStageAllRequested = onStageAllRequested,
                        onCommitRequested = onGitCommitRequested,
                        onShowDiff = onShowDiffRequested,
                        onCloneRequested = onGitCloneRequested
                    )
                }
                VSCodeViewModel.SidebarTab.MARKETPLACE -> {
                    ExtensionsSideBar(
                        theme = theme,
                        extensions = extensions,
                        onExtensionToggle = onExtensionToggle
                    )
                }
                VSCodeViewModel.SidebarTab.AI_CHAT -> {
                    AiChatSideBar(
                        theme = theme,
                        chatHistory = chatHistory,
                        isAiLoading = isAiLoading,
                        onAiPromptSubmitted = onAiPromptSubmitted
                    )
                }
                VSCodeViewModel.SidebarTab.SETTINGS -> {
                    SettingsSideBar(
                        theme = theme,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// Sub explorer bar layout
@Composable
fun ExplorerSideBar(
    theme: IDETheme,
    projects: List<WorkspaceProject>,
    activeProj: WorkspaceProject?,
    projectFiles: List<WorkspaceFile>,
    activePath: String?,
    onProjectSelected: (Int) -> Unit,
    onFileSelected: (WorkspaceFile) -> Unit,
    onDeleteFileRequested: (String) -> Unit,
    onDeleteProject: (Int) -> Unit
) {
    var expandedProjectDropdown by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        Spacer(modifier = Modifier.height(10.dp))
        
        // Active workspace selection button switcher
        Text("WORKSPACE PROJECT", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = theme.accentColor)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.04f), RoundedCornerShape(4.dp))
                .clickable { expandedProjectDropdown = !expandedProjectDropdown }
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeProj?.name ?: "No Workspace Loaded",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (activeProj != null && activeProj.gitRemoteUrl.isNotEmpty()) {
                    Text(
                        activeProj.gitRemoteUrl,
                        fontSize = 9.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(16.dp))

            DropdownMenu(
                expanded = expandedProjectDropdown,
                onDismissRequest = { expandedProjectDropdown = false },
                modifier = Modifier.background(theme.sidebarBg).border(1.dp, Color.White.copy(0.1f))
            ) {
                projects.forEach { p ->
                    DropdownMenuItem(
                        text = {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p.name, color = theme.textColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                if (projects.size > 1) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete Project",
                                        tint = Color.Red.copy(0.5f),
                                        modifier = Modifier.size(16.dp).clickable {
                                            onDeleteProject(p.id)
                                        }
                                    )
                                }
                            }
                        },
                        onClick = {
                            onProjectSelected(p.id)
                            expandedProjectDropdown = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Files Tree hierarchy List
        Text("FILES", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = theme.accentColor)
        Spacer(modifier = Modifier.height(6.dp))
        
        if (projectFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                Text("No files. Click '+' to add active scripts.", fontStyle = FontStyle.Italic, color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(projectFiles) { file ->
                    val isSelected = file.filePath == activePath
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isSelected) theme.accentColor.copy(0.18f) else Color.Transparent)
                            .clickable { onFileSelected(file) }
                            .padding(vertical = 5.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Deduce beautiful icon based on file type Extension
                            val iconPair = when {
                                file.filePath.endsWith(".html") -> Pair(Icons.Filled.Html, Color(0xFFE34F26))
                                file.filePath.endsWith(".css") -> Pair(Icons.Filled.Css, Color(0xFF1572B6))
                                file.filePath.endsWith(".js") -> Pair(Icons.Filled.Javascript, Color(0xFFF7DF1E))
                                file.filePath.endsWith(".py") -> Pair(Icons.Filled.Code, Color(0xFF3776AB))
                                file.filePath.endsWith(".md") -> Pair(Icons.Filled.Book, Color(0xFF58A6FF))
                                else -> Pair(Icons.AutoMirrored.Filled.Article, Color.LightGray)
                            }

                            Icon(imageVector = iconPair.first, contentDescription = null, tint = iconPair.second, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = file.filePath,
                                color = if (isSelected) theme.textColor else theme.textColor.copy(0.81f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Close delete file option
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Delete local file",
                            tint = Color.Gray.copy(0.4f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onDeleteFileRequested(file.filePath) }
                        )
                    }
                }
            }
        }
    }
}

// Side Search & Replace Column Component
@Composable
fun SearchSideBar(
    theme: IDETheme,
    searchPattern: String,
    replacePattern: String,
    searchResults: List<VSCodeViewModel.SearchMatch>,
    onSearchChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onReplaceAllRequested: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = searchPattern,
            onValueChange = onSearchChange,
            placeholder = { Text("Search patterns", fontSize = 11.sp, color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textColor,
                unfocusedTextColor = theme.textColor,
                focusedContainerColor = Color.White.copy(0.02f)
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = replacePattern,
                onValueChange = onReplaceChange,
                placeholder = { Text("Replace text", fontSize = 11.sp, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.textColor,
                    unfocusedTextColor = theme.textColor,
                    focusedContainerColor = Color.White.copy(0.02f)
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onReplaceAllRequested,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.accentColor)
                    .size(38.dp)
            ) {
                Icon(Icons.Filled.FindReplace, contentDescription = "Replace All", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        Text("${searchResults.size} MATCHED OCCURRENCES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(searchResults) { match ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.White.copy(0.03f), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(match.filePath, fontSize = 10.sp, color = theme.accentColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Ln ${match.lineNumber}: ${match.lineContent.trim()}", fontSize = 11.sp, color = theme.textColor.copy(0.8f), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// Side Git Commit management panel
@Composable
fun GitSideBar(
    theme: IDETheme,
    activeBranch: String,
    stagedFiles: List<String>,
    modifiedFiles: List<WorkspaceFile>,
    onStageRequested: (String) -> Unit,
    onUnstageRequested: (String) -> Unit,
    onStageAllRequested: () -> Unit,
    onCommitRequested: (String) -> Unit,
    onShowDiff: (WorkspaceFile) -> Unit,
    onCloneRequested: () -> Unit
) {
    var commitMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        // Top Remote triggers info
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("branch: $activeBranch", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = theme.textColor)
            }
            Text(
                "Remote clone",
                fontSize = 10.sp,
                color = theme.accentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCloneRequested() }
            )
        }

        HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(vertical = 4.dp))

        // Commit message input row
        OutlinedTextField(
            value = commitMessage,
            onValueChange = { commitMessage = it },
            placeholder = { Text("Commit messages (Ctrl+Enter)", fontSize = 11.sp, color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                if (commitMessage.isNotBlank()) {
                    onCommitRequested(commitMessage)
                    commitMessage = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = stagedFiles.isNotEmpty()
        ) {
            Icon(Icons.Filled.Commit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Commit Git Changes (${stagedFiles.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(14.dp))

        // Staged Files lists block
        Text("STAGED CHANGES (${stagedFiles.size})", fontSize = 10.sp, color = Color.Green.copy(0.8f), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        
        if (stagedFiles.isEmpty()) {
            Text("No staged content. Stage with '+' below.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 6.dp))
        } else {
            Column {
                stagedFiles.forEach { path ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(path, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = theme.textColor.copy(0.9f), modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("A", fontSize = 10.sp, color = Color.Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Unstage File",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onUnstageRequested(path) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Modified file list row (un-staged changes)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("MODIFIED UNSTAGED (${modifiedFiles.size})", fontSize = 10.sp, color = Color.Yellow.copy(0.8f), fontWeight = FontWeight.Bold)
            if (modifiedFiles.isNotEmpty()) {
                Text(
                    "Stage All",
                    color = theme.accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onStageAllRequested() }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        if (modifiedFiles.isEmpty()) {
            Text("Nothing uncommitted, working tree is clean.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 6.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(modifiedFiles) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowDiff(file) } // Open diff instantly
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.filePath, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = theme.textColor)
                            Text("Line Diff comparison available", fontSize = 9.sp, color = theme.accentColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("M", fontSize = 10.sp, color = Color.Yellow, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Stage File",
                                tint = theme.accentColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onStageRequested(file.filePath) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Side Extensions marketplace display
@Composable
fun ExtensionsSideBar(
    theme: IDETheme,
    extensions: List<InstalledExtension>,
    onExtensionToggle: (String, Boolean) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val filtered = extensions.filter {
        it.name.contains(searchText, ignoreCase = true) || it.description.contains(searchText, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search Extensions Marketplace", fontSize = 11.sp, color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("RECOMMENDED GALAXY (${filtered.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(filtered) { ext ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f)),
                    border = BorderStroke(1.dp, if (ext.isEnabled) theme.accentColor.copy(0.24f) else Color.White.copy(0.06f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = when (ext.iconName) {
                                        "smart_toy" -> Icons.Filled.SmartToy
                                        "brush" -> Icons.Filled.Brush
                                        "history" -> Icons.Filled.History
                                        "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
                                        "palette" -> Icons.Filled.Palette
                                        "sensors" -> Icons.Filled.Sensors
                                        "sync" -> Icons.Filled.Sync
                                        "border_color" -> Icons.Filled.BorderColor
                                        "label" -> Icons.AutoMirrored.Filled.Label
                                        "folder" -> Icons.Filled.Folder
                                        "html" -> Icons.Filled.Code
                                        "image" -> Icons.Filled.Image
                                        else -> Icons.Filled.Extension
                                    },
                                    contentDescription = null,
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(ext.name, fontWeight = FontWeight.Bold, color = theme.textColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(ext.publisher, fontSize = 9.sp, color = Color.Gray)
                                }
                            }

                            // Enable toggle switch
                            Switch(
                                checked = ext.isEnabled,
                                onCheckedChange = { onExtensionToggle(ext.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = theme.accentColor,
                                    uncheckedThumbColor = Color.DarkGray,
                                    uncheckedTrackColor = Color.LightGray.copy(0.2f)
                                ),
                                modifier = Modifier.scale(0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ext.description, fontSize = 10.sp, color = theme.textColor.copy(0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("⭐ ${ext.rating}  •  ${ext.downloads} DLs", fontSize = 9.sp, color = Color.Yellow.copy(0.8f))
                            Text(ext.version, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// Custom Extension scale helper replaced by native Compose scale modifier

// Primary Gemini Assist chat section inside sidebar
@Composable
fun AiChatSideBar(
    theme: IDETheme,
    chatHistory: List<ChatMessage>,
    isAiLoading: Boolean,
    onAiPromptSubmitted: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll automatically on append
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(chatHistory) { msg ->
                val isUsr = msg.sender == "user"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUsr) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isUsr) theme.accentColor.copy(0.24f) else Color.White.copy(0.04f))
                            .border(1.dp, if (isUsr) theme.accentColor.copy(0.4f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                if (isUsr) "YOU" else "GEMINI COPILOT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUsr) theme.accentColor else Color.Yellow.copy(0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(msg.text, fontSize = 11.sp, color = theme.textColor)
                        }
                    }
                }
            }

            if (isAiLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = theme.accentColor, strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gemini computing response...", fontSize = 10.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask Copilot...", fontSize = 11.sp, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = theme.textColor, unfocusedTextColor = theme.textColor),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank()) {
                        onAiPromptSubmitted(textInput)
                        textInput = ""
                    }
                }),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onAiPromptSubmitted(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.accentColor)
                    .size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Side Settings editor variables preferences config
@Composable
fun SettingsSideBar(
    theme: IDETheme,
    viewModel: VSCodeViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Theme selector selector
        Column {
            Text("EDITOR THEME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            listOf("Sleek Interface", "GitHub Obsidian Dark", "Monokai Retro", "Solarized Soft Amber").forEach { name ->
                val isSelected = settings.themeName == name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) theme.accentColor.copy(0.15f) else Color.Transparent)
                        .clickable { viewModel.updateTheme(name) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { viewModel.updateTheme(name) }, colors = RadioButtonDefaults.colors(selectedColor = theme.accentColor))
                    Text(name, fontSize = 12.sp, color = theme.textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(0.08f))

        // Font Size adjust bar slider
        Column {
            Text("FONT SIZE (${settings.fontSize.toInt()}sp)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { viewModel.updateFontSize(settings.fontSize - 1f) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = theme.textColor)
                }
                Slider(
                    value = settings.fontSize,
                    onValueChange = { viewModel.updateFontSize(it) },
                    valueRange = 10f..24f,
                    colors = SliderDefaults.colors(thumbColor = theme.accentColor, activeTrackColor = theme.accentColor),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.updateFontSize(settings.fontSize + 1f) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = theme.textColor)
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(0.08f))

        // Toggle properties
        Column {
            Text("WORD WRAPPING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Wrap long code lines", fontSize = 11.sp, color = theme.textColor.copy(0.8f))
                Switch(
                    checked = settings.isWordWrapEnabled,
                    onCheckedChange = { viewModel.toggleWordWrap(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = theme.accentColor)
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(0.08f))

        // Wallpaper Selector UI Block
        Column {
            Text("EDITOR WALLPAPER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            listOf("Classic None", "Technical Grid", "Galactic Nebula", "Sleek Cyber").forEach { name ->
                val isSelected = settings.wallpaperName == name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) theme.accentColor.copy(0.15f) else Color.Transparent)
                        .clickable { viewModel.updateWallpaper(name) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.updateWallpaper(name) },
                        colors = RadioButtonDefaults.colors(selectedColor = theme.accentColor)
                    )
                    Text(name, fontSize = 12.sp, color = theme.textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

// --- Horizontal Tab elements layout ---
@Composable
fun EditorTabsRow(
    openTabs: List<String>,
    activePath: String?,
    isUnsaved: Boolean,
    theme: IDETheme,
    onTabSelected: (String) -> Unit,
    onTabClose: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(theme.activityBarBg)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        openTabs.forEach { path ->
            val isSelected = path == activePath
            
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(if (isSelected) theme.editorBg else Color.Transparent)
                    .clickable { onTabSelected(path) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Formatting bullet indicator for unsaved text edits
                if (isSelected && isUnsaved) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Yellow, CircleShape).padding(end = 4.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                
                Text(
                    text = path.substringAfterLast("/"),
                    color = if (isSelected) theme.textColor else theme.textColor.copy(0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close tab",
                    tint = if (isSelected) theme.textColor.copy(0.6f) else Color.Gray,
                    modifier = Modifier
                        .size(12.dp)
                        .clickable { onTabClose(path) }
                )
            }
            VerticalDivider(color = Color.White.copy(0.08f), modifier = Modifier.fillMaxHeight().width(1.dp))
        }
    }
}

// Editor header Action toolbar
@Composable
fun EditorToolbarActions(
    isUnsaved: Boolean,
    theme: IDETheme,
    isMdPreviewEnabled: Boolean,
    isHtmlLiveEnabled: Boolean,
    editorViewMode: String,
    onViewModeChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onFormatClicked: () -> Unit,
    onAiMagicClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.editorBg.copy(0.97f))
            .padding(vertical = 4.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isUnsaved) Color.Yellow else Color.Green, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isUnsaved) "Unsaved Edit changes" else "Automatic formatted sync",
                fontSize = 10.sp,
                color = theme.textColor.copy(0.6f)
            )
            
            if (isMdPreviewEnabled || isHtmlLiveEnabled) {
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(0.06f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("edit" to "Code", "preview" to "Preview").forEach { (mode, label) ->
                        val active = editorViewMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (active) theme.accentColor else Color.Transparent)
                                .clickable { onViewModeChanged(mode) }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else theme.textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Save Button
            IconButton(onClick = onSaveClicked, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Save, contentDescription = "Save code content", tint = if (isUnsaved) theme.accentColor else Color.Gray, modifier = Modifier.size(16.dp))
            }
            // Format Button
            IconButton(onClick = onFormatClicked, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Brush, contentDescription = "Format file", tint = theme.accentColor, modifier = Modifier.size(16.dp))
            }
            // Ask AI Button
            IconButton(onClick = onAiMagicClicked, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "Gemini Magic Complete", tint = Color.Yellow, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// Code edit Textfield box with margin index
@Composable
fun CodeTextBox(
    modifier: Modifier = Modifier,
    liveContent: String,
    settings: EditorSettings,
    theme: IDETheme,
    onContentUpdated: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val totalLines = liveContent.lines().size.coerceAtLeast(1)

    // Robust VisualTransformation to apply high-style syntax highlighting dynamically 
    // to a standard, non-jumping Compose String-based BasicTextField
    val syntaxHighlightingTransformation = remember(theme, settings) {
        VisualTransformation { text ->
            val rawText = text.text
            val highlighted = buildAnnotatedString {
                withStyle(style = SpanStyle(color = theme.textColor, fontFamily = FontFamily.Monospace, fontSize = settings.fontSize.sp)) {
                    append(rawText)
                }

                // Keyword highlighting
                val regex = "\\b(const|let|var|function|class|import|return|from|as|if|else|for|while|do|def|fun|val|package|try|catch|true|false|null)\\b".toRegex()
                for (m in regex.findAll(rawText)) {
                    addStyle(
                        style = SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold),
                        start = m.range.first,
                        end = m.range.last + 1
                    )
                }

                // HTML CSS tags highlight matcher
                val htmlRegex = "</?[a-zA-Z0-9]+>?".toRegex()
                for (hm in htmlRegex.findAll(rawText)) {
                    addStyle(
                        style = SpanStyle(color = theme.accentColor),
                        start = hm.range.first,
                        end = hm.range.last + 1
                    )
                }

                // Numbers matcher
                val numRegex = "\\b[0-9]+\\b".toRegex()
                for (nm in numRegex.findAll(rawText)) {
                    addStyle(
                        style = SpanStyle(color = theme.numberColor),
                        start = nm.range.first,
                        end = nm.range.last + 1
                    )
                }

                // Quotation string matcher
                val quoteRegex = "(\"[^\"]*\")|('[^']*')".toRegex()
                for (qm in quoteRegex.findAll(rawText)) {
                    addStyle(
                        style = SpanStyle(color = theme.stringColor),
                        start = qm.range.first,
                        end = qm.range.last + 1
                    )
                }

                // Components and custom PascalCase types/identifiers matcher (yellow Highlight)
                val componentRegex = "\\b([A-Z][a-zA-Z0-9_]*)\\b".toRegex()
                for (cm in componentRegex.findAll(rawText)) {
                    addStyle(
                        style = SpanStyle(color = Color(0xFFDCDCAA)),
                        start = cm.range.first,
                        end = cm.range.last + 1
                    )
                }
            }
            TransformedText(highlighted, OffsetMapping.Identity)
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(theme.editorBg)
            .drawBehind {
                val wp = settings.wallpaperName
                if (wp == "Technical Grid") {
                    // Draw architectural blueprint structural grid lines
                    val gridSize = 45.dp.toPx()
                    val gridColor = Color(0xFF2E93FF).copy(0.05f)
                    
                    // Vertical lines
                    var x = 0f
                    while (x < size.width) {
                        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                        x += gridSize
                    }
                    // Horizontal lines
                    var y = 0f
                    while (y < size.height) {
                        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                        y += gridSize
                    }
                    // Highlight intersection points with blueprint coordinates
                    x = 0f
                    while (x < size.width) {
                        y = 0f
                        while (y < size.height) {
                            drawCircle(color = Color(0xFF2E93FF).copy(0.12f), radius = 2f, center = Offset(x, y))
                            y += gridSize
                        }
                        x += gridSize
                    }
                } else if (wp == "Galactic Nebula") {
                    // Draw space theme: rich glowing purple and cyan gas nebula dusts
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF9C4DFC).copy(0.18f), Color.Transparent),
                            center = Offset(size.width * 0.75f, size.height * 0.3f),
                            radius = size.width * 0.7f
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00E5FF).copy(0.14f), Color.Transparent),
                            center = Offset(size.width * 0.25f, size.height * 0.7f),
                            radius = size.width * 0.75f
                        )
                    )
                    // High-altitude twinkling points
                    val stars = listOf(
                        Offset(size.width * 0.15f, 150f),
                        Offset(size.width * 0.45f, 400f),
                        Offset(size.width * 0.85f, 250f),
                        Offset(size.width * 0.35f, 750f),
                        Offset(size.width * 0.75f, 1100f),
                        Offset(size.width * 0.60f, 600f)
                    )
                    for (star in stars) {
                        drawCircle(color = Color.White.copy(0.35f), radius = 1.8f, center = star)
                        drawCircle(color = Color(0xFF00E5FF).copy(0.12f), radius = 5.0f, center = star)
                    }
                } else if (wp == "Sleek Cyber") {
                    // Futuristic linear glow circuits
                    val cyberCyan = Color(0xFF00FFCC).copy(0.04f)
                    val cyberMagenta = Color(0xFFFF2E93).copy(0.03f)
                    
                    var y = 0f
                    val cyberStep = 50.dp.toPx()
                    while (y < size.height) {
                        drawLine(
                            brush = Brush.linearGradient(listOf(cyberCyan, cyberMagenta)),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5f
                        )
                        y += cyberStep
                    }
                    drawLine(color = Color(0xFF00FFCC).copy(0.07f), start = Offset(size.width * 0.85f, 0f), end = Offset(size.width * 0.85f, size.height), strokeWidth = 1.8f)
                    drawLine(color = Color(0xFFFF2E93).copy(0.06f), start = Offset(size.width * 0.85f, 300f), end = Offset(size.width * 0.98f, 380f), strokeWidth = 1.5f)
                }
            }
    ) {
        // Line counts vertical margins
        Column(
            modifier = Modifier
                .width(36.dp)
                .background(theme.sidebarBg.copy(0.4f))
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (idx in 1..totalLines) {
                Text(
                    text = "$idx  ",
                    color = theme.textColor.copy(0.3f),
                    fontSize = settings.fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    lineHeight = (settings.fontSize * 1.3).sp
                )
            }
        }

        // Horizontal slider containing edit code basic text field
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp, horizontal = 10.dp)
        ) {
            val interceptedOnValueChange: (String) -> Unit = { newVal ->
                var finalVal = newVal
                
                // 1. Auto Close Tag Feature
                if (newVal.length == liveContent.length + 1) {
                    val typedChar = newVal.lastOrNull()
                    if (typedChar == '>') {
                        val beforeTag = newVal.substring(0, newVal.length - 1)
                        val lastLess = beforeTag.lastIndexOf('<')
                        if (lastLess != -1 && lastLess < beforeTag.length) {
                            val candidate = beforeTag.substring(lastLess + 1)
                            if (!candidate.contains('>') && !candidate.contains('/') && candidate.isNotBlank()) {
                                val tagName = candidate.split(' ', '\n', '\t')[0]
                                if (tagName.matches("[a-zA-Z0-9]+".toRegex())) {
                                    finalVal = newVal + "</$tagName>"
                                }
                            }
                        }
                    }
                }
                
                // 2. HTML & CSS Snippets Skeletons
                if (finalVal.endsWith("html5 ")) {
                    finalVal = finalVal.substring(0, finalVal.length - 6) + 
                        "<!DOCTYPE html>\n<html>\n<head>\n  <title>Void Studio Page</title>\n</head>\n<body>\n  <h1>Welcome to Void Studio</h1>\n</body>\n</html>"
                } else if (finalVal.endsWith("flex-center ")) {
                    finalVal = finalVal.substring(0, finalVal.length - 12) + 
                        "display: flex;\njustify-content: center;\nalign-items: center;"
                } else if (finalVal.endsWith("div-snippet ")) {
                    finalVal = finalVal.substring(0, finalVal.length - 12) + 
                        "<div class=\"container\">\n  \n</div>"
                } else if (finalVal.endsWith("./ ")) {
                    finalVal = finalVal.substring(0, finalVal.length - 3) + "./src/assets/styles.css"
                }
                
                onContentUpdated(finalVal)
            }

            BasicTextField(
                value = liveContent,
                onValueChange = interceptedOnValueChange,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.fontSize.sp,
                    lineHeight = (settings.fontSize * 1.3).sp,
                    color = theme.textColor
                ),
                cursorBrush = SolidColor(theme.accentColor),
                visualTransformation = syntaxHighlightingTransformation,
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            )
        }
    }
}

// ⌨️ Custom coding bracket input keyboard ribbon helper
@Composable
fun CodingKeyboardRibbon(
    onSymbolClicked: (String) -> Unit,
    theme: IDETheme
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(theme.sidebarBg)
            .border(BorderStroke(1.dp, Color.White.copy(0.06f)))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick tools
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(theme.accentColor)
                .clickable { keyboardController?.hide() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.KeyboardHide, contentDescription = "Hide Keyboard", tint = Color.White, modifier = Modifier.size(16.dp))
        }

        val symbols = listOf(
            "  ", "{", "}", "[", "]", "(", ")", ";", "=", "+", "-", "*", "/", "<", ">", "\"", "'", "!", "?", "_", ":", "."
        )

        symbols.forEach { sym ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(0.06f))
                    .clickable { 
                        val insert = if (sym == "  ") "  " else sym
                        onSymbolClicked(insert) 
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (sym == "  ") "Tab" else sym,
                    color = theme.textColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Split Markdown Render Pane screen
@Composable
fun MarkdownPreviewPane(
    modifier: Modifier = Modifier,
    markdownText: String,
    theme: IDETheme
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("LIVE DOCUMENT VIEW Preview", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = theme.textColor.copy(0.5f))
        }

        val lines = markdownText.lines()
        lines.forEach { line ->
            when {
                line.trim().startsWith("# ") -> {
                    Text(
                        text = line.trim().substring(2),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.accentColor,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                line.trim().startsWith("## ") -> {
                    Text(
                        text = line.trim().substring(3),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textColor,
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }
                line.trim().startsWith("### ") -> {
                    Text(
                        text = line.trim().substring(4),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.textColor.copy(0.9f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
                        Text("• ", color = theme.accentColor, fontWeight = FontWeight.Black)
                        Text(line.trim().substring(2), fontSize = 11.sp, color = theme.textColor.copy(0.85f))
                    }
                }
                line.trim().startsWith("> ") -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color.White.copy(0.04f))
                            .border(BorderStroke(2.dp, theme.accentColor), RoundedCornerShape(2.dp))
                            .padding(8.dp)
                    ) {
                        Text(line.trim().substring(2), fontSize = 11.sp, color = theme.textColor.copy(0.81f), fontStyle = FontStyle.Italic)
                    }
                }
                else -> {
                    if (line.isNotBlank()) {
                        Text(line, fontSize = 11.sp, color = theme.textColor.copy(0.78f), modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// Live Server dynamic HTML App Preview screen
@Composable
fun LiveServerPreviewPane(
    modifier: Modifier = Modifier,
    htmlText: String,
    theme: IDETheme
) {
    Column(modifier = modifier) {
        // Live server simulated status bar
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.04f)).padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Spinning/pulsing Live beacon
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("FIVE SERVER (port 5500)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text("http://localhost:5500/index.html", fontSize = 9.sp, color = theme.textColor.copy(0.4f))
            }
            Icon(
                imageVector = Icons.Filled.Sensors,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(14.dp)
            )
        }

        AndroidView(
            factory = { context ->
                try {
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        setBackgroundColor(0L.toInt()) // transparent background
                    }
                } catch (e: Throwable) {
                    // Fallback TexView if WebView standard package is disabled or missing
                    android.widget.TextView(context).apply {
                        text = "Live Preview not supported on this device: ${e.localizedMessage}"
                        setTextColor(android.graphics.Color.RED)
                        setPadding(16, 16, 16, 16)
                    }
                }
            },
            update = { view ->
                if (view is WebView) {
                    try {
                        // Feed the htmlText into WebView nicely formatted
                        val styledHtml = if (!htmlText.contains("<body>") && !htmlText.contains("<html")) {
                            "<html><body style='color:#FFFFFF; background-color:#1E1E1E; font-family:sans-serif; padding:12px;'><h3>Five Server Live Preview</h3><p>$htmlText</p></body></html>"
                        } else if (!htmlText.contains("color:") && !htmlText.contains("<style>")) {
                            if (htmlText.contains("<body")) {
                                htmlText.replace("<body", "<body style='color:#FFFFFF; background-color:#1E1E1E; font-family:sans-serif; padding:12px;' ")
                            } else {
                                "<html><body style='color:#FFFFFF; background-color:#1E1E1E; font-family:sans-serif; padding:12px;'>$htmlText</body></html>"
                            }
                        } else {
                            htmlText
                        }
                        view.loadDataWithBaseURL("http://localhost:5500/", styledHtml, "text/html", "UTF-8", null)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                } else if (view is android.widget.TextView) {
                    view.text = "Live Preview unavailable: WebView package is missing or disabled."
                }
            },
            modifier = Modifier.fillMaxSize().weight(1f)
        )
    }
}

// Side-by-Side comparison git diff screen Layout
@Composable
fun DiffViewerLayout(
    diffResult: Pair<WorkspaceFile, List<DiffLine>>,
    theme: IDETheme,
    onCloseDiff: () -> Unit
) {
    val file = diffResult.first
    val diffList = diffResult.second

    Column(modifier = Modifier.fillMaxSize().background(theme.editorBg)) {
        // Diff Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.sidebarBg)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Compare, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("Working Tree Comparison file: ${file.filePath}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                    Text("Deletions vs. Insertions", fontSize = 9.sp, color = Color.Gray)
                }
            }

            IconButton(onClick = onCloseDiff, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close Diff View", tint = theme.textColor)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
            items(diffList) { line ->
                val lineBg = when (line.type) {
                    DiffType.INSERT -> Color.Green.copy(0.12f)
                    DiffType.DELETE -> Color.Red.copy(0.12f)
                    DiffType.NORMAL -> Color.Transparent
                }
                val prefix = when (line.type) {
                    DiffType.INSERT -> "+ "
                    DiffType.DELETE -> "- "
                    DiffType.NORMAL -> "  "
                }
                val lineTextCol = when (line.type) {
                    DiffType.INSERT -> Color.Green
                    DiffType.DELETE -> Color.Red
                    DiffType.NORMAL -> theme.textColor
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(lineBg)
                        .padding(vertical = 1.dp)
                ) {
                    // Line indexing indications
                    Text(
                        "${line.initialLineNo ?: ""} | ${line.currentLineNo ?: ""} ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.Gray.copy(0.6f),
                        modifier = Modifier.width(44.dp)
                    )
                    Text(
                        prefix + line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = lineTextCol
                    )
                }
            }
        }
    }
}

// Side explorer empty state card
@Composable
fun EditorEmptyState(
    theme: IDETheme,
    onNewFileRequested: () -> Unit,
    onCloneRequested: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.editorBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Filled.Code, contentDescription = null, tint = theme.textColor.copy(0.24f), modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Void Editor 📱", fontWeight = FontWeight.Bold, color = theme.textColor, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("An integrated offline-first sandbox editor for phone.", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(20.dp))

            // Quick commands buttons layout pairs
            Button(
                onClick = onNewFileRequested,
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New scripting tab file")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCloneRequested,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.textColor),
                border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clone repository from URL")
            }
        }
    }
}

// Console drawer bottom screen panel
@Composable
fun TerminalDrawerLayout(
    theme: IDETheme,
    terminalHistory: List<CommandLine>,
    terminalInput: String,
    onInputChange: (String) -> Unit,
    onCommandSubmitted: (String) -> Unit,
    onCloseTerminal: () -> Unit
) {
    val terminalListState = rememberLazyListState()

    // Auto scroll bottom when lines append
    LaunchedEffect(terminalHistory.size) {
        if (terminalHistory.isNotEmpty()) {
            terminalListState.animateScrollToItem(terminalHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(theme.terminalBg)
            .border(BorderStroke(1.dp, Color.White.copy(0.08f)))
    ) {
        // Drawer Header bar options selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.sidebarBg)
                .padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Text("TERMINAL  •  PowerShell", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text("OUTPUT", fontSize = 9.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text("PROBLEMS (0)", fontSize = 9.sp, color = Color.Gray)
            }

            IconButton(onClick = onCloseTerminal, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close Terminal", tint = theme.textColor, modifier = Modifier.size(14.dp))
            }
        }

        // Output lines List
        LazyColumn(
            state = terminalListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            items(terminalHistory) { line ->
                val lineCol = when {
                    line.isError -> Color.Red
                    line.isSuccess -> Color.Green
                    line.isPrompt -> theme.accentColor
                    else -> theme.textColor.copy(0.9f)
                }
                Text(
                    text = line.text,
                    color = lineCol,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (line.isPrompt) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Terminal prompt text inputs row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.terminalBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PS > ", color = theme.accentColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            
            BasicTextField(
                value = terminalInput,
                onValueChange = onInputChange,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    color = theme.textColor,
                    fontSize = 11.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(theme.accentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onCommandSubmitted(terminalInput)
                }),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Active bottom status strip line
@Composable
fun StatusBar(
    theme: IDETheme,
    activeProjName: String,
    branchName: String,
    isTerminalOpen: Boolean,
    hasPrettier: Boolean,
    onTerminalToggleRequested: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(theme.statusBarBg)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(branchName, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.width(14.dp))
            Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Main remote sync", color = Color.White, fontSize = 10.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Prettier Status
            if (hasPrettier) {
                Text("Prettier active  •  ", color = Color.White.copy(0.84f), fontSize = 10.sp)
            }
            Text("Spaces: 4  •  UTF-8  •  ", color = Color.White.copy(0.84f), fontSize = 10.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onTerminalToggleRequested() }
            ) {
                Icon(
                    imageVector = if (isTerminalOpen) Icons.Filled.Terminal else Icons.Filled.Keyboard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isTerminalOpen) "Hide console" else "Show console", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

// --- Sleek Header Bar following the "Sleek Interface" Design Vibe ---
@Composable
fun EditorHeaderBar(
    theme: IDETheme,
    activePath: String?,
    activeProjName: String,
    onMenuClicked: () -> Unit,
    onPlayClicked: () -> Unit,
    onMoreClicked: () -> Unit
) {
    val directoryName = if (activePath != null) {
        val parts = activePath.split("/")
        if (parts.size > 1) {
            parts.dropLast(1).joinToString(" / ")
        } else {
            activeProjName
        }
    } else {
        activeProjName
    }
    
    val fileName = activePath?.substringAfterLast("/") ?: "Void Workspace"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(Color(0xFF1C1C1C))
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onMenuClicked() }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu Sidebar",
                        tint = theme.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = directoryName.uppercase(),
                        fontSize = 9.sp,
                        color = theme.textColor.copy(0.4f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = fileName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayClicked,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.04f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Run Script",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMoreClicked,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.04f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "More Actions",
                        tint = theme.textColor.copy(0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(0.08f), thickness = 1.dp)
    }
}
