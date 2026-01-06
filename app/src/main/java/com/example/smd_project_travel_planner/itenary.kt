package com.example.smd_project_travel_planner

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch

class itenary : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItineraryAdapter
    private lateinit var itineraryList: MutableList<ItineraryItem>
    private lateinit var btnAddActivity: Button
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var databaseReference: DatabaseReference
    private var tripId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_itenary)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tripId = intent.getStringExtra("TRIP_ID")
        val tripTitle = intent.getStringExtra("TRIP_TITLE")

        tvTitle = findViewById(R.id.tvTitle)
        if (tripTitle != null) {
            tvTitle.text = tripTitle
        }

        recyclerView = findViewById(R.id.recyclerViewItinerary)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        itineraryList = mutableListOf()
        adapter = ItineraryAdapter(itineraryList)
        recyclerView.adapter = adapter

        btnAddActivity = findViewById(R.id.btnAddActivity)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        btnAddActivity.setOnClickListener {
            showAddActivityDialog()
        }

        loadItinerary()
    }

    private fun loadItinerary() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && tripId != null) {
             val db = com.example.smd_project_travel_planner.data.AppDatabase.getDatabase(this)
             
             kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                 val localItems = db.expenseDao().getAllForTrip(tripId!!) // Reusing ExpenseDao which accesses "expenses" table
                 
                 runOnUiThread {
                     itineraryList.clear()
                     for (entity in localItems) {
                         // Map Entity to Domain Model
                         val item = ItineraryItem(
                             id = entity.id,
                             title = entity.title,
                             time = entity.time,
                             description = "",
                             day = entity.day,
                             type = entity.type,
                             date = entity.date,
                             cost = entity.cost.toString()
                         )
                         itineraryList.add(item)
                     }
                     
                     // Sort
                     itineraryList.sortWith(compareBy<ItineraryItem> { 
                            it.day.toIntOrNull() ?: Int.MAX_VALUE 
                     }.thenBy { it.time })
                        
                     adapter.notifyDataSetChanged()
                 }
             }
        }
    }

    private fun showAddActivityDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_itinerary, null)
        val etDay = dialogView.findViewById<EditText>(R.id.etDay)
        val etTitle = dialogView.findViewById<EditText>(R.id.etActivityTitle)
        val etType = dialogView.findViewById<EditText>(R.id.etType)
        val etTime = dialogView.findViewById<EditText>(R.id.etActivityTime)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etCost = dialogView.findViewById<EditText>(R.id.etCost)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val day = etDay.text.toString().trim()
                val title = etTitle.text.toString().trim()
                val type = etType.text.toString().trim()
                val time = etTime.text.toString().trim()
                val date = etDate.text.toString().trim()
                val cost = etCost.text.toString().trim()

                if (title.isNotEmpty() && day.isNotEmpty()) {
                    saveItineraryItem(day, title, type, time, date, cost)
                } else {
                    Toast.makeText(this, "Day and Title are mandatory", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveItineraryItem(day: String, title: String, type: String, time: String, date: String, cost: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && tripId != null) {
            val userId = currentUser.uid
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("trips").child(tripId!!).child("itinerary")
            
            val itemId = databaseReference.push().key ?: return
            
            // 1. Save to Local DB (Room) IMMEDIATELY
            val costVal = cost.toDoubleOrNull() ?: 0.0
            val expenseEntity = com.example.smd_project_travel_planner.data.ExpenseEntity(
                id = itemId,
                tripId = tripId!!,
                day = day,
                title = title,
                type = type,
                time = time,
                date = date,
                cost = costVal,
                isSynced = false
            )
            
            val db = com.example.smd_project_travel_planner.data.AppDatabase.getDatabase(this)
            
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                db.expenseDao().insert(expenseEntity)
                
                // 2. Try to Sync to Firebase
                if (isNetworkAvailable()) {
                    val item = ItineraryItem(itemId, title, time, "", day, type, date, cost)
                    databaseReference.child(itemId).setValue(item)
                        .addOnSuccessListener {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                db.expenseDao().markSynced(itemId)
                            }
                            runOnUiThread { Toast.makeText(this@itenary, "Activity Added (Synced)", Toast.LENGTH_SHORT).show() }
                        }
                        .addOnFailureListener {
                            runOnUiThread { Toast.makeText(this@itenary, "Saved Locally (Sync Failed)", Toast.LENGTH_SHORT).show() }
                        }
                } else {
                     runOnUiThread { Toast.makeText(this@itenary, "Saved to Local Storage (Offline)", Toast.LENGTH_SHORT).show() }
                }
                
                // Refresh API
                loadItinerary()
            }
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}