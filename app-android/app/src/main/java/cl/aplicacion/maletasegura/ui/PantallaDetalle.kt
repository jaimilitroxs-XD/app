package cl.aplicacion.maletasegura.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aplicacion.maletasegura.ble.Conexion
import cl.aplicacion.maletasegura.ble.EstadoAlarma
import cl.aplicacion.maletasegura.ble.EstadoMaleta
import cl.aplicacion.maletasegura.ble.Protocolo
import cl.aplicacion.maletasegura.datos.Evento
import cl.aplicacion.maletasegura.datos.MaletaGuardada

@Composable
fun PantallaDetalle(
    maleta: MaletaGuardada,
    estado: EstadoMaleta,
    ultimoEvento: Evento?,
    onEnviar: (comando: String) -> Unit,
    onReconectar: () -> Unit,
    onOlvidar: () -> Unit,
    onVolver: () -> Unit,
) {
    BackHandler(onBack = onVolver)
    var confirmarOlvido by remember { mutableStateOf(false) }
    val conectada = estado.conexion == Conexion.CONECTADO

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Encabezado
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonVolver(onVolver)
            Column {
                Text(maleta.nombre, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    if (conectada) "● Conectada · cifrado · …${maleta.direccion.takeLast(5)}" else "● Sin conexión",
                    color = if (conectada) Colores.Verde else Colores.Rojo, fontSize = 13.sp,
                )
            }
        }

        if (!conectada) {
            Tarjeta {
                Text("La maleta no está conectada. Revisa que esté encendida y cerca del celular.", color = Colores.TextoSuave)
                Spacer(Modifier.height(10.dp))
                BotonPrincipal("Reconectar", onReconectar)
            }
        } else {
            // Tarjeta grande con el estado
            val (titulo, color) = when (estado.alarma) {
                EstadoAlarma.ARMADA -> "Armada" to Colores.Azul
                EstadoAlarma.DESARMADA -> "Desarmada" to Colores.TextoSuave
                EstadoAlarma.ALARMA -> "¡Alarma!" to Colores.Rojo
                EstadoAlarma.DESCONOCIDO -> "Consultando…" to Colores.TextoSuave
            }
            val tapa = when (estado.maletaCerrada) { true -> "Cerrada"; false -> "Abierta"; null -> "—" }
            Column(
                Modifier.fillMaxWidth().background(color, RoundedCornerShape(20.dp)).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("ESTADO DE LA ALARMA", color = Color.White, fontSize = 13.sp)
                Text(titulo, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Maleta: $tapa", color = Color.White, fontSize = 16.sp)
            }

            // Armar / Desarmar
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onEnviar(Protocolo.ARMAR) },
                    enabled = estado.alarma != EstadoAlarma.ARMADA,
                    shape = Redondeado,
                    colors = ButtonDefaults.buttonColors(containerColor = Colores.Tinta),
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text("Armar", fontSize = 16.sp) }
                OutlinedButton(
                    onClick = { onEnviar(Protocolo.DESARMAR) },
                    enabled = estado.alarma != EstadoAlarma.DESARMADA,
                    shape = Redondeado,
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text("Desarmar", fontSize = 16.sp, color = Colores.Tinta) }
            }

            // Control manual
            Etiqueta("Control manual")
            Tarjeta {
                Interruptor("Buzzer", estado.buzzerManual) { onEnviar(if (it) Protocolo.BUZZER_ON else Protocolo.BUZZER_OFF) }
                Interruptor("LED de aviso", estado.ledManual) { onEnviar(if (it) Protocolo.LED_ON else Protocolo.LED_OFF) }
            }
        }

        ultimoEvento?.let {
            Etiqueta("Último evento")
            Tarjeta { FilaEvento(it) }
        }

        TextButton(onClick = { confirmarOlvido = true }, modifier = Modifier.align(Alignment.End)) {
            Text("Olvidar esta maleta", color = Colores.Rojo)
        }
    }

    if (confirmarOlvido) {
        AlertDialog(
            onDismissRequest = { confirmarOlvido = false },
            title = { Text("¿Olvidar ${maleta.nombre}?") },
            text = { Text("Se desconectará y dejará de aparecer en tu lista. Puedes volver a agregarla cuando quieras.") },
            confirmButton = { TextButton(onClick = { confirmarOlvido = false; onOlvidar() }) { Text("Olvidar", color = Colores.Rojo) } },
            dismissButton = { TextButton(onClick = { confirmarOlvido = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Interruptor(texto: String, encendido: Boolean, onCambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = encendido, onCheckedChange = onCambio)
    }
}
