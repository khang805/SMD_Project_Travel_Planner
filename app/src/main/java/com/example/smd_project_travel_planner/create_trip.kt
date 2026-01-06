package com.example.smd_project_travel_planner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class create_trip : AppCompatActivity() {

    private lateinit var etTripTitle: EditText
    private lateinit var etDestination: EditText
    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var btnAddPhoto: Button
    private lateinit var btnCreateTrip: Button
    private lateinit var closeIcon: ImageView
    private lateinit var databaseReference: DatabaseReference
    private var imageBase64: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_trip)

        etTripTitle = findViewById(R.id.etTripTitle)
        etDestination = findViewById(R.id.etDestination)
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        btnCreateTrip = findViewById(R.id.btnCreateTrip)
        closeIcon = findViewById(R.id.closeIcon)

        closeIcon.setOnClickListener {
            finish()
        }

        btnAddPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(intent)
        }

        btnCreateTrip.setOnClickListener {
            saveTrip()
        }
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    imageBase64 = bitmapToBase64(bitmap)
                    btnAddPhoto.text = "Photo Selected"
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Compress image to avoid hitting Firebase size limits (10MB, but best to keep small)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveTrip() {
        val title = etTripTitle.text.toString().trim()
        val destination = etDestination.text.toString().trim()
        val startDate = etStartDate.text.toString().trim()
        val endDate = etEndDate.text.toString().trim()

        if (title.isEmpty() || destination.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("trips")
            
            val tripId = databaseReference.push().key
            if (tripId != null) {
                // 1. Save to Local DB (Room) IMMEDIATELY
                val db = com.example.smd_project_travel_planner.data.AppDatabase.getDatabase(this)
                val tripEntity = com.example.smd_project_travel_planner.data.TripEntity(
                    id = tripId,
                    title = title,
                    destination = destination,
                    startDate = startDate,
                    endDate = endDate,
                    imageBase64 = imageBase64,
                    isSynced = false
                )
                
                CoroutineScope(Dispatchers.IO).launch {
                    db.tripDao().insert(tripEntity)
                    
                    // 2. Try to Sync to Firebase
                    if (isNetworkAvailable()) {
                        val trip = Trip(tripId, title, destination, startDate, endDate, imageBase64)
                        databaseReference.child(tripId).setValue(trip)
                            .addOnSuccessListener {
                                CoroutineScope(Dispatchers.IO).launch {
                                    db.tripDao().markSynced(tripId)
                                }
                                runOnUiThread { 
                                    Toast.makeText(this@create_trip, "Trip Created (Synced)", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }
                            .addOnFailureListener {
                                runOnUiThread { 
                                    Toast.makeText(this@create_trip, "Saved Locally (Sync Failed)", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }
                    } else {
                         runOnUiThread { 
                             Toast.makeText(this@create_trip, "Saved Locally (Offline)", Toast.LENGTH_SHORT).show()
                             finish()
                         }
                    }
                }
            }
        } else {
             Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}