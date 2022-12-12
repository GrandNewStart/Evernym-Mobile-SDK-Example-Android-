package com.ade.evernym.sdk

import android.util.Log
import com.ade.evernym.App
import com.ade.evernym.print
import com.evernym.sdk.vcx.VcxException
import com.evernym.sdk.vcx.utils.UtilsApi
import com.evernym.sdk.vcx.vcx.VcxApi
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

object SDKInitialization {

    fun initVCX(completionHandler: (String?)->Unit) {
        StorageUtils.configureStoragePermissions()
        App.shared.isLoading.postValue(true)
        if (SDKStorage.appProvisioned) {
            Log.d("SDKInitialization", "initVCX: (1)")
            initialize(completionHandler)
        } else {
            Log.d("SDKInitialization", "initVCX: (2)")
            SDKStorage.removeVcxConfig()
            provisionCloudAgentAndInitializeSdk(completionHandler)
        }
    }

    private fun getProvisionToken(completionHandler: (String?)->Unit) {
        val client = OkHttpClient()
        val json = JSONObject()
        json.put("sponseeId", UUID.randomUUID().toString())
        val requestBody = json.toString().toRequestBody("application/json;charset=utf8".toMediaType())
        val request = Request.Builder()
            .url("https://dev-did-sponsor.dw.myabcwallet.com/generate")
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("SDKInitialization", "getProvisionToken.onFailure: ${e.localizedMessage}")
                completionHandler(e.localizedMessage)
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    if (response.code == 200) {
                        completionHandler(it.string())
                        return
                    }
                }
                completionHandler(null)
            }
        })
    }

    private fun provisionCloudAgentAndInitializeSdk(completionHandler: (String?)->Unit) {
        val config = SDKConfig.getSDKConfig()
        Log.d("SDKInitialization", "provisionCloudAgentAndInitializeSdk: VcxConfig - $config")
        getProvisionToken { token ->
            if (token == null) {
                App.shared.isLoading.postValue(false)
                completionHandler("failed to get provisiton token")
                return@getProvisionToken
            }
            Log.d("SDKInitialization", "provisionCloudAgentAndInitializeSdk: Token - $token")
            UtilsApi.vcxAgentProvisionWithTokenAsync(config, token).whenComplete { oneTimeInfo, error ->
                error?.let {
                    it.printStackTrace()
                    Log.e("SDKInitialization", "provisionCloudAgentAndInitializeSdk: (1) ${it.localizedMessage}")
                    App.shared.isLoading.postValue(false)
                    completionHandler(it.localizedMessage)
                    return@whenComplete
                }
                Log.d("SDKInitialization", "provisionCloudAgentAndInitializeSdk: $oneTimeInfo")
                oneTimeInfo?.let {
                    SDKStorage.appProvisioned = true
                    SDKStorage.storeVcxConfig(it)
                    App.shared.isLoading.postValue(false)
                    initialize(completionHandler)
                    return@whenComplete
                }
            }
        }
    }

    private fun initialize(completionHandler: (String?)->Unit) {
        val config = SDKStorage.getVcxConfig()
        Log.d("SDKInitialization", "initialize: $config")
        try {
            VcxApi.vcxInitWithConfig(config).whenComplete { _: Int?, error: Throwable? ->
                error?.let {
                    Log.e("SDKInitialization", "initialize: (1) ${it.localizedMessage}")
                    App.shared.isLoading.postValue(false)
                    completionHandler(it.localizedMessage)
                    return@whenComplete
                }
                completionHandler(null)
            }
        } catch(e: VcxException) {
            e.print("SDKInitialization", "initialize: (2)")
            SDKStorage.removeVcxConfig()
            SDKStorage.appProvisioned = false
            App.shared.isLoading.postValue(false)
            completionHandler(e.localizedMessage)
        }
    }

}