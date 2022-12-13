package com.ade.evernym.activities.proofRequestList

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.activities.proofRequest.ProofRequestActivity
import com.ade.evernym.sdk.SDKStorage

class ProofRequestListActivity: AppCompatActivity() {

    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        this.title = "ProofRequests(${SDKStorage.proofRequests.count()})"
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupRecyclerView()
        SDKStorage.proofRequestsLiveData.observe(this) {
            this.setupRecyclerView()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    @SuppressLint("SetTextI18n")
    private fun setupRecyclerView() {
        this@ProofRequestListActivity.title = "ProofRequests(${SDKStorage.proofRequests.count()})"
        recyclerView.apply {
            adapter = ProofRequestAdapter().apply {
                onItemClick = { position ->
                    val intent = Intent(this@ProofRequestListActivity, ProofRequestActivity::class.java).apply {
                        putExtra("id", SDKStorage.proofRequests[position].id)
                    }
                    startActivity(intent)
                }
            }
        }
    }

}