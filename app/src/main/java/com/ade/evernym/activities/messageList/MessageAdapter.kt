package com.ade.evernym.activities.messageList

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDMessage
import com.bumptech.glide.Glide

class MessageAdapter(private val items: ArrayList<DIDMessage>) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    var onItemClick: (message: DIDMessage)->Unit = {}
    var onItemLongClick: (message: DIDMessage, view: View)->Boolean = { _, _ -> false }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            val message = items[position]
            val connection = DIDConnection.getByPwDid(message.pwDid)
            itemView.apply {
                findViewById<TextView>(R.id.connectionTextView).apply {
                    text = connection?.name ?: "Unknown Connection"
                }
                findViewById<TextView>(R.id.typeTextView).apply {
                    text = message.type
                }
                findViewById<ImageView>(R.id.imageView).apply {
                    connection?.let {
                        Glide.with(context)
                            .load(it.logo)
                            .placeholder(R.drawable.ic_baseline_image_24)
                            .into(this)
                    }
                }
                setOnClickListener { onItemClick(message) }
                setOnLongClickListener { onItemLongClick(message, this) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.count()

}