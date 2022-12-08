package com.ade.evernym.sdk.models

data class DIDMessage(
    val pwDid: String,
    val uid: String,
    val type: String,
    val payload: String
)