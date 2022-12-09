package com.ade.evernym.activities.credential

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credential)
        intent.getStringExtra("id")?.let {
            DIDCredential.getById(it)?.let { credential ->
                this.credential = credential
                this.setupTextViews()
                this.setupImageView()
                this.setupButtons()
                return
            }
            finish()
        }
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
                acceptButton.setOnClickListener {
                    this.showLoadingScreen(true)
                    CredentialHandler.acceptCredential(this.credential) { credential, error ->
                        error?.let {
                            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                            return@acceptCredential
                        }
                        this.credential = credential!!
                        this.showLoadingScreen(false)
                        this.setupTextViews()
                        this.setupButtons()
                    }
                }
                rejectButton.setOnClickListener {
                    runOnUiThread {
                        CredentialHandler.rejectCredential(this.credential) { error ->
                            error?.let {
                                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                                return@rejectCredential
                            }
                            finish()
                        }
                    }
                }
            }
            if (credential.status == "accepted") {
                acceptButton.visibility = View.GONE
                rejectButton.visibility = View.VISIBLE
                rejectButton.text = "Delete"
                rejectButton.setOnClickListener {
                    runOnUiThread{
                        CredentialHandler.rejectCredential(this.credential) { error ->
                            error?.let {
                                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                                return@rejectCredential
                            }
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun showLoadingScreen(show: Boolean) {
        runOnUiThread{
            loadingScreen.visibility = if (show) View.VISIBLE else View.GONE
        }
    }


}