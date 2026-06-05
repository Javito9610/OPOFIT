package com.opofit.miapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImagePickerUtil {
    fun uriToBitmap(context: Context, uri: Uri, maxSidePx: Int = 1200): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val sample = max(1, max(bounds.outWidth, bounds.outHeight) / maxSidePx)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null
            scaleBitmap(bitmap, maxSidePx)
        } catch (_: Exception) {
            null
        }
    }

    fun bitmapToJpegBase64(bitmap: Bitmap): String? {
        return try {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Exception) {
            null
        }
    }

    fun uriToJpegBase64(context: Context, uri: Uri, maxSidePx: Int = 800): String? {
        val scaled = uriToBitmap(context, uri, maxSidePx) ?: return null
        val b64 = bitmapToJpegBase64(scaled)
        scaled.recycle()
        return b64
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSidePx: Int): Bitmap {
        if (bitmap.width <= maxSidePx && bitmap.height <= maxSidePx) return bitmap
        val ratio = maxSidePx.toFloat() / max(bitmap.width, bitmap.height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        ).also { if (it !== bitmap) bitmap.recycle() }
    }
}
