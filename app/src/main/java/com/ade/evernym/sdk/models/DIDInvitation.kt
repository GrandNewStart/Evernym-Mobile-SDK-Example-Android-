package com.ade.evernym.sdk.models

import com.ade.evernym.sdk.SDKStorage
import com.ade.evernym.sdk.enums.InvitationType
import org.json.JSONObject

data class DIDInvitation (

    var id: String,
    var icon: String,
    var name: String,
    var message: String,
    var attachment: DIDMessageAttachment?

) {

    fun getType(): InvitationType {
        val invitation = JSONObject(message)
        val type = invitation.getString("@type")
        return if (type.contains("out-of-band")) InvitationType.OutOfBand else InvitationType.Connection
    }

    fun delete(id: String) {
        SDKStorage.invitations.let {
            it.removeIf { invitation -> invitation.id == id }
            SDKStorage.invitations = it
        }
    }

    fun getExistingConnection(completionHandler: (DIDConnection?, String?)->Unit) {
        val connections = SDKStorage.connections
        if (connections.isEmpty()) {
            completionHandler(null, null)
            return
        }
        for (connection in connections) {
            if (connection.isSameConnection(this)) {
                if (connection.pwDid.isEmpty()) {
                    completionHandler(null, null)
                } else {
                    completionHandler(connection, null)
                }
                return
            }
        }
        completionHandler(null, null)
    }
}