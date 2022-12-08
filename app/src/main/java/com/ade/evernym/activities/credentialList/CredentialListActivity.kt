package com.ade.evernym.activities.credentialList

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.sdk.SDKStorage

class CredentialListActivity: AppCompatActivity() {

    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_list)
        setupRecyclerView()
    }

    @SuppressLint("SetTextI18n")
    private fun setupRecyclerView() {
        findViewById<TextView>(R.id.messageTextView).text = "Credentials(${SDKStorage.credentials.count()})"
        recyclerView.apply {
            adapter = CredentialAdapter().apply {
                onItemClick = { position ->

                }
            }
        }
    }

}