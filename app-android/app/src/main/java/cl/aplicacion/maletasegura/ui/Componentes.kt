package cl.aplicacion.maletasegura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aplicacion.maletasegura.datos.Evento
import cl.aplicacion.maletasegura.datos.TipoEvento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Redondeado = RoundedCornerShape(14.dp)

@Composable
fun Titulo(texto: String) {
    Text(texto, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Colores.Tinta)
}

@Composable
fun Etiqueta(texto: String) {
    Text(texto.uppercase(), color = Colores.TextoSuave, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Colores.Tarjeta, Redondeado)
            .border(1.dp, Colores.Borde, Redondeado)
            .padding(16.dp),
    ) { contenido() }
}

@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = Redondeado,
        colors = ButtonDefaults.buttonColors(containerColor = Colores.Azul),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text(texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun BotonSecundario(texto: String, habilitado: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = Redondeado,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text(texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    esClave: Boolean = false,
    teclado: KeyboardType = KeyboardType.Text,
    ayuda: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        visualTransformation = if (esClave) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (esClave) KeyboardType.Password else teclado),
        supportingText = ayuda?.let { { Text(it) } },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun BotonVolver(onVolver: () -> Unit) {
    OutlinedButton(
        onClick = onVolver,
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.size(44.dp),
    ) { Text("‹", fontSize = 26.sp, color = Colores.Tinta) }
}

@Composable
fun MensajeError(texto: String?) {
    if (texto != null) Text(texto, color = Colores.Rojo, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

private val formatoHora = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

@Composable
fun FilaEvento(ev: Evento) {
    val color = when (ev.tipo) {
        TipoEvento.ALERTA -> Colores.Rojo
        TipoEvento.ALARMA -> Colores.Azul
        TipoEvento.CONEXION -> Colores.TextoSuave
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(ev.titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("${ev.maleta} · ${ev.detalle}", fontSize = 13.sp, color = Colores.TextoSuave)
        }
        Text(formatoHora.format(Date(ev.momento)), fontSize = 13.sp, color = Colores.TextoSuave)
    }
}
