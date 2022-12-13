package com.ade.evernym.sdk.handlers

import android.util.Log
import com.ade.evernym.getStringArray
import com.ade.evernym.getStringOptional
import com.ade.evernym.print
import com.ade.evernym.sdk.SDKStorage
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDInvitation
import com.ade.evernym.sdk.models.DIDMessage
import com.evernym.sdk.vcx.VcxException
import com.evernym.sdk.vcx.connection.ConnectionApi
import org.json.JSONObject
import java.util.*

object ConnectionHandler {

    fun getConnection(invitation: DIDInvitation, completionHandler: (DIDConnection?,String?)->Unit) {
        Log.d("ConnectionHandler", "getConnection")
        invitation.getExistingConnection { existingConnection, error ->
            error?.let {
                Log.e("ConnectionHandler", "getConnection: (1) $it")
                completionHandler(null, it)
                return@getExistingConnection
            }
            if (existingConnection == null) {
                Log.d("ConnectionHandler", "getConnection: no existing connection")
                createConnectionObject(invitation.message) { handle, error1 ->
                    error1?.let {
                        Log.e("ConnectionHandler", "getConnection: (2) $it")
                        completionHandler(null, it)
                        return@createConnectionObject
                    }
                    ConnectionApi.connectionInviteDetails(handle!!, 0).whenCompleteAsync { details, error2 ->
                        error2?.let {
                            Log.e("ConnectionHandler", "getConnection: (3) ${it.localizedMessage}")
                            completionHandler(null, it.localizedMessage)
                            return@whenCompleteAsync
                        }
                        val connectionJSON = JSONObject(details)
                        val name = connectionJSON.getStringOptional("label")
                        val logo = connectionJSON.getStringOptional("profileUrl")
                        if (name == null || logo == null) {
                            Log.e("ConnectionHandler", "getConnection: (4) no connection name, logo")
                            completionHandler(null, "no connection name, logo")
                            return@whenCompleteAsync
                        }
                        ConnectionApi.connectionSerialize(handle).whenCompleteAsync { serialized, error ->
                            error?.let {
                                Log.e("ConnectionHandler", "getConnection: (5) ${it.localizedMessage}")
                                completionHandler(null, it.localizedMessage)
                                return@whenCompleteAsync
                            }
                            val connection = DIDConnection(
                                UUID.randomUUID().toString(),
                                name,
                                logo,
                                "pending",
                                invitation.message,
                                "",
                                serialized
                            )
                            DIDConnection.add(connection)
                            completionHandler(connection, null)
                        }
                    }
                }
            } else {
                Log.d("ConnectionHandler", "getConnection: existing connection")
                existingConnection.deserialize { handle ->
                    if (handle == null) {
                        Log.e("ConnectionHandler", "getConnection: (6) failed to deserialize connection")
                        completionHandler(null, "failed to deserialize connection")
                        return@deserialize
                    }
                    ConnectionApi.connectionInviteDetails(handle, 0).whenCompleteAsync { details, error ->
                        error?.let {
                            Log.e("ConnectionHandler", "getConnection: (6) failed to deserialize connection")
                            completionHandler(null, "failed to deserialize connection")
                            return@whenCompleteAsync
                        }
                        val connectionJSON = JSONObject(details)
                        val name = connectionJSON.getStringOptional("label")
                        val logo = connectionJSON.getStringOptional("profileUrl")
                        if (name == null || logo == null) {
                            Log.e("ConnectionHandler", "getConnection: (7) failed to get connection name, logo")
                            completionHandler(null, "failed to get connection name, logo")
                            return@whenCompleteAsync
                        }
                        ConnectionApi.connectionSerialize(handle).whenCompleteAsync { serialized, error1 ->
                            error1?.let {
                                Log.e("ConnectionHandler", "getConnection: (8) ${it.localizedMessage}")
                                completionHandler(null, it.localizedMessage)
                                return@whenCompleteAsync
                            }
                            ConnectionApi.connectionGetPwDid(handle).whenCompleteAsync { pwDid, error2 ->
                                error2?.let {
                                    Log.e("ConnectionHandler", "getConnection: (9) ${it.localizedMessage}")
                                    completionHandler(null, it.localizedMessage)
                                    return@whenCompleteAsync
                                }
                                existingConnection.serialized = serialized
                                existingConnection.name = name
                                existingConnection.logo = logo
                                existingConnection.pwDid = pwDid
                                existingConnection.invitation = invitation.message
                                DIDConnection.update(existingConnection)
                                completionHandler(existingConnection, null)
                            }
                        }
                    }
                }
            }
        }
    }

