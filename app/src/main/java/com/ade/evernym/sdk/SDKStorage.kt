package com.ade.evernym.sdk

import android.content.Context.MODE_PRIVATE
import androidx.lifecycle.MutableLiveData
import com.ade.evernym.App
import com.ade.evernym.sdk.models.DIDConnection
import com.ade.evernym.sdk.models.DIDCredential
import com.ade.evernym.sdk.models.DIDInvitation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SDKStorage {

    val connectionsLiveData = MutableLiveData<ArrayList<DIDConnection>>()
    val credentialsLiveData = MutableLiveData<ArrayList<DIDCredential>>()

    var appProvisioned: Boolean
    get() {
        val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
        return prefs.getBoolean("appProvisioned", false)
    }
    set(value) {
        val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
        prefs.edit().let {
            it.putBoolean("appProvisioned", value)
            it.apply()
        }
    }

    var invitations: ArrayList<DIDInvitation>
    get() {
        val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
        val string = prefs.getString("invitations", "[]")
        val gson = Gson()
        val typeToken = object : TypeToken<ArrayList<DIDInvitation>>(){}.type
        return gson.fromJson(string, typeToken)
    }
    set(value) {
        val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
        prefs.edit().let {
            val gson = Gson()
            val json = gson.toJson(value)
            it.putString("invitations", json)
            it.apply()
        }
    }

    var connections: ArrayList<DIDConnection>
        get() {
            val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
            val string = prefs.getString("connections", "[]")
            val gson = Gson()
            val typeToken = object : TypeToken<ArrayList<DIDConnection>>(){}.type
            return gson.fromJson(string, typeToken)
        }
        set(value) {
            val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
            prefs.edit().let {
                val gson = Gson()
                val json = gson.toJson(value)
                it.putString("connections", json)
                it.apply()
            }
            connectionsLiveData.postValue(value)
        }

    var credentials: ArrayList<DIDCredential>
        get() {
            val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
            val string = prefs.getString("credentials", "[]")
            val gson = Gson()
            val typeToken = object : TypeToken<ArrayList<DIDCredential>>(){}.type
            return gson.fromJson(string, typeToken)
        }
        set(value) {
            val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
            prefs.edit().let {
                val gson = Gson()
                val json = gson.toJson(value)
                it.putString("credentials", json)
                it.apply()
            }
            credentialsLiveData.postValue(value)
        }

    fun removeVcxConfig() {
        val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
        prefs.edit().let {
            it.remove("vcxConfig")
            it.apply()
        }
    }

    fun storeVcxConfig(config: String) {
        val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
        prefs.edit().let {
            it.putString("vcxConfig", config)
            it.apply()
        }
    }

    fun getVcxConfig(): String? {
        val prefs = App.shared.getSharedPreferences("storage", MODE_PRIVATE)
        return prefs.getString("vcxConfig", null)
    }

}