package com.example.smd_project_travel_planner

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SearchUsersActivity : AppCompatActivity() {

    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<User>
    private lateinit var adapter: UserAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var searchInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search_users)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()

        userList = ArrayList()
        adapter = UserAdapter(this, userList)

        userRecyclerView = findViewById(R.id.userRecyclerView)
        searchInput = findViewById(R.id.etSearchUser) // You'll need to use search box from layout if available or I will create one.

        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter

        // Back Button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Fetch Users
        mDbRef.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (postSnapshot in snapshot.children) {
                    val uid = postSnapshot.key
                    val username = postSnapshot.child("Username").getValue(String::class.java)
                    val email = postSnapshot.child("email").getValue(String::class.java)
                    val profileImage = postSnapshot.child("profileImage").getValue(String::class.java)

                    val currentUser = User(uid, username, email, profileImage)

                    if (mAuth.currentUser?.uid != currentUser.uid && username != null) {
                        userList.add(currentUser)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        
        // Search Filter
        searchInput.addTextChangedListener(object: TextWatcher {
             override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
             override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                 filter(s.toString())
             }
             override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun filter(text: String) {
        val filteredList = ArrayList<User>()
        for (user in userList) {
            if (user.username?.lowercase()?.contains(text.lowercase()) == true) {
                filteredList.add(user)
            }
        }
        adapter = UserAdapter(this@SearchUsersActivity, filteredList)
        userRecyclerView.adapter = adapter
    }
}
