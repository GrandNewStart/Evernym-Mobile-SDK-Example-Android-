package com.ade.evernym.activities.credentialList

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.activities.credential.CredentialActivity
import com.ade.evernym.sdk.SDKStorage

class CredentialListActivity: AppCompatActivity() {

    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        this.title = "Credentials(${SDKStorage.credentials.count()})"
        setupRecyclerView()
        SDKStorage.credentialsLiveData.observe(this) {
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
            adapter = CredentialAdapter().apply {
                onItemClick = { position ->
                    startActivity(
                        Intent(this@CredentialListActivity, CredentialActivity::class.java).apply {
                            putExtra("id", SDKStorage.credentials[position].id)
                        }
                    )
                }
            }
        }
    }

}