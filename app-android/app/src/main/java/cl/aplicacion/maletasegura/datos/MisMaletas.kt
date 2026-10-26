package cl.aplicacion.maletasegura.datos

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MaletaGuardada(val direccion: String, val nombre: String)

object MisMaletas {
    private var correo: String? = null

    private val _lista = MutableStateFlow<List<MaletaGuardada>>(emptyList())
    val lista: StateFlow<List<MaletaGuardada>> = _lista

    fun cargar(correoUsuario: String) {
        correo = correoUsuario
        _lista.value = leer()
    }

    fun agregar(direccion: String, nombre: String) {
        val c = correo ?: return
        val fila = ContentValues().apply {
            put("correo", c)
            put("direccion", direccion)
            put("nombre", nombre.trim().ifBlank { "Maleta" })
        }
        BaseDatos.db.insertWithOnConflict("maletas", null, fila, SQLiteDatabase.CONFLICT_REPLACE)
        _lista.value = leer()
    }

    fun olvidar(direccion: String) {
        val c = correo ?: return
        BaseDatos.db.delete("maletas", "correo = ? AND direccion = ?", arrayOf(c, direccion))
        _lista.value = leer()
    }

    fun nombreDe(direccion: String): String =
        _lista.value.firstOrNull { it.direccion == direccion }?.nombre ?: "Maleta"

    private fun leer(): List<MaletaGuardada> {
        val c = correo ?: return emptyList()
        val lista = mutableListOf<MaletaGuardada>()
        BaseDatos.db.rawQuery("SELECT direccion, nombre FROM maletas WHERE correo = ? ORDER BY rowid", arrayOf(c)).use { fila ->
            while (fila.moveToNext()) lista += MaletaGuardada(fila.getString(0), fila.getString(1))
        }
        return lista
    }
}