    fun getConnection(message: DIDMessage): DIDConnection? {
        for (connection in SDKStorage.connections) {
            if (connection.pwDid == message.pwDid) {
                return connection
            }
        }
        return null
    }

    private fun createConnectionObject(invitation: String, completionHandler: (Int?, String?)->Unit) {
        val invitationJSON  = JSONObject(invitation)
        val invitationId    = invitationJSON.getString("@id")
        val type            = invitationJSON.getString("@type")

        if (type.contains("out-of-band")) {
            ConnectionApi.vcxCreateConnectionWithOutofbandInvite(invitationId, invitation).whenCompleteAsync { handle, error ->
                error?.let {
                    Log.e("ConnectionHandler", "createConnectionObject: (1) ${it.localizedMessage}")
                    completionHandler(null, it.localizedMessage)
                    return@whenCompleteAsync
                }
                completionHandler(handle!!, null)
            }
        } else {
            ConnectionApi.vcxCreateConnectionWithInvite(invitationId, invitation).whenCompleteAsync { handle, error ->
                error?.let {
                    Log.e("ConnectionHandler", "createConnectionObject: (2) ${it.localizedMessage}")
                    completionHandler(null, it.localizedMessage)
                    return@whenCompleteAsync
                }
                completionHandler(handle!!, null)
            }
        }
    }

    fun acceptConnection(connection: DIDConnection, completionHandler: (DIDConnection?,String?) -> Unit) {
        val invitation = JSONObject(connection.invitation)
        val type = invitation.getString("@type")

        if (connection.status == "pending") {
            Log.d("ConnectionHandler", "acceptConnection: (1-1) accepted connection")
            connect(connection) { updateConnection, error ->
                error?.let {
                    Log.e("ConnectionHandler", "acceptConnection: (1) $it")
                    completionHandler(null, it)
                    return@connect
                }
                DIDConnection.update(updateConnection!!)
                completionHandler(updateConnection, null)
            }
        }
        if (connection.status == "accepted") {
            Log.d("ConnectionHandler", "acceptConnection: (1-2) reuse existing connection")
            if (type.contains("out-of-band")) {
                reuse(connection) { updateConnection, error ->
                    error?.let {
                        Log.e("ConnectionHandler", "acceptConnection: (2) $it")
                        completionHandler(null, it)
                        return@reuse
                    }
                    DIDConnection.update(updateConnection!!)
                    completionHandler(updateConnection, null)
                }
            }
        }
    }

    private fun connect(connection: DIDConnection, completionHandler: (DIDConnection?, String?) -> Unit) {
        connection.deserialize { handle ->
            if (handle == null) {
                Log.e("ConnectionHandler", "connect: (1) failed to deserialize connection")
                completionHandler(null, "failed to deserialize connection")
                return@deserialize
            }
            ConnectionApi.vcxConnectionConnect(handle, "{}").whenComplete { _, error ->
                error?.let {
                    Log.e("ConnectionHandler", "connect: (2) ${it.localizedMessage}")
                    completionHandler(null, it.localizedMessage)
                    return@whenComplete
                }
                ConnectionApi.connectionSerialize(handle).whenComplete { serialized, error ->
                    error?.let {
                        Log.e("ConnectionHandler", "connect: (3) ${it.localizedMessage}")
                        completionHandler(null, it.localizedMessage)
                        return@whenComplete
                    }
                    connection.serialized = serialized
                    DIDConnection.update(connection)
                    awaitConnectionComplete(connection, completionHandler)
                }
            }
        }
    }

