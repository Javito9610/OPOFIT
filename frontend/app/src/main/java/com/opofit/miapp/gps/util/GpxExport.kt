package com.opofit.miapp.gps.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.opofit.miapp.gps.model.ActivitySummary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Genera un fichero GPX 1.1 estándar (compatible con Strava, Wikiloc, Komoot,
 * Garmin Connect, etc.) y lanza un share sheet.
 */
object GpxExport {
    private val ISO: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun build(activity: ActivitySummary): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append(
            """<gpx version="1.1" creator="OpoFit" xmlns="http://www.topografix.com/GPX/1/1" """ +
                """xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v2">"""
        ).append('\n')
        sb.append("  <metadata>\n")
        sb.append("    <name>${escape(activity.type.display)} OpoFit</name>\n")
        sb.append("    <time>${ISO.format(Date(activity.startedAtMs))}</time>\n")
        sb.append("  </metadata>\n")
        sb.append("  <trk>\n")
        sb.append("    <name>${escape(activity.type.display)} ${ISO.format(Date(activity.startedAtMs))}</name>\n")
        sb.append("    <type>${activity.type.name.lowercase()}</type>\n")
        sb.append("    <trkseg>\n")
        for (p in activity.points) {
            sb.append("      <trkpt lat=\"%.7f\" lon=\"%.7f\">".format(Locale.US, p.lat, p.lng)).append('\n')
            p.altitude?.let { sb.append("        <ele>%.2f</ele>".format(Locale.US, it)).append('\n') }
            sb.append("        <time>${ISO.format(Date(p.timestampMs))}</time>\n")
            if (p.hrBpm != null || p.cadenceSpm != null) {
                sb.append("        <extensions><gpxtpx:TrackPointExtension>\n")
                p.hrBpm?.let { sb.append("          <gpxtpx:hr>$it</gpxtpx:hr>\n") }
                p.cadenceSpm?.let { sb.append("          <gpxtpx:cad>$it</gpxtpx:cad>\n") }
                sb.append("        </gpxtpx:TrackPointExtension></extensions>\n")
            }
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n  </trk>\n</gpx>\n")
        return sb.toString()
    }

    fun shareIntent(context: Context, activity: ActivitySummary): Intent? {
        if (activity.points.isEmpty()) return null
        val dir = File(context.filesDir, "gps_exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(activity.startedAtMs))
        val file = File(dir, "opofit_${activity.type.name.lowercase()}_${stamp}.gpx")
        file.writeText(build(activity))
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Actividad OpoFit (GPX)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
