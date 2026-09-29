package com.shueibhub.notepadholo

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
)

enum class AccentOption(@StringRes val label: Int, val color: Color) {
    ORIGINAL(R.string.accent_original, Color(0xFFAA3CFF)),
    WHITE(R.string.accent_white, Color(0xFFF2F2F7)),
    RED(R.string.accent_red, Color(0xFFFF5C6C)),
    ORANGE(R.string.accent_orange, Color(0xFFFF9A3D)),
    YELLOW(R.string.accent_yellow, Color(0xFFFFD84D)),
    GREEN(R.string.accent_green, Color(0xFF3DDC97)),
    BLUE(R.string.accent_blue, Color(0xFF4DA3FF)),
    PURPLE(R.string.accent_purple, Color(0xFF7C5CFF)),
}

sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Editor(val id: String?) : Screen
}

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("notepad_store", Context.MODE_PRIVATE)

    val notes = mutableStateListOf<Note>()

    var screen by mutableStateOf<Screen>(Screen.Home)
    var query by mutableStateOf("")
    var accentIndex by mutableIntStateOf(0)
        private set

    init {
        runCatching {
            val raw = prefs.getString("notes", null)
            if (raw != null) {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) notes.add(arr.getJSONObject(i).toNote())
            }
        }
        accentIndex = prefs.getInt("accent", 0).coerceIn(0, AccentOption.entries.lastIndex)
    }

    fun visibleNotes(): List<Note> {
        val q = query.trim()
        return notes
            .filter { q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true) }
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
    }

    fun upsert(note: Note) {
        if (note.title.isBlank() && note.content.isBlank()) {
            delete(note.id)
            return
        }
        val i = notes.indexOfFirst { it.id == note.id }
        if (i >= 0) notes[i] = note else notes.add(note)
        persist()
    }

    fun delete(id: String) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) {
            notes.removeAt(i)
            persist()
        }
    }

    fun togglePin(id: String) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) {
            notes[i] = notes[i].copy(pinned = !notes[i].pinned)
            persist()
        }
    }

    fun deleteAll() {
        notes.clear()
        persist()
    }

    fun setAccent(index: Int) {
        accentIndex = index.coerceIn(0, AccentOption.entries.lastIndex)
        prefs.edit().putInt("accent", accentIndex).apply()
    }

    fun exportJson(): String {
        val arr = JSONArray()
        notes.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    /** يرجع عدد الملاحظات المستوردة، أو -1 لو البيانات غير صالحة */
    fun importJson(text: String): Int = try {
        val arr = JSONArray(text.trim())
        var count = 0
        for (i in 0 until arr.length()) {
            val n = arr.getJSONObject(i).toNote()
            val idx = notes.indexOfFirst { it.id == n.id }
            if (idx >= 0) notes[idx] = n else notes.add(n)
            count++
        }
        persist()
        count
    } catch (e: Exception) {
        -1
    }

    private fun persist() {
        prefs.edit().putString("notes", exportJson()).apply()
    }

    private fun Note.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("content", content)
        .put("updatedAt", updatedAt)
        .put("pinned", pinned)

    private fun JSONObject.toNote() = Note(
        id = optString("id").ifBlank { UUID.randomUUID().toString() },
        title = optString("title"),
        content = optString("content"),
        updatedAt = optLong("updatedAt", System.currentTimeMillis()),
        pinned = optBoolean("pinned", false),
    )
}

/** يحفظ ملف TXT ويرجع وصف المكان، أو null عند الفشل */
fun saveTxt(context: Context, name: String, text: String): String? = try {
    val clean = name.trim().ifBlank { "note" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val fileName = if (clean.endsWith(".txt", ignoreCase = true)) clean else "$clean.txt"
    if (Build.VERSION.SDK_INT >= 29) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Notepad")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("insert failed")
        context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
        "Downloads/Notepad/$fileName"
    } else {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val f = File(dir, fileName)
        f.writeText(text)
        f.absolutePath
    }
} catch (e: Exception) {
    null
}
