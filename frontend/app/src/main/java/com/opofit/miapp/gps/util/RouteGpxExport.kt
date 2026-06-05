package com.opofit.miapp.gps.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.opofit.miapp.gps.service.PlannedRoute
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object RouteGpxExport {
    private val ISO: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun build(route: PlannedRoute): String {
        val now = ISO.format(Date())
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append(
            """<gpx version="1.1" creator="OpoFit" xmlns="http://www.topografix.com/GPX/1/1">"""
        ).append('\n')
        sb.append("  <metadata><name>${escape(route.nombre)}</name><time>$now</time></metadata>\n")
        sb.append("  <trk><name>${escape(route.nombre)}</name><trkseg>\n")
        for (p in route.puntos) {
            sb.append("    <trkpt lat=\"%.7f\" lon=\"%.7f\"><time>$now</time></trkpt>\n".format(Locale.US, p.lat, p.lng))
        }
        sb.append("  </trkseg></trk>\n</gpx>")
        return sb.toString()
    }

    fun shareIntent(context: Context, route: PlannedRoute): Intent? {
        if (route.puntos.size < 2) return null
        val dir = File(context.filesDir, "route_exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(dir, "opofit_ruta_${stamp}.gpx")
        file.writeText(build(route))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, route.nombre)
        }
    }

    private fun escape(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
