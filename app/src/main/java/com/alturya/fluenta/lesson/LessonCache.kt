package com.alturya.fluenta.lesson

// File-based offline cache for lesson payloads.
// Stores the last N lessons as JSON files in cacheDir.
// Used as fallback when network is unavailable — covers the "no wifi on subway" UX.
//
// No Room/KSP needed: Gson is already in deps and files survive process restarts.
// TTL: 7 days (matches server Redis cache). LRU eviction after MAX_CACHED lessons.

import android.content.Context
import com.google.gson.Gson
import com.alturya.fluenta.network.LessonPlayResponse
import java.io.File

object LessonCache {

    private const val MAX_CACHED = 20
    private const val TTL_MS = 7 * 24 * 60 * 60 * 1000L
    private val gson = Gson()

    private fun file(context: Context, lessonId: String): File =
        File(context.cacheDir, "lesson_${lessonId.take(36)}.json")

    fun save(context: Context, lessonId: String, payload: LessonPlayResponse) {
        runCatching {
            file(context, lessonId).writeText(gson.toJson(payload))
            evictOld(context)
        }
    }

    fun load(context: Context, lessonId: String): LessonPlayResponse? {
        val f = file(context, lessonId)
        if (!f.exists()) return null
        if (System.currentTimeMillis() - f.lastModified() > TTL_MS) { f.delete(); return null }
        return runCatching { gson.fromJson(f.readText(), LessonPlayResponse::class.java) }.getOrNull()
    }

    private fun evictOld(context: Context) {
        val files = context.cacheDir.listFiles { f -> f.name.startsWith("lesson_") && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_CACHED).forEach { it.delete() }
    }
}
