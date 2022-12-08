package com.ade.evernym

import android.util.Log
import com.ade.evernym.activities.main.MainActivity
import com.ade.evernym.sdk.handlers.ConnectionHandler
import com.ade.evernym.sdk.handlers.InvitationHandler
import com.ade.evernym.sdk.models.DIDConnection

object QRHandler {

    fun handle(code: String) {
        MainActivity.instance.setMessage("processing...")
        if (code.startsWith("abcDidQrType=connect;")) {
            handleConnection(code.trim().split(";")[1])
        }
        if (code.startsWith("abcDidQrType=login;")) {
            handleLogIn(code.trim().split(";")[1])
        }
        if (code.startsWith("abcDidQrType=issue-cred;")) {
            handleCredentialOffer(code.trim().split(";")[1])
        }
        if (code.startsWith("abcDidQrType=submit-proof;")) {
            handleProofRequest(code.trim().split(";")[1])
        }
    }

    private fun handleConnection(code: String) {
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleConnection: (1) $it")
                MainActivity.instance.setMessage("Failed to get invitation.")
                MainActivity.instance.showLoadingScreen(false)
                return@getInvitation
            }
            ConnectionHandler.getConnection(invitation!!) { connection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleConnection: (2) $it")
                    MainActivity.instance.setMessage("Failed to get connection.")
                    MainActivity.instance.showLoadingScreen(false)
                    return@getConnection
                }
                if (connection!!.pwDid.isEmpty()) {
                    DIDConnection.add(connection)
                    MainActivity.instance.showConnections(connection)
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
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleLogIn: (1) $it")
                MainActivity.instance.setMessage("Failed to get invitation.")
                MainActivity.instance.showLoadingScreen(false)
                return@getInvitation
            }
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
                    ConnectionHandler.getConnection(invitation) { connection, error3 ->
                        error3?.let {
                            Log.e("QRHandler", "handleLogIn: (3) $it")
                            MainActivity.instance.setMessage("Failed to get connection.")
                            MainActivity.instance.showLoadingScreen(false)
                            return@getConnection
                        }
                        DIDConnection.add(connection!!)
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

    }

    private fun handleProofRequest(code: String) {

    }

}