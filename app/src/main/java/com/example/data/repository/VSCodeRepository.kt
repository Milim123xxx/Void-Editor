package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.database.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// --- Gemini Moshi classes ---
data class GeminiPart(val text: String)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiRequest(val contents: List<GeminiContent>)

class VSCodeRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val projectDao = db.projectDao()
    private val fileDao = db.fileDao()
    private val extensionDao = db.extensionDao()
    private val settingsDao = db.settingsDao()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val geminiRequestAdapter = moshi.adapter(GeminiRequest::class.java)

    // Flows for UI updates
    val allProjects: Flow<List<WorkspaceProject>> = projectDao.getAllProjectsFlow()
    val allExtensions: Flow<List<InstalledExtension>> = extensionDao.getAllExtensionsFlow()
    val editorSettings: Flow<EditorSettings?> = settingsDao.getSettingsFlow()

    fun getFilesByProject(projectId: Int): Flow<List<WorkspaceFile>> =
        fileDao.getFilesByProjectFlow(projectId)

    // Initialize Database values
    suspend fun initializeAndSeed() = withContext(Dispatchers.IO) {
        // 1. Seed Editor Settings
        if (settingsDao.getSettings() == null) {
            settingsDao.insertOrUpdateSettings(EditorSettings())
        }

        // 2. Seed Extensions (Using custom self-healing seed for new additions)
        if (extensionDao.getExtensionsCount() < 12) {
            val initialExtensions = listOf(
                InstalledExtension(
                    id = "gemini-copilot",
                    name = "Gemini AI Copilot",
                    description = "An AI coding companion powered by Google Gemini API to write, complete, describe, and refactor lines of code directly on your mobile screen.",
                    publisher = "Google",
                    version = "v1.2.0",
                    isEnabled = true,
                    rating = 4.9f,
                    downloads = "4.2M",
                    iconName = "smart_toy",
                    readme = "# Gemini AI Copilot\n\nTake your mobile coding to the next level with cutting-edge AI assistance powered by **Gemini 3.5 Flash**.\n\n### Features:\n- 💡 **AI Autocomplete**: Ask for smart suggestions on any code block.\n- 🔍 **Explain Code**: Highlights lines of code and explains logic.\n- ⚙️ **Refactor Code**: Simplifies algorithms and handles legacy blocks."
                ),
                InstalledExtension(
                    id = "prettier-formatter",
                    name = "Prettier Formatter",
                    description = "Opinionated script and layout formatter that cleans up custom script spacing, tag alignments, and color elements instantly.",
                    publisher = "Prettier",
                    version = "v3.1.2",
                    isEnabled = true,
                    rating = 4.7f,
                    downloads = "2.8M",
                    iconName = "brush",
                    readme = "# Prettier Formatter\n\nAutomatically tidy up your layout alignments, spaces, brackets, and line wraps on save.\n\n### Formating Styles:\n- Tab spacing: 2 or 4\n- Trailing commas\n- Braces styling"
                ),
                InstalledExtension(
                    id = "gitlens",
                    name = "GitLens Mobile",
                    description = "Adds deep visual author indicators, local history visualizers, branch diff comparisons, and staging flows inside your project manager.",
                    publisher = "GitKraken",
                    version = "v11.5.0",
                    isEnabled = false,
                    rating = 4.8f,
                    downloads = "1.5M",
                    iconName = "history",
                    readme = "# GitLens Mobile\n\nSupercharge git control inside the app. Exposes precise insert/delete visual comparisons, stage changes indicators, and visual history grids."
                ),
                InstalledExtension(
                    id = "markdown-preview",
                    name = "Markdown Preview Enabler",
                    description = "Renders styled rich HTML outputs dynamically for all your written documentation `.md` logs in side-by-side split screen previews.",
                    publisher = "Microsoft",
                    version = "v2.0.4",
                    isEnabled = true,
                    rating = 4.6f,
                    downloads = "980K",
                    iconName = "menu_book",
                    readme = "# Markdown Previewer\n\nEnables real-time rendering of your project Markdown documentations right on your mobile viewport."
                ),
                InstalledExtension(
                    id = "python-intellisense",
                    name = "Python IntelliSense",
                    description = "Brings deep syntax detection, rich structure tips, and autocompletions for all your Python modules and main scripts.",
                    publisher = "Microsoft",
                    version = "v2026.5.0",
                    isEnabled = false,
                    rating = 4.8f,
                    downloads = "1.1M",
                    iconName = "code",
                    readme = "# Python IntelliSense\n\nBrings signature helps, automatic snippets, standard library structures, and formatting assistance for `.py` files."
                ),
                InstalledExtension(
                    id = "rainbow-brackets",
                    name = "Rainbow Brackets Tool",
                    description = "Colors deep matching bracket scopes across curves, boxes, and curls to isolate block syntax layers clearly.",
                    publisher = "2dfall",
                    version = "v1.4.2",
                    isEnabled = true,
                    rating = 4.5f,
                    downloads = "410K",
                    iconName = "palette",
                    readme = "# Rainbow Brackets\n\nColorize nested brackets instantly to stay clean when coding long conditions, loop ranges, and parameters."
                ),
                InstalledExtension(
                    id = "live-server",
                    name = "Live Server (Five Server)",
                    description = "Launches an instant, local development web server to run, render, and live-reload HTML paths directly inside your mobile editor.",
                    publisher = "Yannick",
                    version = "v2.3.0",
                    isEnabled = true,
                    rating = 4.8f,
                    downloads = "3.1M",
                    iconName = "sensors",
                    readme = "# Live Server (Five Server)\n\nDeploy static HTML pages locally with the press of a key and synchronize editing live inside the web viewport.\n\n### Features:\n- 🚀 **One-Click Launch**: Press the 'Run' icon on any html route to open the Live Viewport.\n- 🔄 **Hot Live Reload**: Saved changes display instantly."
                ),
                InstalledExtension(
                    id = "auto-rename-tag",
                    name = "Auto Rename Tag",
                    description = "Automatically renames matching paired HTML/XML tags. When you modify an opening token, the closing token mirrors it cleanly.",
                    publisher = "Jun Han",
                    version = "v1.1.2",
                    isEnabled = true,
                    rating = 4.9f,
                    downloads = "2.4M",
                    iconName = "sync",
                    readme = "# Auto Rename Tag\n\nMaintains pristine paired markup effortlessly. Modifying `<div>` into `<section>` will update the matching `</div>` to `</section>` automatically as you type."
                ),
                InstalledExtension(
                    id = "highlight-matching",
                    name = "Highlight Matching Tag/Brackets",
                    description = "Identifies and highlights the bounding HTML/XML matching tags or code block bracket pairs instantly near the editing cursor.",
                    publisher = "vincaslt",
                    version = "v1.0.4",
                    isEnabled = true,
                    rating = 4.7f,
                    downloads = "1.2M",
                    iconName = "border_color",
                    readme = "# Highlight Matching Tag/Brackets\n\nHighlights active code blocks so you never get lost in long code: highlights corresponding braces `{}` or code tags `<a>...</a>`."
                ),
                InstalledExtension(
                    id = "auto-close-tag",
                    name = "Auto Close Tag",
                    description = "Automatically appends the closing HTML/XML element tags as soon as you type the opening tag complete marker and moves cursor inside.",
                    publisher = "Jun Han",
                    version = "v0.5.8",
                    isEnabled = true,
                    rating = 4.8f,
                    downloads = "1.9M",
                    iconName = "label",
                    readme = "# Auto Close Tag\n\nAccelerate markup composition. Typing `<html>` immediately inserts `</html>` so you can continue code nests without friction."
                ),
                InstalledExtension(
                    id = "path-intellisense",
                    name = "Path Intellisense",
                    description = "Autocompletes active workspace file paths during input, listing current available project files and parent directory paths instantly.",
                    publisher = "Christian",
                    version = "v2.9.1",
                    isEnabled = true,
                    rating = 4.7f,
                    downloads = "950K",
                    iconName = "folder",
                    readme = "# Path Intellisense\n\nSmart filesystem lookup. Typing `./` inside quotes lists workspace folders and files for seamless reference imports."
                ),
                InstalledExtension(
                    id = "html-snippets",
                    name = "HTML Snippets Studio",
                    description = "Provides structured HTML5 boilerplates, semantic tags, tables, forms, list shortcuts, and element declarations.",
                    publisher = "Abishek",
                    version = "v1.5.0",
                    isEnabled = true,
                    rating = 4.8f,
                    downloads = "810K",
                    iconName = "html",
                    readme = "# HTML Snippets\n\nProvides fast expansions for common layout skeletons:\n- `html5` -> Full modern markup body structure.\n- `form` -> Standard interactive form framework.\n- `table` -> Structured grid cells ready for input."
                ),
                InstalledExtension(
                    id = "css-snippets",
                    name = "CSS Snippets Studio",
                    description = "Speedy autocompletions for modern styles and layouts. Supports Flexbox containers, grid parameters, keyframe motions, and text styles.",
                    publisher = "Marek",
                    version = "v2.2.0",
                    isEnabled = true,
                    rating = 4.6f,
                    downloads = "740K",
                    iconName = "palette",
                    readme = "# CSS Snippets\n\nAllows fast styles addition:\n- `flex-center` -> Display Flex, Align Center, Justify Center\n- `glassmorphism` -> Sheer blur, light border, and translucent backgrounds\n- `neon-glow` -> Radiant custom drop shadows"
                ),
                InstalledExtension(
                    id = "editor-wallpaper",
                    name = "Sleek Editor Background Wallpapers",
                    description = "Add gorgeous customizable technical grid overlays or galactic nebula space-themed background wallpapers behind your workspace code editors.",
                    publisher = "Void Studio",
                    version = "v1.0.0",
                    isEnabled = true,
                    rating = 4.9f,
                    downloads = "620K",
                    iconName = "image",
                    readme = "# Sleek Editor Background Wallpapers\n\nInfuse Void Studio with modern ambiance. Choose your wallpaper background behind the visual code text editor:\n- **Technical Grid**: Architectural lines overlaying slate themes.\n- **Galactic Nebula**: Luminous purple dust cloud representing deep space void.\n- **Sleek Cyber**: Minimal neon tech vectors.\n- **Classic Solid**: Slate dark coding ambiance without images."
                )
            )
            for (ext in initialExtensions) {
                extensionDao.insertExtension(ext)
            }
        }

        // 3. Seed Default Project if empty
        val currentProjects = allProjects.firstOrNull() ?: emptyList()
        if (currentProjects.isEmpty()) {
            val defaultProject = WorkspaceProject(
                name = "My First App",
                description = "Default boilerplate project to experiment with coding, simulated Git, and AI assistance.",
                gitRemoteUrl = "https://github.com/aistudio/my-first-app.git"
            )
            val pId = projectDao.insertProject(defaultProject).toInt()

            updateActiveProject(pId)

            // Seed Files for this project
            val readmeContent = """# My First App 🚀

Welcome to your first project on VS Code Mobile! 

This app replicates the layout, feel, and power of a complete desktop IDE right in your hand.

### Try these interactive tasks:
1. 📝 Edit `index.html` or `script.js` to add your custom features.
2. 🔀 Select the **Git icon in the Activity Bar** (left navigation) to stage files, view code diffs line-by-line, and make commits on your local git log.
3. 🤖 Highlight or select your code helper box and tap **"Ask Gemini AI"** to generate code modifications automatically!
4. ⚙️ Install or disable tools in the **Extensions Marketplace** (the Blocks icon) to experience customized formats and AI assistants.

Have fun coding on the go!
"""
            val htmlContent = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>VS Code Mobile Preview</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="hero">
    <h1>VS Code Mobile 📱</h1>
    <p>A desktop-grade coding experience right on your device!</p>
    <button id="sparkle-btn">Enable Coding Magic ✨</button>
  </div>
  <script src="script.js"></script>
</body>
</html>
"""
            val cssContent = """body {
  background-color: #0d1117;
  color: #c9d1d9;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  margin: 0;
}

.hero {
  text-align: center;
  padding: 2rem;
  border-radius: 12px;
  background-color: #161b22;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  max-width: 400px;
}

h1 {
  color: #58a6ff;
  font-size: 2rem;
  margin-bottom: 1rem;
}

button {
  background-color: #238636;
  color: #ffffff;
  border: none;
  font-size: 1rem;
  font-weight: 600;
  padding: 0.75rem 1.5rem;
  border-radius: 6px;
  cursor: pointer;
  transition: filter 0.2s;
}

button:hover {
  filter: brightness(1.1);
}
"""
            val jsContent = """// Welcoming code logic
document.getElementById('sparkle-btn').addEventListener('click', () => {
  const isDark = document.body.style.backgroundColor === 'rgb(13, 17, 23)';
  
  if (isDark) {
    document.body.style.backgroundColor = '#1f1f2e';
    alert('✨ Advanced coding viewport activated! Let your imagination run wild!');
  } else {
    document.body.style.backgroundColor = '#0d1117';
    alert('🛸 Back to classic dark mode. Coding on mobile is so simple!');
  }
});
"""

            fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "README.md", content = readmeContent, initialContent = readmeContent))
            fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "index.html", content = htmlContent, initialContent = htmlContent))
            fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "style.css", content = cssContent, initialContent = cssContent))
            fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "script.js", content = jsContent, initialContent = jsContent))
        }
    }

    // --- File Operations ---
    suspend fun createFile(projectId: Int, filePath: String, initialText: String = ""): Boolean = withContext(Dispatchers.IO) {
        val existing = fileDao.getFileByPath(projectId, filePath)
        if (existing == null) {
            fileDao.insertFile(WorkspaceFile(projectId = projectId, filePath = filePath, content = initialText, initialContent = initialText))
            true
        } else {
            false
        }
    }

    suspend fun saveFileContent(projectId: Int, filePath: String, newContent: String) = withContext(Dispatchers.IO) {
        val fileRef = fileDao.getFileByPath(projectId, filePath)
        if (fileRef != null) {
            val dirty = fileRef.initialContent != newContent
            fileDao.updateFile(fileRef.copy(
                content = newContent,
                hasUncommittedChanges = dirty,
                lastUpdated = System.currentTimeMillis()
            ))

            // Auto format on save if extension is enabled
            val extensions = extensionDao.getAllExtensionsFlow().firstOrNull() ?: emptyList()
            val prettier = extensions.firstOrNull { it.id == "prettier-formatter" }
            if (prettier != null && prettier.isEnabled) {
                // Formatting code simulated: tidy commas, semi-colons and curly alignments
                var formatted = newContent
                if (filePath.endsWith(".js") || filePath.endsWith(".css") || filePath.endsWith(".html")) {
                    // Quick simulated formatting rule: ensure single lines have clean tabs, trim ends
                    formatted = formatted.lines().joinToString("\n") { line ->
                        var trimmed = line.trimEnd()
                        if (trimmed.startsWith("  ") && !trimmed.contains("   ")) {
                            trimmed = "  " + trimmed.trimStart()
                        }
                        trimmed
                    }
                    if (formatted != newContent) {
                        fileDao.updateFile(fileRef.copy(
                            content = formatted,
                            hasUncommittedChanges = fileRef.initialContent != formatted,
                            lastUpdated = System.currentTimeMillis()
                        ))
                    }
                }
            }
        }
    }

    suspend fun deleteFile(projectId: Int, filePath: String) = withContext(Dispatchers.IO) {
        fileDao.deleteFileByPath(projectId, filePath)
    }

    suspend fun addProject(name: String, description: String): Int = withContext(Dispatchers.IO) {
        val newProj = WorkspaceProject(name = name, description = description)
        val pId = projectDao.insertProject(newProj).toInt()
        
        // Seed standard main setup file
        val readme = "# $name\n\nCreated new Workspace project.\nWrite your code files here."
        fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "README.md", content = readme, initialContent = readme))
        pId
    }

    suspend fun deleteProject(id: Int) = withContext(Dispatchers.IO) {
        projectDao.deleteProject(id)
    }

    suspend fun getProjectById(id: Int): WorkspaceProject? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    suspend fun updateActiveProject(id: Int) = withContext(Dispatchers.IO) {
        val s = settingsDao.getSettings() ?: EditorSettings()
        settingsDao.insertOrUpdateSettings(s.copy(activeProjectId = id))
    }

    suspend fun saveSettings(s: EditorSettings) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdateSettings(s)
    }

    // --- Extension Toggles ---
    suspend fun toggleExtension(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val ext = extensionDao.getExtensionById(id)
        if (ext != null) {
            extensionDao.updateExtension(ext.copy(isEnabled = enabled))
        }
    }

    // --- Git Simulated Engine ---
    suspend fun gitStageFile(projectId: Int, filePath: String) = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: return@withContext
        val staged = JSONArray(project.stagedFilesJson)
        val filesList = mutableListOf<String>()
        for (i in 0 until staged.length()) {
            filesList.add(staged.getString(i))
        }
        if (!filesList.contains(filePath)) {
            filesList.add(filePath)
            val updatedJson = JSONArray(filesList).toString()
            projectDao.updateProject(project.copy(stagedFilesJson = updatedJson))
        }
    }

    suspend fun gitUnstageFile(projectId: Int, filePath: String) = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: return@withContext
        val staged = JSONArray(project.stagedFilesJson)
        val filesList = mutableListOf<String>()
        for (i in 0 until staged.length()) {
            val path = staged.getString(i)
            if (path != filePath) {
                filesList.add(path)
            }
        }
        val updatedJson = JSONArray(filesList).toString()
        projectDao.updateProject(project.copy(stagedFilesJson = updatedJson))
    }

    suspend fun gitStageAll(projectId: Int, modifiedFiles: List<String>) = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: return@withContext
        val updatedJson = JSONArray(modifiedFiles).toString()
        projectDao.updateProject(project.copy(stagedFilesJson = updatedJson))
    }

    suspend fun gitCommit(projectId: Int, message: String, author: String = "developer@vscode.mobile") = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: return@withContext
        val stagedListJson = project.stagedFilesJson
        val stagedJsonArray = JSONArray(stagedListJson)
        if (stagedJsonArray.length() == 0) return@withContext

        // Clear uncommitted dirty markers for staged files
        for (i in 0 until stagedJsonArray.length()) {
            val path = stagedJsonArray.getString(i)
            val vFile = fileDao.getFileByPath(projectId, path)
            if (vFile != null) {
                fileDao.updateFile(vFile.copy(
                    initialContent = vFile.content, // sync initial content
                    hasUncommittedChanges = false
                ))
            }
        }

        // Add history entry
        val history = JSONArray(project.gitHistoryJson)
        val commitObj = JSONObject().apply {
            put("id", "c" + (1000..9999).random())
            put("message", message)
            put("author", author)
            put("timestamp", System.currentTimeMillis())
            put("filesCount", stagedJsonArray.length())
        }
        history.put(commitObj)

        projectDao.updateProject(project.copy(
            gitHistoryJson = history.toString(),
            stagedFilesJson = "[]" // empty staged
        ))
    }

    suspend fun gitCreateBranch(projectId: Int, name: String) = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: return@withContext
        projectDao.updateProject(project.copy(gitBranch = name))
    }

    suspend fun gitClone(remoteUrl: String): Int = withContext(Dispatchers.IO) {
        val projName = when {
            remoteUrl.contains("/") -> remoteUrl.substringAfterLast("/").replace(".git", "")
            else -> "cloned-repo"
        }
        val cleanName = projName.replaceFirstChar { it.uppercase() }
        val newProj = WorkspaceProject(
            name = cleanName,
            description = "Simulated Git cloned repository from $remoteUrl.",
            gitRemoteUrl = remoteUrl,
            gitBranch = "main"
        )
        val pId = projectDao.insertProject(newProj).toInt()

        // Generate framework code-snippets based on repo name
        if (remoteUrl.contains("react", ignoreCase = true)) {
            val appJs = """import React, { useState } from 'react';

export default function App() {
  const [count, setCount] = useState(0);

  return (
    <div style={{ textAlign: 'center', padding: '50px' }}>
      <h1>React Cloned App 🚀</h1>
      <p>Click details to trigger hook changes:</p>
      <button onClick={() => setCount(count + 1)}>
        Count: {count}
      </button>
    </div>
  );
}
"""
            val indexHtml = """<!DOCTYPE html>
<html>
<head>
  <title>React Mounted viewport</title>
</head>
<body>
  <div id="root"></div>
</body>
</html>
"""
            fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "src/App.js", content = appJs, initialContent = appJs))
            fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "public/index.html", content = indexHtml, initialContent = indexHtml))
        } else if (remoteUrl.contains("python", ignoreCase = true) || remoteUrl.contains("ai", ignoreCase = true)) {
            val appPy = """# Beautiful Python Cloned Script
import os
import math

def calculate_analytics():
    print("🛸 AI analysis computing online...")
    dataset = [8.4, 9.1, 10.5, 11.2, 12.0]
    mean = sum(dataset) / len(dataset)
    variance = sum((x - mean) ** 2 for x in dataset) / len(dataset)
    std_dev = math.sqrt(variance)
    
    print(f"Mean: {mean:.2f}")
    print(f"Standard Deviation: {std_dev:.2f}")

if __name__ == "__main__":
    calculate_analytics()
"""
            fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "main.py", content = appPy, initialContent = appPy))
        } else {
            // Generic sample
            val code = """// Default Cloned file
console.log("Successfully cloned code workspace!");
function testSample() {
  return "Connected from Git: $remoteUrl";
}
"""
            fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "src/index.js", content = code, initialContent = code))
        }

        val readme = """# Cloned Project: $cleanName

This workspace has been successfully cloned from **$remoteUrl**!

You can edit files, view side-by-side git diffs, stage files for local commit operations, or execute simulation scripts in the terminal panel below.
"""
        fileDao.insertFile(WorkspaceFile(projectId = pId, filePath = "README.md", content = readme, initialContent = readme))
        pId
    }

    // Line binary diff checker
    fun calculateLineDiffs(initial: String, current: String): List<DiffLine> {
        val initialLines = initial.lines()
        val currentLines = current.lines()
        val results = mutableListOf<DiffLine>()

        // Simpler, highly readable diffing for coding screens
        var i = 0
        var j = 0
        while (i < initialLines.size || j < currentLines.size) {
            when {
                i < initialLines.size && j < currentLines.size && initialLines[i] == currentLines[j] -> {
                    results.add(DiffLine(type = DiffType.NORMAL, text = currentLines[j], initialLineNo = i + 1, currentLineNo = j + 1))
                    i++
                    j++
                }
                j < currentLines.size && (i >= initialLines.size || !initialLines.contains(currentLines[j])) -> {
                    results.add(DiffLine(type = DiffType.INSERT, text = currentLines[j], currentLineNo = j + 1))
                    j++
                }
                i < initialLines.size -> {
                    results.add(DiffLine(type = DiffType.DELETE, text = initialLines[i], initialLineNo = i + 1))
                    i++
                }
            }
        }
        return results
    }

    // --- Gemini API Gateway helper ---
    suspend fun queryCopilotAssistant(prompt: String, codeSnippet: String = "", systemInstruction: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "⚠️ Gemini API key is missing. Please open the Settings panel to configure or add a valid GEMINI_API_KEY inside the Secrets panel of AI Studio!"
        }

        val formattedPrompt = if (codeSnippet.isNotEmpty()) {
            "$prompt\n\n```\n$codeSnippet\n```"
        } else {
            prompt
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = formattedPrompt))
                )
            )
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val jsonRequest = geminiRequestAdapter.toJson(request)
        val body = jsonRequest.toRequestBody("application/json".toMediaType())

        val postRequest = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        try {
            val response = client.newCall(postRequest).execute()
            if (!response.isSuccessful) {
                return@withContext "Error callback: HTTP status ${response.code}\n${response.body?.string()}"
            }
            val responseBody = response.body?.string() ?: return@withContext "Empty response returned from AI model."
            
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstChoice = candidates.getJSONObject(0)
                val contentObj = firstChoice.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text") ?: "No textual output returned."
                }
            }
            "AI response was parsed but text content is unavailable. Verify request payload."
        } catch (e: Exception) {
            "Network Exception: ${e.message ?: "Unknown issue occurred during processing."}"
        }
    }
}

// Diff model
enum class DiffType { NORMAL, INSERT, DELETE }
data class DiffLine(
    val type: DiffType,
    val text: String,
    val initialLineNo: Int? = null,
    val currentLineNo: Int? = null
)
