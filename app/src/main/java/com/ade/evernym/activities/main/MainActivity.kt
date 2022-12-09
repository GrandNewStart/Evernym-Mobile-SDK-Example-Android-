package com.ade.evernym.activities.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.App
import com.ade.evernym.QRHandler
import com.ade.evernym.R
import com.ade.evernym.activities.connection.ConnectionActivity
import com.ade.evernym.activities.connectionList.ConnectionListActivity
import com.ade.evernym.activities.credential.CredentialActivity
import com.ade.evernym.activities.credentialList.CredentialListActivity
import com.ade.evernym.activities.proofRequest.ProofRequestActivity
import com.ade.evernym.activities.proofRequestList.ProofRequestListActivity
import com.ade.evernym.printLog
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDCredential
import com.ade.evernym.sdk.models.DIDProofRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions


class MainActivity : AppCompatActivity() {

    private var sdkInitialized = false

    private val cameraButton: FloatingActionButton by lazy { findViewById(R.id.cameraButton) }
    private val messageTextView: TextView by lazy { findViewById(R.id.messageTextView) }
    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }
    private val loadingScreen: FrameLayout by lazy { findViewById(R.id.loadingScreen) }

    private val qrCodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        result.contents?.let {
            handleQR(it)
            return@let
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        setContentView(R.layout.activity_main)
        awaitSDKInitialization()
        setupButton()
        setupRecyclerView()
        setMessage("Initializing SDK...")
    }

    private fun awaitSDKInitialization() {
        App.shared.sdkInitialized.observe(this) { initialized ->
            this.sdkInitialized = initialized
            if (initialized) {
                Toast.makeText(this, "SDK Ready", Toast.LENGTH_SHORT).show()
                setMessage("SDK Ready")
            } else {
                Toast.makeText(this, "SDK Failed", Toast.LENGTH_SHORT).show()
                setMessage("SDK Failed")
            }
        }
    }

    private fun setupButton() {
        cameraButton.setOnClickListener {
            if (!sdkInitialized) {
                Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            qrCodeLauncher.launch(
                ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("QR Scan")
                    setBeepEnabled(false)
                    setOrientationLocked(true)
                }
            )
        }
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            adapter = MainAdapter().apply {
                onItemClick = { position ->
                    when (position) {
                        0 -> { startActivity(Intent(this@MainActivity, ConnectionListActivity::class.java)) }
                        1 -> { startActivity(Intent(this@MainActivity, CredentialListActivity::class.java)) }
                        2 -> { startActivity(Intent(this@MainActivity, ProofRequestListActivity::class.java)) }
                    }
                }
            }
        }
    }

    fun setMessage(message: String?) {
        runOnUiThread {
            messageTextView.text = message
        }
    }

    private fun handleQR(code: String) {
        Log.d("MainActivity", "handleQR: $code")
        showLoadingScreen(true)
        QRHandler.handle(code)
    }

    fun showLoadingScreen(show: Boolean) {
        runOnUiThread {
            loadingScreen.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    fun showConnection(connection: DIDConnection) {
        Log.d("MainActivity", "showConnectionInvitation: ${connection.getDescription()}")
        setMessage("Ready")
        runOnUiThread {
            startActivity(
                Intent(this@MainActivity, ConnectionActivity::class.java).apply {
                    putExtra("id", connection.id)
                }
            )
        }
    }

    fun showCredential(credential: DIDCredential) {
        printLog("MainActivity", "showConnectionInvitation: ${credential.getDescription()}")
        setMessage("Ready")
        runOnUiThread {
            startActivity(
                Intent(this@MainActivity, CredentialActivity::class.java).apply {
                    putExtra("id", credential.id)
                }
            )
        }
    }

    fun showProofRequest(proofRequest: DIDProofRequest) {
        setMessage("Ready")
        runOnUiThread {
            startActivity(
                Intent(this@MainActivity, ProofRequestActivity::class.java).apply {
                    putExtra("id", proofRequest.id)
                }
            )
        }
    }

    companion object {
        lateinit var instance: MainActivity
    }

}