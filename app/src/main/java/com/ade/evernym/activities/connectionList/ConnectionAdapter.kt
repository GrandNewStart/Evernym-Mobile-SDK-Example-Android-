package com.ade.evernym.activities.connectionList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.handleBase64Scheme
import com.ade.evernym.sdk.SDKStorage
import com.bumptech.glide.Glide

class ConnectionAdapter : RecyclerView.Adapter<ConnectionAdapter.ViewHolder>() {

    private var list = SDKStorage.connections
    var onItemClick: (position: Int) -> Unit = {}

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            itemView.findViewById<TextView>(R.id.itemTextView).apply {
                val connection = list[position]
                text = connection.name.handleBase64Scheme()
                setTextColor(resources.getColor(if (connection.status == "pending") R.color.gray else R.color.black, null))
            }
            Glide.with(itemView)
                .load(list[position].logo)
                .placeholder(R.drawable.ic_baseline_image_24)
                .into(itemView.findViewById(R.id.imageView))
            itemView.setOnClickListener { onItemClick(position) }
        }

    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.apply {
            if (layoutManager == null) {
                layoutManager =
                    LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        LayoutInflater.from(parent.context).inflate(R.layout.item_main, parent, false).let {
            return ViewHolder(it)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = list.count()

}