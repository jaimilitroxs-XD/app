package cl.aplicacion.maletasegura

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import cl.aplicacion.maletasegura.ui.AppMaleta
import cl.aplicacion.maletasegura.ui.MaletaTema

class MainActivity : ComponentActivity() {
    private val permisos = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,  // buscar la maleta
        Manifest.permission.BLUETOOTH_CONNECT,  // conectarse y vincularse
        Manifest.permission.POST_NOTIFICATIONS,  // mostrar las alertas
    )

    private val permisosOk = mutableStateOf(false)

    private val pedirPermisos =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permisosOk.value = tieneBluetooth()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permisosOk.value = tieneBluetooth()
        if (!permisosOk.value) pedirPermisos.launch(permisos)

        setContent {
            MaletaTema {
                AppMaleta(
                    permisosOk = permisosOk.value,
                    onPedirPermisos = { pedirPermisos.launch(permisos) },
                )
            }
        }
    }

    private fun tieneBluetooth(): Boolean =
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT).all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
}
