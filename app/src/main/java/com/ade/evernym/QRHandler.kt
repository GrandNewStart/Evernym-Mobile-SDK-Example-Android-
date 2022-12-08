package com.ade.evernym

import android.util.Log
import com.ade.evernym.activities.main.MainActivity
import com.ade.evernym.sdk.handlers.ConnectionHandler
import com.ade.evernym.sdk.handlers.CredentialHandler
import com.ade.evernym.sdk.handlers.InvitationHandler
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDCredential

object QRHandler {

    fun handle(code: String) {
        MainActivity.instance.setMessage("processing...")
        if (code.startsWith("abcDidQrType=connect;")) {
            handleConnection(code.trim().split(";")[1])
            return
        }
        if (code.startsWith("abcDidQrType=login;")) {
            handleLogIn(code.trim().split(";")[1])
            return
        }
        if (code.startsWith("abcDidQrType=issue-cred;")) {
            handleCredentialOffer(code.trim().split(";")[1])
            return
        }
        if (code.startsWith("abcDidQrType=submit-proof;")) {
            handleProofRequest(code.trim().split(";")[1])
            return
        }

        MainActivity.instance.setMessage("Fetching invitation...")
        InvitationHandler.getInvitation(code) { invitation, error ->
            invitation?.let { invitation ->
                MainActivity.instance.setMessage("Fetching connection...")
                ConnectionHandler.getConnection(invitation) { connection, error ->
                    connection?.let { connection ->
                        MainActivity.instance.setMessage("Connecting...")
                        ConnectionHandler.acceptConnection(connection) { updatedConnection, error ->
                            updatedConnection?.let { updatedConnection ->
                                MainActivity.instance.setMessage("Connected")
                                DIDConnection.add(updatedConnection)
                                invitation.attachment?.let { attachment ->
                                    if (attachment.isCredentialAttachment()) {
                                        MainActivity.instance.setMessage("Fetching credential...")
                                        CredentialHandler.getCredential(updatedConnection, attachment) { credential, error ->
                                            MainActivity.instance.setMessage("Accepting credential...")
                                            credential?.let { credential ->
                                                CredentialHandler.acceptCredential(credential) { updatedCredential, error ->
                                                    updatedCredential?.let {
                                                        DIDCredential.add(credential)
                                                        MainActivity.instance.showLoadingScreen(false)
                                                        MainActivity.instance.showCredential(credential)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleConnection(code: String) {
        MainActivity.instance.setMessage("Fetching invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleConnection: (1) $it")
                MainActivity.instance.setMessage("Failed to get invitation.")
                MainActivity.instance.showLoadingScreen(false)
                return@getInvitation
            }
            MainActivity.instance.setMessage("Fetching connection...")
            ConnectionHandler.getConnection(invitation!!) { connection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleConnection: (2) $it")
                    MainActivity.instance.setMessage("Failed to get connection.")
                    MainActivity.instance.showLoadingScreen(false)
                    return@getConnection
                }
                if (connection!!.pwDid.isEmpty()) {
                    DIDConnection.add(connection)
                    MainActivity.instance.showConnection(connection)
                    MainActivity.instance.showLoadingScreen(false)
                } else {
                    DIDConnection.update(connection)
                    MainActivity.instance.setMessage("Connection reused.")
                    MainActivity.instance.showLoadingScreen(false)
                }
            }
        }
    }

    private fun handleLogIn(code: String) {
        MainActivity.instance.setMessage("Fetching invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleLogIn: (1) $it")
                MainActivity.instance.setMessage("Failed to get invitation.")
                MainActivity.instance.showLoadingScreen(false)
                return@getInvitation
            }
            MainActivity.instance.setMessage("Checking existing connection...")
            invitation!!.getExistingConnection { existingConnection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleLogIn: (2) $it")
                    MainActivity.instance.setMessage("Failed to get existing connection.")
                    MainActivity.instance.showLoadingScreen(false)
                    return@getExistingConnection
                }
                if (existingConnection == null) {
                    MainActivity.instance.setMessage("No existing connection. Unable to log in.")
                    MainActivity.instance.showLoadingScreen(false)
                } else {
                    MainActivity.instance.setMessage("Fetching connection...")
                    ConnectionHandler.getConnection(invitation) { connection, error3 ->
                        error3?.let {
                            Log.e("QRHandler", "handleLogIn: (3) $it")
                            MainActivity.instance.setMessage("Failed to get connection.")
                            MainActivity.instance.showLoadingScreen(false)
                            return@getConnection
                        }
                        DIDConnection.add(connection!!)
                        MainActivity.instance.setMessage("Connecting...")
                        ConnectionHandler.acceptConnection(connection) { updatedConnection, error4 ->
                            error4?.let {
                                Log.e("QRHandler", "handleLogIn: (4) $it")
                                MainActivity.instance.setMessage("Failed to connect.")
                                MainActivity.instance.showLoadingScreen(false)
                                return@acceptConnection
                            }
                            MainActivity.instance.setMessage("Successfully connected.")
                            MainActivity.instance.showLoadingScreen(false)
                        }
                    }
                }
            }
        }
    }

    private fun handleCredentialOffer(code: String) {
        MainActivity.instance.setMessage("Fetching invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleCredentialOffer: (1) $it")
                MainActivity.instance.setMessage("Failed to get invitation.")
                MainActivity.instance.showLoadingScreen(false)
                return@getInvitation
            }
            MainActivity.instance.setMessage("Checking existing connection...")
            invitation!!.getExistingConnection { existingConnection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleLogIn: (2) $it")
                    MainActivity.instance.setMessage("Failed to get existing connection.")
                    MainActivity.instance.showLoadingScreen(false)
                    return@getExistingConnection
                }
                if (existingConnection == null) {
                    MainActivity.instance.setMessage("No existing connection. Unable to receive credential.")
                    MainActivity.instance.showLoadingScreen(false)
                    return@getExistingConnection
                }
                if (invitation.attachment == null) {
                    Log.e("QRHandler", "handleLogIn: (4) invitation has no attachment")
                    MainActivity.instance.setMessage("Invitation has no attachment")
                    MainActivity.instance.showLoadingScreen(false)
                    return@getExistingConnection
                }
                MainActivity.instance.setMessage("Fetching credential...")
                CredentialHandler.getCredential(existingConnection, invitation.attachment!!) { credential, error3 ->
                    error3?.let {
                        Log.e("QRHandler", "handleLogIn: (3) $it")
                        MainActivity.instance.setMessage("No existing connection. Unable to receive credential.")
                        MainActivity.instance.showLoadingScreen(false)
                        return@getCredential
                    }
                    DIDCredential.add(credential!!)
                    MainActivity.instance.showLoadingScreen(false)
                    MainActivity.instance.showCredential(credential)
                }
            }
        }
    }

    private fun handleProofRequest(code: String) {

    }

}