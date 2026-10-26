package cl.aplicacion.maletasegura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aplicacion.maletasegura.ble.EstadoMaleta

@Composable
fun PantallaAlarma(
    nombre: String,
    estado: EstadoMaleta,
    otrasVigiladas: Int,
    onSilenciar: () -> Unit,
    onDesarmar: () -> Unit,
) {
    val causa = when (estado.ultimaAlerta) {
        "APERTURA" -> "Apertura detectada"
        "VIBRACION" -> "Movimiento detectado"
        else -> "La alarma se activó"
    }

    Column(
        Modifier.fillMaxSize().background(Colores.Rojo).safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Text("¡ALARMA!", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text(
            nombre, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 6.dp),
        )
        Text(causa, color = Color.White, fontSize = 20.sp)
        Text("El buzzer y el LED están sonando en la maleta.", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (otrasVigiladas > 0) {
            Text("Tus otras $otrasVigiladas maletas siguen vigiladas", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = onSilenciar,
            shape = Redondeado,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Silenciar buzzer", color = Color.White, fontSize = 16.sp) }

        Button(
            onClick = onDesarmar,
            shape = Redondeado,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Colores.Rojo),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Desarmar $nombre", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}
