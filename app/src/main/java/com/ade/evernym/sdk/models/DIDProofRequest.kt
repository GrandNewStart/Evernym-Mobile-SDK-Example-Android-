package com.ade.evernym.sdk.models

import android.util.Log
import com.ade.evernym.*
import com.ade.evernym.sdk.SDKStorage
import com.evernym.sdk.vcx.VcxException
import com.evernym.sdk.vcx.proof.DisclosedProofApi
import org.json.JSONObject
import java.util.*

data class DIDProofRequest(
    var id: String,
    var threadId: String,
    var name: String,
    var connectionId: String,
    var connectionName: String,
    var connectionLogo: String,
    var serialized: String,
    var timestamp: String
) {

    fun printDescription() {
        JSONObject().apply {
            put("id", id)
            put("threadId", threadId)
            put("name", name)
            put("connectionId", connectionId)
            put("connectionName", connectionName)
            put("connectionLogo", connectionLogo)
            put("serialized", JSONObject(serialized))
            put("timestamp", timestamp)
            printLog("--->", this.toString())
        }
    }

    fun deserialize(completionHandler: (Int?) -> Unit) {
        DisclosedProofApi.proofDeserialize(serialized).whenCompleteAsync { handle, error ->
            (error as? VcxException)?.let {
                it.print("DIDProofRequest", "deserialize: ")
                completionHandler(null)
                return@whenCompleteAsync
            }
            completionHandler(handle)
        }
    }

    companion object {

        fun create(connection: DIDConnection, serialized: String): DIDProofRequest? {
            val json = JSONObject(serialized)
            val data = json.getJSONObjectOptional("data")
            if (data == null) {
                Log.e("DIDProofRequest", "create: (1) cannot find field, 'data'")
                return null
            }
            val prover = data.getJSONObjectOptional("prover_sm")
            if (prover == null) {
                Log.e("DIDProofRequest", "create: (2) cannot find field, 'prover_sm'")
                return null
            }
            val state = prover.getJSONObjectOptional("state")
            if (state == null) {
                Log.e("DIDProofRequest", "create: (3) cannot find field, 'state'")
                return null
            }
            val requestReceived = state.getJSONObjectOptional("RequestReceived")
            if (requestReceived == null) {
                Log.e("DIDProofRequest", "create: (4) cannot find field, 'RequestReceived'")
                return null
            }
            val threadId = requestReceived.getJSONObjectOptional("thread")?.getStringOptional("thid")
            if (threadId == null) {
                Log.e("DIDProofRequest", "create: (5) cannot find field, 'thid'")
                return null
            }
            val proofRequestString = requestReceived.getJSONObjectOptional("presentation_request")
                ?.getJSONArrayOptional("request_presentations~attach")
                ?.getJSONObject(0)
                ?.getJSONObjectOptional("data")
                ?.getStringOptional("base64")
                ?.decodeBase64()
            if (proofRequestString == null) {
                Log.e("DIDProofRequest", "create: (6) cannot decode base64 data")
                return null
            }
            val proofRequestJSON = JSONObject(proofRequestString)
            return DIDProofRequest(
                UUID.randomUUID().toString(),
                "",
                proofRequestJSON.getStringOptional("name") ?: "No Name",
                connection.id,
                connection.name,
                connection.logo,
                serialized,
                Date().toString()
            )
        }

        fun getById(id: String): DIDProofRequest? {
            return try {
                SDKStorage.proofRequests.first { it.id == id }
            } catch(e: Exception) {
                null
            }
        }

        fun add(proofRequest: DIDProofRequest) {
            val proofRequests = SDKStorage.proofRequests
            for (i in 0 until proofRequests.count()) {
                if (proofRequests[i].id == proofRequest.id) {
                    proofRequests[i] = proofRequest
                    SDKStorage.proofRequests = proofRequests
                    return
                }
            }
            proofRequests.add(proofRequest)
            SDKStorage.proofRequests = proofRequests
        }

        fun delete(proofRequest: DIDProofRequest) {
            val proofRequests = SDKStorage.proofRequests
            for (i in 0 until proofRequests.count()) {
                if (proofRequests[i].id == proofRequest.id) {
                    proofRequests.removeAt(i)
                    SDKStorage.proofRequests = proofRequests
                    return
                }
            }
        }

        /*
         result value
         {
             "attrs": {
                 {key}: {
                     "credential": {
                         "cred_def_id": String,
                         "referent": String,
                         "schema_id": String,
                         "{key}":"{value}",
                         "{key}":"{value}",
                         "{key}":"{value}",
                         "{key}":"{value}",
                         ...
                     },
                     "missing": false,
                     "self_attest_allowed": false,
                     "name": {key}
                 }
             }
         }
         */
        fun createSelectedCredentialObject(
            retrievedCredentials: String,
            attributes: JSONObject
        ): JSONObject {
            val json = JSONObject(retrievedCredentials)
            val attrs = json.getJSONObject("attrs")
            val result = JSONObject()
            for (key in attrs.keys()) {
                val attr = attributes.getJSONObjectOptional(key)
                val credentials = attrs.getJSONArrayOptional(key)
                if (attr == null || credentials == null) {
                    continue
                }
                for (i in 0 until credentials.length()) {
                    val credential = credentials.getJSONObject(i)
                    val cred_info = credential.getJSONObject("cred_info")
                    val cred_def_id = cred_info.getString("cred_def_id")
                    val schema_id = cred_info.getString("schema_id")
                    val referent = attr.getString("referent")
                    if (referent == cred_info.getString("referent")) {
                        credential.put("cred_def_id", cred_def_id)
                        credential.put("referent", referent)
                        credential.put("schema_id", schema_id)
                        val item = JSONObject().apply {
                            put("credential", credential)
                            put("missing", false)
                            put("self_attest_allowed", false)
                            put("name", key)
                        }
                        result.put(key, item)
                    }
                }
            }
            return JSONObject().apply { put("attrs", result) }
        }

    }

}