    private fun reuse(connection: DIDConnection, completionHandler: (DIDConnection?, String?) -> Unit) {
        val invitationJSON = JSONObject(connection.invitation)
        val handshakeProtocols = invitationJSON.getStringArray("handshake_protocols")
        if (handshakeProtocols.isEmpty()) {
            Log.e("ConnectionHandler", "reuse: (1-1) empty 'handshake_protocols")
            completionHandler(null, "empty 'handshake_protocols'")
            return
        }
        if (handshakeProtocols.first().isEmpty()) {
            Log.e("ConnectionHandler", "reuse: (1-2) empty 'handshake_protocols")
            completionHandler(null, "empty 'handshake_protocols'")
            return
        }
        connection.deserialize { handle ->
            if (handle == null) {
                Log.e("ConnectionHandler", "reuse: (2) failed to deserialize connection")
                completionHandler(null, "failed to deserialize connection")
                return@deserialize
            }
            ConnectionApi.connectionSendReuse(handle, connection.invitation).whenCompleteAsync { _, error ->
                error?.let {
                    Log.e("ConnectionHandler", "reuse: (3) ${it.localizedMessage}")
                    completionHandler(null, it.localizedMessage)
                    return@whenCompleteAsync
                }
                ConnectionApi.connectionSerialize(handle).whenCompleteAsync { serialized, error ->
                    error?.let {
                        Log.e("ConnectionHandler", "reuse: (4) ${it.localizedMessage}")
                        completionHandler(null, it.localizedMessage)
                        return@whenCompleteAsync
                    }
                    connection.serialized = serialized
                    DIDConnection.update(connection)
                    awaitReuseComplete(connection, completionHandler)
                }
            }
        }
    }

    private fun awaitConnectionComplete(connection: DIDConnection, completionHandler: (DIDConnection?, String?)->Unit) {
        connection.deserialize { handle ->
            if (handle == null) {
                Log.e("ConnectionHandler", "awaitConnectionComplete: (1) failed to deserialize connection")
                completionHandler(null, "failed to deserialize connection")
                return@deserialize
            }
            var count = 0
            while(count < 5) {
                try {
                    val state = ConnectionApi.vcxConnectionUpdateState(handle).get()
                    Log.d("ConnectionHandler", "awaitConnectionComplete: await attempt($count) - state($state)")
                    if (state == 4) break
                } catch(e: VcxException) {
                    e.print("ConnectionHandler", "awaitConnectionComplete: (2)")
                }
                count++
                Thread.sleep(1000)
            }
            ConnectionApi.connectionSerialize(handle).whenComplete { serialized, error ->
                error?.let {
                    Log.e("ConnectionHandler", "awaitConnectionComplete: (3) ${it.localizedMessage}")
                    completionHandler(null, it.localizedMessage)
                    return@whenComplete
                }
                ConnectionApi.connectionGetPwDid(handle).whenComplete { pwDid, error ->
                    error?.let {
                        Log.e("ConnectionHandler", "awaitConnectionComplete: (4) ${it.localizedMessage}")
                        completionHandler(null, it.localizedMessage)
                        return@whenComplete
                    }
                    connection.serialized = serialized
                    connection.pwDid = pwDid
                    connection.status = "accepted"
                    DIDConnection.update(connection)
                    completionHandler(connection, null)
                }
            }
        }
    }

    private fun awaitReuseComplete(connection: DIDConnection, completionHandler: (DIDConnection?, String?)->Unit) {
        connection.deserialize { handle ->
            if (handle == null) {
                Log.e("ConnectionHandler", "awaitReuseComplete: (1) failed to deserialize connection")
                completionHandler(null, "failed to deserialize connection")
                return@deserialize
            }
            var count = 0
            while(count < 5) {
                try {
                    val state = ConnectionApi.vcxConnectionUpdateState(handle).get()
                    Log.d("ConnectionHandler", "awaitReuseComplete: await attempt($count) - state ($state)")
                    if (state == 4) {
                        completionHandler(connection, null)
                        return@deserialize
                    }
                } catch(e: Exception) {
                    Log.e("ConnectionHandler", "awaitReuseComplete: (2) ${e.localizedMessage}")
                }
                count++
                Thread.sleep(1000)
            }
        }
    }

    fun deleteConnection(connection: DIDConnection, completionHandler: (String?)->Unit) {
        if (connection.status == "pending") {
            DIDConnection.delete(connection)
            completionHandler(null)
            return
        }
        connection.deserialize { handle ->
            if (handle == null) {
                Log.e(
                    "ConnectionHandler",
                    "deleteConnection: (1) failed to deserialize connection"
                )
                completionHandler("failed to deserialize connection")
                return@deserialize
            }
            ConnectionApi.deleteConnection(handle).whenCompleteAsync { _, error ->
                (error as? VcxException)?.let {
                    it.print("ConnectionHandler", "deleteConnection: (2)")
                    completionHandler(it.localizedMessage)
                    return@whenCompleteAsync
                }
                DIDConnection.delete(connection)
                completionHandler(null)
            }
        }
    }

}