package com.lagradost.cloudstream4.network

import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory

/**
 * WebView resolver — JVM implementation.
 *
 * Phase 1: stub. Plugins with `usesWebView = true` (mostly Cloudflare-protected
 * sites) will see their Cloudflare challenges not solved. This is a known
 * limitation; the upstream CS3 also has `WebViewResolver.jvm.kt` as `TODO`.
 *
 * Phase 2 plan: use JCEF (Chromium Embedded Framework) — a real Chromium that
 * can solve Cloudflare JS challenges. JCEF is heavier than JavaFX WebView but
 * actually works for modern Cloudflare Turnstile.
 *
 * For now, this interceptor is a no-op pass-through.
 */
object WebViewResolverJvm {
    private val log = LoggerFactory.getLogger(WebViewResolverJvm::class.java)

    /** Pass-through interceptor. Phase 2 will replace this with JCEF-backed challenge solving. */
    val interceptor: Interceptor = Interceptor { chain ->
        val req = chain.request()
        val resp = chain.proceed(req)
        if (isChallenge(resp)) {
            log.warn(
                "Cloudflare/DdosGuard challenge detected for ${req.url} — " +
                    "WebView resolver not yet implemented on desktop; request will fail. " +
                    "This will be fixed in Phase 2 via JCEF."
            )
        }
        resp
    }

    private fun isChallenge(resp: Response): Boolean {
        val code = resp.code
        if (code != 403 && code != 503) return false
        val server = resp.header("server") ?: ""
        return server.contains("cloudflare", ignoreCase = true) ||
            server.contains("ddos-guard", ignoreCase = true)
    }

    fun init() {
        log.info("WebViewResolverJvm (stub) initialized — Phase 2 will add JCEF for Cloudflare")
    }
}
