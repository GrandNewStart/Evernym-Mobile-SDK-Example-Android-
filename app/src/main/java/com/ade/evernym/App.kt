package com.ade.evernym

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.ade.evernym.sdk.SDKInitialization

class App: Application() {

    val sdkInitialized by lazy { MutableLiveData<Boolean>() }
    val progressText = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>()

    override fun onCreate() {
        super.onCreate()
        shared = this

        progressText.postValue("Initializing SDK...")
        isLoading.postValue(true)
        SDKInitialization.initVCX { error ->
            if (error == null) {
                Log.d("App", "onCreate: sdk initialized")
                sdkInitialized.postValue(true)
            } else {
                Log.e("App", "onCreate: sdk initialization failed ($error)")
                sdkInitialized.postValue(false)
                isLoading.postValue(false)
                progressText.postValue("SDK Failed")
            }
        }
    }

    companion object {
        lateinit var shared: App
    }

}