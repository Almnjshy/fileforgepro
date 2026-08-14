package com.fileforge.pro.core.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM symmetric encryption (Master Spec §53, §71).
 *
 * Used by the Secure Vault to encrypt file contents at rest.
 *
 * Key derivation uses PBKDF2 with HMAC-SHA256, 600k iterations (OWASP 2023 recommendation),
 * 128-bit salt, 256-bit key output.
 */
object AesGcmCrypto {

    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH = 16        // bytes
    private const val IV_LENGTH = 12          // bytes (GCM standard)
    private const val TAG_LENGTH_BITS = 128
    private const val ITERATIONS = 600_000
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"

    private val random = SecureRandom()

    /**
     * Derive a 256-bit AES key from a password + salt.
     */
    fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, ALGORITHM)
    }

    /**
     * Generate a new random salt.
     */
    fun newSalt(): ByteArray = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }

    /**
     * Encrypt [plaintext] with [key]. Output = IV(12) + ciphertext+tag.
     */
    fun encrypt(plaintext: ByteArray, key: SecretKey): ByteArray {
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val cipherText = cipher.doFinal(plaintext)
        return iv + cipherText
    }

    /**
     * Decrypt data produced by [encrypt] using [key].
     */
    fun decrypt(ivPlusCipher: ByteArray, key: SecretKey): ByteArray {
        require(ivPlusCipher.size > IV_LENGTH) { "Ciphertext too short" }
        val iv = ivPlusCipher.copyOfRange(0, IV_LENGTH)
        val cipherText = ivPlusCipher.copyOfRange(IV_LENGTH, ivPlusCipher.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(cipherText)
    }

    /**
     * Convenience: encrypt [plaintext] directly with a password.
     * Returns salt(16) + iv(12) + ciphertext.
     */
    fun encryptWithPassword(plaintext: ByteArray, password: CharArray): ByteArray {
        val salt = newSalt()
        val key = deriveKey(password, salt)
        val encrypted = encrypt(plaintext, key)
        return salt + encrypted
    }

    /**
     * Convenience: decrypt salt(16) + iv(12) + ciphertext produced by [encryptWithPassword].
     */
    fun decryptWithPassword(payload: ByteArray, password: CharArray): ByteArray {
        require(payload.size > SALT_LENGTH + IV_LENGTH) { "Payload too short" }
        val salt = payload.copyOfRange(0, SALT_LENGTH)
        val ivPlusCipher = payload.copyOfRange(SALT_LENGTH, payload.size)
        val key = deriveKey(password, salt)
        return decrypt(ivPlusCipher, key)
    }
}
