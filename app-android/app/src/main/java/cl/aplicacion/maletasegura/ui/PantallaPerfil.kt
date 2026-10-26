package cl.aplicacion.maletasegura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aplicacion.maletasegura.datos.Usuario

@Composable
fun PantallaPerfil(
    usuario: Usuario,
    cantidadMaletas: Int,
    onCerrarSesion: () -> Unit,
) {
    val iniciales = usuario.nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Titulo("Perfil")
        Tarjeta {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(60.dp).background(Colores.Azul, CircleShape), contentAlignment = Alignment.Center) {
                    Text(iniciales, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Column {
                    Text(usuario.nombre, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(usuario.correo, color = Colores.TextoSuave, fontSize = 14.sp)
                }
            }
        }

        Etiqueta("Mi cuenta")
        Tarjeta {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("Maletas vinculadas", fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("$cantidadMaletas", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Etiqueta("Seguridad")
        Tarjeta {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("✓ Contraseña guardada cifrada en este celular", fontSize = 14.sp)
                Text("✓ Maletas vinculadas con PIN y conexión cifrada", fontSize = 14.sp)
                Text("✓ Solo tú ves tus maletas", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onCerrarSesion, shape = Redondeado, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Cerrar sesión", color = Colores.Rojo, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
