package com.ade.evernym.sdk.handlers

import android.util.Log
import com.ade.evernym.*
import com.ade.evernym.sdk.SDKStorage
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDMessageAttachment
import com.ade.evernym.sdk.models.DIDProofRequest
import com.evernym.sdk.vcx.VcxException
import com.evernym.sdk.vcx.proof.DisclosedProofApi
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object ProofRequestHandler {

    fun getProofRequest(
        connection: DIDConnection,
        attachment: DIDMessageAttachment,
        completionHandler: (DIDProofRequest?, String?) -> Unit
    ) {
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Fetching proof request...")

        if (!attachment.isProofAttachment()) {
            Log.e("ProofRequestHandler", "getProofRequest: (1) attachment is not proof request")
            App.shared.isLoading.postValue(false)
            App.shared.progressText.postValue("Failed to fetch proof request")
            completionHandler(null, "attachment is not proof request")
            return
        }
        DisclosedProofApi.proofCreateWithRequest(
            UUID.randomUUID().toString(),
            attachment.data
        ).whenCompleteAsync { handle, error1 ->
            (error1 as? VcxException)?.let {
                it.print("ProofRequestHandler", "getProofRequest: (2)")
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to fetch proof request")
                completionHandler(null, it.localizedMessage)
                return@whenCompleteAsync
            }
            DisclosedProofApi.proofSerialize(handle).whenCompleteAsync { serialized, error2 ->
                (error2 as? VcxException)?.let {
                    it.print("ProofRequestHandler", "getProofRequest: (3)")
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Failed to fetch proof request")
                    completionHandler(null, it.localizedMessage)
                    return@whenCompleteAsync
                }
                val proofRequest = DIDProofRequest.create(connection, serialized)
                if (proofRequest == null) {
                    Log.e(
                        "ProofRequestHandler",
                        "getProofRequest: (4) failed to create proof request"
                    )
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Failed to fetch proof request")
                    completionHandler(null, "failed to create proof request")
                } else {
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue(null)
                    completionHandler(proofRequest, null)
                }
            }
        }
    }

    private fun retrieveCredentials(
        proofRequest: DIDProofRequest,
        completionHandler: (String?, String?) -> Unit
    ) {
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Retrieving credentials...")

        proofRequest.deserialize { handle ->
            if (handle == null) {
                Log.d(
                    "ProofRequestHandler",
                    "retrieveCredentials: (1) failed to deserialize proof request"
                )
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to retrieve credentials")
                completionHandler(null, "failed to deserialize proof request")
                return@deserialize
            }
            DisclosedProofApi.proofRetrieveCredentials(handle).whenCompleteAsync { creds, error ->
                (error as? VcxException)?.let {
                    it.print("ProofRequestHandler", "retrieveCredentials: (2)")
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Failed to fetch proof request")
                    completionHandler("ProofRequestHandler", it.localizedMessage)
                    return@whenCompleteAsync
                }
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue(null)
                completionHandler(creds, null)
            }
        }
    }

    /**
    return value: 각 Key 별로 사용 가능한 Credential의 정보 리스트
    {
    "key_1": [
    {
    "credential_id": String,
    "credential_name": String,
    "connection_name": String,
    "connection_logo": String,
    "value": String,
    "referent": String
    }
    ],
    ...
    }
     */
    fun getCredentialOptions(
        proofRequest: DIDProofRequest,
        completionHandler: (JSONObject?, String?) -> Unit
    ) {
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Extracting credential options...")

        retrieveCredentials(proofRequest) { retrievedCredentials, error1 ->
            error1?.let {
                Log.e("ProofRequestHandler", "getCredentialOptions: (1) $it")
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to extract credential options")
                completionHandler(null, it)
                return@let
            }

            val attrs = JSONObject(retrievedCredentials!!).getJSONObjectOptional("attrs")
            if (attrs == null) {
                Log.e(
                    "ProofRequestHandler",
                    "getCredentialOptions: (2) failed to serialize retrieved credentials"
                )
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to extract credential options")
                completionHandler(null, "failed to serialize retrieved credentials")
                return@retrieveCredentials
            }

            val key_referents = JSONObject()
            for (key in attrs.keys()) {
                val credentials = attrs.getJSONArray(key)
                val referents = JSONArray()
                for (i in 0 until credentials.length()) {
                    val cred_info =
                        credentials.getJSONObject(i).getJSONObjectOptional("cred_info") ?: continue
                    val referent = cred_info.getStringOptional("referent") ?: continue
                    referents.put(referent)
                }
                key_referents.put(key, referents)
            }

            val result = JSONObject()
            for (key in attrs.keys()) {
                val items = JSONArray()
                val array = key_referents.getStringArray(key)
                val credentials = SDKStorage.credentials
                for (credential in credentials) {
                    if (array.contains(credential.referent)) {
                        val attributes = JSONArray(credential.attributes)
                        for (i in 0 until attributes.length()) {
                            val name    = attributes.getJSONObject(i).getStringOptional("name") ?: continue
                            val value   = attributes.getJSONObject(i).getStringOptional("value") ?: "Unknown"
                            if (name == key) {
                                JSONObject().apply {
                                    put("value", value)
                                    put("referent", credential.referent)
                                    put("credentialName", credential.name)
                                    put("connectionName", credential.connectionName)
                                    put("connectionLogo", credential.connectionLogo)
                                    put("createdAt", credential.createdAt)
                                    items.put(this)
                                }
                                break
                            }
                        }
                    }
                }
                result.put(key, items)
            }

            App.shared.isLoading.postValue(false)
            App.shared.progressText.postValue(null)
            completionHandler(result, null)
        }
    }

    /**
    attributes
    {
    "{attribute_key}" : {
    "value": "{credential_value}",
    "referent": "{credential_referent}"
    },
    ...
    }
     */
    fun acceptProofRequest(
        proofRequest: DIDProofRequest,
        attributes: JSONObject,
        completionHandler: (String?) -> Unit
    ) {
        App.shared.isLoading.postValue(true)
        App.shared.progressText.postValue("Sharing proofs...")

        val connection = DIDConnection.getById(proofRequest.connectionId)
        if (connection == null) {
            Log.e(
                "ProofRequestHandler",
                "acceptProofRequest: (1) cannot find connection by id(${proofRequest.connectionId})"
            )
            App.shared.isLoading.postValue(false)
            App.shared.progressText.postValue("Failed to share proofs")
            completionHandler("cannot find connection by id(${proofRequest.connectionId})")
            return
        }
        connection.deserialize { connectionHandle ->
            if (connectionHandle == null) {
                Log.e(
                    "ProofRequestHandler",
                    "acceptProofRequest: (2) failed to deserialize connection"
                )
                App.shared.isLoading.postValue(false)
                App.shared.progressText.postValue("Failed to share proofs")
                completionHandler("failed to deserialize connection")
                return@deserialize
            }
            proofRequest.deserialize { proofRequestHandle ->
                if (proofRequestHandle == null) {
                    Log.e(
                        "ProofRequestHandler",
                        "acceptProofRequest: (3) failed to deserialize proof request"
                    )
                    App.shared.isLoading.postValue(false)
                    App.shared.progressText.postValue("Failed to share proofs")
                    completionHandler("failed to deserialize proof request")
                    return@deserialize
                }
                DisclosedProofApi.proofRetrieveCredentials(proofRequestHandle)
                    .whenComplete { retrievedCredentials, error1 ->
                        (error1 as? VcxException)?.let {
                            it.print("ProofRequestHandler", "acceptProofRequest: (4)")
                            App.shared.isLoading.postValue(false)
                            App.shared.progressText.postValue("Failed to share proofs")
                            completionHandler(it.localizedMessage)
                            return@whenComplete
                        }
                        val credentials = DIDProofRequest.createSelectedCredentialObject(
                            retrievedCredentials,
                            attributes
                        )
                        DisclosedProofApi.proofGenerate(
                            proofRequestHandle,
                            credentials.toString(),
                            "{}"
                        ).whenComplete { _, error2 ->
                            (error2 as? VcxException)?.let {
                                it.print("ProofRequestHandler", "acceptProofRequest: (5)")
                                App.shared.isLoading.postValue(false)
                                App.shared.progressText.postValue("Failed to share proofs")
                                completionHandler(it.localizedMessage)
                                return@whenComplete
                            }
                            DisclosedProofApi.proofSend(
                                proofRequestHandle,
                                connectionHandle
                            ).whenComplete { _, error3 ->
                                (error3 as? VcxException)?.let {
                                    it.print("ProofRequestHandler", "acceptProofRequest: (6)")
                                    App.shared.isLoading.postValue(false)
                                    App.shared.progressText.postValue("Failed to share proofs")
                                    completionHandler(it.localizedMessage)
                                    return@whenComplete
                                }
                                DIDProofRequest.delete(proofRequest)
                                App.shared.isLoading.postValue(false)
                                App.shared.progressText.postValue(null)
                                completionHandler(null)
                            }
                        }
                    }
            }
        }
    }

    fun rejectProofRequest(
        proofRequest: DIDProofRequest,
        completionHandler: (String?) -> Unit
    ) {
        val connection = DIDConnection.getById(proofRequest.connectionId)
        if (connection == null) {
            Log.e(
                "ProofRequestHandler",
                "rejectProofRequest: (1) cannot find connection by id(${proofRequest.connectionId})"
            )
            completionHandler("cannot find connection by id(${proofRequest.connectionId})")
            return
        }

        if (connection.status == "pending") {
            DIDConnection.delete(connection)
            DIDProofRequest.delete(proofRequest)
            completionHandler(null)
            return
        }

        connection.deserialize { connectionHandle ->
            if (connectionHandle == null) {
                Log.e(
                    "ProofRequestHandler",
                    "rejectProofRequest: (2) failed to deserialize connection"
                )
                completionHandler("failed to deserialize connection")
                return@deserialize
            }
            proofRequest.deserialize { proofRequestHandle ->
                if (proofRequestHandle == null) {
                    Log.e(
                        "ProofRequestHandler",
                        "rejectProofRequest: (3) failed to deserialize proof request"
                    )
                    completionHandler("failed to deserialize proof request")
                    return@deserialize
                }
                DisclosedProofApi.proofDeclineRequest(
                    proofRequestHandle,
                    connectionHandle,
                    "",
                    null
                ).whenComplete { _, error ->
                    (error as? VcxException)?.let {
                        it.print("ProofRequestHandler", "rejectProofRequest: (4)")
                        completionHandler(it.localizedMessage)
                        return@whenComplete
                    }
                    DIDProofRequest.delete(proofRequest)
                    completionHandler(null)
                }
            }
        }
    }

}