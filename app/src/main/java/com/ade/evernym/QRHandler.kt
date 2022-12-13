package com.ade.evernym

import android.os.Handler
import android.os.Looper
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
        Log.d("QRHandler", "handleConnection: $code")
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
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Connection fetched")
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
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Connection registered")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(MainActivity.instance, "Connection registered", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun handleLogIn(code: String) {
        Log.d("QRHandler", "handleLogIn: $code")
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Reading invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleLogIn: (1) $it")
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to read invitation")
                return@getInvitation
            }
            App.shared.progressText.postValue("Fetching connection...")
            ConnectionHandler.getConnection(invitation!!) { connection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleLogIn: (2) $it")
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Failed to check existing connection")
                    return@getConnection
                }
                if (connection!!.pwDid.isEmpty()) {
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("No existing connection. Unable to log in")
                    return@getConnection
                }
                App.shared.progressText.postValue("Connecting...")
                ConnectionHandler.acceptConnection(connection) { _, error3 ->
                    error3?.let {
                        Log.e("QRHandler", "handleLogIn: (4) $it")
                        App.shared.isLoading.postValue(false)
                        App.shared.progressText.postValue("Connection failed")
                        return@acceptConnection
                    }
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("User logged in")
                }
            }
        }
    }

    private fun handleCredentialOffer(code: String) {
        Log.d("QRHandler", "handleCredentialOffer: $code")
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Reading invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleCredentialOffer: (1) $it")
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to read invitation")
                return@getInvitation
            }
            invitation!!.getExistingConnection { existingConnection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleCredentialOffer: (2) $it")
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Failed to check existing connection")
                    return@getExistingConnection
                }
                if (existingConnection == null) {
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("No existing connection. Unable to receive credential")
                    return@getExistingConnection
                }
                if (invitation.attachment == null) {
                    Log.e("QRHandler", "handleCredentialOffer: (3) invitation has no attachment")
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Invitation has no invitation")
                    return@getExistingConnection
                }
                App.shared.progressText.postValue("Fetching connection...")
                ConnectionHandler.getConnection(invitation) { connection, error ->
                    error?.let {
                        Log.e("QRHandler", "handleCredentialOffer: (4) $it")
                        App.shared.isLoading.postValue(false)
                        App.shared.progressText.postValue("Failed to fetch connection")
                        return@getConnection
                    }
                    App.shared.progressText.postValue("Fetching credential...")
                    CredentialHandler.getCredential(connection!!, invitation.attachment!!) { credential, error3 ->
                        error3?.let {
                            Log.e("QRHandler", "handleCredentialOffer: (3) $it")
                            App.shared.isLoading.postValue(false)
                            App.shared.progressText.postValue("Failed to fetch credential")
                            return@getCredential
                        }
                        App.shared.isLoading.postValue(false)
                        App.shared.progressText.postValue("Credential fetched")
                        DIDCredential.add(credential!!)
                        MainActivity.instance.showCredential(credential)
                    }
                }
            }
        }
    }

    private fun handleProofRequest(code: String) {
        Log.d("QRHandler", "handleProofRequest: $code")
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Reading invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler", "handleProofRequest: (1) $it")
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to read invitation")
                return@getInvitation
            }
            App.shared.progressText.postValue("Fetching connection...")
            ConnectionHandler.getConnection(invitation!!) { connection, error2 ->
                error2?.let {
                    Log.e("QRHandler", "handleProofRequest: (2) $it")
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Failed to check existing connection")
                    return@getConnection
                }
                if (connection!!.pwDid.isEmpty()) {
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("No existing connection. Unable to handle proof request")
                    return@getConnection
                }
                if (invitation.attachment == null) {
                    Log.e("QRHandler", "handleProofRequest: (3) invitation has no attachment")
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Invitation has no attachment")
                    return@getConnection
                }
                App.shared.progressText.postValue("Fetching proof request...")
                ProofRequestHandler.getProofRequest(connection, invitation.attachment!!) { proofRequest, error3 ->
                    error3?.let {
                        Log.e("QRHandler", "handleProofRequest: (4) $it")
                        App.shared.isLoading.postValue(false)
                        App.shared.progressText.postValue("Failed to fetch proof request")
                        return@getProofRequest
                    }
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Proof request fetched")
                    DIDProofRequest.add(proofRequest!!)
                    MainActivity.instance.showProofRequest(proofRequest)
                }
            }
        }
    }

    private fun handleQRWithoutScheme(code: String) {
        Log.d("QRHandler", "handleQRWithoutScheme: $code")
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Reading invitation...")
        InvitationHandler.getInvitation(code) { invitation, error1 ->
            error1?.let {
                Log.e("QRHandler","handleQRWithoutScheme: (1) $it")
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Reading invitation...")
                return@getInvitation
            }
            invitation!!

            App.shared.progressText.postValue("Fetching connection...")
            ConnectionHandler.getConnection(invitation) { connection, error2 ->
                error2?.let {
                    Log.e("QRHandler","handleQRWithoutScheme: (2) $it")
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Failed to fetch connection")
                    return@getConnection
                }
                connection!!

                if (invitation.attachment == null) {
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Connection fetched")
                    MainActivity.instance.showConnection(connection)
                    return@getConnection
                }

                val attachment = invitation.attachment!!

                if (attachment.isCredentialAttachment()) {
                    App.shared.progressText.postValue("Fetching credential...")
                    CredentialHandler.getCredential(connection, attachment) { credential, error3 ->
                        error3?.let {
                            Log.e("QRHandler","handleQRWithoutScheme: (3) $it")
                            App.shared.isLoading.postValue(false)
                            App.shared.progressText.postValue("Credential fetch failed")
                            return@getCredential
                        }
                        App.shared.isLoading.postValue(false)
                        App.shared.progressText.postValue("Credential fetched")
                        DIDCredential.add(credential!!)
                        MainActivity.instance.showCredential(credential)
                    }
                    return@getConnection
                }

                if (attachment.isProofAttachment()) {
                    App.shared.progressText.postValue("Fetching proof request...")
                    ProofRequestHandler.getProofRequest(connection, attachment) { proofRequest, error4 ->
                        error4?.let {
                            Log.e("QRHandler","handleQRWithoutScheme: (4) $it")
                            App.shared.isLoading.postValue(false)
                            App.shared.progressText.postValue("Proof request fetch failed")
                            return@getProofRequest
                        }
                        App.shared.isLoading.postValue(false)
                        App.shared.progressText.postValue("Proof request fetched")
                        DIDProofRequest.add(proofRequest!!)
                        MainActivity.instance.showProofRequest(proofRequest)
                    }
                    return@getConnection
                }

                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Unknown attachment type")
            }
        }
    }

}