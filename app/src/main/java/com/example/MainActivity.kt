package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MaterialTheme {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B1E)),
          color = Color(0xFF0D0B1E)
        ) {
          GameWebViewScreen(
            modifier = Modifier
              .fillMaxSize()
              .safeDrawingPadding()
          )
        }
      }
    }
  }
}

class WebAppInterface(private val context: Context) {
  @JavascriptInterface
  fun vibrate(durationMs: Long) {
    try {
      val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }

      val ms = if (durationMs in 1..200) durationMs else 18L
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vib?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vib?.vibrate(ms)
      }
    } catch (_: Exception) {
      // Ignored if vibrator not available
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameWebViewScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var reloadTrigger by remember { mutableIntStateOf(0) }

  val webView = remember(reloadTrigger) {
    WebView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      setBackgroundColor(0xFF0D0B1E.toInt())
      overScrollMode = View.OVER_SCROLL_NEVER

      // Use software layer type to avoid Mesa DRM rendernode lookup errors in emulator environments
      setLayerType(View.LAYER_TYPE_SOFTWARE, null)

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
        mediaPlaybackRequiresUserGesture = false
        cacheMode = WebSettings.LOAD_DEFAULT
      }

      webViewClient = object : WebViewClient() {
        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
          // Handle render process termination gracefully without crashing the Android Activity
          view?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.destroy()
          }
          reloadTrigger++
          return true
        }
      }
      webChromeClient = WebChromeClient()
      addJavascriptInterface(WebAppInterface(context), "AndroidBridge")
      loadUrl("file:///android_asset/index.html")
    }
  }

  DisposableEffect(webView) {
    webView.onResume()
    onDispose {
      webView.onPause()
      webView.destroy()
    }
  }

  BackHandler(enabled = true) {
    if (webView.canGoBack()) {
      webView.goBack()
    } else {
      (context as? ComponentActivity)?.finish()
    }
  }

  AndroidView(
    factory = { webView },
    modifier = modifier
  )
}
