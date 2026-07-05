package com.qihe.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val serverUrl: String by lazy { BuildConfig.SERVER_URL }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                databaseEnabled = true
                // 【关键】允许 file:// 页面访问其他 file:// 资源（加载同目录 js/css）
                allowFileAccessFromFileURLs = true
                // 【关键】允许 file:// 页面发起网络请求（调用后端 API）
                allowUniversalAccessFromFileURLs = true
                // 屏幕适配
                useWideViewPort = true
                loadWithOverviewMode = true
                // 允许混合内容
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                // 禁止缩放
                builtInZoomControls = false
                displayZoomControls = false
                // 缓存
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                setSupportMultipleWindows(false)
            }

            // Android 桥接对象
            addJavascriptInterface(AndroidBridge(), "__android")

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null

                    // 拦截 index.html，在 </head> 前注入服务器地址
                    if (url.endsWith("index.html")) {
                        return try {
                            val inputStream = assets.open("www/index.html")
                            val content = injectServerUrl(inputStream)
                            WebResourceResponse(
                                "text/html", "UTF-8",
                                ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_8))
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    return null
                }
            }

            webChromeClient = WebChromeClient()
        }

        setContentView(webView)

        // 加载本地文件
        webView.loadUrl("file:///android_asset/www/index.html")
    }

    /**
     * 在 HTML 的 </head> 前注入服务器地址和 Android 标识，
     * 确保在 bundler 解包、api-client.js 加载前设置好。
     */
    private fun injectServerUrl(inputStream: InputStream): String {
        val html = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val scriptTag = "\n<script>window.__QIHE_SERVER_URL__='$serverUrl';window.__QIHE_ANDROID__=true;</script>"
        return html.replace("</head>", "$scriptTag</head>")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    /**
     * JS 桥接对象 — 暴露给 WebView 中的 JavaScript 调用
     */
    inner class AndroidBridge {
        @android.webkit.JavascriptInterface
        fun getServerUrl(): String = serverUrl

        @android.webkit.JavascriptInterface
        fun getAppVersion(): String = BuildConfig.VERSION_NAME

        @android.webkit.JavascriptInterface
        fun toast(message: String) {
            runOnUiThread {
                android.widget.Toast.makeText(
                    this@MainActivity, message,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
