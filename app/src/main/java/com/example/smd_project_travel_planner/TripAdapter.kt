package com.example.smd_project_travel_planner

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TripAdapter(private val tripList: List<Trip>) :
    RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgTrip: ImageView = itemView.findViewById(R.id.imgTrip)
        val tvTripTitle: TextView = itemView.findViewById(R.id.tvTripTitle)
        val tvTripDestination: TextView = itemView.findViewById(R.id.tvTripDestination)
        val tvTripDates: TextView = itemView.findViewById(R.id.tvTripDates)
        val btnViewExpenses: android.widget.Button = itemView.findViewById(R.id.btnViewExpenses)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]
        holder.tvTripTitle.text = trip.title
        holder.tvTripDestination.text = trip.destination
        holder.tvTripDates.text = "${trip.startDate} - ${trip.endDate}"

        if (trip.imageBase64.isNotEmpty()) {
            try {
                val decodedString = Base64.decode(trip.imageBase64, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                holder.imgTrip.setImageBitmap(decodedByte)
            } catch (e: Exception) {
                e.printStackTrace()
                holder.imgTrip.setImageResource(R.drawable.ic_launcher_background) // Fallback
            }
        } else {
             holder.imgTrip.setImageResource(R.drawable.ic_launcher_background) // Fallback
        }

        holder.itemView.setOnClickListener {
            val intent = android.content.Intent(holder.itemView.context, itenary::class.java)
            intent.putExtra("TRIP_ID", trip.tripId)
            intent.putExtra("TRIP_TITLE", trip.title)
            holder.itemView.context.startActivity(intent)
        }
        
        holder.btnViewExpenses.setOnClickListener {
             val intent = android.content.Intent(holder.itemView.context, expenses::class.java)
            intent.putExtra("TRIP_ID", trip.tripId)
            intent.putExtra("TRIP_TITLE", trip.title)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return tripList.size
    }
}
