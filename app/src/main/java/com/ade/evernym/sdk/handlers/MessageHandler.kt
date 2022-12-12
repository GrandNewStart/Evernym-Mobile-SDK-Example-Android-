package com.ade.evernym.sdk.handlers

import com.ade.evernym.getJSONObjectOptional
import com.ade.evernym.getStringOptional
import com.ade.evernym.print
import com.ade.evernym.sdk.models.DIDMessage
import com.evernym.sdk.vcx.VcxException
import com.evernym.sdk.vcx.utils.UtilsApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object MessageHandler {

    @OptIn(DelicateCoroutinesApi::class)
    fun downloadPendingMessages(completionHandler: (ArrayList<DIDMessage>, String?)->Unit) {
        val result = arrayListOf<DIDMessage>()
        UtilsApi.vcxGetMessages("MS-103", null, null).whenComplete { string, error ->
            (error as? VcxException)?.let {
                it.print("MessageHandler", "downloadPendingMessages")
                GlobalScope.launch(Dispatchers.Main) {
                    completionHandler(result, it.localizedMessage)
                }
                return@whenComplete
            }
            val messages = JSONArray(string)
            for (i in 0 until messages.length()) {
                val message = messages.getJSONObject(i)
                val pwDid = message.getString("pairwiseDID") ?: continue
                val msgs = message.getJSONArray("msgs") ?: continue
                for (j in 0 until msgs.length()) {
                    val msg = msgs.getJSONObject(j)
                    val uid = msg.getString("uid") ?: continue
                    val decryptedPayload = msg.getString("decryptedPayload") ?: continue
                    val type = JSONObject(decryptedPayload).getJSONObjectOptional("@type")?.getStringOptional("name") ?: continue
                    result.add(DIDMessage(pwDid, uid, type, decryptedPayload))
                }
            }
            GlobalScope.launch(Dispatchers.Main) {
                completionHandler(result, null)
            }
        }
    }

    fun update(pwDid: String, messageUid: String, completionHandler: (String?)->Unit) {
        val pwDidsJSon = """[{"pairwiseDID":"$pwDid","uids":["$messageUid"]}]"""
        UtilsApi.vcxUpdateMessages("MS-106", pwDidsJSon).whenComplete { _, error ->
            (error as? VcxException)?.let {
                it.print("MessageHandler", "update")
                completionHandler(it.localizedMessage)
                return@whenComplete
            }
            completionHandler(null)
        }
    }

}