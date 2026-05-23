package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. Workspace Project ---
@Entity(tableName = "workspace_projects")
data class WorkspaceProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val gitRemoteUrl: String = "",
    val gitBranch: String = "main",
    val gitHistoryJson: String = "[]", // Represets list of mock commits
    val stagedFilesJson: String = "[]", // List of file paths staged
    val createdTimestamp: Long = System.currentTimeMillis()
)

// --- 2. Workspace File ---
@Entity(
    tableName = "workspace_files",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceProject::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId", "filePath"], unique = true)]
)
data class WorkspaceFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val filePath: String, // e.g. "src/main.js" or "index.html"
    val content: String,
    val initialContent: String = "", // Used for git diff checks
    val hasUncommittedChanges: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

// --- 3. Extension Metadata ---
@Entity(tableName = "installed_extensions")
data class InstalledExtension(
    @PrimaryKey val id: String, // e.g. "gemini-copilot", "prettier-formatter"
    val name: String,
    val description: String,
    val publisher: String,
    val version: String,
    val isEnabled: Boolean = false,
    val rating: Float = 4.8f,
    val downloads: String = "120K",
    val iconName: String, // e.g. "smart_toy", "brush", "code"
    val readme: String = ""
)

// --- 4. Editor Settings ---
@Entity(tableName = "editor_settings")
data class EditorSettings(
    @PrimaryKey val id: Int = 1,
    val themeName: String = "Sleek Interface",
    val fontSize: Float = 14f,
    val isWordWrapEnabled: Boolean = true,
    val isAutoSaveEnabled: Boolean = false,
    val terminalOpen: Boolean = false,
    val activeProjectId: Int = -1,
    val wallpaperName: String = "Classic None"
)

// --- DAOs ---

@Dao
interface ProjectDao {
    @Query("SELECT * FROM workspace_projects ORDER BY createdTimestamp DESC")
    fun getAllProjectsFlow(): Flow<List<WorkspaceProject>>

    @Query("SELECT * FROM workspace_projects WHERE id = :id")
    suspend fun getProjectById(id: Int): WorkspaceProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: WorkspaceProject): Long

    @Update
    suspend fun updateProject(project: WorkspaceProject)

    @Query("DELETE FROM workspace_projects WHERE id = :id")
    suspend fun deleteProject(id: Int)
}

@Dao
interface FileDao {
    @Query("SELECT * FROM workspace_files WHERE projectId = :projectId ORDER BY filePath ASC")
    fun getFilesByProjectFlow(projectId: Int): Flow<List<WorkspaceFile>>

    @Query("SELECT * FROM workspace_files WHERE projectId = :projectId AND filePath = :filePath")
    suspend fun getFileByPath(projectId: Int, filePath: String): WorkspaceFile?

    @Query("SELECT * FROM workspace_files WHERE id = :fileId")
    suspend fun getFileById(fileId: Int): WorkspaceFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: WorkspaceFile): Long

    @Update
    suspend fun updateFile(file: WorkspaceFile)

    @Query("DELETE FROM workspace_files WHERE id = :fileId")
    suspend fun deleteFile(fileId: Int)

    @Query("DELETE FROM workspace_files WHERE projectId = :projectId AND filePath = :filePath")
    suspend fun deleteFileByPath(projectId: Int, filePath: String)
}

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM installed_extensions")
    fun getAllExtensionsFlow(): Flow<List<InstalledExtension>>

    @Query("SELECT * FROM installed_extensions WHERE id = :id")
    suspend fun getExtensionById(id: String): InstalledExtension?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: InstalledExtension)

    @Update
    suspend fun updateExtension(extension: InstalledExtension)

    @Query("SELECT COUNT(*) FROM installed_extensions")
    suspend fun getExtensionsCount(): Int
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM editor_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<EditorSettings?>

    @Query("SELECT * FROM editor_settings WHERE id = 1")
    suspend fun getSettings(): EditorSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: EditorSettings)
}
