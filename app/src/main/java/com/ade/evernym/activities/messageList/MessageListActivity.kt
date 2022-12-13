package com.ade.evernym.activities.messageList

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ade.evernym.R
import com.ade.evernym.getStringOptional
import com.ade.evernym.sdk.handlers.ConnectionHandler
import com.ade.evernym.sdk.handlers.CredentialHandler
import com.ade.evernym.sdk.handlers.MessageHandler
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDCredential
import com.ade.evernym.sdk.models.DIDMessage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject

class MessageListActivity : AppCompatActivity() {

    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }
    private val refreshButton: FloatingActionButton by lazy { findViewById(R.id.refrehButton) }
    private val loadingScreen: FrameLayout by lazy { findViewById(R.id.loadingScreen) }
    private val progressTextView: TextView by lazy { findViewById(R.id.progressTextView) }

    private var messages = ArrayList<DIDMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_list)
        this.title = "Messages"
        setupRecyclerView()
        setupButtons()
        reload()
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            adapter = MessageAdapter(this@MessageListActivity.messages).apply {
                onItemClick = { message ->
                    when (message.type) {
                        "aries" -> {
                            this@MessageListActivity.handleAries(message)
                        }
                        "credential-offer" -> {
                            this@MessageListActivity.handleCredentialOffer(message)
                        }
                        "problem-report" -> {
                            val payload = JSONObject(message.payload)
                            payload.getStringOptional("@msg")?.let {
                                payload.put("@msg", JSONObject(it))
                            }
                            Log.e("MessageListActivity", "$payload")
                        }
                        else -> {
                            Toast.makeText(context, "unknown message type", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                onItemLongClick = { message, itemView ->
                    PopupMenu(this@MessageListActivity, itemView).let { popup ->
                        menuInflater.inflate(R.menu.menu_message, popup.menu)
                        popup.setOnMenuItemClickListener { item ->
                            if (item.itemId == R.id.delete) {
                                this@MessageListActivity.showLoadingScreen(true)
                                MessageHandler.update(message.pwDid, message.uid) { error ->
                                    error?.let {
                                        Log.e("MessageListActivity", it)
                                        this@MessageListActivity.showLoadingScreen(false)
                                        return@update
                                    }
                                    runOnUiThread {
                                        this@MessageListActivity.reload()
                                    }
                                }
                            }
                            false
                        }
                        popup.show()
                    }
                    false
                }
            }
        }
    }

    private fun setupButtons() {
        refreshButton.setOnClickListener {
            this.reload()
        }
    }

    private fun showLoadingScreen(show: Boolean) {
        loadingScreen.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setMessage(message: String?) {
        progressTextView.text = message
    }

    private fun reload() {
        showLoadingScreen(true)
        setMessage("Fetching messages...")
        MessageHandler.downloadPendingMessages { messages, error ->
            error?.let {
                runOnUiThread {
                    this@MessageListActivity.showLoadingScreen(false)
                    this@MessageListActivity.setMessage("Fetching messages...")
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                }
                return@downloadPendingMessages
            }
            runOnUiThread {
                this@MessageListActivity.showLoadingScreen(false)
                this@MessageListActivity.setMessage("Message fetched")
                this@MessageListActivity.messages = messages
                this@MessageListActivity.setupRecyclerView()
                if (messages.isEmpty()) {
                    Toast.makeText(this, "No new message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleAries(message: DIDMessage) {
        showLoadingScreen(true)
        setMessage("Connecting...")
        ConnectionHandler.getConnection(message)?.let {
            ConnectionHandler.acceptConnection(it) { _, error1 ->
                error1?.let {
                    Log.e("MessageListActivity", "setupRecyclerView: (1) $it")
                    runOnUiThread {
                        this@MessageListActivity.showLoadingScreen(false)
                        this@MessageListActivity.setMessage("Connection failed")
                    }
                    return@acceptConnection
                }
                MessageHandler.update(message.pwDid, message.uid) { error2 ->
                    error2?.let {
                        Log.e("MessageListActivity", "setupRecyclerView: (2) $it")
                        runOnUiThread {
                            this@MessageListActivity.showLoadingScreen(false)
                            this@MessageListActivity.setMessage("Message update failed")
                        }
                        return@update
                    }
                    runOnUiThread {
                        Toast.makeText(this, "Connection accepted", Toast.LENGTH_SHORT).show()
                        this@MessageListActivity.reload()
                    }
                }
            }
            return@let
        }
    }

    private fun handleCredentialOffer(message: DIDMessage) {
        val connection = DIDConnection.getByPwDid(message.pwDid) ?: return
        showLoadingScreen(true)
        setMessage("Fetching credential...")
        CredentialHandler.getCredential(connection, message) { credential, error1 ->
            error1?.let {
                Log.e("MessageListActivity", "handleCredentialOffer: (1) $it")
                runOnUiThread {
                    this@MessageListActivity.showLoadingScreen(false)
                    this@MessageListActivity.setMessage("Credential fetch failed")
                    Toast.makeText(this, "Credential fetch failed", Toast.LENGTH_SHORT).show()
                }
                return@getCredential
            }
            DIDCredential.add(credential!!)
            runOnUiThread { this@MessageListActivity.setMessage("Accepting credential...") }
            CredentialHandler.acceptCredential(credential) { _, error2 ->
                error2?.let {
                    Log.e("MessageListActivity", "handleCredentialOffer: (2) $it")
                    runOnUiThread {
                        this@MessageListActivity.showLoadingScreen(false)
                        this@MessageListActivity.setMessage("Credential acceptance failed")
                        Toast.makeText(this, "Credential acceptance failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                    return@acceptCredential
                }
                MessageHandler.update(message.pwDid, message.uid) { error3 ->
                    error3?.let {
                        Log.e("MessageListActivity", "handleCredentialOffer: (3): $it")
                        runOnUiThread {
                            this@MessageListActivity.showLoadingScreen(false)
                            this@MessageListActivity.setMessage("Message update failed")
                            Toast.makeText(this, "Message update failed", Toast.LENGTH_SHORT).show()
                        }
                        return@update
                    }
                    runOnUiThread {
                        this@MessageListActivity.showLoadingScreen(false)
                        this@MessageListActivity.setMessage("Credential accepted")
                        Toast.makeText(this, "Credential accepted", Toast.LENGTH_SHORT).show()
                        this@MessageListActivity.reload()
                    }
                }
            }
        }
    }

}