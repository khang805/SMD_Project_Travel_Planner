package com.example.smd_project_travel_planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExpenseAdapter(private val expenseList: List<ExpenseItem>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvExpenseTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvExpenseCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvExpenseAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val item = expenseList[position]
        holder.tvTitle.text = item.title
        holder.tvCategory.text = item.category
        holder.tvAmount.text = "$${item.amount}"
    }

    override fun getItemCount(): Int {
        return expenseList.size
    }
}
