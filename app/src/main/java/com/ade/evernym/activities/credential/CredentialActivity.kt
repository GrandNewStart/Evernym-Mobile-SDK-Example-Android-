package com.ade.evernym.activities.credential

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ade.evernym.App
import com.ade.evernym.R
import com.ade.evernym.getStringOptional
import com.ade.evernym.handleBase64Scheme
import com.ade.evernym.sdk.handlers.CredentialHandler
import com.ade.evernym.sdk.models.DIDCredential
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import org.json.JSONArray

class CredentialActivity: AppCompatActivity() {

    private lateinit var credential: DIDCredential

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
            DIDCredential.getById(it)?.let { credential ->
                this.credential = credential
                this.title = credential.name.handleBase64Scheme()
                this.setupTextViews()
                this.setupImageView()
                this.setupButtons()
                return
            }
            finish()
        }
        App.shared.isLoading.observe(this) { this.showLoadingScreen(it) }
        App.shared.progressText.observe(this) { this.setMessage(it) }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    @SuppressLint("SetTextI18n")
    private fun setupTextViews() {
        runOnUiThread {
            titleTextView.text = credential.name.handleBase64Scheme()
            var detailText = ""
            detailText += "ID: ${credential.id}\n\n"
            detailText += "THREAD ID: ${credential.threadId}\n\n"
            detailText += "REFERENT: ${credential.referent}\n\n"
            detailText += "DEFINITION ID: ${credential.definitionId}\n\n"
            detailText += "NAME: ${credential.name.handleBase64Scheme()}\n\n"
            detailText += "CONNECTION ID: ${credential.connectionId}\n\n"
            detailText += "CONNECTION NAME: ${credential.connectionName.handleBase64Scheme()}\n\n"
            detailText += "STATUS: ${credential.status}\n\n"
            detailText += "CREATED AT: ${credential.createdAt}\n\n"
            val attributes = JSONArray(credential.attributes)
            for (i in 0 until attributes.length()) {
                attributes.getJSONObject(i).apply {
                    val name = getStringOptional("name")
                    val value = getStringOptional("value")
                    if (name != null && value != null) {
                        detailText += "${name.handleBase64Scheme()} : ${value.handleBase64Scheme()}\n"
                    }
                }
            }
            detailTextView.text = detailText
        }
    }

    private fun setupImageView() {
        Glide.with(this)
            .load(credential.connectionLogo)
            .placeholder(R.drawable.ic_baseline_image_24)
            .into(imageView)
    }

    private fun setupButtons() {
        runOnUiThread {
            if (credential.status == "pending") {
                acceptButton.visibility = View.VISIBLE
                rejectButton.visibility = View.VISIBLE
                acceptButton.text = "Accept"
                rejectButton.text = "Reject"
                acceptButton.setOnClickListener { this.accept() }
                rejectButton.setOnClickListener { this.reject() }
            }
            if (credential.status == "accepted") {
                acceptButton.visibility = View.GONE
                rejectButton.visibility = View.VISIBLE
                rejectButton.text = "Delete"
                rejectButton.setOnClickListener { this.reject() }
            }
        }
    }

    private fun showLoadingScreen(show: Boolean) {
        loadingScreen.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setMessage(message: String?) {
        this.progressTextView.text = message
    }

    private fun accept() {
        showLoadingScreen(true)
        setMessage("Accepting credential...")
        CredentialHandler.acceptCredential(this.credential) { credential, error ->
            error?.let {
                runOnUiThread {
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                    this@CredentialActivity.showLoadingScreen(false)
                    this@CredentialActivity.setMessage("Credential acceptance failed")
                }
                return@acceptCredential
            }
            runOnUiThread {
                this@CredentialActivity.showLoadingScreen(false)
                this@CredentialActivity.setMessage("Credential accepted")
                Toast.makeText(this, "Accepted", Toast.LENGTH_SHORT).show()
            }
            this.credential = credential!!
            this.setupTextViews()
            this.setupButtons()
        }
    }

    private fun reject() {
        showLoadingScreen(true)
        setMessage("Deleting credential...")
        CredentialHandler.rejectCredential(this.credential) { error ->
            error?.let {
                runOnUiThread {
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                    this@CredentialActivity.showLoadingScreen(false)
                    this@CredentialActivity.setMessage("Credential delete failed")
                }
                return@rejectCredential
            }
            runOnUiThread {
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                this@CredentialActivity.showLoadingScreen(false)
                this@CredentialActivity.setMessage("Credential deleted")
            }
            finish()
        }
    }

}