package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoEngine {

    private const val KEY_ALIAS = "EnclaveVaultEncryptionKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val IV_SIZE_BYTES = 12

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Reads cleartext from inputStream, encrypts it in chunks, 
     * writes ciphertext to outputStream and returns the Cipher Initialization Vector (IV).
     */
    fun encrypt(inputStream: InputStream, outputStream: OutputStream): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = getSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv

        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val encryptedBytes = cipher.update(buffer, 0, bytesRead)
            if (encryptedBytes != null) {
                outputStream.write(encryptedBytes)
            }
        }
        val finalBytes = cipher.doFinal()
        if (finalBytes != null) {
            outputStream.write(finalBytes)
        }

        outputStream.flush()
        return iv
    }

    /**
     * Reads ciphertext from inputStream, decrypts using key and iv, and writes cleartext to outputStream.
     */
    fun decrypt(inputStream: InputStream, outputStream: OutputStream, iv: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = getSecretKey()
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val decryptedBytes = cipher.update(buffer, 0, bytesRead)
            if (decryptedBytes != null) {
                outputStream.write(decryptedBytes)
            }
        }
        val finalBytes = cipher.doFinal()
        if (finalBytes != null) {
            outputStream.write(finalBytes)
        }
        outputStream.flush()
    }
}
