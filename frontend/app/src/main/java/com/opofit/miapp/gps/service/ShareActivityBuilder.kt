package com.opofit.miapp.gps.service

import com.opofit.miapp.data.responsemodels.PostStats
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.utils.SegmentSlugUtil

fun buildPendingShareFromEntreno(
    titulo: String,
    idHistorial: Int,
    duracionMin: Int,
    ejerciciosCount: Int
): PendingShare {
    val duracionSec = duracionMin.coerceAtLeast(1) * 60
    return PendingShare(
        fuente = "ENTRENO",
        idHistorialSesion = idHistorial,
        tituloSugerido = titulo.ifBlank { "Entrenamiento" },
        stats = PostStats(
            tipo = "ENTRENO",
            duracionSec = duracionSec,
            ejercicios = ejerciciosCount
        )
    )
}

fun buildPendingShareFromGps(activity: ActivitySummary): PendingShare {
    val segmentSlugs = activity.bestSegments.mapNotNull { seg ->
        val slug = SegmentSlugUtil.slugFromLabel(seg.label) ?: return@mapNotNull null
        slug to (seg.durationSec * 1000L)
    }
    return PendingShare(
        fuente = "GPS",
        gpsUuid = activity.id,
        tituloSugerido = "${activity.type.display} · ${"%.1f".format(activity.distanceM / 1000)} km",
        stats = PostStats(
            tipo = activity.type.name,
            distanciaM = activity.distanceM,
            duracionSec = activity.durationSec,
            ritmoMedioSpkm = activity.avgPaceSecPerKm,
            desnivelM = activity.elevationGainM,
            avgHrBpm = activity.avgHrBpm,
            kcal = activity.kcal
        ),
        segmentSlugs = segmentSlugs
    )
}
