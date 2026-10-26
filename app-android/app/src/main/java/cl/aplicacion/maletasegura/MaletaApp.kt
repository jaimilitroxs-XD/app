package cl.aplicacion.maletasegura

import android.app.Application
import cl.aplicacion.maletasegura.ble.MaletaBle
import cl.aplicacion.maletasegura.datos.BaseDatos
import cl.aplicacion.maletasegura.datos.Cuentas
import cl.aplicacion.maletasegura.servicio.Notificaciones

class MaletaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BaseDatos.iniciar(this)  // abre (o crea) la base SQLite
        Cuentas.iniciar(this)
        Notificaciones.crearCanales(this)
        MaletaBle.iniciar(this)
    }
}
