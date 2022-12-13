package com.ade.evernym.sdk.models

import android.util.Log
import com.ade.evernym.handleBase64Scheme
import com.ade.evernym.printLog
import com.ade.evernym.sdk.SDKStorage
import com.evernym.sdk.vcx.connection.ConnectionApi
import org.json.JSONObject
import java.util.*

data class DIDConnection(
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var logo: String,
    var status: String,
    var invitation: String,
    var pwDid: String,
    var serialized: String,
    var timestamp: String = Date().toString()
) {

    fun printDescription() {
        JSONObject().apply {
            put("id",id)
            put("name",name)
            put("logo",logo)
            put("status",status)
            put("pwDid",pwDid)
            put("invitation",JSONObject(invitation))
            put("serialized",JSONObject(serialized))
            put("timestamp",timestamp)
            printLog("--->", this.toString())
        }
    }

    fun getDescription(): String {
        return """
            
            ID: $id
            NAME: ${name.handleBase64Scheme()}
            LOGO: $logo
            STATUS: $status
            PWDID: $pwDid
            INVITATION: $invitation
            SERIALIZED: $serialized
            
        """.trimIndent()
    }

    fun deserialize(completionHandler: (Int?)->Unit) {
        ConnectionApi.connectionDeserialize(serialized).whenCompleteAsync { handle, error ->
            error?.let {
                Log.e("DIDConnection", "deserialize: ${error.localizedMessage}")
                completionHandler(null)
                return@whenCompleteAsync
            }
            handle?.let {
                completionHandler(it)
                return@whenCompleteAsync
            }
        }
    }

    fun isSameConnection(new: DIDInvitation): Boolean {
        val newInvitation = JSONObject(new.message)
        val storedInvitation = JSONObject(this.invitation)
        try {
            val newId = newInvitation.getString("@id")
            val storedId = storedInvitation.getString("@id")
            if (newId == storedId) { return true }
        } catch(e: Exception) { }
        try {
            val newPublicDid = newInvitation.getString("public_did")
            val storedPublicDid = storedInvitation.getString("public_did")
            if (newPublicDid == storedPublicDid) { return true }
        } catch(e: Exception) { }
        try {
            val newRecipientKey = newInvitation.getJSONArray("recipientKeys")[0].toString()
            val storedRecipientKey = storedInvitation.getJSONArray("recipientKeys")[0].toString()
            if (newRecipientKey == storedRecipientKey) { return true }
        } catch(e: Exception) { }
        return false
    }

    companion object {

        fun getById(id: String): DIDConnection? {
            return try {
                SDKStorage.connections.first { it.id == id }
            } catch(e: Exception) {
                null
            }
        }

        fun getByPwDid(pwDid: String): DIDConnection? {
            return try {
                SDKStorage.connections.first { it.pwDid == pwDid }
            } catch(e: Exception) {
                null
            }
        }

        fun add(connection: DIDConnection) {
            val connections = SDKStorage.connections
            for (i in 0 until connections.count()) {
                if (connections[i].id == connection.id) {
                    connections[i] = connection
                    SDKStorage.connections = connections
                    return
                }
            }
            connections.add(connection)
            SDKStorage.connections = connections
        }

        fun update(connection: DIDConnection) {
            val connections = SDKStorage.connections
            for (i in 0 until connections.count()) {
                if (connections[i].id == connection.id) {
                    connections[i] = connection
                    SDKStorage.connections = connections
                    return
                }
            }
        }

        fun delete(connection: DIDConnection) {
            val connections = SDKStorage.connections
            connections.removeAll { it.id == connection.id }
            SDKStorage.connections = connections
            if (connection.status == "pending") { return }
            connection.deserialize { handle ->
                handle?.let {
                    ConnectionApi.deleteConnection(it).whenCompleteAsync { _, error ->
                        error?.let {
                            Log.e("DIDConnection", "delete: ${error.localizedMessage}")
                            return@whenCompleteAsync
                        }
                    }
                }
            }
        }

    }

}
