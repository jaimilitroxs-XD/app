package cl.aplicacion.maletasegura.servicio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cl.aplicacion.maletasegura.MainActivity
import cl.aplicacion.maletasegura.R

object Notificaciones {
    const val CANAL_ALERTAS = "alertas"  // con sonido y vibración
    const val CANAL_SERVICIO = "servicio"  // aviso fijo y silencioso del servicio

    private val VIBRACION = longArrayOf(0, 600, 250, 600, 250, 900)  // espera, vibra, pausa, vibra…
    private var siguienteId = 100

    fun crearCanales(context: Context) {
        val alertas = NotificationChannel(CANAL_ALERTAS, "Alertas de la maleta", NotificationManager.IMPORTANCE_HIGH)
        alertas.enableVibration(true)
        alertas.vibrationPattern = VIBRACION
        alertas.setSound(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
        )
        val servicio = NotificationChannel(CANAL_SERVICIO, "Vigilancia activa", NotificationManager.IMPORTANCE_LOW)

        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(alertas, servicio))
    }

    fun abrirApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun alerta(context: Context, titulo: String, texto: String) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val notificacion = NotificationCompat.Builder(context, CANAL_ALERTAS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(abrirApp(context))
            .build()
        NotificationManagerCompat.from(context).notify(siguienteId++, notificacion)
    }
}
