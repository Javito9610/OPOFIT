package com.opofit.miapp.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareCardExport {

    fun renderBitmap(context: Context, width: Int = 1080, height: Int = 1920, content: @Composable () -> Unit): Bitmap {
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MaterialTheme { content() } }
            layoutParams = ViewGroup.LayoutParams(width, height)
        }
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        composeView.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        composeView.draw(canvas)
        return bitmap
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
