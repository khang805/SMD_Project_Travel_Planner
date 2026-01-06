package com.example.smd_project_travel_planner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var imgProfile: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var btnChangePhoto: TextView
    private lateinit var btnDone: TextView
    private lateinit var btnCancel: TextView
    private lateinit var btnLogout: android.widget.Button
    
    // Fields
    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etWebsite: EditText
    private lateinit var etBio: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    
    private var selectedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAuth = FirebaseAuth.getInstance()
        val uid = mAuth.currentUser?.uid
        if (uid == null) {
            finish()
            return
        }
        mDbRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid)

        bindViews()
        loadCurrentProfile()
        setupListeners()
    }

    private fun bindViews() {
        imgProfile = findViewById(R.id.profilePic)
        btnChangePhoto = findViewById(R.id.changePhoto)
        btnDone = findViewById(R.id.btnDone)
        btnCancel = findViewById(R.id.btnCancel)
        btnLogout = findViewById(R.id.btnLogout)
        
        etName = findViewById(R.id.etName)
        etUsername = findViewById(R.id.etUsername)
        etWebsite = findViewById(R.id.etWebsite)
        etBio = findViewById(R.id.etBio)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
    }

    private fun loadCurrentProfile() {
        mDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    etName.setText(snapshot.child("Name").getValue(String::class.java) ?: "")
                    etUsername.setText(snapshot.child("Username").getValue(String::class.java) ?: "")
                    etWebsite.setText(snapshot.child("Website").getValue(String::class.java) ?: "")
                    etBio.setText(snapshot.child("Bio").getValue(String::class.java) ?: "")
                    etEmail.setText(snapshot.child("email").getValue(String::class.java) ?: "")
                    etPhone.setText(snapshot.child("Phone").getValue(String::class.java) ?: "")
                    
                    val encodedImage = snapshot.child("profileImage").getValue(String::class.java)
                    if (!encodedImage.isNullOrEmpty()) {
                         try {
                            val decodedString = Base64.decode(encodedImage, Base64.DEFAULT)
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            imgProfile.setImageBitmap(decodedByte)
                        } catch (e: Exception) {}
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) { }
        })
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { finish() }

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri: Uri? = data?.data
                if (imageUri != null) {
                    try {
                        val inputStream = contentResolver.openInputStream(imageUri)
                        selectedBitmap = BitmapFactory.decodeStream(inputStream)
                        imgProfile.setImageBitmap(selectedBitmap)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(intent)
        }

        btnDone.setOnClickListener {
            saveProfile()
        }

        btnLogout.setOnClickListener {
            mAuth.signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun saveProfile() {
        val updates = mutableMapOf<String, Any>()
        updates["Name"] = etName.text.toString()
        updates["Username"] = etUsername.text.toString()
        updates["Website"] = etWebsite.text.toString()
        updates["Bio"] = etBio.text.toString()
        updates["email"] = etEmail.text.toString()
        updates["Phone"] = etPhone.text.toString()

        if (selectedBitmap != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            selectedBitmap?.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val encodedImage: String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            updates["profileImage"] = encodedImage
        }

        mDbRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
            }
    }
}
