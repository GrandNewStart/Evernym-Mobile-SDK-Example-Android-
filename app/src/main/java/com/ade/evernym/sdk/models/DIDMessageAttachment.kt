package com.ade.evernym.sdk.models

data class DIDMessageAttachment(

    var type: String,
    var data: String
) {
    fun isCredentialAttachment(): Boolean {
        return type.contains("issue-credential/1.0/offer-credential")
    }
    fun isProofAttachment(): Boolean {
        return type.contains("/present-proof/1.0/request-presentation")
    }
    fun isQuestion(): Boolean {
        return type.contains("committedanswer/1.0/question")
    }
}
