package cl.aplicacion.maletasegura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aplicacion.maletasegura.R

@Composable
fun PantallaLogin(onIngresar: (correo: String, clave: String) -> String?, onIrARegistro: () -> Unit) {
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.size(24.dp))
        Box(
            Modifier.size(84.dp).background(Colores.Azul, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_notificacion), contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Titulo("Maleta Segura")
        Text("Inicia sesión para vigilar tus maletas", color = Colores.TextoSuave, fontSize = 15.sp)

        Tarjeta {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto("Correo electrónico", correo, { correo = it }, teclado = KeyboardType.Email)
                CampoTexto("Contraseña", clave, { clave = it }, esClave = true)
                MensajeError(error)
                BotonPrincipal("Ingresar") { error = onIngresar(correo, clave) }
            }
        }
        Text("🔒 Tu contraseña se guarda cifrada en este celular", color = Colores.TextoSuave, fontSize = 13.sp)

        Spacer(Modifier.size(8.dp))
        TextButton(onClick = onIrARegistro) {
            Text("¿No tienes cuenta? ", color = Colores.TextoSuave)
            Text("Crear cuenta", color = Colores.Azul, fontWeight = FontWeight.SemiBold)
        }
    }
}
