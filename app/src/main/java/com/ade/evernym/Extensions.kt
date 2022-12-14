package com.ade.evernym

import android.util.Log
import com.evernym.sdk.vcx.VcxException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

fun JSONObject.getStringOptional(key: String): String? {
    return try {
        getString(key)
    } catch(e: JSONException) {
        null
    }
}

fun JSONObject.getJSONObjectOptional(key: String): JSONObject? {
    return try {
        getJSONObject(key)
    } catch(e: JSONException) {
        null
    }
}

fun JSONObject.getJSONArrayOptional(key: String): JSONArray? {
    return try {
        getJSONArray(key)
    } catch(e: JSONException) {
        null
    }
}

fun JSONObject.getStringArray(key: String): ArrayList<String> {
    val array = getJSONArrayOptional(key) ?: return arrayListOf()
    val result = ArrayList<String>()
    for (i in 0 until array.length()) {
        try {
            result.add(array.getString(i))
        } catch(e: JSONException) {}
    }
    return result
}

fun JSONArray.getJSONObjectOptional(index: Int): JSONObject? {
    return try {
        getJSONObject(index)
    } catch(e: JSONException) {
        null
    }
}

fun VcxException.print(tag: String, name: String) {
    Log.e(
        tag,
        """
            $name
            ===VCX EXCEPTION===
                CODE: ${this.sdkErrorCode}
                MESSAGE: ${this.message}
                CAUSE: ${this.sdkCause}
                
        """.trimIndent()
    )
}

fun String.handleBase64Scheme(): String {
    return if (this.startsWith("base64;")) {
        return String(Base64.getDecoder().decode(split(";")[1]))
    } else {
        this
    }
}

fun String.decodeBase64(): String {
    return String(Base64.getDecoder().decode(this))
}

fun printLog(tag: String, text: String) {
    val MAX_LEN = 2000
    val len = text.length
    if (len > MAX_LEN) {
        var idx = 0
        var nextIdx = 0
        while(idx < len) {
            nextIdx += MAX_LEN
            Log.d(tag, text.substring(idx, if (nextIdx > len) len else nextIdx))
            Log.d(tag, "\n\n")
            idx = nextIdx
        }
    } else {
        Log.d(tag, "\n\n")
        Log.d(tag, text)
        Log.d(tag, "\n\n")
    }
}