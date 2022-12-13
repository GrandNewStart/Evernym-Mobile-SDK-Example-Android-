package com.ade.evernym.sdk.handlers

import android.util.Log
import android.webkit.URLUtil
import com.ade.evernym.getStringOptional
import com.ade.evernym.print
import com.ade.evernym.sdk.models.DIDInvitation
import com.ade.evernym.sdk.models.DIDMessageAttachment
import com.evernym.sdk.vcx.VcxException
import com.evernym.sdk.vcx.utils.UtilsApi
import org.json.JSONObject
import java.util.*

object InvitationHandler {

    fun getInvitation(url: String, completionHandler: (invitation: DIDInvitation?, error: String?)->Unit) {
        Log.d("InvitationHandler", "getInvitation: $url")

        if (!URLUtil.isValidUrl(url)) {
            completionHandler(null, "url not valid")
            return
        }

        UtilsApi.vcxResolveMessageByUrl(url).whenCompleteAsync { message, error ->
            error?.let {
                Log.e("InvitationHandler", "getInvitation: (1) ${it.localizedMessage}")
                completionHandler(null, it.localizedMessage)
                return@whenCompleteAsync
            }

            val messageJSON = JSONObject(message)
            val type = messageJSON.getStringOptional("@type")
            if (type == null) {
                Log.e("InvitationHandler", "getInvitation: (2) no type info")
                completionHandler(null, "no type info")
                return@whenCompleteAsync
            }

            if (type.contains("invitation")) {
                getAttachment(message) { attachment ->
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
                return@whenCompleteAsync
            }

            Log.e("InvitationHandler", "getInvitation: (3) message type - $type")
            completionHandler(null, "message type - $type")
        }
    }

    private fun getAttachment(message: String, completionHandler: (attachment: DIDMessageAttachment?)->Unit) {
        Log.d("InvitationHandler", "getAttachment")
        UtilsApi.vcxExtractAttachedMessage(message).whenCompleteAsync { extraction, error ->
            (error as? VcxException)?.let {
                if (it.sdkErrorCode != 1100) {
                    it.print("InvitationHandler", "getAttachment: (1-1)")
                }
                completionHandler(null)
                return@whenCompleteAsync
            }
            val attachment = JSONObject(extraction)
            val type = attachment.getStringOptional("@type")
            if (type == null) {
                Log.e("InvitationHandler", "getAttachment: (2) attachment has no '@type'")
                completionHandler(null)
                return@whenCompleteAsync
            }
            completionHandler(DIDMessageAttachment(type, extraction))
        }
    }

}