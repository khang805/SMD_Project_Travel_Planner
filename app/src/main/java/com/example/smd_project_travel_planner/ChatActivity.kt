package com.example.smd_project_travel_planner

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
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

class ChatActivity : AppCompatActivity() {

    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference
    private lateinit var tvName: TextView
    private lateinit var imgProfile: de.hdodenhof.circleimageview.CircleImageView

    var receiverRoom: String? = null
    var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
             val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
             v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
             insets
        }

        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        val profileImage = intent.getStringExtra("profileImage") 

        mDbRef = FirebaseDatabase.getInstance().getReference()

        senderRoom = receiverUid + "_" + senderUid
        receiverRoom = senderUid + "_" + receiverUid
        
        tvName = findViewById(R.id.tvName)
        imgProfile = findViewById(R.id.imgProfile)
        tvName.text = name

        // Set Profile Image
        if (!profileImage.isNullOrEmpty()) {
            try {
                val decodedString = android.util.Base64.decode(profileImage, android.util.Base64.DEFAULT)
                val decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                imgProfile.setImageBitmap(decodedByte)
            } catch (e: Exception) {
                imgProfile.setImageResource(R.drawable.img_1)
            }
        }

        messageRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sentButton)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)

        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = messageAdapter
        
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Logic for adding data to recyclerView
        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for(postSnapshot in snapshot.children){
                        val message = postSnapshot.getValue(Message::class.java)
                        messageList.add(message!!)
                    }
                    messageAdapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        messageRecyclerView.smoothScrollToPosition(messageList.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

        // Adding the message to database
        sendButton.setOnClickListener {
            val message = messageBox.text.toString()
            if (message.isNotEmpty()) {
                val messageObject = Message(message, senderUid, System.currentTimeMillis())

                mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        mDbRef.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
                    }
                messageBox.setText("")
            }
        }
    }
}
