package com.ade.evernym.activities.proofRequest

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.getJSONObjectOptional
import com.ade.evernym.handleBase64Scheme
import com.ade.evernym.sdk.handlers.ConnectionHandler
import com.ade.evernym.sdk.handlers.ProofRequestHandler
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDProofRequest
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

class ProofRequestActivity : AppCompatActivity() {

    private lateinit var proofRequest: DIDProofRequest
    private var availableCredentials = JSONObject()
    private var keys = ArrayList<String>()
    private var selectedCredentials = JSONObject()

    private val titleTextView: TextView by lazy { findViewById(R.id.titleTextView) }
    private val imageView: ImageView by lazy { findViewById(R.id.imageView) }
    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }
    private val acceptButton: MaterialButton by lazy { findViewById(R.id.acceptButton) }
    private val rejectButton: MaterialButton by lazy { findViewById(R.id.rejectButton) }
    private val loadingScreen: FrameLayout by lazy { findViewById(R.id.loadingScreen) }
    private val progressTextView: TextView by lazy { findViewById(R.id.progressTextView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proof_request)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        intent.getStringExtra("id")?.let {
            DIDProofRequest.getById(it)?.let { proofRequest ->
                this.proofRequest = proofRequest
                this.proofRequest.printDescription()
                this.title = proofRequest.name.handleBase64Scheme()
                setupTextViews()
                setupImageView()
                setupRecyclerView()
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
        ProofRequestHandler.getCredentialOptions(this.proofRequest) { options, error ->
            error?.let {
                Log.e("ProofRequestActivity", "setData: $it")
                return@getCredentialOptions
            }
            this.availableCredentials = options!!
            this.keys.clear()
            this.availableCredentials.keys().forEach { key -> this.keys.add(key) }
            this.selectedCredentials = JSONObject()
            Log.d("---> (1)", keys.toString())
            Log.d("---> (2)", this.availableCredentials.toString())
            for (key in this.availableCredentials.keys()) {
                options.getJSONArray(key).getJSONObjectOptional(0)?.let {
                    val referent = it.getString("referent")
                    val value = JSONObject().apply {
                        put("referent", referent)
                        put("value", it.getString("value"))
                    }
                    this.selectedCredentials.put(key, value)
                }
            }
            Log.d("---> (3)", this.selectedCredentials.toString())
            this.setAdapter()
        }
    }

    private fun setupButtons() {
        acceptButton.text = "Share"
        acceptButton.setOnClickListener { this.share() }
        rejectButton.setOnClickListener { this.reject() }
    }

    private fun showLoadingScreen(show: Boolean) {
        runOnUiThread {
            this.loadingScreen.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun setMessage(message: String?) {
        runOnUiThread {
            this.progressTextView.text = message
            this.titleTextView.text = message
        }
    }

    private fun setAdapter() {
        runOnUiThread {
            this.recyclerView.adapter = ProofRequestAdapter(
                this.keys,
                this.selectedCredentials
            ).apply {
                onItemClick = { position ->
                    this@ProofRequestActivity.showAlert(this@ProofRequestActivity.keys[position])
                }
            }
        }
    }

    private fun showAlert(key: String) {
        val credentialOptions = this.availableCredentials.getJSONArray(key)
        if (credentialOptions.length() == 0) {
            return
        }
        var selectedCredential: JSONObject? = null
        val proofRecyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(this@ProofRequestActivity).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            val selectedReferent = this@ProofRequestActivity.selectedCredentials.getJSONObject(key)
                .getString("referent")
            adapter = ProofAdapter(credentialOptions, selectedReferent).apply {
                onItemClick = { selectedCredential = it }
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Select proof")
            .setView(proofRecyclerView)
            .setPositiveButton("OK") { alert, _ ->
                selectedCredential?.let { cred ->
                    val selected = JSONObject().apply {
                        put("referent", cred.getString("referent"))
                        put("value", cred.getString("value"))
                    }
                    Toast.makeText(this, selected.getString("referent"), Toast.LENGTH_SHORT).show()
                    this@ProofRequestActivity.selectedCredentials.put(key, selected)
                }
                alert.dismiss()
            }
            .create()
            .show()
    }

    private fun share() {
        showLoadingScreen(true)
        setMessage("Sharing proofs...")
        val connection = DIDConnection.getById(this.proofRequest.connectionId)!!
        ConnectionHandler.acceptConnection(connection) { _, error1 ->
            error1?.let {
                Log.e("ProofRequestActivity", "setupButtons: (1) $it")
                runOnUiThread {
                    this@ProofRequestActivity.showLoadingScreen(false)
                    this@ProofRequestActivity.setMessage("Failed to share proofs")
                    Toast.makeText(this, "Failed to share proofs", Toast.LENGTH_SHORT).show()
                }
                return@acceptConnection
            }
            ProofRequestHandler.acceptProofRequest(
                this.proofRequest,
                this.selectedCredentials
            ) { error2 ->
                error2?.let {
                    Log.e("ProofRequestActivity", "setupButtons: (2) $it")
                    runOnUiThread {
                        this@ProofRequestActivity.showLoadingScreen(false)
                        this@ProofRequestActivity.setMessage("Failed to share proofs")
                        Toast.makeText(this, "Failed to share proofs", Toast.LENGTH_SHORT).show()
                    }
                    return@acceptProofRequest
                }
                DIDProofRequest.delete(this.proofRequest)
                runOnUiThread {
                    this@ProofRequestActivity.showLoadingScreen(false)
                    this@ProofRequestActivity.setMessage("Proof shared")
                    Toast.makeText(this, "Proof shared", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun reject() {
//        showLoadingScreen(true)
//        setMessage("Rejecting proof request...")
//        ProofRequestHandler.rejectProofRequest(this.proofRequest) { error ->
//            runOnUiThread {
//                error?.let {
//                    Log.e("ProofRequestActivity", "setupButtons: $it")
//                    this@ProofRequestActivity.showLoadingScreen(false)
//                    this@ProofRequestActivity.setMessage("Failed to reject proof request")
//                    return@runOnUiThread
//                }
//                this@ProofRequestActivity.showLoadingScreen(false)
//                this@ProofRequestActivity.setMessage("Proof request rejected")
//                finish()
//            }
//        }

        DIDProofRequest.delete(this.proofRequest)
        finish()
    }

}