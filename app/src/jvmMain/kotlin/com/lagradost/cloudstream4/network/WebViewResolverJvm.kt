package com.lagradost.cloudstream4.network

import org.slf4j.LoggerFactory

/**
 * WebView resolver — bridges to the CDP-based Cloudflare solver.
 *
 * Cloudflare-protected plugins (HDrezka, TamilDhool, NetMirror, etc.)
 * need a real browser to solve JS challenges. We use Chrome DevTools Protocol
 * (CDP) to drive the system's Chrome/Chromium headlessly.
 *
 * See CdpCloudflareSolver for the implementation.
 */
object WebViewResolverJvm {
    private val log = LoggerFactory.getLogger(WebViewResolverJvm::class.java)

    fun init() {
        log.info("WebViewResolverJvm initialized — using CDP Cloudflare solver")
        // Check if Chrome is available
        val hasChrome = try {
            val candidates = listOf(
                "/usr/bin/google-chrome", "/usr/bin/google-chrome-stable",
                "/usr/bin/chromium", "/usr/bin/chromium-browser",
                "/snap/bin/chromium", "/usr/bin/brave-browser",
                System.getenv("CHROME_PATH"),
            )
            candidates.any { it != null && java.io.File(it).exists() }
        } catch (e: Exception) { false }
        if (hasChrome) {
            log.info("Chrome/Chromium found — Cloudflare bypass is ACTIVE")
        } else {
            log.warn("Chrome/Chromium not found — Cloudflare-protected sites won't work. Install: sudo apt install chromium")
        }

        // Register shutdown hook to clean up Chrome
        Runtime.getRuntime().addShutdownHook(Thread {
            CdpCloudflareSolver.shutdown()
        })
    }

    /** The real interceptor — solves Cloudflare challenges via CDP. */
    val interceptor = CdpCloudflareSolver.interceptor
}
