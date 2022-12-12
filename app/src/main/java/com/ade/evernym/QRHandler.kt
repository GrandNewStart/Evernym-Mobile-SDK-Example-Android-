package com.ade.evernym

import android.util.Log
import android.widget.Toast
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
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Reading invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleConnection: (1) $it")
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to read invitation")
                return@getInvitation
            }
            App.shared.progressText.postValue("Fetching connection...")
            ConnectionHandler.getConnection(invitation!!) { connection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleConnection: (2) $it")
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Failed to fetch connection")
                    return@getConnection
                }
                if (connection!!.pwDid.isEmpty()) {
                    DIDConnection.add(connection)
                    MainActivity.instance.showConnection(connection)
                    return@getConnection
                }
                ConnectionHandler.acceptConnection(connection) { _, error ->
                    error?.let {
                        Log.e("QRHandler", "handleConnection: (3) $it")
                        App.shared.isLoading.postValue(false)
                        App.shared.progressText.postValue("Failed to accept connection")
                        return@acceptConnection
                    }
                    Toast.makeText(MainActivity.instance, "Connection registered", Toast.LENGTH_SHORT).show()
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Connection registered")
                }
            }
        }
    }

    private fun handleLogIn(code: String) {
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Reading invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleLogIn: (1) $it")
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to read invitation")
                return@getInvitation
            }
            invitation!!.getExistingConnection { existingConnection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleLogIn: (2) $it")
                    return@getExistingConnection
                }
                if (existingConnection == null) {
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue(null)
                    App.shared.progressText.postValue("No existing connection. Unable to log in.")
                } else {
                    App.shared.progressText.postValue("Fetching connection...")
                    ConnectionHandler.getConnection(invitation) { connection, error3 ->
                        error3?.let {
                            Log.e("QRHandler", "handleLogIn: (3) $it")
                            App.shared.isLoading.postValue(false)
                            App.shared.progressText.postValue("Connection fetch failed")
                            return@getConnection
                        }
                        DIDConnection.add(connection!!)
                        App.shared.progressText.postValue("Connecting...")
                        ConnectionHandler.acceptConnection(connection) { _, error4 ->
                            error4?.let {
                                Log.e("QRHandler", "handleLogIn: (4) $it")
                                App.shared.isLoading.postValue(false)
                                App.shared.progressText.postValue("Connection acceptance failed")
                                return@acceptConnection
                            }
                            App.shared.isLoading.postValue(false)
                            App.shared.progressText.postValue("User logged in")
                        }
                    }
                }
            }
        }
    }

    private fun handleCredentialOffer(code: String) {
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleCredentialOffer: (1) $it")
                return@getInvitation
            }
            invitation!!.getExistingConnection { existingConnection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleCredentialOffer: (2) $it")
                    return@getExistingConnection
                }
                if (existingConnection == null) {
                    App.shared.progressText.postValue("No existing connection. Unable to receive credential.")
                    return@getExistingConnection
                }
                if (invitation.attachment == null) {
                    Log.e("QRHandler", "handleCredentialOffer: (3) invitation has no attachment")
                    return@getExistingConnection
                }
                ConnectionHandler.getConnection(invitation) { connection, error ->
                    error?.let {
                        Log.e("QRHandler", "handleCredentialOffer: (4) $it")
                        return@getConnection
                    }
                    App.shared.progressText.postValue("Fetching credential...")
                    CredentialHandler.getCredential(connection!!, invitation.attachment!!) { credential, error3 ->
                        error3?.let {
                            Log.e("QRHandler", "handleCredentialOffer: (3) $it")
                            return@getCredential
                        }
                        DIDCredential.add(credential!!)
                        MainActivity.instance.showCredential(credential)
                    }
                }
            }
        }
    }

    private fun handleProofRequest(code: String) {
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleProofRequest: (1) $it")
                return@getInvitation
            }
            ConnectionHandler.getConnection(invitation!!) { connection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleProofRequest: (2) $it")
                    return@getConnection
                }
                if (connection!!.pwDid.isEmpty()) {
                    App.shared.progressText.postValue("No existing connection. Unable to receive credential.")
                    return@getConnection
                }
                if (invitation.attachment == null) {
                    Log.e("QRHandler", "handleProofRequest: (3) invitation has no attachment")
                    return@getConnection
                }
                App.shared.progressText.postValue("Fetching proof request...")
                ProofRequestHandler.getProofRequest(connection, invitation.attachment!!) { proofRequest, error3 ->
                    error3?.let {
                        Log.e("QRHandler", "handleProofRequest: (4) $it")
                        return@getProofRequest
                    }
                    DIDProofRequest.add(proofRequest!!)
                    MainActivity.instance.showProofRequest(proofRequest)
                }
            }
        }
    }

    private fun handleQRWithoutScheme(code: String) {
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler","handleQRWithoutScheme: (1) $it")
                return@getInvitation
            }
            invitation!!

            ConnectionHandler.getConnection(invitation) { connection, error2 ->
                error2?.let {
                    Log.e("QRHandler","handleQRWithoutScheme: (2) $it")
                    return@getConnection
                }
                connection!!

                if (invitation.attachment == null) {
                    MainActivity.instance.showConnection(connection)
                    return@getConnection
                }

                val attachment = invitation.attachment!!

                if (attachment.isCredentialAttachment()) {
                    CredentialHandler.getCredential(connection, attachment) { credential, error3 ->
                        error3?.let {
                            Log.e("QRHandler","handleQRWithoutScheme: (3) $it")
                            return@getCredential
                        }
                        DIDCredential.add(credential!!)
                        MainActivity.instance.showCredential(credential)
                    }
                }

                if (attachment.isProofAttachment()) {
                    ProofRequestHandler.getProofRequest(connection, attachment) { proofRequest, error4 ->
                        error4?.let {
                            Log.e("QRHandler","handleQRWithoutScheme: (4) $it")
                            return@getProofRequest
                        }
                        DIDProofRequest.add(proofRequest!!)
                        MainActivity.instance.showProofRequest(proofRequest)
                    }
                }
            }
        }
    }

}