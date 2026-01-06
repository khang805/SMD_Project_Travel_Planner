package com.example.smd_project_travel_planner

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

class TripGalleryActivity : AppCompatActivity() {

    private lateinit var rvGallery: RecyclerView
    private lateinit var adapter: GalleryAdapter
    private lateinit var spinnerTrips: Spinner
    private lateinit var fabUpload: FloatingActionButton
    
    private val imageUrls = mutableListOf<String>()
    private val tripList = mutableListOf<TripInfo>() // Helper class for Spinner
    private var selectedTripId: String? = null
    
    // SERVER URL (XAMPP SETUP)
    private val SERVER_URL = "http://172.17.44.141/travel_planner/upload.php"

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            uploadImage(uri)
        }
    }
    
    data class TripInfo(val id: String, val title: String) {
        override fun toString(): String = title // For Spinner display
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trip_gallery)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvGallery = findViewById(R.id.rvGallery)
        rvGallery.layoutManager = GridLayoutManager(this, 3)
        adapter = GalleryAdapter(imageUrls)
        rvGallery.adapter = adapter
        
        spinnerTrips = findViewById(R.id.spinnerTrips)
        fabUpload = findViewById(R.id.fabUpload)
        fabUpload.isEnabled = false // Disable until trip selected

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        fabUpload.setOnClickListener {
            if (selectedTripId != null) {
                selectImageLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Please select a trip first", Toast.LENGTH_SHORT).show()
            }
        }

        fetchTrips()
    }
    
    private fun fetchTrips() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("users").child(userId).child("trips")
        
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripList.clear()
                tripList.add(TripInfo("", "Select a Trip...")) // Default hint
                
                for (tripSnap in snapshot.children) {
                    val id = tripSnap.key ?: continue
                    val title = tripSnap.child("title").getValue(String::class.java) ?: "Untitled Trip"
                    tripList.add(TripInfo(id, title))
                }
                
                setupSpinner()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TripGalleryActivity, "Failed to load trips", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tripList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTrips.adapter = adapter
        
        spinnerTrips.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = tripList[position]
                if (selected.id.isNotEmpty()) {
                    selectedTripId = selected.id
                    fabUpload.isEnabled = true
                    loadImagesForTrip(selected.id)
                } else {
                    selectedTripId = null
                    fabUpload.isEnabled = false
                    imageUrls.clear()
                    this@TripGalleryActivity.adapter.notifyDataSetChanged()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadImagesForTrip(tripId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // CHANGED: Load from specific trip gallery
        val ref = FirebaseDatabase.getInstance().getReference("users").child(userId).child("trips").child(tripId).child("gallery")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                imageUrls.clear()
                for (child in snapshot.children) {
                    val url = child.getValue(String::class.java)
                    if (url != null) {
                        imageUrls.add(url)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TripGalleryActivity, "Failed to load images", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadImage(uri: Uri) {
        val file = getFileFromUri(uri) ?: return
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", file.name,
                        file.asRequestBody("image/*".toMediaTypeOrNull()))
                    .build()

                val request = Request.Builder()
                    .url(SERVER_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string()

                if (response.isSuccessful && responseString != null) {
                    val json = JSONObject(responseString)
                    if (json.has("url")) {
                        val imageUrl = json.getString("url")
                        // CHANGED: Save to specific trip gallery
                        saveUrlToFirebase(imageUrl)
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TripGalleryActivity, "Server Error: ${json.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                     withContext(Dispatchers.Main) {
                        Toast.makeText(this@TripGalleryActivity, "Upload Failed: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TripGalleryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveUrlToFirebase(url: String) {
        val tripId = selectedTripId ?: return // Safety check
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // CHANGED: Save to specific trip gallery
        val ref = FirebaseDatabase.getInstance().getReference("users").child(userId).child("trips").child(tripId).child("gallery")
        
        ref.push().setValue(url)
            .addOnSuccessListener {
                 runOnUiThread { Toast.makeText(this, "Uploaded Successfully!", Toast.LENGTH_SHORT).show() }
            }
    }
    
    private fun getFileFromUri(uri: Uri): File? {
        try {
            val contentResolver = contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload", ".jpg", cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
