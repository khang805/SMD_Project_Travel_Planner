package com.example.smd_project_travel_planner

import com.example.smd_project_travel_planner.data.AppDatabase
import com.example.smd_project_travel_planner.data.TripEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ViewTrips : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var tripList: MutableList<Trip>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var tvNoTrips: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_trips)

        recyclerView = findViewById(R.id.recyclerViewTrips)
        recyclerView.layoutManager = LinearLayoutManager(this)
        tvNoTrips = findViewById(R.id.tvNoTrips)

        tripList = mutableListOf()
        adapter = TripAdapter(tripList)
        recyclerView.adapter = adapter
        
        loadTrips()
    }
    
    private fun loadTrips() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = AppDatabase.getDatabase(this)
        
        // 1. Load from Local DB (Room) - Display immediately
        CoroutineScope(Dispatchers.IO).launch {
            val localTrips = db.tripDao().getAll()
            
            withContext(Dispatchers.Main) {
                updateUI(localTrips)
            }
            
            // 2. If Online, Sync from Firebase -> Room
            if (isNetworkAvailable()) {
                val userId = currentUser.uid
                databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("trips")
                
                databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                         // Get list of Firebase IDs
                         val firebaseTripIds = mutableSetOf<String>()
                         
                         if (snapshot.exists()) {
                             val firebaseTrips = mutableListOf<TripEntity>()
                             for (tripSnapshot in snapshot.children) {
                                 val trip = tripSnapshot.getValue(Trip::class.java)
                                 if (trip != null) {
                                     firebaseTripIds.add(trip.tripId) // Track cloud IDs
                                     
                                     // Convert Trip -> TripEntity
                                     val entity = TripEntity(
                                         id = trip.tripId,
                                         title = trip.title,
                                         destination = trip.destination,
                                         startDate = trip.startDate,
                                         endDate = trip.endDate,
                                         imageBase64 = trip.imageBase64,
                                         isSynced = true // Coming from cloud, so it is synced
                                     )
                                     firebaseTrips.add(entity)
                                 }
                             }
                             
                             // Save Updates to Room
                             CoroutineScope(Dispatchers.IO).launch {
                                 // 1. Insert/Update from Cloud
                                 for (entity in firebaseTrips) {
                                     db.tripDao().insert(entity)
                                 }
                                 
                                 // 2. Identify and Delete Deleted Trips
                                 // Get all local trips, if a local trip ID is NOT in firebaseTripIds, delete it.
                                 // NOTE: careful not to delete partial unsynced stuff? 
                                 // Actually, if we are calling this "Sync Down", we assume Cloud is Truth.
                                 // But we have "Offline Created" trips with isSynced=false. We should NOT delete those.
                                 
                                 val allLocalTrips = db.tripDao().getAll()
                                 for (localTrip in allLocalTrips) {
                                     // Only delete if it WAS synced (meaning it existed on server) and now is gone.
                                     // OR if we assume all non-pending trips should match cloud.
                                     // Safer: If it is marked as `isSynced=true` locally, but not found in Firebase list, DELETE IT.
                                     if (localTrip.isSynced && !firebaseTripIds.contains(localTrip.id)) {
                                         db.tripDao().deleteById(localTrip.id)
                                     }
                                 }

                                 // Reload fully from Room to capture everything
                                 val updatedTrips = db.tripDao().getAll()
                                 withContext(Dispatchers.Main) {
                                     updateUI(updatedTrips)
                                 }
                             }
                         } else {
                             // Snapshot empty? Meaning NO trips on server.
                             // Delete all `isSynced=true` trips locally.
                             CoroutineScope(Dispatchers.IO).launch {
                                 val allLocalTrips = db.tripDao().getAll()
                                 for (localTrip in allLocalTrips) {
                                     if (localTrip.isSynced) {
                                         db.tripDao().deleteById(localTrip.id)
                                     }
                                 }
                                 val updatedTrips = db.tripDao().getAll()
                                 withContext(Dispatchers.Main) {
                                     updateUI(updatedTrips)
                                 }
                             }
                         }
                    }

                    override fun onCancelled(error: DatabaseError) {
                         // Failed to sync down
                    }
                })
            }
        }
    }
    
    private fun updateUI(entities: List<TripEntity>) {
        tripList.clear()
        for (entity in entities) {
            val trip = Trip(entity.id, entity.title, entity.destination, entity.startDate, entity.endDate, entity.imageBase64)
            tripList.add(trip)
        }
        adapter.notifyDataSetChanged()
        
        if (tripList.isEmpty()) {
            tvNoTrips.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoTrips.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}
