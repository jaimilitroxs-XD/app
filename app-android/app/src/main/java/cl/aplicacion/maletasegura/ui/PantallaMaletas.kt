package cl.aplicacion.maletasegura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aplicacion.maletasegura.ble.Conexion
import cl.aplicacion.maletasegura.ble.EstadoAlarma
import cl.aplicacion.maletasegura.ble.EstadoMaleta
import cl.aplicacion.maletasegura.datos.MaletaGuardada

@Composable
fun PantallaMaletas(
    nombreUsuario: String,
    maletas: List<MaletaGuardada>,
    estados: Map<String, EstadoMaleta>,
    onAbrir: (direccion: String) -> Unit,
    onReconectar: (direccion: String) -> Unit,
    onAgregar: () -> Unit,
) {
    val conectadas = maletas.count { estados[it.direccion]?.conexion == Conexion.CONECTADO }
    val armadas = maletas.count { estados[it.direccion]?.alarma == EstadoAlarma.ARMADA }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text("Hola, $nombreUsuario", color = Colores.TextoSuave, fontSize = 14.sp)
            Titulo("Mis maletas")
            Text("${maletas.size} maletas · $conectadas conectadas · $armadas armadas", color = Colores.TextoSuave, fontSize = 14.sp)
        }

        if (maletas.isEmpty()) {
            Text("Todavía no tienes maletas. Toca \"Agregar maleta\" para vincular la primera.", color = Colores.TextoSuave)
        }

        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(maletas, key = { it.direccion }) { maleta ->
                TarjetaMaleta(
                    maleta = maleta,
                    estado = estados[maleta.direccion] ?: EstadoMaleta(),
                    onAbrir = { onAbrir(maleta.direccion) },
                    onReconectar = { onReconectar(maleta.direccion) },
                )
            }
        }

        OutlinedButton(
            onClick = onAgregar,
            shape = Redondeado,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("+  Agregar maleta", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun TarjetaMaleta(maleta: MaletaGuardada, estado: EstadoMaleta, onAbrir: () -> Unit, onReconectar: () -> Unit) {
    val conectada = estado.conexion == Conexion.CONECTADO
    val colorBarra = when {
        estado.alarma == EstadoAlarma.ALARMA -> Colores.Rojo
        !conectada -> Colores.Rojo
        estado.alarma == EstadoAlarma.ARMADA -> Colores.Azul
        else -> Colores.TextoSuave
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(Redondeado)
            .background(Colores.Tarjeta)
            .border(1.dp, Colores.Borde, Redondeado)
            .clickable(onClick = onAbrir),
    ) {
        Box(Modifier.width(6.dp).fillMaxHeight().background(colorBarra))
        Row(
            Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(maleta.nombre, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    when (estado.conexion) {
                        Conexion.CONECTADO -> {
                            EtiquetaAlarma(estado.alarma)
                            Text("● Conectada", color = Colores.Verde, fontSize = 12.sp)
                            estado.maletaCerrada?.let { Text(if (it) "· Cerrada" else "· Abierta", color = Colores.TextoSuave, fontSize = 12.sp) }
                        }
                        Conexion.VINCULANDO -> Text("Esperando el PIN…", color = Colores.Azul, fontSize = 12.sp)
                        Conexion.CONECTANDO -> Text("Conectando…", color = Colores.Azul, fontSize = 12.sp)
                        Conexion.DESCONECTADO -> Text("● Sin conexión", color = Colores.Rojo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (estado.conexion == Conexion.DESCONECTADO) {
                Text(
                    "Reconectar", color = Colores.Azul, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onReconectar).padding(8.dp),
                )
            } else {
                Text("›", fontSize = 24.sp, color = Colores.TextoSuave)
            }
        }
    }
}

@Composable
private fun EtiquetaAlarma(alarma: EstadoAlarma) {
    val (texto, fondo, color) = when (alarma) {
        EstadoAlarma.ARMADA -> Triple("Armada", Colores.Azul, Color.White)
        EstadoAlarma.ALARMA -> Triple("¡Alarma!", Colores.Rojo, Color.White)
        EstadoAlarma.DESARMADA -> Triple("Desarmada", Color(0xFFECEAE4), Colores.Tinta)
        EstadoAlarma.DESCONOCIDO -> Triple("…", Color(0xFFECEAE4), Colores.Tinta)
    }
    Text(
        texto, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(fondo, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
