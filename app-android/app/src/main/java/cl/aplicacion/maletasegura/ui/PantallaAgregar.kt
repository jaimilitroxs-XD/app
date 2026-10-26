package cl.aplicacion.maletasegura.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aplicacion.maletasegura.ble.Encontrado

@Composable
fun PantallaAgregar(
    encontrados: List<Encontrado>,
    buscando: Boolean,
    nombreSugerido: String,
    onBuscar: () -> Unit,
    onVincular: (direccion: String, nombre: String) -> Unit,
    onVolver: () -> Unit,
) {
    BackHandler(onBack = onVolver)
    var elegida by remember { mutableStateOf<String?>(null) }
    var nombre by remember { mutableStateOf(nombreSugerido) }

    val esVistaPrevia = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!esVistaPrevia) onBuscar() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonVolver(onVolver)
            Titulo("Agregar maleta")
        }

        Row(
            Modifier.fillMaxWidth().background(Colores.AzulClaro, Redondeado).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (buscando) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            Text(
                if (buscando) "Buscando maletas cercanas… Enciende la maleta y acércala al celular."
                else "Búsqueda terminada.",
                color = Colores.Azul, fontSize = 14.sp, modifier = Modifier.weight(1f),
            )
            if (!buscando) Text("Buscar", color = Colores.Azul, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onBuscar))
        }

        Etiqueta("Maletas encontradas")
        if (encontrados.isEmpty() && !buscando) {
            Text("No se encontraron maletas nuevas.", color = Colores.TextoSuave)
        }
        encontrados.forEach { m ->
            val seleccionada = elegida == m.direccion
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Colores.Tarjeta, Redondeado)
                    .border(if (seleccionada) 2.dp else 1.dp, if (seleccionada) Colores.Azul else Colores.Borde, Redondeado)
                    .clickable { elegida = m.direccion }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = seleccionada, onClick = { elegida = m.direccion })
                Column {
                    Text("${m.nombre} · …${m.direccion.takeLast(5)}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(if (m.vinculada) "Ya vinculada con este celular" else "Nueva · pedirá el PIN", color = Colores.TextoSuave, fontSize = 12.sp)
                }
            }
        }

        CampoTexto("Nombre para identificarla", nombre, { nombre = it })
        Text("🔒 Android te pedirá el PIN de 6 dígitos de la maleta solo la primera vez. La conexión queda cifrada.", color = Colores.TextoSuave, fontSize = 13.sp)

        Spacer(Modifier.weight(1f))
        val dir = elegida
        BotonPrincipal(if (dir == null) "Elige una maleta" else "Vincular y agregar") {
            if (dir != null) onVincular(dir, nombre)
        }
    }
}
