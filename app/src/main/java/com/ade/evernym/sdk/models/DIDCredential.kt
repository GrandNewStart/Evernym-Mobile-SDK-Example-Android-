package com.ade.evernym.sdk.models

data class DIDCredential(
    var id: String,
    var name: String,
    var referent: String,
    var connectionId: String,
    var connectionName: String,
    var connectionLogo: String,
    var status: String,
    var serialized: String
) {

    fun getDescription(): String {
        return """
            
            ID: $id
            NAME: $name
            REFERENT: $referent
            CONNECTION ID: $connectionId
            CONNECTION NAME: $connectionName
            CONNECTION LOGO: $connectionLogo
            STATUS: $status
            SERIALIZED: $serialized
            
        """.trimIndent()
    }
}