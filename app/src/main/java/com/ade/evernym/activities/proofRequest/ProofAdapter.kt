package com.ade.evernym.activities.proofRequest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.bumptech.glide.Glide
import org.json.JSONArray
import org.json.JSONObject

class ProofAdapter(private val items: JSONArray, private var selectedReferent: String) :
    RecyclerView.Adapter<ProofAdapter.ViewHolder>() {

    var onItemClick: (item: JSONObject) -> Unit = {}

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            val item = items.getJSONObject(position)
            val isSelected = item.getString("referent") == selectedReferent
            val logo = item.getString("connectionLogo")
            val credentialName = item.getString("credentialName")
            val value = item.getString("value")
            itemView.apply {
                findViewById<TextView>(R.id.titleTextView).text = credentialName
                findViewById<TextView>(R.id.detailTextView).text = value
                Glide.with(this)
                    .load(logo)
                    .placeholder(R.drawable.ic_baseline_image_24)
                    .into(findViewById(R.id.imageView))
                setOnClickListener {
                    selectedReferent = item.getString("referent")
                    notifyItemRangeChanged(0, items.length())
                    onItemClick(item)
                }
                setBackgroundColor(
                    if (isSelected)
                        context.getColor(R.color.gray) else
                        context.getColor(R.color.clear)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_proof_option, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.length()

}