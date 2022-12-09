package com.ade.evernym.sdk.models

import android.util.Log
import com.ade.evernym.getStringOptional
import com.ade.evernym.sdk.SDKStorage
import com.evernym.sdk.vcx.credential.CredentialApi
import org.json.JSONObject

data class DIDCredential(
    var id: String,
    var threadId: String,
    var referent: String,
    var definitionId: String,
    var name: String,
    var attributes: String,
    var connectionId: String,
    var connectionName: String,
    var connectionLogo: String,
    var status: String,
    var createdAt: String,
    var serialized: String
) {

    fun getDescription(): String {
        return """
            
            ID: $id
            THREAD ID: $threadId
            REFERENT: $referent
            DEFINITION ID: $definitionId
            NAME: $name
            ATTRIBUTES: $attributes
            CONNECTION ID: $connectionId
            CONNECTION NAME: $connectionName
            CONNECTION LOGO: $connectionLogo
            STATUS: $status
            CREATED AT: $createdAt
            SERIALIZED: $serialized
            
        """.trimIndent()
    }

    fun getAttribute(key: String): String? {
        val json = JSONObject(attributes)
        for (k in json.keys()) {
            if (key == k) {
                return json.getStringOptional(key)
            }
        }
        return null
    }

    fun deserialize(completionHandler: (Int?)->Unit) {
        CredentialApi.credentialDeserialize(serialized).whenComplete { handle, error ->
            error?.let {
                Log.e("DIDCredential", "deserialize: ${it.localizedMessage}")
                completionHandler(null)
                return@whenComplete
            }
            completionHandler(handle)
        }
    }

    fun getReferent(completionHandler: (Pair<String,String>?)->Unit) {
        deserialize { handle1 ->
            handle1?.let { handle2 ->
                CredentialApi.credentialGetInfo(handle2).whenComplete { info, error ->
                    error?.let {
                        Log.e("DIDCredential", "getReferent: (1) ${it.localizedMessage}")
                        completionHandler(null)
                        return@whenComplete
                    }
                    val json = JSONObject(info)
                    val referent = json.getStringOptional("referent")
                    val credDefId = json.getStringOptional("cred_def_id")
                    if (referent == null || credDefId == null) {
                        Log.e("DIDCredential", "getReferent: (2) cannot find 'referent' or 'cred_def_id'")
                        completionHandler(null)
                        return@whenComplete
                    }
                    completionHandler(Pair(referent, credDefId))
                }
            }
        }
    }

    companion object {

        fun getById(id: String): DIDCredential? {
            return SDKStorage.credentials.first { cred -> cred.id == id }
        }

        fun getByReferent(referent: String): DIDCredential? {
            return SDKStorage.credentials.first { cred -> cred.referent == referent }
        }

        fun add(credential: DIDCredential) {
            val credentials = SDKStorage.credentials
            for (i in 0 until credentials.count()) {
                if (credentials[i].id == credential.id) {
                    credentials[i] = credential
                    SDKStorage.credentials = credentials
                    return
                }
            }
            credentials.add(credential)
            SDKStorage.credentials = credentials
        }

        fun update(credential: DIDCredential) {
            val credentials = SDKStorage.credentials
            for (i in 0 until credentials.count()) {
                if (credentials[i].id == credential.id) {
                    credentials[i] = credential
                    SDKStorage.credentials = credentials
                    return
                }
            }
        }

        fun delete(credential: DIDCredential) {
            val credentials = SDKStorage.credentials
            for (i in 0 until credentials.count()) {
                if (credentials[i].id == credential.id) {
                    credentials.removeAt(i)
                    SDKStorage.credentials = credentials
                    return
                }
            }
        }

        fun getCredentialReferents(completionHandler: (List<Pair<String,String>>, String?)->Unit) {
            val credentials = SDKStorage.credentials
            val totalCount = credentials.count()
            var currentCount = 0
            val result = arrayListOf<Pair<String,String>>()
            for (i in 0 until totalCount) {
                credentials[i].getReferent { pair ->
                    pair?.let {  result.add(it)  }
                    currentCount++
                    if (currentCount == totalCount) {
                        completionHandler(result, null)
                    }
                }
            }
        }

    }

}