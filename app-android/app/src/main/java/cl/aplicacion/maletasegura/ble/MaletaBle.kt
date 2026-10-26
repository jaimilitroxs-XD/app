package cl.aplicacion.maletasegura.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import cl.aplicacion.maletasegura.datos.Historial
import cl.aplicacion.maletasegura.datos.MisMaletas
import cl.aplicacion.maletasegura.datos.TipoEvento
import cl.aplicacion.maletasegura.servicio.MaletaService
import cl.aplicacion.maletasegura.servicio.Notificaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Conexion { DESCONECTADO, VINCULANDO, CONECTANDO, CONECTADO }

data class Encontrado(val direccion: String, val nombre: String, val vinculada: Boolean)

data class EstadoMaleta(
    val conexion: Conexion = Conexion.DESCONECTADO,
    val alarma: EstadoAlarma = EstadoAlarma.DESCONOCIDO,
    val maletaCerrada: Boolean? = null,
    val buzzerManual: Boolean = false,
    val ledManual: Boolean = false,
    val ultimaAlerta: String? = null,
)

@SuppressLint("MissingPermission")  // los permisos se piden en MainActivity
object MaletaBle {
    private val _estados = MutableStateFlow<Map<String, EstadoMaleta>>(emptyMap())
    val estados: StateFlow<Map<String, EstadoMaleta>> = _estados

    private val _encontrados = MutableStateFlow<List<Encontrado>>(emptyList())
    val encontrados: StateFlow<List<Encontrado>> = _encontrados

    private val _buscando = MutableStateFlow(false)
    val buscando: StateFlow<Boolean> = _buscando

    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso

    private class Enlace {
        var gatt: BluetoothGatt? = null  // la conexión abierta
        var caracComando: BluetoothGattCharacteristic? = null  // donde se escriben los comandos
        var desconexionManual = false
        val cola = ArrayDeque<String>()
        var escribiendo = false
    }

    private val enlaces = mutableMapOf<String, Enlace>()
    private lateinit var app: Context

    private val handler = Handler(Looper.getMainLooper())

    private fun adaptador() =
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    fun iniciar(context: Context) {
        app = context.applicationContext
        ContextCompat.registerReceiver(
            app, receptorVinculo,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    fun bluetoothActivo(): Boolean = adaptador()?.isEnabled == true

    fun limpiarAviso() { _aviso.value = null }

    fun estadoDe(direccion: String): EstadoMaleta = _estados.value[direccion] ?: EstadoMaleta()

    private fun actualizar(direccion: String, cambio: (EstadoMaleta) -> EstadoMaleta) {
        _estados.value = _estados.value + (direccion to cambio(estadoDe(direccion)))
    }

    fun buscar() {
        val escaner = adaptador()?.bluetoothLeScanner
        if (escaner == null) {
            _aviso.value = "Activa el Bluetooth del celular"
            return
        }
        _encontrados.value = emptyList()
        val filtro = ScanFilter.Builder().setServiceUuid(ParcelUuid(Protocolo.UUID_SERVICIO)).build()
        val ajustes = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        escaner.startScan(listOf(filtro), ajustes, alEncontrar)
        _buscando.value = true
        handler.postDelayed({ dejarDeBuscar() }, 15_000)
    }

    fun dejarDeBuscar() {
        if (!_buscando.value) return
        adaptador()?.bluetoothLeScanner?.stopScan(alEncontrar)
        _buscando.value = false
    }

    private val alEncontrar = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val d = result.device
            val maleta = Encontrado(
                direccion = d.address,
                nombre = result.scanRecord?.deviceName ?: "MaletaSegura",
                vinculada = d.bondState == BluetoothDevice.BOND_BONDED,
            )
            _encontrados.value = _encontrados.value.filter { it.direccion != maleta.direccion } + maleta
        }
    }

