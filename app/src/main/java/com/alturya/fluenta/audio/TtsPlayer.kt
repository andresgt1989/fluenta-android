package com.alturya.fluenta.audio

import android.content.Context
import android.media.MediaPlayer
import com.alturya.fluenta.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

// Plays TTS audio fetched from the authenticated /api/tts endpoint.
// Caches downloaded mp3s in cacheDir keyed by text hash to avoid refetching.
object TtsPlayer {

    private var player: MediaPlayer? = null

    suspend fun play(context: Context, text: String, l2: String = "en"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = cacheFile(context, "$l2:$text")
            if (!file.exists() || file.length() == 0L) {
                val res = ApiClient.api.getTts(text, l2)
                val body = res.body()
                if (!res.isSuccessful || body == null) {
                    return@withContext Result.failure(Exception("TTS no disponible (${res.code()})"))
                }
                file.outputStream().use { out -> body.byteStream().copyTo(out) }
            }
            withContext(Dispatchers.Main) { startPlayback(file) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun startPlayback(file: File) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { it.release(); if (player == it) player = null }
            prepare()
            start()
        }
    }

    fun stop() {
        player?.let { runCatching { if (it.isPlaying) it.stop(); it.release() } }
        player = null
    }

    private fun cacheFile(context: Context, text: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(context.cacheDir, "tts_$hash.mp3")
    }
}
