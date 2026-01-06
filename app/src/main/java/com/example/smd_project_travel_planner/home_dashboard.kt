package com.example.smd_project_travel_planner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class home_dashboard : AppCompatActivity() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted.
        } else {
            // Permission is denied.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setupClickListeners()
        setupBottomNav()
        startBudgetMonitoring()
        
        // Setup Live Network Sync
        setupNetworkCallback()
    }
    
    private fun setupNetworkCallback() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val builder = NetworkRequest.Builder()
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Trigger Sync when network becomes available
                syncPendingExpenses()
            }
        }
        
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        
        // Initial check
        syncPendingExpenses() 
        
        // Update FCM Token for Notifications
        updateFCMToken()
    }
    
    private fun updateFCMToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    val userId = currentUser.uid
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                    
                    userRef.child("fcmToken").setValue(token)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
    
    private fun syncPendingExpenses() {
        val db = com.example.smd_project_travel_planner.data.AppDatabase.getDatabase(this)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            // 1. Sync Expenses/Itinerary
            val unsyncedItems = db.expenseDao().getUnsynced()
            if (unsyncedItems.isNotEmpty()) {
                val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("trips")
                for (item in unsyncedItems) {
                    val firebaseItem = ItineraryItem(item.id, item.title, item.time, "", item.day, item.type, item.date, item.cost.toString())
                    dbRef.child(item.tripId).child("itinerary").child(item.id).setValue(firebaseItem)
                        .addOnSuccessListener {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { db.expenseDao().markSynced(item.id) }
                        }
                }
            }
            
            // 2. Sync Trips
            val unsyncedTrips = db.tripDao().getUnsynced()
            if (unsyncedTrips.isNotEmpty()) {
                val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("trips")
                for (trip in unsyncedTrips) {
                    val firebaseTrip = Trip(trip.id, trip.title, trip.destination, trip.startDate, trip.endDate, trip.imageBase64)
                    dbRef.child(trip.id).setValue(firebaseTrip)
                        .addOnSuccessListener {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { db.tripDao().markSynced(trip.id) }
                        }
                }
                
                 runOnUiThread { 
                   android.widget.Toast.makeText(this@home_dashboard, "Syncing ${unsyncedTrips.size} trips & ${unsyncedItems.size} items...", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else if (unsyncedItems.isNotEmpty()) {
                 runOnUiThread { 
                   android.widget.Toast.makeText(this@home_dashboard, "Syncing ${unsyncedItems.size} items...", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        val exp = findViewById<ImageView>(R.id.Expenses)
        val m = findViewById<ImageView>(R.id.Maps)
        val t = findViewById<Button>(R.id.btnAddTrip)
        val cardTrips = findViewById<LinearLayout>(R.id.cardTrips)
        val cardPlanner = findViewById<LinearLayout>(R.id.cardPlanner)
        val cardExpenses = findViewById<LinearLayout>(R.id.cardExpenses)
        val cardMaps = findViewById<LinearLayout>(R.id.cardMaps)

        val tripsIntent = Intent(this, ViewTrips::class.java)
        val expensesIntent = Intent(this, expenses::class.java)
        val mapsIntent = Intent(this, maps::class.java)
        val plannerIntent = Intent(this, SearchUsersActivity::class.java)
        val galleryIntent = Intent(this, TripGalleryActivity::class.java)

        cardTrips.setOnClickListener { startActivity(tripsIntent) }
        cardPlanner.setOnClickListener { startActivity(plannerIntent) }
        cardExpenses.setOnClickListener { startActivity(galleryIntent) } // Mapped to Gallery
        cardMaps.setOnClickListener { startActivity(mapsIntent) }
        
        // Settings / Profile Button
        findViewById<android.widget.ImageButton>(R.id.btnSettings).setOnClickListener {
             startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Add Trip Button
        t.setOnClickListener {
            startActivity(Intent(this, create_trip::class.java))
        }
    }
    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.btnNavTrips).setOnClickListener {
            startActivity(Intent(this, ViewTrips::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavPlanner).setOnClickListener {
            startActivity(Intent(this, itenary::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavExpenses).setOnClickListener {
            startActivity(Intent(this, TripGalleryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavMaps).setOnClickListener {
            startActivity(Intent(this, maps::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }

    private fun startBudgetMonitoring() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val tripsRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("trips")

        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                for (tripSnapshot in snapshot.children) {
                    processTripBudget(tripSnapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BudgetMonitor", "Failed to read trips", error.toException())
            }
        })
    }

    private fun processTripBudget(tripSnapshot: DataSnapshot) {
        val budgetSnapshot = tripSnapshot.child("budget")
        val itinerarySnapshot = tripSnapshot.child("itinerary")
        val tripTitle = tripSnapshot.child("title").getValue(String::class.java) ?: "Trip"

        // Map Day -> Budget Amount
        val dayBudgets = mutableMapOf<String, Double>()
        for (daySnap in budgetSnapshot.children) {
            val day = daySnap.key ?: continue
            val amount = daySnap.getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
            dayBudgets[day] = amount
        }

        // Map Day -> Total Spent
        val daySpent = mutableMapOf<String, Double>()
        for (itemSnap in itinerarySnapshot.children) {
            val item = itemSnap.getValue(ItineraryItem::class.java) ?: continue
            val cost = item.cost.toDoubleOrNull() ?: 0.0
            if (cost > 0) {
                val current = daySpent.getOrDefault(item.day, 0.0)
                daySpent[item.day] = current + cost
            }
        }

        // Check for violations
        for ((day, budget) in dayBudgets) {
            val spent = daySpent[day] ?: 0.0
            if (budget > 0 && spent > budget) {
                sendBudgetNotification(tripTitle, day, budget, spent)
            }
        }
    }

    private fun sendBudgetNotification(tripTitle: String, day: String, budget: Double, spent: Double) {
        val channelId = "budget_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Budget Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = (tripTitle + day).hashCode()

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Budget Exceeded: $tripTitle")
            .setContentText("Day $day: Spent $$spent (Limit: $$budget)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}