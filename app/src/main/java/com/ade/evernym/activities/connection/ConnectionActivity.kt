package com.ade.evernym.activities.connection

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ade.evernym.R
import com.ade.evernym.handleBase64Scheme
import com.ade.evernym.sdk.handlers.ConnectionHandler
import com.ade.evernym.sdk.models.DIDConnection
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class ConnectionActivity : AppCompatActivity() {

    private lateinit var connection: DIDConnection

    private val titleTextView: TextView by lazy { findViewById(R.id.titleTextView) }
    private val imageView: ImageView by lazy { findViewById(R.id.imageView) }
    private val detailTextView: TextView by lazy { findViewById(R.id.detailTextView) }
    private val acceptButton: MaterialButton by lazy { findViewById(R.id.acceptButton) }
    private val rejectButton: MaterialButton by lazy { findViewById(R.id.rejectButton) }
    private val loadingScreen: FrameLayout by lazy { findViewById(R.id.loadingScreen) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        intent.getStringExtra("id")?.let { this.connection = DIDConnection.getById(it) }
        setupTextViews()
        setupImageView()
        setupButtons()
    }

    @SuppressLint("SetTextI18n")
    private fun setupTextViews() {
        titleTextView.text = connection.name.handleBase64Scheme()
        detailTextView.text = """
                ID: ${connection.id}
                STATUS: ${connection.status}
                PW DID: ${connection.pwDid}
            """.trimIndent()
    }

    private fun setupImageView() {
        Glide.with(this)
            .load(connection.logo)
            .placeholder(R.drawable.ic_baseline_image_24)
            .into(imageView)
    }

    private fun setupButtons() {
        if (connection.status == "pending") {
            acceptButton.visibility = View.VISIBLE
            rejectButton.visibility = View.VISIBLE
            acceptButton.setOnClickListener {
                showLoadingScreen(true)
                ConnectionHandler.acceptConnection(connection) { updatedConnection, error ->
                    runOnUiThread {
                        error?.let {
                            Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                            Log.e("ConnectionActivity", "setupButtons: $error")
                            return@runOnUiThread
                        }
                        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()
                        this.connection = updatedConnection!!
                        setupTextViews()
                        setupButtons()
                        showLoadingScreen(false)
                    }
                }
            }
            rejectButton.setOnClickListener {
                runOnUiThread {
                    DIDConnection.delete(connection)
                    finish()
                }
            }
        }
        if (connection.status == "accepted") {
            acceptButton.visibility = View.GONE
            rejectButton.visibility = View.VISIBLE
            rejectButton.setOnClickListener {
                runOnUiThread {
                    DIDConnection.delete(connection)
                    finish()
                }
            }
        }
    }

    private fun showLoadingScreen(show: Boolean) {
        loadingScreen.visibility = if (show) View.VISIBLE else View.GONE
    }

}