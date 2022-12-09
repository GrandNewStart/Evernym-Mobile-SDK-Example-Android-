package com.ade.evernym.activities.proofRequestList

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.activities.proofRequest.ProofRequestActivity
import com.ade.evernym.sdk.SDKStorage

class ProofRequestListActivity: AppCompatActivity() {

    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proof_request_list)
        setupRecyclerView()
        SDKStorage.proofRequestsLiveData.observe(this) {
            this.setupRecyclerView()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupRecyclerView() {
        findViewById<TextView>(R.id.messageTextView).text = "ProofRequests(${SDKStorage.proofRequests.count()})"
        recyclerView.apply {
            adapter = ProofRequestAdapter().apply {
                onItemClick = { position ->
                    val intent = Intent(this@ProofRequestListActivity, ProofRequestActivity::class.java).apply {
                        putExtra("id", SDKStorage.connections[position].id)
                    }
                    startActivity(intent)
                }
            }
        }
    }

}