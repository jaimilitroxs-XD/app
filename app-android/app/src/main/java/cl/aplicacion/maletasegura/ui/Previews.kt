package cl.aplicacion.maletasegura.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cl.aplicacion.maletasegura.ble.Conexion
import cl.aplicacion.maletasegura.ble.Encontrado
import cl.aplicacion.maletasegura.ble.EstadoAlarma
import cl.aplicacion.maletasegura.ble.EstadoMaleta
import cl.aplicacion.maletasegura.datos.Evento
import cl.aplicacion.maletasegura.datos.MaletaGuardada
import cl.aplicacion.maletasegura.datos.TipoEvento
import cl.aplicacion.maletasegura.datos.Usuario
import java.util.Calendar

private fun hoyA(hora: Int, minuto: Int): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, hora)
    set(Calendar.MINUTE, minuto)
}.timeInMillis

private val grande = MaletaGuardada("8C:4B:14:01:2F:26", "Maleta grande")
private val mochila = MaletaGuardada("8C:4B:14:02:A1:07", "Mochila")
private val cabina = MaletaGuardada("8C:4B:14:03:5C:E2", "Maleta de cabina")
private val maletasEjemplo = listOf(grande, mochila, cabina)

private val estadosEjemplo = mapOf(
    grande.direccion to EstadoMaleta(Conexion.CONECTADO, EstadoAlarma.ARMADA, maletaCerrada = true, ledManual = true),
    mochila.direccion to EstadoMaleta(Conexion.CONECTADO, EstadoAlarma.DESARMADA, maletaCerrada = false),
    cabina.direccion to EstadoMaleta(Conexion.DESCONECTADO),
)

private val eventosEjemplo = listOf(
    Evento(hoyA(14, 49), TipoEvento.ALARMA, "Alarma desarmada", "Desde el celular", "Maleta grande"),
    Evento(hoyA(14, 47), TipoEvento.ALERTA, "Se abrió tu maleta", "Alerta enviada al celular", "Maleta grande"),
    Evento(hoyA(14, 32), TipoEvento.ALARMA, "Alarma armada", "Maleta cerrada", "Maleta grande"),
    Evento(hoyA(12, 40), TipoEvento.CONEXION, "Conexión perdida", "Fuera de alcance o sin batería", "Maleta de cabina"),
    Evento(hoyA(11, 5), TipoEvento.ALERTA, "Se detectó movimiento en tu maleta", "Alerta enviada al celular", "Mochila"),
    Evento(hoyA(9, 12), TipoEvento.CONEXION, "Maleta vinculada", "PIN aceptado", "Mochila"),
)

@Composable
private fun Marco(contenido: @Composable () -> Unit) {
    MaletaTema {
        Surface(color = Colores.Fondo, modifier = Modifier.fillMaxSize()) { contenido() }
    }
}

@Preview(name = "1 · Iniciar sesión", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VistaPreviaLogin() = Marco { PantallaLogin(onIngresar = { _, _ -> null }, onIrARegistro = {}) }

@Preview(name = "2 · Crear cuenta", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VistaPreviaRegistro() = Marco { PantallaRegistro(onRegistrar = { _, _, _, _ -> null }, onIrALogin = {}) }

@Preview(name = "3 · Mis maletas", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VistaPreviaMaletas() = Marco {
    PantallaMaletas("Camila", maletasEjemplo, estadosEjemplo, {}, {}, {})
}

@Preview(name = "4 · Agregar maleta", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VistaPreviaAgregar() = Marco {
    PantallaAgregar(
        encontrados = listOf(
            Encontrado("8C:4B:14:04:A1:07", "MaletaSegura", vinculada = false),
            Encontrado("8C:4B:14:05:5C:E2", "MaletaSegura", vinculada = false),
        ),
        buscando = false,
        nombreSugerido = "Maleta 4",
        onBuscar = {}, onVincular = { _, _ -> }, onVolver = {},
    )
}

@Preview(name = "5 · Detalle de maleta", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VistaPreviaDetalle() = Marco {
    PantallaDetalle(grande, estadosEjemplo.getValue(grande.direccion), eventosEjemplo[2], {}, {}, {}, {})
}

@Preview(name = "6 · Alarma", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VistaPreviaAlarma() = Marco {
    PantallaAlarma(
        nombre = "Maleta grande",
        estado = EstadoMaleta(Conexion.CONECTADO, EstadoAlarma.ALARMA, maletaCerrada = false, ultimaAlerta = "APERTURA"),
        otrasVigiladas = 2, onSilenciar = {}, onDesarmar = {},
    )
}

@Preview(name = "7 · Alertas", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VistaPreviaAlertas() = Marco { PantallaAlertas(eventosEjemplo, onBorrar = {}) }

@Preview(name = "8 · Perfil", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VistaPreviaPerfil() = Marco {
    PantallaPerfil(Usuario("Camila Rojas", "camila@correo.cl"), 3, {})
}

@Preview(name = "0 · Permisos", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VistaPreviaPermisos() = Marco { PantallaPermisos(onPedirPermisos = {}) }
