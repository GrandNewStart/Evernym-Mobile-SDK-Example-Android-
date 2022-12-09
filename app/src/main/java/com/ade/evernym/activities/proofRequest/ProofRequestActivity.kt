package com.ade.evernym.activities.proofRequest

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.handleBase64Scheme
import com.ade.evernym.sdk.handlers.ProofRequestHandler
import com.ade.evernym.sdk.models.DIDProofRequest
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

class ProofRequestActivity : AppCompatActivity() {

    private lateinit var proofRequest: DIDProofRequest
    private var selectedCredentials = JSONObject()

    private val titleTextView: TextView by lazy { findViewById(R.id.titleTextView) }
    private val imageView: ImageView by lazy { findViewById(R.id.imageView) }
    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }
    private val acceptButton: MaterialButton by lazy { findViewById(R.id.acceptButton) }
    private val rejectButton: MaterialButton by lazy { findViewById(R.id.rejectButton) }
    private val loadingScreen: FrameLayout by lazy { findViewById(R.id.loadingScreen) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proof_request)
        intent.getStringExtra("id")?.let {
            DIDProofRequest.getById(it)?.let { proofRequest ->
                this.proofRequest = proofRequest
                setupTextViews()
                setupImageView()
                setupRecyclerView()
                setupButtons()
                return
            }
            finish()
        }
    }

    private fun setupTextViews() {
        runOnUiThread {
            titleTextView.text = this.proofRequest.name.handleBase64Scheme()
        }
    }

    private fun setupImageView() {
        runOnUiThread {
            Glide.with(this)
                .load(this.proofRequest.connectionLogo)
                .placeholder(R.drawable.ic_baseline_image_24)
                .into(this.imageView)
        }
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            adapter = ProofRequestAdapter(this@ProofRequestActivity.proofRequest)
        }
    }

    private fun setupButtons() {
        acceptButton.setOnClickListener {
            showLoadingScreen(true)
            ProofRequestHandler.acceptProofRequest(
                this.proofRequest,
                this.selectedCredentials
            ) { error ->
                error?.let {
                    Log.e("ProofRequestActivity", "setupButtons: $it")
                    this.showLoadingScreen(false)
                    return@acceptProofRequest
                }
                DIDProofRequest.delete(this.proofRequest)
                finish()
            }
        }
        rejectButton.setOnClickListener {
            this.showLoadingScreen(true)
            ProofRequestHandler.rejectProofRequest(this.proofRequest) { error ->
                error?.let {
                    Log.e("ProofRequestActivity", "setupButtons: $it")
                    this.showLoadingScreen(false)
                    return@rejectProofRequest
                }
                finish()
            }
        }
    }

    private fun showLoadingScreen(show: Boolean) {
        loadingScreen.visibility = if (show) View.VISIBLE else View.GONE
    }

}