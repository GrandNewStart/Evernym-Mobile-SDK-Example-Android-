package com.ade.evernym.activities.connectionList

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.activities.connection.ConnectionActivity
import com.ade.evernym.sdk.SDKStorage

class ConnectionListActivity: AppCompatActivity() {

    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_list)
        setupRecyclerView()
        SDKStorage.connectionsLiveData.observe(this) {
            setupRecyclerView()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupRecyclerView() {
        findViewById<TextView>(R.id.messageTextView).text = "Connections(${SDKStorage.connections.count()})"
        recyclerView.apply {
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