    fun conectar(direccion: String) {
        dejarDeBuscar()
        val enlace = enlaces.getOrPut(direccion) { Enlace() }
        enlace.desconexionManual = false
        val maleta = adaptador()?.getRemoteDevice(direccion) ?: return
        if (maleta.bondState == BluetoothDevice.BOND_BONDED) {
            abrirGatt(maleta)
        } else {
            actualizar(direccion) { it.copy(conexion = Conexion.VINCULANDO) }
            maleta.createBond()
        }
    }

    fun conectarTodas(direcciones: List<String>) {
        direcciones.forEach { if (estadoDe(it).conexion == Conexion.DESCONECTADO) conectar(it) }
    }

    private val receptorVinculo = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val maleta = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
            val dir = maleta.address
            if (estadoDe(dir).conexion != Conexion.VINCULANDO) return
            when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
                BluetoothDevice.BOND_BONDED -> {
                    Historial.agregar(TipoEvento.CONEXION, "Maleta vinculada", "PIN aceptado", MisMaletas.nombreDe(dir))
                    abrirGatt(maleta)
                }
                BluetoothDevice.BOND_NONE -> {
                    actualizar(dir) { it.copy(conexion = Conexion.DESCONECTADO) }
                    _aviso.value = "Vinculación fallida. Revisa el PIN e inténtalo de nuevo."
                }
            }
        }
    }

    fun desconectar(direccion: String) {
        val enlace = enlaces[direccion] ?: return
        enlace.desconexionManual = true
        enlace.gatt?.disconnect()
        enlace.gatt?.close()
        enlaces.remove(direccion)
        actualizar(direccion) { EstadoMaleta() }
        revisarServicio()
    }

    fun desconectarTodas() {
        enlaces.keys.toList().forEach { desconectar(it) }
        _estados.value = emptyMap()
    }

    private fun abrirGatt(maleta: BluetoothDevice) {
        val enlace = enlaces.getOrPut(maleta.address) { Enlace() }
        actualizar(maleta.address) { it.copy(conexion = Conexion.CONECTANDO) }
        enlace.gatt = maleta.connectGatt(app, false, eventosGatt, BluetoothDevice.TRANSPORT_LE)
    }

    private val eventosGatt = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            handler.post {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    g.requestMtu(185)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    alDesconectarse(g)
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            handler.post { g.discoverServices() }  // ¿qué servicios tiene la maleta?
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            handler.post {
                val dir = g.device.address
                val enlace = enlaces[dir] ?: return@post
                val servicio = g.getService(Protocolo.UUID_SERVICIO)
                val caracEstado = servicio?.getCharacteristic(Protocolo.UUID_ESTADO)
                enlace.caracComando = servicio?.getCharacteristic(Protocolo.UUID_COMANDO)
                if (caracEstado == null || enlace.caracComando == null) {
                    _aviso.value = "Este dispositivo no es una Maleta Segura"
                    desconectar(dir)
                    return@post
                }
                g.setCharacteristicNotification(caracEstado, true)
                g.writeDescriptor(
                    caracEstado.getDescriptor(Protocolo.UUID_CCCD),
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                )
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            handler.post {
                val dir = g.device.address
                actualizar(dir) { it.copy(conexion = Conexion.CONECTADO) }
                Historial.agregar(TipoEvento.CONEXION, "Celular conectado", "Enlace cifrado", MisMaletas.nombreDe(dir))
                MaletaService.iniciar(app)  // mantiene las conexiones con la app minimizada
                enviar(dir, Protocolo.ESTADO)  // preguntamos cómo está la maleta
            }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            val dir = g.device.address
            handler.post { recibir(dir, String(value, Charsets.UTF_8)) }  // llegó un mensaje de esa maleta
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            val dir = g.device.address
            handler.post {
                enlaces[dir]?.escribiendo = false
                enviarSiguiente(dir)
            }
        }
    }

    private fun alDesconectarse(g: BluetoothGatt) {
        val dir = g.device.address
        g.close()
        val enlace = enlaces.remove(dir)
        if (enlace == null || enlace.desconexionManual) return

        val estado = estadoDe(dir)
        val estabaArmada = estado.alarma == EstadoAlarma.ARMADA || estado.alarma == EstadoAlarma.ALARMA
        actualizar(dir) { EstadoMaleta() }
        revisarServicio()
        val nombre = MisMaletas.nombreDe(dir)
        Historial.agregar(TipoEvento.CONEXION, "Conexión perdida", "Fuera de alcance o sin batería", nombre)
        if (estabaArmada) {
            Notificaciones.alerta(app, "Se perdió la conexión con $nombre", "Puede estar fuera de alcance. Revísala.")
        }
    }

    private fun revisarServicio() {
        if (_estados.value.values.none { it.conexion == Conexion.CONECTADO }) MaletaService.detener(app)
    }

    fun enviar(direccion: String, comando: String) {
        if (comando !in Protocolo.COMANDOS_VALIDOS) return  // solo comandos conocidos
        val enlace = enlaces[direccion]
        if (enlace == null || estadoDe(direccion).conexion != Conexion.CONECTADO) {
            _aviso.value = "${MisMaletas.nombreDe(direccion)} no está conectada"
            return
        }
        enlace.cola.addLast(comando)
        enviarSiguiente(direccion)
    }

    private fun enviarSiguiente(direccion: String) {
        val enlace = enlaces[direccion] ?: return
        if (enlace.escribiendo) return
        val g = enlace.gatt ?: return
        val c = enlace.caracComando ?: return
        val comando = enlace.cola.removeFirstOrNull() ?: return
        enlace.escribiendo = true
        g.writeCharacteristic(c, comando.toByteArray(Charsets.UTF_8), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    }

    private fun recibir(direccion: String, texto: String) {
        val nombre = MisMaletas.nombreDe(direccion)
        when (val m = Protocolo.parsear(texto)) {
            is Mensaje.Estado -> {
                val anterior = estadoDe(direccion).alarma
                actualizar(direccion) {
                    it.copy(
                        alarma = m.alarma,
                        maletaCerrada = m.maletaCerrada,
                        ultimaAlerta = if (m.alarma == EstadoAlarma.ALARMA) it.ultimaAlerta else null,
                    )
                }
                if (anterior != EstadoAlarma.DESCONOCIDO && anterior != m.alarma) {
                    if (m.alarma == EstadoAlarma.ARMADA) Historial.agregar(TipoEvento.ALARMA, "Alarma armada", "Maleta cerrada", nombre)
                    if (m.alarma == EstadoAlarma.DESARMADA) Historial.agregar(TipoEvento.ALARMA, "Alarma desarmada", "Desde el celular", nombre)
                }
            }
            is Mensaje.Alerta -> {
                val texto = Protocolo.textoAlerta(m.causa)
                actualizar(direccion) { it.copy(ultimaAlerta = m.causa) }
                Historial.agregar(TipoEvento.ALERTA, texto, "Alerta enviada al celular", nombre)
                Notificaciones.alerta(app, "¡Alerta en $nombre!", texto)
            }
            is Mensaje.Ok -> when (m.comando) {
                Protocolo.BUZZER_ON -> actualizar(direccion) { it.copy(buzzerManual = true) }
                Protocolo.BUZZER_OFF -> actualizar(direccion) { it.copy(buzzerManual = false) }
                Protocolo.LED_ON -> actualizar(direccion) { it.copy(ledManual = true) }
                Protocolo.LED_OFF -> actualizar(direccion) { it.copy(ledManual = false) }
            }
            is Mensaje.Error -> _aviso.value = "$nombre: ${Protocolo.textoError(m.motivo)}"
            is Mensaje.Otro -> Unit  // mensajes informativos: se ignoran
        }
    }
}
