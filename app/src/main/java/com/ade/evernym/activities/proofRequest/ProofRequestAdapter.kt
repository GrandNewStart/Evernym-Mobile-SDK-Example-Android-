package com.ade.evernym.activities.proofRequest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.getJSONObjectOptional
import com.ade.evernym.handleBase64Scheme
import com.ade.evernym.sdk.models.DIDCredential
import com.bumptech.glide.Glide
import org.json.JSONObject

class ProofRequestAdapter(
    private val keys: ArrayList<String>,
    private val proof: JSONObject
) :
    RecyclerView.Adapter<ProofRequestAdapter.ViewHolder>() {

    var onItemClick: (position: Int) -> Unit = {}

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            val item = proof.getJSONObjectOptional(keys[position])
            itemView.apply {
                val textView = findViewById<TextView>(R.id.itemTextView)
                textView.text = "Missing"
                textView.setTextColor(context.getColor(R.color.red))
                if (item != null) {
                    textView.text = (item.getString("value") ?: "Unknown").handleBase64Scheme()
                    textView.setTextColor(context.getColor(R.color.black))
                    item.getString("referent")?.let { referent ->
                        val credential = DIDCredential.getByReferent(referent)!!
                        Glide.with(itemView)
                            .load(credential.connectionLogo)
                            .placeholder(R.drawable.ic_baseline_image_24)
                            .into(findViewById(R.id.imageView))
                    }
                }
                setOnClickListener { onItemClick(position) }
            }
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
        LayoutInflater.from(parent.context).inflate(R.layout.item_proof, parent, false).let {
            return ViewHolder(it)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = keys.count()


}