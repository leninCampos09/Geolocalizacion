package com.example.geolocalizacion

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var btnObtenerCoordenadas: Button
    private lateinit var btnCompartirUbicacion: Button
    private lateinit var btnVerMapa: Button
    private lateinit var txtLatitud: TextView
    private lateinit var txtLongitud: TextView
    private lateinit var txtDireccion: TextView

    private var latitudActual: Double = 0.0
    private var longitudActual: Double = 0.0
    private var rastreoActivo = false

    companion object {
        private const val REQUEST_CODE_LOCATION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        btnObtenerCoordenadas = findViewById(R.id.btnObtenerCoordenadas)
        btnCompartirUbicacion = findViewById(R.id.btnCompartirUbicacion)
        btnVerMapa = findViewById(R.id.btnVerMapa)  // Botón para ver mapa
        txtLatitud = findViewById(R.id.txtLatitud)
        txtLongitud = findViewById(R.id.txtLongitud)
        txtDireccion = findViewById(R.id.txtDireccion)

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar actualizaciones de ubicación
        configurarActualizacionUbicacion()

        // Evento para obtener coordenadas
        btnObtenerCoordenadas.setOnClickListener {
            if (rastreoActivo) {
                detenerRastreo()
            } else {
                verificarPermisosYObtenerUbicacion()
            }
        }

        // Evento para compartir ubicación en WhatsApp
        btnCompartirUbicacion.setOnClickListener {
            compartirUbicacionWhatsApp()
        }

        // Evento para abrir el mapa con las coordenadas
        btnVerMapa.setOnClickListener {
            if (latitudActual != 0.0 && longitudActual != 0.0) {
                // Crear un Intent para abrir MapaActivity y pasar las coordenadas
                val intent = Intent(this, MapaActivity::class.java).apply {
                    putExtra("LATITUD", latitudActual)
                    putExtra("LONGITUD", longitudActual)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarPermisosYObtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
        } else {
            iniciarRastreo()
        }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarRastreo() {
        rastreoActivo = true
        btnObtenerCoordenadas.text = "DETENER RASTREO"
        Toast.makeText(this, "Rastreo activado", Toast.LENGTH_SHORT).show()

        // Obtener la última ubicación disponible
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                actualizarUI(it.latitude, it.longitude)
            }
        }

        // Iniciar actualizaciones en tiempo real
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun detenerRastreo() {
        rastreoActivo = false
        btnObtenerCoordenadas.text = "OBTENER COORDENADAS"
        Toast.makeText(this, "Rastreo detenido", Toast.LENGTH_SHORT).show()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun configurarActualizacionUbicacion() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    actualizarUI(location.latitude, location.longitude)
                }
            }
        }
    }

    private fun actualizarUI(latitud: Double, longitud: Double) {
        latitudActual = latitud
        longitudActual = longitud
        txtLatitud.text = "$latitud"
        txtLongitud.text = "$longitud"
        obtenerDireccion(latitud, longitud)
    }

    private fun obtenerDireccion(latitud: Double, longitud: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                val direcciones = geocoder.getFromLocation(latitud, longitud, 1)

                if (!direcciones.isNullOrEmpty()) {
                    val direccion = direcciones[0]
                    val nombreLugar = direccion.featureName ?: "Lugar desconocido"
                    val ciudad = direccion.locality ?: direccion.subAdminArea ?: "Ciudad no encontrada"

                    withContext(Dispatchers.Main) {
                        txtDireccion.text = "📍 $nombreLugar, $ciudad"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        txtDireccion.text = "Dirección no encontrada"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    txtDireccion.text = "Error obteniendo dirección"
                }
            }
        }
    }

    private fun compartirUbicacionWhatsApp() {
        if (latitudActual == 0.0 || longitudActual == 0.0) {
            Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val mensaje = "Hola, te adjunto mi ubicación:\nhttps://maps.google.com/?q=$latitudActual,$longitudActual"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(mensaje))
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarRastreo()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }
}


