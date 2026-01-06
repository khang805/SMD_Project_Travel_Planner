package com.example.smd_project_travel_planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItineraryAdapter(private val itineraryList: List<ItineraryItem>) :
    RecyclerView.Adapter<ItineraryAdapter.ItineraryViewHolder>() {

    class ItineraryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayHeader: TextView = itemView.findViewById(R.id.tvDayHeader)
        val tvTitle: TextView = itemView.findViewById(R.id.tvItineraryTitle)
        val tvType: TextView = itemView.findViewById(R.id.tvItineraryType)
        val tvTime: TextView = itemView.findViewById(R.id.tvItineraryTime)
        val tvCost: TextView = itemView.findViewById(R.id.tvItineraryCost)
        val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItineraryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itinerary, parent, false)
        return ItineraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItineraryViewHolder, position: Int) {
        val item = itineraryList[position]
        
        // Bind Basic Data
        holder.tvTitle.text = item.title
        holder.tvType.text = item.type
        holder.tvTime.text = item.time
        holder.tvCost.text = item.cost

        // Logic for Day Header
        // Display header if this is the first item OR if the day is different from the previous item
        val showHeader = if (position == 0) {
            true
        } else {
            val prevItem = itineraryList[position - 1]
            item.day != prevItem.day
        }

        if (showHeader) {
            holder.tvDayHeader.visibility = View.VISIBLE
            holder.tvDayHeader.text = "Day ${item.day}"
            if (item.date.isNotEmpty()) {
                holder.tvDayHeader.text = "Day ${item.day} - ${item.date}"
            }
        } else {
            holder.tvDayHeader.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return itineraryList.size
    }
}
