package com.ade.evernym.sdk.handlers

import android.util.Log
import android.webkit.URLUtil
import com.ade.evernym.getJSONArrayOptional
import com.ade.evernym.getJSONObjectOptional
import com.ade.evernym.getStringOptional
import com.ade.evernym.sdk.models.DIDInvitation
import com.ade.evernym.sdk.models.DIDMessageAttachment
import com.evernym.sdk.vcx.utils.UtilsApi
import org.json.JSONObject
import java.util.*

object InvitationHandler {

    fun getInvitation(url: String, completionHandler: (invitation: DIDInvitation?, error: String?)->Unit) {
        if (!URLUtil.isValidUrl(url)) {
            completionHandler(null, "url not valid")
            return
        }
        UtilsApi.vcxResolveMessageByUrl(url).whenComplete { message, error ->
            error?.let {
                Log.e("InvitationHandler", "getInvitation: (1) ${it.localizedMessage}")
                completionHandler(null, it.localizedMessage)
                return@whenComplete
            }
            message?.let {
                val messageJSON = JSONObject(it)
                val type = messageJSON.getStringOptional("@type")
                if (type == null) {
                    Log.e("InvitationHandler", "getInvitation: (2) no type info")
                    completionHandler(null, "no type info")
                    return@whenComplete
                }
                if (type.contains("invitation")) {
                    getAttachment(it) { attachment ->
                        val invitation = DIDInvitation(
                            UUID.randomUUID().toString(),
                            messageJSON.getStringOptional("profileUrl") ?: "",
                            messageJSON.getStringOptional("label") ?: "",
                            message,
                            attachment
                        )
                        completionHandler(invitation, null)
                        return@getAttachment
                    }
                    return@whenComplete
                }
                Log.e("InvitationHandler", "getInvitation: (3) message type - $type")
                completionHandler(null, "message type - $type")
            }
        }
    }

    private fun getAttachment(message: String, completionHandler: (attachment: DIDMessageAttachment?)->Unit) {
        val messageJSON = JSONObject(message)
        UtilsApi.vcxExtractAttachedMessage(message).whenComplete { extraction, error ->
            error?.let {
                Log.e("InvitationHandler", "getAttachment: (1) ${error.localizedMessage}")
                completionHandler(null)
                return@whenComplete
            }
            val attachment = JSONObject(extraction)
            val type = attachment.getStringOptional("@type")
            val requestAttach = messageJSON.getJSONArrayOptional("request~attach")
            val item = requestAttach?.getJSONObjectOptional(0)
            val id = item?.getString("@id")
            if (type == null || requestAttach == null || item == null || id == null) {
                Log.e("InvitationHandler", "getAttachment: (2) ${error.localizedMessage}")
                completionHandler(null)
                return@whenComplete
            }
            attachment.put("@id", id)
            completionHandler(DIDMessageAttachment(type, attachment.toString()))
        }
    }

}