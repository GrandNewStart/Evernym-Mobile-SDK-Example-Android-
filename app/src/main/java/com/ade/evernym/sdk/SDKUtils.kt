package com.ade.evernym.sdk

import com.ade.evernym.App
import java.io.*

object SDKUtils {

    fun write(file: File, resId: Int): File {
        try {
            FileOutputStream(file).use { stream ->
                App.shared.resources.openRawResource(resId).use { genesisStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (genesisStream.read(buffer).also { bytesRead = it } != -1) {
                        stream.write(buffer, 0, bytesRead)
                    }
                    stream.close()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return file
    }

}