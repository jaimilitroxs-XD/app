package cl.aplicacion.maletasegura.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Colores {
    val Fondo = Color(0xFFF4F2EE)
    val Tinta = Color(0xFF1B1F24)
    val TextoSuave = Color(0xFF4A5260)
    val Azul = Color(0xFF1F5C99)
    val AzulClaro = Color(0xFFE4ECF5)
    val Rojo = Color(0xFFB33A0B)
    val Verde = Color(0xFF2E6B3F)
    val Borde = Color(0xFFD9D5CC)
    val Tarjeta = Color(0xFFFFFFFF)
}

@Composable
fun MaletaTema(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Colores.Azul,
            background = Colores.Fondo,
            surface = Colores.Tarjeta,
            onBackground = Colores.Tinta,
            onSurface = Colores.Tinta,
        ),
        content = contenido,
    )
}
