package com.example.smd_project_travel_planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GalleryAdapter(private val imageUrls: List<String>) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imgGalleryItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Create a simple explicit layout for item instead of separate file if simple
        // Or better, create a simple item layout. 
        // Let's assume R.layout.item_gallery_image exists or create it.
        // I will create it in next step. For now referencing it.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = imageUrls[position]
        Glide.with(holder.itemView.context)
            .load(url)
            .centerCrop()
            .into(holder.imageView)
            
        holder.itemView.setOnClickListener {
            // Optional: Open full screen
        }
    }

    override fun getItemCount(): Int {
        return imageUrls.size
    }
}
