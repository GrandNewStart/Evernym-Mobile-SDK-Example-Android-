package com.ade.evernym

import android.util.Log
import com.ade.evernym.activities.main.MainActivity
import com.ade.evernym.sdk.handlers.ConnectionHandler
import com.ade.evernym.sdk.handlers.CredentialHandler
import com.ade.evernym.sdk.handlers.InvitationHandler
import com.ade.evernym.sdk.handlers.ProofRequestHandler
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDCredential
import com.ade.evernym.sdk.models.DIDProofRequest

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

        handleQRWithoutScheme(code.trim())
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
                        ConnectionHandler.acceptConnection(connection) { _, error4 ->
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
                    Log.e("QRHandler", "handleCredentialOffer: (2) $it")
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
                    Log.e("QRHandler", "handleCredentialOffer: (3) invitation has no attachment")
                    MainActivity.instance.setMessage("Invitation has no attachment")
                    MainActivity.instance.showLoadingScreen(false)
                    return@getExistingConnection
                }
                ConnectionHandler.getConnection(invitation) { connection, error ->
                    error?.let {
                        Log.e("QRHandler", "handleCredentialOffer: (4) $it")
                        MainActivity.instance.setMessage("Failed to get connection.")
                        MainActivity.instance.showLoadingScreen(false)
                        return@getConnection
                    }
                    MainActivity.instance.setMessage("Fetching credential...")
                    CredentialHandler.getCredential(connection!!, invitation.attachment!!) { credential, error3 ->
                        error3?.let {
                            Log.e("QRHandler", "handleCredentialOffer: (3) $it")
                            MainActivity.instance.setMessage("Failed to fetch credential.")
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
    }

    private fun handleProofRequest(code: String) {
        MainActivity.instance.setMessage("Fetching invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleProofRequest: (1) $it")
                MainActivity.instance.setMessage("Failed to get invitation.")
                MainActivity.instance.showLoadingScreen(false)
                return@getInvitation
            }
            MainActivity.instance.setMessage("Checking existing connection...")
            invitation!!.getExistingConnection { existingConnection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleProofRequest: (2) $it")
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
                    Log.e("QRHandler", "handleProofRequest: (3) invitation has no attachment")
                    MainActivity.instance.setMessage("Invitation has no attachment")
                    MainActivity.instance.showLoadingScreen(false)
                    return@getExistingConnection
                }
                ConnectionHandler.getConnection(invitation) { connection, error ->
                    error?.let {
                        Log.e("QRHandler", "handleProofRequest: (4) $it")
                        MainActivity.instance.setMessage("Failed to get connection.")
                        MainActivity.instance.showLoadingScreen(false)
                        return@getConnection
                    }
                    MainActivity.instance.setMessage("Fetching proof request...")
                    ProofRequestHandler.getProofRequest(connection!!, invitation.attachment!!) { proofRequest, error3 ->
                        error3?.let {
                            Log.e("QRHandler", "handleProofRequest: (5) $it")
                            MainActivity.instance.setMessage("Failed to fetch proof request.")
                            MainActivity.instance.showLoadingScreen(false)
                            return@getProofRequest
                        }
                        DIDProofRequest.add(proofRequest!!)
                        MainActivity.instance.showLoadingScreen(false)
                        MainActivity.instance.showProofRequest(proofRequest)
                    }
                }
            }
        }
    }

    private fun handleQRWithoutScheme(code: String) {
        MainActivity.instance.showLoadingScreen(true)
        MainActivity.instance.setMessage("Fetching invitation...")

        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                MainActivity.instance.showLoadingScreen(false)
                MainActivity.instance.setMessage("Invitation fetch failed")
                return@getInvitation
            }

            MainActivity.instance.setMessage("Fetching connection...")
            ConnectionHandler.getConnection(invitation!!) { connection, error2 ->
                error2?.let {
                    MainActivity.instance.showLoadingScreen(false)
                    MainActivity.instance.setMessage("Connection fetch failed")
                    return@getConnection
                }
                if (invitation.attachment == null) {
                    MainActivity.instance.showLoadingScreen(false)
                    MainActivity.instance.showConnection(connection!!)
                    return@getConnection
                }
                if (invitation.attachment!!.isCredentialAttachment()) {
                    MainActivity.instance.setMessage("Fetching credential...")
                    CredentialHandler.getCredential(connection!!, invitation.attachment!!) { credential, error3 ->
                        error3?.let {
                            MainActivity.instance.showLoadingScreen(false)
                            MainActivity.instance.setMessage("Credential fetch failed")
                            return@getCredential
                        }
                        DIDCredential.add(credential!!)
                        MainActivity.instance.showLoadingScreen(false)
                        MainActivity.instance.showCredential(credential)
                    }
                }
                if (invitation.attachment!!.isProofAttachment()) {
                    MainActivity.instance.setMessage("Fetching proof request...")
                    ProofRequestHandler.getProofRequest(connection!!, invitation.attachment!!) { proofRequest, error4 ->
                        error4?.let {
                            MainActivity.instance.showLoadingScreen(false)
                            MainActivity.instance.setMessage("Proof request fetch failed")
                            return@getProofRequest
                        }
                        DIDProofRequest.add(proofRequest!!)
                        MainActivity.instance.showLoadingScreen(false)
                        MainActivity.instance.showProofRequest(proofRequest)
                    }
                }
            }
        }
    }

}