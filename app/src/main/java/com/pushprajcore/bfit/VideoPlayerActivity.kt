package com.pushprajcore.bfit

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val webView = findViewById<WebView>(R.id.webView)
        val closeBtn = findViewById<ImageButton>(R.id.closeBtn)

        closeBtn.setOnClickListener { finish() }

        val videoId = intent.getStringExtra("VIDEO_ID") ?: "dQw4w9WgXcQ"

        // Setup WebView for YouTube Embedded Player
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
        
        val html = """
            <html>
                <body style="margin:0;padding:0;background-color:#000000;">
                    <iframe width="100%" height="100%" 
                            src="https://www.youtube.com/embed/$videoId?autoplay=1&fs=1" 
                            frameborder="0" 
                            allow="autoplay; encrypted-media; fullscreen" 
                            allowfullscreen>
                    </iframe>
                </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
    }
}
