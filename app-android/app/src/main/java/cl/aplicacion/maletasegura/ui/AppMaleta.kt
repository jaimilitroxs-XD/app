package cl.aplicacion.maletasegura.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aplicacion.maletasegura.R
import cl.aplicacion.maletasegura.ble.Conexion
import cl.aplicacion.maletasegura.ble.EstadoAlarma
import cl.aplicacion.maletasegura.ble.EstadoMaleta
import cl.aplicacion.maletasegura.ble.MaletaBle
import cl.aplicacion.maletasegura.ble.Protocolo
import cl.aplicacion.maletasegura.datos.Cuentas
import cl.aplicacion.maletasegura.datos.Historial
import cl.aplicacion.maletasegura.datos.MisMaletas
import kotlinx.coroutines.delay

enum class Seccion(val titulo: String, val icono: Int) {
    MALETAS("Maletas", R.drawable.ic_nav_maletas),
    ALERTAS("Alertas", R.drawable.ic_nav_alertas),
    PERFIL("Perfil", R.drawable.ic_nav_perfil),
}

@Composable
fun AppMaleta(permisosOk: Boolean, onPedirPermisos: () -> Unit) {
    val usuario by Cuentas.usuario.collectAsState()
    val aviso by MaletaBle.aviso.collectAsState()

    Surface(color = Colores.Fondo, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            val u = usuario
            when {
                !permisosOk -> PantallaPermisos(onPedirPermisos)
                u == null -> PantallasDeAcceso()
                else -> PantallaPrincipal(u.nombre.substringBefore(" "))
            }
            Aviso(aviso)
        }
    }
}

@Composable
private fun PantallasDeAcceso() {
    var registrando by rememberSaveable { mutableStateOf(false) }
    if (registrando) {
        PantallaRegistro(
            onRegistrar = { nombre, correo, clave, repetir -> Cuentas.registrar(nombre, correo, clave, repetir) },
            onIrALogin = { registrando = false },
        )
    } else {
        PantallaLogin(
            onIngresar = { correo, clave -> Cuentas.iniciarSesion(correo, clave) },
            onIrARegistro = { registrando = true },
        )
    }
}

@Composable
private fun PantallaPrincipal(primerNombre: String) {
    val maletas by MisMaletas.lista.collectAsState()
    val estados by MaletaBle.estados.collectAsState()
    val eventos by Historial.eventos.collectAsState()
    val encontrados by MaletaBle.encontrados.collectAsState()
    val buscando by MaletaBle.buscando.collectAsState()

    var seccion by rememberSaveable { mutableStateOf(Seccion.MALETAS) }
    var agregando by rememberSaveable { mutableStateOf(false) }
    var abierta by rememberSaveable { mutableStateOf<String?>(null) }  // dirección de la maleta abierta

    LaunchedEffect(Unit) { MaletaBle.conectarTodas(maletas.map { it.direccion }) }

    val enAlarma = maletas.firstOrNull {
        val e = estados[it.direccion]
        e?.conexion == Conexion.CONECTADO && e.alarma == EstadoAlarma.ALARMA
    }
    if (enAlarma != null) {
        PantallaAlarma(
            nombre = enAlarma.nombre,
            estado = estados[enAlarma.direccion] ?: EstadoMaleta(),
            otrasVigiladas = maletas.count { it != enAlarma && estados[it.direccion]?.conexion == Conexion.CONECTADO },
            onSilenciar = { MaletaBle.enviar(enAlarma.direccion, Protocolo.BUZZER_OFF) },
            onDesarmar = { MaletaBle.enviar(enAlarma.direccion, Protocolo.DESARMAR) },
        )
        return
    }

    Scaffold(
        containerColor = Colores.Fondo,
        bottomBar = {
            // La barra de menú inferior (Material 3).
            NavigationBar(containerColor = Colores.Tarjeta) {
                Seccion.entries.forEach { s ->
                    NavigationBarItem(
                        selected = seccion == s,
                        onClick = { seccion = s; agregando = false; abierta = null },
                        icon = { Icon(painterResource(s.icono), contentDescription = null) },
                        label = { Text(s.titulo) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Colores.Azul,
                            selectedTextColor = Colores.Azul,
                            indicatorColor = Colores.AzulClaro,
                            unselectedIconColor = Colores.TextoSuave,
                            unselectedTextColor = Colores.TextoSuave,
                        ),
                    )
                }
            }
        },
    ) { margenes ->
        Box(Modifier.padding(margenes).fillMaxSize()) {
            when (seccion) {
                Seccion.MALETAS -> {
                    val maletaAbierta = maletas.firstOrNull { it.direccion == abierta }
                    when {
                        agregando -> PantallaAgregar(
                            encontrados = encontrados.filter { e -> maletas.none { it.direccion == e.direccion } },
                            buscando = buscando,
                            nombreSugerido = "Maleta ${maletas.size + 1}",
                            onBuscar = { MaletaBle.buscar() },
                            onVincular = { dir, nombre ->
                                MisMaletas.agregar(dir, nombre)
                                MaletaBle.conectar(dir)
                                agregando = false
                            },
                            onVolver = { MaletaBle.dejarDeBuscar(); agregando = false },
                        )
                        maletaAbierta != null -> PantallaDetalle(
                            maleta = maletaAbierta,
                            estado = estados[maletaAbierta.direccion] ?: EstadoMaleta(),
                            ultimoEvento = eventos.firstOrNull { it.maleta == maletaAbierta.nombre },
                            onEnviar = { MaletaBle.enviar(maletaAbierta.direccion, it) },
                            onReconectar = { MaletaBle.conectar(maletaAbierta.direccion) },
                            onOlvidar = {
                                MaletaBle.desconectar(maletaAbierta.direccion)
                                MisMaletas.olvidar(maletaAbierta.direccion)
                                abierta = null
                            },
                            onVolver = { abierta = null },
                        )
                        else -> PantallaMaletas(
                            nombreUsuario = primerNombre,
                            maletas = maletas,
                            estados = estados,
                            onAbrir = { abierta = it },
                            onReconectar = { MaletaBle.conectar(it) },
                            onAgregar = { agregando = true },
                        )
                    }
                }
                Seccion.ALERTAS -> PantallaAlertas(eventos = eventos, onBorrar = { Historial.borrar() })
                Seccion.PERFIL -> {
                    val usuario by Cuentas.usuario.collectAsState()
                    usuario?.let { u ->
                        PantallaPerfil(
                            usuario = u,
                            cantidadMaletas = maletas.size,
                            onCerrarSesion = {
                                MaletaBle.desconectarTodas()
                                Cuentas.cerrarSesion()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Aviso(texto: String?) {
    if (texto == null) return
    LaunchedEffect(texto) {
        delay(4000)
        MaletaBle.limpiarAviso()
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(bottom = 88.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(
            color = Colores.Tinta,
            shape = Redondeado,
            modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { MaletaBle.limpiarAviso() },
        ) {
            Text(texto, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(16.dp))
        }
    }
}
