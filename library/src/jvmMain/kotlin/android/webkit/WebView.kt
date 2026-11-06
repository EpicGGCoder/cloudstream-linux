package android.webkit

/**
 * JVM stubs for Android's android.webkit package.
 *
 * Plugins that use WebView (Cloudflare-protected sites) reference these
 * classes. On JVM desktop we don't have a real Android WebView — Phase 2
 * will add JCEF (Chromium Embedded Framework) as a real replacement.
 * For now these stubs let bytecode link.
 */
open class WebSettings {
    var javaScriptEnabled: Boolean = false
    var domStorageEnabled: Boolean = false
    var userAgentString: String = ""
    var blockNetworkImage: Boolean = false
    var cacheMode: Int = 0
    var loadsImagesAutomatically: Boolean = true
    var mediaPlaybackRequiresUserGesture: Boolean = true
    var javaScriptCanOpenWindowsAutomatically: Boolean = false
    var allowFileAccess: Boolean = true
    var allowContentAccess: Boolean = true
    var databaseEnabled: Boolean = false
    var saveFormData: Boolean = false
    var savePassword: Boolean = false
    var geolocationEnabled: Boolean = false
    var appCacheEnabled: Boolean = false
    var builtInZoomControls: Boolean = false
    var displayZoomControls: Boolean = true
    var supportZoom: Boolean = true
    var loadWithOverviewMode: Boolean = false
    var useWideViewPort: Boolean = true

    companion object {
        const val LOAD_DEFAULT = 1
        const val LOAD_CACHE_ELSE_NETWORK = 2
        const val LOAD_NO_CACHE = 3
        const val LOAD_CACHE_ONLY = 4
    }
}

open class WebView {
    constructor()
    constructor(context: android.content.Context?)

    val settings: WebSettings = WebSettings()
    fun loadUrl(url: String) {}
    fun evaluateJavascript(script: String, callback: android.webkit.ValueCallback<String?>? = null) {}
    fun stopLoading() {}
    fun destroy() {}
    fun goBack() {}
    fun goForward() {}
    fun reload() {}
    fun canGoBack(): Boolean = false
    fun canGoForward(): Boolean = false
    fun setWebViewClient(client: WebViewClient?) {}
    fun setWebChromeClient(client: Any?) {}
    fun addJavascriptInterface(obj: Any, name: String) {}
    fun setInitialScale(scaleInPercent: Int) {}
    fun clearCache(includeDiskFiles: Boolean) {}
    fun clearHistory() {}
}

interface ValueCallback<T> {
    fun onReceiveValue(value: T?)
}

open class ServiceWorkerController {
    companion object {
        @JvmStatic
        fun getInstance(): ServiceWorkerController? = null
    }
}

interface ServiceWorkerClient

open class WebResourceRequest {
    fun getUrl(): android.net.Uri? = null
    fun isForMainFrame(): Boolean = false
    fun getRequestHeaders(): Map<String, String> = emptyMap()
    fun getMethod(): String = "GET"
}

open class WebResourceResponse {
    private var mimeType: String = "text/html"
    private var encoding: String = "utf-8"
    private var data: java.io.InputStream? = null

    constructor()
    constructor(mimeType: String, encoding: String, data: java.io.InputStream?) {
        this.mimeType = mimeType
        this.encoding = encoding
        this.data = data
    }

    fun getMimeType(): String = mimeType
    fun getEncoding(): String = encoding
    fun getData(): java.io.InputStream? = data
}

open class WebViewClient {
    open fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
    open fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false
    open fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {}
    open fun onPageFinished(view: WebView?, url: String?) {}
    open fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? = null
}

open class CookieManager {
    companion object {
        @JvmStatic
        fun getInstance(): CookieManager = CookieManager()
    }
    fun getCookie(url: String): String? = null
    fun setCookie(url: String, value: String) {}
    fun removeAllCookies(callback: ValueCallback<Boolean>?) {}
    fun flush() {}
}
