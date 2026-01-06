package com.example.smd_project_travel_planner

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.preference.PreferenceManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class maps : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: ImageView // Using the search icon as button? Or add listener to edit text
    // The layout has an ImageView for search icon inside the relative layout, but it's not clickable by default id.
    // I will check the layout again.
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 1. Initialize OSM Configuration (Required)
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        Configuration.getInstance().userAgentValue = packageName // Important for Nominatim Policy
        
        setContentView(R.layout.activity_maps)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // 2. Setup Map
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
        val startPoint = GeoPoint(48.8583, 2.2944) // Default to Paris
        map.controller.setCenter(startPoint)
        
        // 3. Setup UI
        etSearch = findViewById(R.id.etSearch)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }
        
        // 4. Implement Search
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            searchLocation(etSearch.text.toString())
            true
        }
        
        // Use the search icon/row as trigger if user clicks enter? 
        // Or strictly strictly use standard EditorAction for now.
    }
    
    private fun searchLocation(query: String) {
        if (query.isEmpty()) return
        
        val url = "https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=1"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Configuration.getInstance().userAgentValue) // Required
            .build()
            
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (body != null && response.isSuccessful) {
                    val jsonArray = JSONArray(body)
                    if (jsonArray.length() > 0) {
                        val firstResult = jsonArray.getJSONObject(0)
                        val lat = firstResult.getString("lat").toDouble()
                        val lon = firstResult.getString("lon").toDouble()
                        val displayName = firstResult.getString("display_name")
                        
                        withContext(Dispatchers.Main) {
                            val point = GeoPoint(lat, lon)
                            map.controller.setCenter(point)
                            map.controller.setZoom(17.0)
                            
                            // Add Marker
                            map.overlays.clear() // Clear old markers
                            val marker = Marker(map)
                            marker.position = point
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = displayName
                            map.overlays.add(marker)
                            map.invalidate() // Refresh map
                            
                            Toast.makeText(this@maps, "Found: $displayName", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                         withContext(Dispatchers.Main) {
                            Toast.makeText(this@maps, "Location not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@maps, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}