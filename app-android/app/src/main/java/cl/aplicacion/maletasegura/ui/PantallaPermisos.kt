package cl.aplicacion.maletasegura.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaPermisos(onPedirPermisos: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Titulo("Maleta Segura")
        Text(
            "Para encontrar tu maleta, la app necesita permiso para usar el Bluetooth y enviarte notificaciones de alerta.",
            color = Colores.TextoSuave,
            fontSize = 16.sp,
        )
        BotonPrincipal("Dar permisos", onPedirPermisos)
    }
}
