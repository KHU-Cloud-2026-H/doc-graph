package com.docgraph.backend.auth.command.infra

import com.docgraph.backend.auth.command.domain.AccessTokenEncryptor
import com.docgraph.backend.auth.command.infra.notion.NotionOAuthRegistration
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class AesGcmAccessTokenEncryptor(
    private val registration: NotionOAuthRegistration,
) : AccessTokenEncryptor {
    private val random = SecureRandom()

    override fun encrypt(rawToken: String): String {
        require(registration.clientSecret.isNotBlank()) { "Notion OAuth client secret must not be blank" }
        val iv = ByteArray(12)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(rawToken.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    private fun key(): SecretKeySpec {
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(registration.clientSecret.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }
}
