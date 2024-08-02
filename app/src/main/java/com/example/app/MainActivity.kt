package com.example.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import coil.compose.rememberImagePainter
import com.example.app.ui.theme.AppTheme
import pl.droidsonroids.gif.GifImageView
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import java.util.UUID
import java.io.OutputStream
import android.os.Handler
import android.os.Looper
import java.io.InputStream
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import androidx.compose.ui.text.TextStyle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import android.util.Log
import java.io.IOException





class MainActivity : ComponentActivity(), OnMapReadyCallback {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val DEVICE_ADDRESS = "E8:6B:EA:DE:9C:2E" // Dirección MAC del dispositivo Bluetooth al que deseas conectar

    private var isBluetoothConnected = false

    private var map: GoogleMap? = null
    private lateinit var mapView: MapView
    private var startMarker: Marker? = null

    private var height by mutableStateOf("")
    private var airTime by mutableStateOf("")

    private var showNoSignalDialog by mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        SkyShipApp()
                    }

                    if (showNoSignalDialog) {
                        NoSignalAlertDialog(
                            onDismiss = { showNoSignalDialog = false }
                        )
                    }
                }
            }
        }

        // Initialize the MapView
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun SkyShipApp() {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp)
                .padding(top = 0.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡Bienvenido a SkyShip Drone App!",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF9C27B0))
                    .padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = {
                    // Lógica para probar la conexión Bluetooth
                    connectBluetooth()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF9C27B0)
                )
            ) {
                Text(
                    text = "Conectate a tu SkyShip",
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Mostrar el GIF animado
            //GifImage(imageResId = R.drawable.skyship, modifier = Modifier.size(300.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = height,
                    onValueChange = { newValue ->
                        // Validar y limitar el valor de altura
                        val cleanedValue = newValue.filter { it.isDigit() || it == '.' }
                        val numericValue = cleanedValue.toDoubleOrNull()

                        if (numericValue != null && numericValue in 0.0..5.0) {
                            height = cleanedValue
                        } else if (cleanedValue.isEmpty()) {
                            height = cleanedValue
                        }
                    },
                    label = { Text("Altura (0-5m)") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                    maxLines = 1
                )

                // Caja de entrada para tiempo en el aire
                OutlinedTextField(
                    value = airTime,
                    onValueChange = { newValue ->
                        // Verifica si el nuevo valor es un número entero dentro del rango permitido
                        val intValue = newValue.toIntOrNull()
                        if (intValue != null && intValue in 0..20) {
                            airTime = newValue
                        } else if (newValue.isEmpty()) {
                            // Permite que el campo esté vacío
                            airTime = newValue
                        }
                    },
                    label = { Text("Tiempo de vuelo (0-20s)", style = TextStyle(fontSize = 14.sp)) },
                    modifier = Modifier
                        .weight(1f),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        sendDataToESP32()
                    },
                    modifier = Modifier.align(Alignment.CenterVertically),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF9C27B0)
                    )
                ) {
                    Text(
                        text = "Enviar",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

            }

            Spacer(modifier = Modifier.height(10.dp))


            // Add the map view here
            AndroidView({ mapView }, modifier = Modifier.fillMaxSize()) { mapView ->
                mapView.getMapAsync(this@MainActivity)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, 1)
        } else {
            bluetoothDevice = bluetoothAdapter?.getRemoteDevice(DEVICE_ADDRESS)
            try {
                bluetoothSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(DEFAULT_UUID)
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream
                isBluetoothConnected = true
                Toast.makeText(this, "Conexión Bluetooth establecida", Toast.LENGTH_SHORT).show()
                readCoordinates()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al conectar al dispositivo Bluetooth", Toast.LENGTH_SHORT).show()
                isBluetoothConnected = false
            }
        }
    }

    private fun sendDataToESP32() {
        if (isBluetoothConnected) {
            if (height.isNotEmpty() && airTime.isNotEmpty()) {
                try {
                    val message1 = "HEIGHT:$height\n"
                    val message2 = "AIRTIME:$airTime\n"
                    outputStream?.write(message1.toByteArray())
                    Log.d("prueba1", "Mensaje de altura enviado: $message1")
                    outputStream?.write(message2.toByteArray())
                    Log.d("prueba1", "Mensaje de tiempo enviado: $message2")
                    Toast.makeText(this, "Datos enviados al ESP32", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al enviar los datos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No hay conexión Bluetooth establecida", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readCoordinates() {
        Thread {
            try {
                val buffer = ByteArray(1024)
                var bytes: Int
                val stringBuilder = StringBuilder()

                while (true) {
                    bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        stringBuilder.append(message)
                        sendInitialConfirmation()

                        // Si hay un salto de línea que indica el final de los datos
                        if (message.contains("\n")) {
                            val completeData = stringBuilder.toString().trim()
                            Log.d("Bluetooth", "Raw Data received: $completeData")
                            processReceivedData(completeData)
                            stringBuilder.clear()  // Limpiar el buffer para recibir nuevos datos
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("Bluetooth", "Error al leer las coordenadas")
            }
        }.start()
    }

    private fun sendInitialConfirmation() {
        try {
            outputStream?.write("MESSAGE_RECEIVED\n".toByteArray())
            Log.d("Respuestas", "Confirmación inicial enviada")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Respuestas", "Error al enviar confirmación inicial")
        }
    }

    private fun sendCoordinatesConfirmation() {
        try {
            outputStream?.write("COORDINATES_RECEIVED\n".toByteArray())
            Log.d("Respuestas", "Confirmación de coordenadas enviada")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Respuestas", "Error al enviar confirmación de coordenadas")
        }
    }

    private fun sendNoSignalMessage() {
        try {
            outputStream?.write("NO SIGNAL\n".toByteArray())
            showNoSignalDialog = true
            Log.d("Respuestas", "NO SIGNAL message sent")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Respuestas", "Error al enviar NO SIGNAL")
        }
    }


    @Composable
    fun NoSignalAlertDialog(onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No se recibe señal GPS.") },
            text = { Text("Por favor, dirígete a un lugar más despejado y vuelve a intentarlo.") },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF9C27B0)
                    )
                ) {
                    Text("Reintentar", color = Color.White)
                }
            }
        )
    }



    private fun processReceivedData(data: String) {
        if (data.contains("No se recibe señal GPS")) {
            sendNoSignalMessage()
            return
        }

        val lines = data.split("\n")

        var latitude: String? = null
        var longitude: String? = null
        var datetime: String? = null

        for (line in lines) {
            when {
                line.startsWith("Latitud:") -> {
                    latitude = line.replace("Latitud: ", "").trim()
                }
                line.startsWith("Longitud:") -> {
                    longitude = line.replace("Longitud: ", "").trim()
                }
                line.startsWith("Datetime:") -> {
                    datetime = line.replace("Datetime: ", "").trim()
                }
            }
        }

        if (latitude != null && longitude != null) {
            Log.d("Processed Data", "Latitud: $latitude, Longitud: $longitude, Datetime: $datetime")

            // Enviar confirmación de coordenadas recibidas
            sendCoordinatesConfirmation()

            // Convertir las coordenadas a LatLng
            val location = LatLng(latitude.toDouble(), longitude.toDouble())

            Log.d("Processed Data", "se convirtieron: $location")

            // Actualizar o agregar el marcador del dron (startMarker)
            runOnUiThread {
                if (startMarker == null) {
                    startMarker = map?.addMarker(MarkerOptions().position(location).title("origin"))
                } else {
                    startMarker?.position = location
                }
                // Mover la cámara a la ubicación del dron
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            }

            Log.d("Processed Data", "Se actualizaron los datos en el mapa")
        } else {
            Log.e("Processed Data", "Datos incompletos o formato incorrecto: $data")
        }
    }


    private fun showMap() {
        // Refresh the map
        mapView.onResume()
        mapView.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.uiSettings?.isZoomControlsEnabled = true
        map?.uiSettings?.isZoomGesturesEnabled = true
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }


    private fun showToast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

}





