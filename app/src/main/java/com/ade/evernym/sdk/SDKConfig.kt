package com.ade.evernym.sdk

import com.ade.evernym.App
import com.ade.evernym.R
import okio.ByteString.Companion.encodeUtf8
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SDKConfig {

    private const val walletName    = "ABC Wallet"
    private const val agencyUrl     = "https://agency.pstg.evernym.com"
    private const val agencyDid     = "LqnB96M6wBALqRZsrTTwda"
    private const val agencyVerKey  = "BpDPZHLbJFu67sWujecoreojiWZbi2dgf4xnYemUzFvB"

    fun getSDKConfig(): String {
        JSONObject().let {
            it.put("wallet_name", walletName)
            it.put("wallet_key", walletName.encodeUtf8().base64())
            it.put("protocol_type", "3.0")
            it.put("name", "ABC")
            it.put("institution_logo_url", "http://logo.com")
            it.put("agency_url", agencyUrl)
            it.put("agency_did", agencyDid)
            it.put("agency_verkey", agencyVerKey)
            it.put("pool_networks", getNetworks())
            return it.toString()
        }
    }

    private fun getNetworks(): JSONArray {
        var genesisFile_dev = File(App.shared.filesDir.absolutePath, "transaction_genesis_pool_dev")
        var genesisFile_stg = File(App.shared.filesDir.absolutePath, "transaction_genesis_pool_stg")
        var genesisFile_prod = File(App.shared.filesDir.absolutePath, "transaction_genesis_pool_prod")

        if (!genesisFile_dev.exists()) {
            genesisFile_dev = SDKUtils.write(genesisFile_dev, R.raw.transaction_genesis_pool_dev)
        }
        if (!genesisFile_stg.exists()) {
            genesisFile_stg = SDKUtils.write(genesisFile_stg, R.raw.transaction_genesis_pool_stg)
        }
        if (!genesisFile_prod.exists()) {
            genesisFile_prod = SDKUtils.write(genesisFile_prod, R.raw.transaction_genesis_pool_prod)
        }

        JSONArray().let {
            JSONObject().let { obj ->
                obj.put("genesis_path", genesisFile_dev.absolutePath)
                obj.put("namespace_list", JSONArray().apply { put("dev") })
                it.put(obj)
            }
            JSONObject().let { obj ->
                obj.put("genesis_path", genesisFile_stg.absolutePath)
                obj.put("namespace_list", JSONArray().apply { put("stg") })
                it.put(obj)
            }
            JSONObject().let { obj ->
                obj.put("genesis_path", genesisFile_prod.absolutePath)
                obj.put("namespace_list", JSONArray().apply { put("prod") })
                it.put(obj)
            }
            return it
        }
    }

}