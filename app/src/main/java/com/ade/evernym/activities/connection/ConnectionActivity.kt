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
    private val progressTextView: TextView by lazy { findViewById(R.id.progressTextView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        intent.getStringExtra("id")?.let {
            DIDConnection.getById(it)?.let { connection ->
                this.connection = connection
                this.title = connection.name.handleBase64Scheme()
                setupTextViews()
                setupImageView()
                setupButtons()
                return
            }
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
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
            rejectButton.text = "Reject"
            acceptButton.setOnClickListener { this.accept() }
            rejectButton.setOnClickListener { this.reject() }
        }
        if (connection.status == "accepted") {
            acceptButton.visibility = View.GONE
            rejectButton.visibility = View.VISIBLE
            rejectButton.text = "Disconnect"
            rejectButton.setOnClickListener { this.reject() }
        }
    }

    private fun showLoadingScreen(show: Boolean) {
        loadingScreen.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setMessage(message: String?) {
        progressTextView.text = message
    }

    private fun accept() {
        showLoadingScreen(true)
        setMessage("Connecting...")
        ConnectionHandler.acceptConnection(connection) { updatedConnection, error ->
            error?.let {
                runOnUiThread {
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                    this@ConnectionActivity.showLoadingScreen(false)
                    this@ConnectionActivity.setMessage("Connection failed")
                }
                Log.e("ConnectionActivity", "setupButtons: $error")
                return@acceptConnection
            }
            runOnUiThread {
                Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()
                this@ConnectionActivity.showLoadingScreen(false)
                this@ConnectionActivity.setMessage("Connection success")
            }
            this.connection = updatedConnection!!
            setupTextViews()
            setupButtons()
            showLoadingScreen(false)
        }
    }

    private fun reject() {
        showLoadingScreen(true)
        setMessage("Deleting...")
        ConnectionHandler.deleteConnection(this.connection) { error ->
            error?.let {
                runOnUiThread {
                    this@ConnectionActivity.showLoadingScreen(false)
                    this@ConnectionActivity.setMessage("Connection delete failed")
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                }
                Log.e("ConnectionActivity", it)
                return@deleteConnection
            }
            runOnUiThread {
                Toast.makeText(this, "Connection deleted", Toast.LENGTH_SHORT).show()
                this@ConnectionActivity.showLoadingScreen(false)
                this@ConnectionActivity.setMessage("Connection deleted")
                finish()
            }
        }
    }

}