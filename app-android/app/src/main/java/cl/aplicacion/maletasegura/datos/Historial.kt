package cl.aplicacion.maletasegura.datos

import android.content.ContentValues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class TipoEvento { ALERTA, ALARMA, CONEXION }

data class Evento(
    val momento: Long,
    val tipo: TipoEvento,
    val titulo: String,
    val detalle: String,
    val maleta: String,
)

object Historial {
    private const val MAX_EVENTOS = 200

    private var correo: String? = null

    private val _eventos = MutableStateFlow<List<Evento>>(emptyList())
    val eventos: StateFlow<List<Evento>> = _eventos

    fun cargar(correoUsuario: String) {
        correo = correoUsuario
        _eventos.value = leer()
    }

    @Synchronized
    fun agregar(tipo: TipoEvento, titulo: String, detalle: String, maleta: String) {
        val c = correo ?: return
        val fila = ContentValues().apply {
            put("correo", c)
            put("momento", System.currentTimeMillis())
            put("tipo", tipo.name)
            put("titulo", titulo)
            put("detalle", detalle)
            put("maleta", maleta)
        }
        BaseDatos.db.insert("eventos", null, fila)
        BaseDatos.db.execSQL(
            "DELETE FROM eventos WHERE correo = ? AND id NOT IN " +
                "(SELECT id FROM eventos WHERE correo = ? ORDER BY id DESC LIMIT $MAX_EVENTOS)",
            arrayOf(c, c),
        )
        _eventos.value = leer()
    }

    @Synchronized
    fun borrar() {
        val c = correo ?: return
        BaseDatos.db.delete("eventos", "correo = ?", arrayOf(c))
        _eventos.value = emptyList()
    }

    private fun leer(): List<Evento> {
        val c = correo ?: return emptyList()
        val lista = mutableListOf<Evento>()
        BaseDatos.db.rawQuery(
            "SELECT momento, tipo, titulo, detalle, maleta FROM eventos WHERE correo = ? ORDER BY id DESC",
            arrayOf(c),
        ).use { fila ->
            while (fila.moveToNext()) {
                lista += Evento(
                    fila.getLong(0),
                    TipoEvento.valueOf(fila.getString(1)),
                    fila.getString(2),
                    fila.getString(3),
                    fila.getString(4),
                )
            }
        }
        return lista
    }
}
