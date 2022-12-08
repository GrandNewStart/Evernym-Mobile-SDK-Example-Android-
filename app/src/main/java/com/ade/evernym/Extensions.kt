package com.ade.evernym

import android.util.Log
import com.evernym.sdk.vcx.VcxException
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

fun JSONObject.getStringOptional(key: String): String? {
    return try {
        getString(key)
    } catch(e: Exception) {
        null
    }
}

fun JSONObject.getJSONObjectOptional(key: String): JSONObject? {
    return try {
        getJSONObject(key)
    } catch(e: Exception) {
        null
    }
}

fun JSONObject.getJSONArrayOptional(key: String): JSONArray? {
    return try {
        getJSONArray(key)
    } catch(e: Exception) {
        null
    }
}

fun JSONObject.getStringArray(key: String): ArrayList<String> {
    val array = getJSONArrayOptional(key) ?: return arrayListOf()
    val result = ArrayList<String>()
    for (i in 0 until array.length()) {
        try {
            result.add(array.getString(i))
        } catch(e: Exception) {}
    }
    return result
}

fun JSONArray.getJSONObjectOptional(index: Int): JSONObject? {
    return try {
        getJSONObject(index)
    } catch(e: Exception) {
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