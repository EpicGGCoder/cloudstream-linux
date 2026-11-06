package com.lagradost.cloudstream3.network

import okhttp3.Request
import okhttp3.Response
import org.slf4j.LoggerFactory

/**
 * JVM stub for CS3's `com.lagradost.cloudstream3.network.CloudflareKiller`.
 *
 * In the Android app, `CloudflareKiller` uses WebViewResolver to solve
 * Cloudflare JS challenges and passes the resulting cookies back to OkHttp.
 * On JVM desktop, our WebViewResolver is currently a stub (Phase 2 will add
 * JCEF). This CloudflareKiller stub is a pass-through that just logs when
 * a Cloudflare challenge is detected.
 *
 * Plugins that instantiate CloudflareKiller in their constructor will get
 * this stub — provider registration can still proceed; only Cloudflare-
 * protected HTTP requests will fail.
 */
class CloudflareKiller {
    private val log = LoggerFactory.getLogger(CloudflareKiller::class.java)

    /**
     * Attempt to solve a Cloudflare challenge.
     * Stub: always returns the original request unchanged.
     */
    fun solve(request: Request): Request {
        log.debug("CloudflareKiller.solve() called for ${request.url} (stub — no-op)")
        return request
    }

    /**
     * Check if a response is a Cloudflare challenge.
     */
    fun isChallenge(response: Response): Boolean {
        return response.code == 403 || response.code == 503
    }

    companion object {
        private const val TAG = "CloudflareKiller"
    }
}
