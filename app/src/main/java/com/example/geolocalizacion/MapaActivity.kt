package com.example.geolocalizacion

import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapaActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private var latitud: Double = 0.0
    private var longitud: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_mapa)

        // Obtener coordenadas del intent
        latitud = intent.getDoubleExtra("LATITUD", 0.0)
        longitud = intent.getDoubleExtra("LONGITUD", 0.0)

        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Centrar el mapa en la ubicación recibida
        val geoPoint = GeoPoint(latitud, longitud)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(geoPoint)

        // Agregar un marcador en la ubicación
        val marcador = Marker(mapView)
        marcador.position = geoPoint
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marcador.title = "Ubicación actual"
        mapView.overlays.add(marcador)

        // Botón de retroceso
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Cierra la actividad y regresa a la anterior
        }
    }
}
