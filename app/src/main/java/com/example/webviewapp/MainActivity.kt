package com.example.webviewapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var sharedPreferences: SharedPreferences
    private var volumeClickCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val resetCounterRunnable = Runnable { volumeClickCount = 0 }

    private val PREFS_NAME = "WebViewConfig"
    private val KEY_URL = "saved_url"
    private val DEFAULT_URL = "http://10.11.0.141:8080/CaniasWebClient/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupCookieManager()
        setupWebView()
        setupSwipeRefresh()
        loadUrl()
    }

    private fun setupCookieManager() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        
        // Bellek ve önbellek yönetimi
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        
        // Force Mobile User Agent
        val mobileUserAgent = "Mozilla/5.0 (Linux; Android 11; M3 SL20) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.210 Mobile Safari/537.36"
        settings.userAgentString = mobileUserAgent

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefreshLayout.isRefreshing = false
                // Çerezleri fiziksel hafızaya kaydet (oturumu korumak için kritik)
                CookieManager.getInstance().flush()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                }
                return true
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            // reload() bazen session bilgilerini temizleyebilir, URL ile yükleme daha güvenlidir
            val currentUrl = webView.url
            if (currentUrl != null) {
                webView.loadUrl(currentUrl)
            } else {
                webView.reload()
            }
        }
        
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun loadUrl() {
        val savedUrl = sharedPreferences.getString(KEY_URL, DEFAULT_URL)
        if (savedUrl != null) {
            webView.loadUrl(savedUrl)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeClickCount++
            
            handler.removeCallbacks(resetCounterRunnable)
            handler.postDelayed(resetCounterRunnable, 5000)

            if (volumeClickCount >= 10) {
                volumeClickCount = 0
                showSettingsDialog()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("URL Ayarlarını Düzenle")
        
        val input = EditText(this)
        input.setText(sharedPreferences.getString(KEY_URL, DEFAULT_URL))
        builder.setView(input)

        builder.setPositiveButton("Kaydet") { dialog, _ ->
            val newUrl = input.text.toString()
            if (newUrl.isNotEmpty()) {
                sharedPreferences.edit().putString(KEY_URL, newUrl).apply()
                webView.loadUrl(newUrl)
                Toast.makeText(this, "URL Kaydedildi", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        
        builder.setNegativeButton("İptal") { dialog, _ -> 
            dialog.cancel() 
        }

        builder.show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
