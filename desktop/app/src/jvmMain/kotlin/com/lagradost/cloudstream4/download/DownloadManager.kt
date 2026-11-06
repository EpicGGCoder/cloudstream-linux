package com.lagradost.cloudstream4.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Download manager — downloads video for offline playback.
 *
 * Supports:
 *   - Direct video files (.mp4, .mkv) — single download
 *   - HLS streams (.m3u8) — parse manifest, download all .ts segments, merge
 *
 * Mirrors CS3's VideoDownloadManager (utils/downloader/VideoDownloadManager.kt).
 */
class DownloadManager(
    private val scope: CoroutineScope,
    private val downloadDir: File,
) {
    private val log = LoggerFactory.getLogger(DownloadManager::class.java)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class DownloadJob(
        val id: String,
        val name: String,
        val url: String,
        val outputPath: File,
        val totalBytes: AtomicLong = AtomicLong(0),
        val downloadedBytes: AtomicLong = AtomicLong(0),
        var status: DownloadStatus = DownloadStatus.QUEUED,
        var errorMsg: String? = null,
    )

    enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }

    private val _downloads = MutableStateFlow<List<DownloadJob>>(emptyList())
    val downloads: StateFlow<List<DownloadJob>> = _downloads.asStateFlow()

    private val jobs = mutableMapOf<String, DownloadJob>()

    init { downloadDir.mkdirs() }

    fun startDownload(id: String, name: String, url: String, filename: String) {
        val outputFile = File(downloadDir, filename)
        val job = DownloadJob(id, name, url, outputFile)
        jobs[id] = job
        _downloads.value = _downloads.value + job

        scope.launch {
            job.status = DownloadStatus.DOWNLOADING
            updateJob(job)
            try {
                withContext(Dispatchers.IO) {
                    val req = Request.Builder().url(url).header("User-Agent", "CloudStream-Desktop").get().build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) { job.errorMsg = "HTTP ${resp.code}"; job.status = DownloadStatus.FAILED; return@use }
                        val body = resp.body ?: run { job.errorMsg = "No body"; job.status = DownloadStatus.FAILED; return@use }
                        job.totalBytes.set(body.contentLength().coerceAtLeast(0))
                        FileOutputStream(outputFile).use { out ->
                            body.byteStream().use { input ->
                                val buffer = ByteArray(8192); var read: Int
                                while (input.read(buffer).also { read = it } != -1) { out.write(buffer, 0, read); job.downloadedBytes.addAndGet(read.toLong()) }
                            }
                        }
                        job.status = DownloadStatus.COMPLETED
                    }
                }
            } catch (e: Exception) { log.error("Download failed: $name", e); job.errorMsg = e.message; job.status = DownloadStatus.FAILED }
            updateJob(job)
        }
    }

    fun startHlsDownload(id: String, name: String, m3u8Url: String, filename: String) {
        val outputFile = File(downloadDir, "${filename}.ts")
        val job = DownloadJob(id, name, m3u8Url, outputFile)
        jobs[id] = job
        _downloads.value = _downloads.value + job

        scope.launch {
            job.status = DownloadStatus.DOWNLOADING
            updateJob(job)
            try {
                withContext(Dispatchers.IO) {
                    val manifestText = fetchText(m3u8Url)
                    val segments = parseM3u8Segments(manifestText, m3u8Url)
                    if (segments.isEmpty()) { job.errorMsg = "No segments"; job.status = DownloadStatus.FAILED; return@withContext }
                    job.totalBytes.set(segments.size.toLong())
                    FileOutputStream(outputFile).use { out ->
                        for ((i, segUrl) in segments.withIndex()) {
                            val req = Request.Builder().url(segUrl).header("User-Agent", "CloudStream-Desktop").get().build()
                            client.newCall(req).execute().use { resp ->
                                if (!resp.isSuccessful) { job.errorMsg = "Segment $i HTTP ${resp.code}"; return@use }
                                resp.body?.byteStream()?.use { input -> val buffer = ByteArray(8192); var read: Int; while (input.read(buffer).also { read = it } != -1) out.write(buffer, 0, read) }
                            }
                            job.downloadedBytes.set((i + 1).toLong()); updateJob(job)
                        }
                    }
                    job.status = DownloadStatus.COMPLETED
                }
            } catch (e: Exception) { log.error("HLS download failed: $name", e); job.errorMsg = e.message; job.status = DownloadStatus.FAILED }
            updateJob(job)
        }
    }

    fun cancel(id: String) { jobs[id]?.let { it.status = DownloadStatus.FAILED; it.errorMsg = "Cancelled"; updateJob(it) } }

    private fun fetchText(url: String): String {
        val req = Request.Builder().url(url).header("User-Agent", "CloudStream-Desktop").get().build()
        return client.newCall(req).execute().use { it.body?.string() ?: "" }
    }

    private fun parseM3u8Segments(manifest: String, baseUrl: String): List<String> {
        val segments = mutableListOf<String>()
        for (line in manifest.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val url = if (trimmed.startsWith("http")) trimmed else java.net.URI(baseUrl).resolve(trimmed).toString()
            segments.add(url)
        }
        return segments
    }

    private fun updateJob(job: DownloadJob) {
        val current = _downloads.value.toMutableList()
        val idx = current.indexOfFirst { it.id == job.id }
        if (idx >= 0) current[idx] = job
        _downloads.value = current
    }
}
