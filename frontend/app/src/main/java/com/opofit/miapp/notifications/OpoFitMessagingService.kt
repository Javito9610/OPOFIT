package com.opofit.miapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.opofit.miapp.R
import com.opofit.miapp.ui.MainActivity

/**
 * Receptor FCM con múltiples canales según el `data.canal` del backend:
 *
 *   - "comunidad_messages": HIGH priority. Mensajes de grupo, solicitudes de
 *     amistad, invitaciones, mensajes directos. Vibra y se replica al reloj
 *     vía bridge nativo de Android (Wear OS, Zepp, Samsung Health…).
 *   - "opofit_recordatorios": DEFAULT. Recordatorios de entreno diarios.
 *   - "opofit_general": LOW. Genérico para info pasiva.
 *
 * Antes había un solo canal DEFAULT y los mensajes urgentes (alguien te
 * escribe) salían igual de silenciosos que los pasivos.
 */
class OpoFitMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "OpoFit"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val canal = message.data["canal"] ?: "opofit_general"
        val tipo = message.data["tipo"]
        showNotification(title, body, canal, tipo, message.data)
    }

    private fun showNotification(
        title: String,
        body: String,
        canal: String,
        tipo: String?,
        data: Map<String, String>
    ) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(nm)

        // Prioridad del Builder según el canal.
        val priority = when (canal) {
            "comunidad_messages" -> NotificationCompat.PRIORITY_HIGH
            "opofit_recordatorios" -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
        // Categoría adecuada → ayuda al sistema a decidir si replicarla al reloj.
        val category = when (tipo) {
            "mensaje_grupo", "mensaje_directo" -> NotificationCompat.CATEGORY_MESSAGE
            "solicitud_amistad", "invitacion_grupo", "amistad_aceptada" -> NotificationCompat.CATEGORY_SOCIAL
            "recordatorio_entreno" -> NotificationCompat.CATEGORY_REMINDER
            else -> NotificationCompat.CATEGORY_PROMO
        }

        // Tap → abrir MainActivity con extras para que la app pueda navegar.
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (k, v) -> putExtra("notif_$k", v) }
        }
        val pi = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, canal)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setCategory(category)
            .setAutoCancel(true)
            .setContentIntent(pi)
            // setOnlyAlertOnce(false) en mensajes para que cada nuevo mensaje
            // sí alerte (el reloj entonces vibra de nuevo).
            .setOnlyAlertOnce(canal != "comunidad_messages")
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannels(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        listOf(
            ChannelDef(
                id = "comunidad_messages",
                nombre = "Mensajes y comunidad",
                desc = "Mensajes de grupo, amigos e invitaciones. Sonido + vibración.",
                importance = NotificationManager.IMPORTANCE_HIGH
            ),
            ChannelDef(
                id = "opofit_recordatorios",
                nombre = "Recordatorios de entreno",
                desc = "Aviso diario para hacer la sesión de hoy.",
                importance = NotificationManager.IMPORTANCE_DEFAULT
            ),
            ChannelDef(
                id = "opofit_general",
                nombre = "Información general",
                desc = "Noticias, novedades de la app.",
                importance = NotificationManager.IMPORTANCE_LOW
            )
        ).forEach { c ->
            if (nm.getNotificationChannel(c.id) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(c.id, c.nombre, c.importance).apply {
                        description = c.desc
                        enableLights(true)
                        enableVibration(c.importance >= NotificationManager.IMPORTANCE_DEFAULT)
                    }
                )
            }
        }
    }

    private data class ChannelDef(
        val id: String,
        val nombre: String,
        val desc: String,
        val importance: Int
    )
}
