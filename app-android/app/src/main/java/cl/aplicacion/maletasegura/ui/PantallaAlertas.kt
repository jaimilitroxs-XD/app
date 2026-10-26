package cl.aplicacion.maletasegura.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.aplicacion.maletasegura.datos.Evento

@Composable
fun PantallaAlertas(eventos: List<Evento>, onBorrar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Titulo("Alertas")

        if (eventos.isEmpty()) Text("Todavía no hay eventos.", color = Colores.TextoSuave)

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(eventos) { evento -> Tarjeta { FilaEvento(evento) } }
        }

        BotonSecundario("Borrar historial", habilitado = eventos.isNotEmpty(), onClick = onBorrar)
    }
}
