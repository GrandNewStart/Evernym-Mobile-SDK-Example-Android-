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
import com.ade.evernym.activities.connectionList.ConnectionListActivity
import com.ade.evernym.activities.credentialList.CredentialListActivity
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDCredential
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
                        2 -> { Toast.makeText(this@MainActivity, "Proof Requests", Toast.LENGTH_SHORT).show() }
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
        loadingScreen.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun showConnections(connection: DIDConnection) {
        Log.d("MainActivity", "showConnectionInvitation: ${connection.getDescription()}")
        setMessage("Ready")
        startActivity(Intent(this@MainActivity, ConnectionListActivity::class.java))
    }

    fun showCredentials(credential: DIDCredential) {
        Log.d("MainActivity", "showConnectionInvitation: ${credential.getDescription()}")
        setMessage("Ready")
        startActivity(Intent(this@MainActivity, CredentialListActivity::class.java))
    }


    companion object {
        lateinit var instance: MainActivity
    }

}