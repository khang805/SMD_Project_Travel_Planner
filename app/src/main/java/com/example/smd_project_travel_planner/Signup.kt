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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import java.io.ByteArrayOutputStream

class Signup : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = Firebase.database.reference
    private lateinit var imgProfile: de.hdodenhof.circleimageview.CircleImageView
    private var selectedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        imgProfile = findViewById(R.id.imgProfileSignup)
        val tvSelectPhoto = findViewById<TextView>(R.id.tvSelectPhoto)

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val username = findViewById<EditText>(R.id.etUsername)
        val DOB = findViewById<EditText>(R.id.etDateOfBirth)
        val lname = findViewById<EditText>(R.id.etLastName)
        val fname = findViewById<EditText>(R.id.etName)
        val signUpBtn = findViewById<Button>(R.id.createAccountBtn)

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

        tvSelectPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(intent)
        }
        
        imgProfile.setOnClickListener {
             val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(intent)
        }

        signUpBtn.setOnClickListener {
            val emailText = email.text.toString()
            val passText = password.text.toString()
            val nameText = username.text.toString()
            val DOBText = DOB.text.toString()
            val lnameText = lname.text.toString()
            val fnameText = fname.text.toString()

            if (emailText.isEmpty() || passText.isEmpty() || nameText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailText, passText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                        
                        // Get FCM Token
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { taskToken ->
                            val fcmToken = if (taskToken.isSuccessful) taskToken.result else ""

                            // Image to Base64
                            var encodedImage = ""
                            if (selectedBitmap != null) {
                                val byteArrayOutputStream = ByteArrayOutputStream()
                                selectedBitmap?.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                                val byteArray = byteArrayOutputStream.toByteArray()
                                encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
                            }

                            // Create user data map
                            val userMap = mapOf(
                                "Username" to nameText,
                                "email" to emailText,
                                "Date-of-Birth" to DOBText,
                                "First-Name" to fnameText,
                                "Last-Name" to lnameText,
                                "profileSet" to true,
                                "profileImage" to encodedImage,
                                "fcmToken" to fcmToken
                            )

                            // Save user info to Realtime DB
                            database.child("users").child(userId).setValue(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Account Created! Please Login.", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, Login::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show()
                                }
                        }

                    } else {
                        Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}