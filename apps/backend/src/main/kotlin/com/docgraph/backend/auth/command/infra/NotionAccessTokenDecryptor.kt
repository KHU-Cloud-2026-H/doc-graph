package com.docgraph.backend.auth.command.infra

import com.docgraph.backend.auth.command.infra.notion.NotionOAuthRegistration
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class NotionAccessTokenDecryptor(
    private val registration: NotionOAuthRegistration,
) {
    fun decrypt(encryptedToken: String): String {
        require(registration.clientSecret.isNotBlank()) { "Notion OAuth client secret must not be blank" }
        val decoded = Base64.getDecoder().decode(encryptedToken)
        require(decoded.size > IV_LENGTH) { "encrypted Notion access token is invalid" }
        val iv = decoded.copyOfRange(0, IV_LENGTH)
        val payload = decoded.copyOfRange(IV_LENGTH, decoded.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(payload).toString(Charsets.UTF_8)
    }

    private fun key(): SecretKeySpec {
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(registration.clientSecret.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    private companion object {
        private const val IV_LENGTH = 12
    }
}
