package com.ade.evernym.activities.proofRequest

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.sdk.handlers.ProofRequestHandler
import com.ade.evernym.sdk.models.DIDCredential
import com.ade.evernym.sdk.models.DIDProofRequest
import com.bumptech.glide.Glide
import org.json.JSONObject

class ProofRequestAdapter(private val proofRequest: DIDProofRequest) :
    RecyclerView.Adapter<ProofRequestAdapter.ViewHolder>() {

    private var list = JSONObject()
    private var keys = arrayListOf<String>()
    var proof = JSONObject()
    var onItemClick: (position: Int) -> Unit = {}

    init {
        ProofRequestHandler.getCredentialOptions(this.proofRequest) { options, error ->
            error?.let {
                Log.e("ProofRequestAdapter", "init: (1) $it")
                return@getCredentialOptions
            }
            list = options!!
            keys.clear()
            options.keys().forEach { keys.add(it) }
            for (key in keys) {
                options.getJSONArray(key).getJSONObject(0).let {
                    val referent = it.getString("referent")
                    val value = it.getString("value")
                    this.proof.put(
                        key,
                        JSONObject().apply {
                            put("referent", referent)
                            put("value", value)
                        }
                    )
                }
                notifyItemRangeChanged(0, this.keys.count())
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            val item = proof.getJSONObject(keys[position])
            val value = item.getString("value")
            val credential = DIDCredential.getByReferent(item.getString("referent"))!!
            itemView.findViewById<TextView>(R.id.itemTextView).apply {
                text = value
            }
            Glide.with(itemView)
                .load(credential.connectionLogo)
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
        LayoutInflater.from(parent.context).inflate(R.layout.item_proof, parent, false).let {
            return ViewHolder(it)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = keys.count()


}