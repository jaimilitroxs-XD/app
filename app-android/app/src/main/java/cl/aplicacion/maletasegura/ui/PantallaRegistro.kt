package cl.aplicacion.maletasegura.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaRegistro(
    onRegistrar: (nombre: String, correo: String, clave: String, repetir: String) -> String?,
    onIrALogin: () -> Unit,
) {
    BackHandler(onBack = onIrALogin)
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonVolver(onIrALogin)
            Titulo("Crear cuenta")
        }
        Text("Regístrate para vincular y vigilar tus maletas.", color = Colores.TextoSuave, fontSize = 15.sp)

        Tarjeta {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTexto("Nombre", nombre, { nombre = it })
                CampoTexto("Correo electrónico", correo, { correo = it }, teclado = KeyboardType.Email)
                CampoTexto("Contraseña", clave, { clave = it }, esClave = true, ayuda = "Mínimo 8 caracteres, con letras y números")
                CampoTexto("Repetir contraseña", repetir, { repetir = it }, esClave = true)
                MensajeError(error)
                BotonPrincipal("Crear cuenta") { error = onRegistrar(nombre, correo, clave, repetir) }
            }
        }

        TextButton(onClick = onIrALogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("¿Ya tienes cuenta? ", color = Colores.TextoSuave)
            Text("Inicia sesión", color = Colores.Azul, fontWeight = FontWeight.SemiBold)
        }
    }
}
