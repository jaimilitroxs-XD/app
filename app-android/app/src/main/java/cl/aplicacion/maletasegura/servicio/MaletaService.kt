package cl.aplicacion.maletasegura.servicio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cl.aplicacion.maletasegura.R

class MaletaService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val aviso = NotificationCompat.Builder(this, Notificaciones.CANAL_SERVICIO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Vigilando tu maleta")
            .setContentText("Recibirás una alerta si se abre o se mueve")
            .setOngoing(true)
            .setContentIntent(Notificaciones.abrirApp(this))
            .build()
        startForeground(1, aviso, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun iniciar(context: Context) {
            runCatching { context.startForegroundService(Intent(context, MaletaService::class.java)) }
        }

        fun detener(context: Context) {
            context.stopService(Intent(context, MaletaService::class.java))
        }
    }
}
