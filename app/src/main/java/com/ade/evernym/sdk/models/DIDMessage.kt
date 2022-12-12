package com.ade.evernym.sdk.models

import android.util.Log
import org.json.JSONObject

data class DIDMessage(
    val pwDid: String,
    val uid: String,
    val type: String,
    val payload: String
) {

    fun printDescription() {
        JSONObject().apply {
            put("pwDid", pwDid)
            put("uid", uid)
            put("type", type)
            put("payload", JSONObject(payload))
            Log.d("--->", this.toString())
        }
    }

}