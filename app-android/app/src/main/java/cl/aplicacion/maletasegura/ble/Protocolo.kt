package cl.aplicacion.maletasegura.ble

import java.util.UUID

object Protocolo {
    val UUID_SERVICIO: UUID = UUID.fromString("6e1a0001-4b5d-4c3a-9f2e-6d616c657461")
    val UUID_ESTADO: UUID = UUID.fromString("6e1a0002-4b5d-4c3a-9f2e-6d616c657461")
    val UUID_COMANDO: UUID = UUID.fromString("6e1a0003-4b5d-4c3a-9f2e-6d616c657461")
    val UUID_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val ARMAR = "ARMAR"
    const val DESARMAR = "DESARMAR"
    const val BUZZER_ON = "BUZZER_ON"
    const val BUZZER_OFF = "BUZZER_OFF"
    const val LED_ON = "LED_ON"
    const val LED_OFF = "LED_OFF"
    const val ESTADO = "ESTADO"

    val COMANDOS_VALIDOS = setOf(ARMAR, DESARMAR, BUZZER_ON, BUZZER_OFF, LED_ON, LED_OFF, ESTADO)

    fun parsear(texto: String): Mensaje {
        val t = texto.trim()
        return when {
            t.startsWith("ESTADO:") -> {
                val partes = t.removePrefix("ESTADO:").split(";")
                val alarma = when (partes.getOrNull(0)) {
                    "ARMADA" -> EstadoAlarma.ARMADA
                    "ALARMA" -> EstadoAlarma.ALARMA
                    "DESARMADA" -> EstadoAlarma.DESARMADA
                    else -> EstadoAlarma.DESCONOCIDO
                }
                val maleta = partes.firstOrNull { it.startsWith("MALETA:") }?.removePrefix("MALETA:")
                Mensaje.Estado(alarma, maletaCerrada = maleta?.let { it == "CERRADA" })
            }
            t.startsWith("ALERTA:") -> Mensaje.Alerta(t.removePrefix("ALERTA:"))
            t.startsWith("OK:") -> Mensaje.Ok(t.removePrefix("OK:"))
            t.startsWith("ERROR:") -> Mensaje.Error(t.removePrefix("ERROR:"))
            else -> Mensaje.Otro(t)
        }
    }

    fun textoAlerta(causa: String): String = when (causa) {
        "APERTURA" -> "Se abrió tu maleta"
        "VIBRACION" -> "Se detectó movimiento en tu maleta"
        else -> "Alerta: $causa"
    }

    fun textoError(motivo: String): String = when (motivo) {
        "MALETA_ABIERTA" -> "No se puede armar: la maleta está abierta"
        "YA_ARMADA" -> "La alarma ya está armada"
        "COMANDO_DESCONOCIDO" -> "La maleta no reconoció el comando"
        else -> "Error: $motivo"
    }
}

enum class EstadoAlarma { DESCONOCIDO, DESARMADA, ARMADA, ALARMA }

sealed interface Mensaje {
    data class Estado(val alarma: EstadoAlarma, val maletaCerrada: Boolean?) : Mensaje
    data class Alerta(val causa: String) : Mensaje
    data class Ok(val comando: String) : Mensaje
    data class Error(val motivo: String) : Mensaje
    data class Otro(val texto: String) : Mensaje
}
