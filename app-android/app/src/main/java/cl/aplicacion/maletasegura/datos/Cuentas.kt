package cl.aplicacion.maletasegura.datos

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class Usuario(val nombre: String, val correo: String)

private class CuentaGuardada(val nombre: String, val sal: ByteArray, val hash: ByteArray)

object Cuentas {
    private const val ITERACIONES = 120_000
    private const val LARGO_HASH = 256

    private lateinit var prefs: SharedPreferences

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario

    fun iniciar(context: Context) {
        prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE)
        // Si había una sesión abierta, se recupera.
        val correo = prefs.getString("correo", null)
        if (correo != null) leerCuenta(correo)?.let { abrirSesion(correo, it.nombre) }
    }

    fun registrar(nombre: String, correo: String, clave: String, repetir: String): String? {
        val c = correo.trim().lowercase()
        if (nombre.isBlank()) return "Escribe tu nombre"
        if (!Patterns.EMAIL_ADDRESS.matcher(c).matches()) return "El correo no es válido"
        if (clave.length < 8) return "La contraseña debe tener al menos 8 caracteres"
        if (clave.none { it.isLetter() } || clave.none { it.isDigit() }) return "La contraseña debe tener letras y números"
        if (clave != repetir) return "Las contraseñas no coinciden"
        if (leerCuenta(c) != null) return "Ya existe una cuenta con ese correo"

        val sal = ByteArray(16)
        SecureRandom().nextBytes(sal)
        val fila = ContentValues().apply {
            put("correo", c)
            put("nombre", nombre.trim())
            put("sal", sal)
            put("hash", calcularHash(clave, sal))
        }
        BaseDatos.db.insert("usuarios", null, fila)
        abrirSesion(c, nombre.trim())
        return null
    }

    fun iniciarSesion(correo: String, clave: String): String? {
        val c = correo.trim().lowercase()
        val cuenta = leerCuenta(c) ?: return "Correo o contraseña incorrectos"

        if (!MessageDigest.isEqual(calcularHash(clave, cuenta.sal), cuenta.hash)) return

        abrirSesion(c, cuenta.nombre)
        return null
    }

    fun cerrarSesion() {
        prefs.edit().remove("correo").apply()
        _usuario.value = null
    }

    private fun abrirSesion(correo: String, nombre: String) {
        prefs.edit().putString("correo", correo).apply()
        MisMaletas.cargar(correo)
        Historial.cargar(correo)
        _usuario.value = Usuario(nombre, correo)
    }

    private fun leerCuenta(correo: String): CuentaGuardada? =
        BaseDatos.db.rawQuery("SELECT nombre, sal, hash FROM usuarios WHERE correo = ?", arrayOf(correo)).use { fila ->
            if (fila.moveToFirst()) CuentaGuardada(fila.getString(0), fila.getBlob(1), fila.getBlob(2)) else null
        }

    private fun calcularHash(clave: String, sal: ByteArray): ByteArray {
        val spec = PBEKeySpec(clave.toCharArray(), sal, ITERACIONES, LARGO_HASH)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}
