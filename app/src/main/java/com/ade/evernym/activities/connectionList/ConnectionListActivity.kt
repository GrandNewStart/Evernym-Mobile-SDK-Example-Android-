package com.ade.evernym.activities.connectionList

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.activities.connection.ConnectionActivity
import com.ade.evernym.sdk.SDKStorage

class ConnectionListActivity: AppCompatActivity() {

    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        this.title = "Connections(${SDKStorage.connections.count()})"
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupRecyclerView()
        SDKStorage.connectionsLiveData.observe(this) {
            setupRecyclerView()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    @SuppressLint("SetTextI18n")
    private fun setupRecyclerView() {
        recyclerView.apply {
            this@ConnectionListActivity.title = "Connections(${SDKStorage.connections.count()})"
            adapter = ConnectionAdapter().apply {
                onItemClick = { position ->
                    val intent = Intent(this@ConnectionListActivity, ConnectionActivity::class.java).apply {
                        putExtra("id", SDKStorage.connections[position].id)
                    }
                    startActivity(intent)
                }
            }
        }
    }

}