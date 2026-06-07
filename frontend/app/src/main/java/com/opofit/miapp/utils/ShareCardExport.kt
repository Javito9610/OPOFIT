package com.opofit.miapp.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object ShareCardExport {

    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Renderiza un Composable a Bitmap. Requiere Dispatchers.Main: Compose monta y dibuja
     * la jerarquía solo desde el main thread. El ComposeView se adjunta al decorView de la
     * Activity para que tenga un Recomposer asociado y se elimine cuando termina.
     *
     * Bug previo: el ComposeView se creaba sin attach → Compose lanzaba
     * "Cannot locate windowRecomposer; View is not attached to a window".
     */
    suspend fun renderBitmap(
        context: Context,
        width: Int = 1080,
        height: Int = 1920,
        content: @Composable () -> Unit
    ): Bitmap = withContext(Dispatchers.Main) {
        val activity = context.findActivity()
            ?: error("renderBitmap requiere un Context de Activity")
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
            ?: error("No se pudo localizar android.R.id.content")

        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            // Offscreen pero adjunto: alpha 0 + posición fuera de pantalla para no parpadear.
            alpha = 0f
            layoutParams = ViewGroup.LayoutParams(width, height)
            translationX = -width.toFloat() * 2f
            setContent { MaterialTheme { content() } }
        }
        root.addView(composeView)

        try {
            // Esperar a que el árbol esté compuesto + medido. onPreDraw se dispara
            // al primer frame de dibujo después de la composición inicial.
            suspendCancellableCoroutine<Unit> { cont ->
                val listener = object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        composeView.viewTreeObserver.removeOnPreDrawListener(this)
                        if (cont.isActive) cont.resume(Unit)
                        return true
                    }
                }
                composeView.viewTreeObserver.addOnPreDrawListener(listener)
                cont.invokeOnCancellation {
                    try { composeView.viewTreeObserver.removeOnPreDrawListener(listener) } catch (_: Exception) {}
                }
            }

            val w = if (composeView.width > 0) composeView.width else width
            val h = if (composeView.height > 0) composeView.height else height
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            composeView.draw(canvas)
            bitmap
        } finally {
            try { root.removeView(composeView) } catch (_: Exception) {}
        }
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap): android.net.Uri {
        val file = File(context.cacheDir, "opofit_share_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareGeneric(context: Context, bitmap: Bitmap, titulo: String = "Compartir entrenamiento") {
        val uri = saveBitmap(context, bitmap)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir en…"))
    }

    /** Comprueba si una app concreta está instalada (Android 11+ requiere queries en el manifest). */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isInstagramInstalled(context: Context): Boolean = isAppInstalled(context, "com.instagram.android")
    fun isWhatsAppInstalled(context: Context): Boolean =
        isAppInstalled(context, "com.whatsapp") || isAppInstalled(context, "com.whatsapp.w4b")

    fun shareInstagramStory(context: Context, bitmap: Bitmap): Boolean {
        val uri = saveBitmap(context, bitmap)
        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(uri, "image/*")
            putExtra("interactive_asset_uri", uri)
            putExtra("source_application", context.packageName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else false
    }

    fun shareWhatsApp(context: Context, bitmap: Bitmap, texto: String = ""): Boolean {
        val uri = saveBitmap(context, bitmap)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (texto.isNotBlank()) putExtra(Intent.EXTRA_TEXT, texto)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else false
    }
}
