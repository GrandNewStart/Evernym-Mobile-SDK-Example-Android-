package com.ade.evernym.sdk

import android.system.ErrnoException
import android.system.Os
import com.ade.evernym.App

object StorageUtils {
    fun configureStoragePermissions() {
        try {
            Os.setenv("EXTERNAL_STORAGE", App.shared.filesDir.absolutePath, true)
            // When we restore data, then we are not calling createOneTimeInfo
            // and hence ca-crt is not written within app directory
            // since the logic to write ca cert checks for file existence
            // we won't have to pay too much cost for calling this function inside init
        } catch (e: ErrnoException) {

        }
    }
}