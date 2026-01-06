package com.example.smd_project_travel_planner

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(private val context: Context, private val userList: ArrayList<User>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.textName.text = currentUser.username

        // Base64 Image Logic
        val profileImage = currentUser.profileImage
        if (!profileImage.isNullOrEmpty()) {
            try {
                val decodedString = Base64.decode(profileImage, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                holder.imgProfile.setImageBitmap(decodedByte)
            } catch (e: Exception) {
                holder.imgProfile.setImageResource(R.drawable.img_1) // Fallback
            }
        } else {
            holder.imgProfile.setImageResource(R.drawable.img_1) // Default
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("name", currentUser.username)
            intent.putExtra("uid", currentUser.uid)
            intent.putExtra("profileImage", currentUser.profileImage) // Pass image too if needed
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName = itemView.findViewById<TextView>(R.id.tvUsername)
        val imgProfile = itemView.findViewById<CircleImageView>(R.id.imgProfile)
    }
}
