package com.ade.evernym.sdk.handlers

import android.util.Log
import com.ade.evernym.getJSONArrayOptional
import com.ade.evernym.getJSONObjectOptional
import com.ade.evernym.getStringOptional
import com.ade.evernym.print
import com.ade.evernym.sdk.SDKUtils
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDCredential
import com.ade.evernym.sdk.models.DIDMessage
import com.ade.evernym.sdk.models.DIDMessageAttachment
import com.evernym.sdk.vcx.VcxException
import com.evernym.sdk.vcx.credential.CredentialApi
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object CredentialHandler {

    fun getCredential(
        connection: DIDConnection,
        attachment: DIDMessageAttachment,
        completionHandler: (DIDCredential?, String?) -> Unit
    ) {
        val credentialOffer = JSONObject(attachment.data)
        val threadId = SDKUtils.getThreadId(credentialOffer)
        if (threadId == null) {
            Log.e("CredentialHandler", "getCredential: (1) failed to get thread id")
            completionHandler(null, "failed to get thread id")
            return
        }
        val name = credentialOffer.getStringOptional("comment")
        if (name == null) {
            Log.e("CredentialHandler", "getCredential: (2) failed to get credential name")
            completionHandler(null, "failed to get credential name")
            return
        }
        val preview = credentialOffer.getJSONObjectOptional("credential_preview")
        if (preview == null) {
            Log.e("CredentialHandler", "getCredential: (3) failed to get 'credential_preview'")
            completionHandler(null, "failed to get 'credential_preview")
            return
        }
        val attributes = preview.getJSONArrayOptional("attributes")
        if (attributes == null) {
            Log.e("CredentialHandler", "getCredential: (4) failed to get 'attributes'")
            completionHandler(null, "failed to get 'attributes")
            return
        }
        CredentialApi.credentialCreateWithOffer(
            UUID.randomUUID().toString(),
            attachment.data
        ).whenComplete { handle, error1 ->
            (error1 as? VcxException)?.let {
                it.print("CredentialHandler", "getCredential: (5)")
                completionHandler(null, it.localizedMessage)
                return@whenComplete
            }
            CredentialApi.credentialSerialize(handle).whenComplete { serialized, error2 ->
                (error2 as? VcxException)?.let {
                    it.print("CredentialHandler", "getCredential: (6)")
                    completionHandler(null, it.localizedMessage)
                    return@whenComplete
                }
                val credential = DIDCredential(
                    UUID.randomUUID().toString(),
                    threadId,
                    "",
                    "",
                    name,
                    attributes.toString(),
                    connection.id,
                    connection.name,
                    connection.logo,
                    "pending",
                    Date().toString(),
                    serialized
                )
                completionHandler(credential, null)
            }
        }
    }

    fun getCredential(
        connection: DIDConnection,
        message: DIDMessage,
        completionHandler: (DIDCredential?, String?) -> Unit
    ) {
        connection.deserialize { connectionHandle ->
            if (connectionHandle == null) {
                Log.e("CredentialHandler", "getCredential: (1) failed to deserialize connection")
                completionHandler(null, "failed to deserialize connection")
                return@deserialize
            }
            CredentialApi.credentialCreateWithMsgid(
                UUID.randomUUID().toString(),
                connectionHandle,
                message.uid
            ).whenComplete { result, error1 ->
                (error1 as? VcxException)?.let {
                    it.print("CredentialHandler", "getCredential: (2)")
                    completionHandler(null, it.localizedMessage)
                    return@whenComplete
                }
                val credentialOffer =
                    JSONObject(result.offer).getJSONObjectOptional("credential_offer")
                if (credentialOffer == null) {
                    Log.e(
                        "CredentialHandler",
                        "getCredential: (3): failed to get 'credential_offer"
                    )
                    completionHandler(null, "failed to get 'credential_offer")
                    return@whenComplete
                }
                val threadId = credentialOffer.getStringOptional("thread_id")
                if (threadId == null) {
                    Log.e("CredentialHandler", "getCredential: (4): failed to get 'thread_id")
                    completionHandler(null, "failed to get 'thread_id")
                    return@whenComplete
                }
                val name = credentialOffer.getStringOptional("claim_name")
                if (name == null) {
                    Log.e("CredentialHandler", "getCredential: (5): failed to get 'claim_name")
                    completionHandler(null, "failed to get 'claim_name")
                    return@whenComplete
                }
                val attrs = credentialOffer.getJSONObjectOptional("credential_attrs")
                if (attrs == null) {
                    Log.e(
                        "CredentialHandler",
                        "getCredential: (6): failed to get 'credential_attrs"
                    )
                    completionHandler(null, "failed to get 'credential_attrs")
                    return@whenComplete
                }
                val attributesJSON = JSONArray()
                for (key in attrs.keys()) {
                    attrs.getStringOptional(key)?.let { value ->
                        JSONObject().apply {
                            put("name", key)
                            put("value", value)
                            attributesJSON.put(this)
                        }
                    }
                }
                CredentialApi.credentialSerialize(result.credential_handle)
                    .whenComplete { serialized, error2 ->
                        (error2 as? VcxException)?.let {
                            it.print("CredentialHandler", "getCredential: (7)")
                            completionHandler(null, it.localizedMessage)
                            return@whenComplete
                        }
                        val credential = DIDCredential(
                            UUID.randomUUID().toString(),
                            threadId,
                            "",
                            "",
                            name,
                            attributesJSON.toString(),
                            connection.id,
                            connection.name,
                            connection.logo,
                            "pending",
                            Date().toString(),
                            serialized
                        )
                        completionHandler(credential, null)
                    }
            }
        }
    }

    fun acceptCredential(
        credential: DIDCredential,
        completionHandler: (credential: DIDCredential?, String?) -> Unit
    ) {
        val connection = DIDConnection.getById(credential.connectionId)
        if (connection == null) {
            Log.e(
                "CredentialHandler",
                "acceptCredential: (1) cannot find connection by id ${credential.connectionId}"
            )
            completionHandler(null, "cannot find connection by id ${credential.connectionId}")
            return
        }
        ConnectionHandler.acceptConnection(connection) { updateConnection, error1 ->
            error1?.let {
                Log.e("CredentialHandler", "acceptCredential: (2) $it")
                completionHandler(null, it)
                return@acceptConnection
            }
            updateConnection!!.deserialize { connectionHandle ->
                if (connectionHandle == null) {
                    Log.e("CredentialHandler", "acceptCredential: (3) failed to deserialize connection")
                    completionHandler(null, "failed to deserialize connection")
                    return@deserialize
                }
                credential.deserialize { credentialHandle ->
                    if (credentialHandle == null) {
                        Log.e(
                            "CredentialHandler",
                            "acceptCredential: (4) failed to deserialize credential"
                        )
                        completionHandler(null, "failed to deserialize credential")
                        return@deserialize
                    }
                    CredentialApi.credentialSendRequest(
                        credentialHandle,
                        connectionHandle,
                        0
                    ).whenComplete { _, error1 ->
                        (error1 as? VcxException)?.let {
                            it.print("CredentialHandler", "acceptCredential: (5)")
                            completionHandler(null, it.localizedMessage)
                            return@whenComplete
                        }
                        CredentialApi.credentialSerialize(credentialHandle)
                            .whenComplete { serialized, error2 ->
                                (error2 as? VcxException)?.let {
                                    it.print("CredentialHandler", "acceptCredential: (6)")
                                    completionHandler(null, it.localizedMessage)
                                    return@whenComplete
                                }
                                credential.serialized = serialized
                                DIDCredential.update(credential)
                                awaitCredentialReceived(credential) { error3 ->
                                    error3?.let {
                                        Log.e("CredentialHandler", "acceptCredential: (7) $it")
                                        completionHandler(null, it)
                                        return@awaitCredentialReceived
                                    }
                                    completionHandler(credential, null)
                                }
                            }
                    }
                }
            }
        }
    }

    private fun awaitCredentialReceived(
        credential: DIDCredential,
        completionHandler: (String?) -> Unit
    ) {
        credential.deserialize { credentialHandle ->
            if (credentialHandle == null) {
                Log.e(
                    "CredentialHandler",
                    "awaitCredentialReceived: (1) failed to deserialize credential"
                )
                completionHandler("failed to deserialize credential")
                return@deserialize
            }

            try {
                var state = -1
                var count = 0
                while (state != 4) {
                    state = CredentialApi.credentialUpdateState(credentialHandle).get()
                    Log.d("CredentialHandler", "awaitCredentialReceived: state($state)")
                    count++
                    if (count == 10) {
                        completionHandler("Failed")
                        return@deserialize
                    }
                    Thread.sleep(1000)
                }
            } catch (e: VcxException) {
                e.print("CredentialHandler", "awaitCredentialReceived: (2)")
            }
            CredentialApi.credentialSerialize(credentialHandle).whenCompleteAsync { serialized, error2 ->
                (error2 as? VcxException)?.let {
                    it.print("CredentialHandler", "awaitCredentialReceived: (3)")
                    completionHandler(it.localizedMessage)
                    return@whenCompleteAsync
                }
                credential.serialized = serialized
                credential.status = "accepted"
                DIDCredential.update(credential)
                credential.getReferent { data ->
                    if (data == null) {
                        Log.e(
                            "CredentialHandler",
                            "awaitCredentialReceived: (4) failed to get referent"
                        )
                        completionHandler("failed to get referent")
                        return@getReferent
                    }
                    credential.referent = data.first
                    credential.definitionId = data.second
                    DIDCredential.update(credential)
                    completionHandler(null)
                }
            }
        }
    }

    fun rejectCredential(credential: DIDCredential, completionHandler: (String?) -> Unit) {
        if (credential.status == "pending") {
            DIDCredential.delete(credential)
            completionHandler(null)
            return
        }
        val connection = DIDConnection.getById(credential.connectionId)
        if (connection == null) {
            Log.e(
                "CredentialHandler",
                "rejectCredential: (1) cannot find connection by id ${credential.connectionId}"
            )
            completionHandler("cannot find connection by id ${credential.connectionId}")
            return
        }
        connection.deserialize { connectionHandle ->
            if (connectionHandle == null) {
                Log.e("CredentialHandler", "rejectCredential: (2) failed to deserialize connection")
                completionHandler("failed to deserialize connection")
                return@deserialize
            }
            credential.deserialize { credentialHandle ->
                if (credentialHandle == null) {
                    Log.e(
                        "CredentialHandler",
                        "rejectCredential: (3) failed to deserialize credential"
                    )
                    completionHandler("failed to deserialize credential")
                    return@deserialize
                }
                CredentialApi.credentialReject(credentialHandle, connectionHandle, "")
                    .whenComplete { _, error ->
                        (error as? VcxException)?.let {
                            it.print("CredentialHandler", "rejectCredential: (4)")
                            completionHandler(it.localizedMessage)
                            return@whenComplete
                        }
                        DIDCredential.delete(credential)
                        completionHandler(null)
                    }
            }
        }
    }

}