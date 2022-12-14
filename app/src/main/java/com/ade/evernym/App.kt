package com.ade.evernym

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.ade.evernym.sdk.SDKInitialization

class App: Application() {

    val sdkInitialized = MutableLiveData<Boolean>()
    val progressText = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>()

    override fun onCreate() {
        super.onCreate()
        shared = this

        progressText.postValue("Initializing SDK...")
        isLoading.postValue(true)

        SDKInitialization.initVCX { error ->
            this@App.isLoading.postValue(false)
            if (error == null) {
                this@App.sdkInitialized.postValue(true)
                this@App.progressText.postValue("SDK Ready")
            } else {
                Log.e("App", "onCreate: sdk initialization failed ($error)")
                this@App.sdkInitialized.postValue(false)
                this@App.progressText.postValue("SDK Failed")
            }
        }
    }

    companion object {
        lateinit var shared: App
    }

}