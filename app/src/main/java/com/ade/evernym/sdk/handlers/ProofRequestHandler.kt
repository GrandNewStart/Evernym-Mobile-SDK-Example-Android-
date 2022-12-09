package com.ade.evernym.sdk.handlers

import android.util.Log
import com.ade.evernym.getJSONObjectOptional
import com.ade.evernym.getStringArray
import com.ade.evernym.getStringOptional
import com.ade.evernym.print
import com.ade.evernym.sdk.SDKStorage
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDCredential
import com.ade.evernym.sdk.models.DIDMessageAttachment
import com.ade.evernym.sdk.models.DIDProofRequest
import com.evernym.sdk.vcx.VcxException
import com.evernym.sdk.vcx.proof.DisclosedProofApi
import com.evernym.sdk.vcx.proof.ProofApi
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object ProofRequestHandler {

    fun getProofRequest(
        connection: DIDConnection,
        attachment: DIDMessageAttachment,
        completionHandler: (DIDProofRequest?, String?) -> Unit
    ) {
        if (!attachment.isProofAttachment()) {
            Log.e("ProofRequestHandler", "getProofRequest: (1) attachment is not proof request")
            completionHandler(null, "attachment is not proof request")
            return
        }
        DisclosedProofApi.proofCreateWithRequest(
            UUID.randomUUID().toString(),
            attachment.data
        ).whenComplete { handle, error1 ->
            (error1 as? VcxException)?.let {
                it.print("ProofRequestHandler", "getProofRequest: (2)")
                completionHandler(null, it.localizedMessage)
                return@whenComplete
            }
            Log.d("ProofRequestHandler", "---> handle: $handle")

            ProofApi.proofSerialize(handle).whenComplete { serialized, error2 ->
                (error2 as? VcxException)?.let {
                    it.print("ProofRequestHandler", "getProofRequest: (3)")
                    completionHandler(null, it.localizedMessage)
                    return@whenComplete
                }
                val proofRequest = DIDProofRequest.create(connection, serialized)
                if (proofRequest == null) {
                    Log.e(
                        "ProofRequestHandler",
                        "getProofRequest: (4) failed to create proof request"
                    )
                    completionHandler(null, "failed to create proof request")
                    return@whenComplete
                }
                completionHandler(proofRequest, null)
            }
        }
    }

    private fun retrieveCredentials(
        proofRequest: DIDProofRequest,
        completionHandler: (String?, String?) -> Unit
    ) {
        proofRequest.deserialize { handle ->
            if (handle == null) {
                Log.d(
                    "ProofRequestHandler",
                    "retrieveCredentials: (1) failed to deserialize proof request"
                )
                completionHandler(null, "failed to deserialize proof request")
                return@deserialize
            }
            DisclosedProofApi.proofRetrieveCredentials(handle).whenComplete { creds, error ->
                (error as? VcxException)?.let {
                    it.print("ProofRequestHandler", "retrieveCredentials: (2)")
                    completionHandler("ProofRequestHandler", it.localizedMessage)
                    return@whenComplete
                }
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
        retrieveCredentials(proofRequest) { retrievedCredentials, error1 ->
            error1?.let {
                Log.e("ProofRequestHandler", "getCredentialOptions: (1) $it")
                completionHandler(null, it)
                return@let
            }
            val attrs = JSONObject(retrievedCredentials!!).getJSONObjectOptional("attrs")
            if (attrs == null) {
                Log.e(
                    "ProofRequestHandler",
                    "getCredentialOptions: (2) failed to serialize retreived credentials"
                )
                completionHandler(null, "failed to serialize retreived credentials")
                return@retrieveCredentials
            }
            val key_referents = JSONObject()
            for (key in attrs.keys()) {
                val credentials = attrs.getJSONArray(key)
                val referents = arrayListOf<String>()
                for (i in 0 until credentials.length()) {
                    val cred_info =
                        credentials.getJSONObject(i).getJSONObjectOptional("cred_info") ?: continue
                    val referent = cred_info.getStringOptional("referent") ?: continue
                    referents.add(referent)
                }
                key_referents.put(key, referents)
            }
            val referents = SDKStorage.credentials.map { it.referent }.filter { it.isNotEmpty() }
            val result = JSONObject()
            for (key in attrs.keys()) {
                val items = JSONArray()
                val item = JSONObject()
                val array = key_referents.getStringArray(key)
                for (ref in referents) {
                    if (array.contains(ref)) {
                        var value = "Unknown"
                        val cred = DIDCredential.getByReferent(ref) ?: continue
                        val attributes = JSONObject(cred.attributes)
                        for (k in attributes.keys()) {
                            val name =
                                attributes.getJSONObject(k).getStringOptional("name") ?: continue
                            if (name == key) {
                                value = attributes.getJSONObject(k).getStringOptional("value")
                                    ?: continue
                            }
                        }
                        item.apply {
                            put("credentialName", cred.name)
                            put("connectionName", cred.connectionName)
                            put("connectionLogo", cred.connectionLogo)
                            put("createdAt", cred.createdAt)
                            item.put("value", value)
                            item.put("referent", ref)
                        }
                        items.put(item)
                    }
                }
                result.put(key, items)
            }
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
        val connection = DIDConnection.getById(proofRequest.connectionId)
        if (connection == null) {
            Log.e(
                "ProofRequestHandler",
                "acceptProofRequest: (1) cannot find connection by id(${proofRequest.connectionId})"
            )
            completionHandler("cannot find connection by id(${proofRequest.connectionId})")
            return
        }
        connection.deserialize { connectionHandle ->
            if (connectionHandle == null) {
                Log.e(
                    "ProofRequestHandler",
                    "acceptProofRequest: (2) failed to deserialize connection"
                )
                completionHandler("failed to deserialize connection")
                return@deserialize
            }
            proofRequest.deserialize { proofRequestHandle ->
                if (proofRequestHandle == null) {
                    Log.e(
                        "ProofRequestHandler",
                        "acceptProofRequest: (3) failed to deserialize proof request"
                    )
                    completionHandler("failed to deserialize proof request")
                    return@deserialize
                }
                DisclosedProofApi.proofRetrieveCredentials(proofRequestHandle)
                    .whenComplete { retrievedCredentials, error1 ->
                        (error1 as? VcxException)?.let {
                            it.print("ProofRequestHandler", "acceptProofRequest: (4)")
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
                                completionHandler(it.localizedMessage)
                                return@whenComplete
                            }
                            DisclosedProofApi.proofSend(
                                proofRequestHandle,
                                connectionHandle
                            ).whenComplete { _, error3 ->
                                (error3 as? VcxException)?.let {
                                    it.print("ProofRequestHandler", "acceptProofRequest: (6)")
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