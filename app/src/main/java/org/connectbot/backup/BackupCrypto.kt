/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2026 Kenny Root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot.backup

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupCrypto(
    private val randomBytes: (Int) -> ByteArray = { size -> ByteArray(size).also { SecureRandom().nextBytes(it) } },
    private val iterations: Int = DEFAULT_ITERATIONS,
) : BackupCryptoEngine {
    override fun encrypt(plaintext: ByteArray, password: String): String {
        val salt = randomBytes(SALT_BYTES)
        val iv = randomBytes(IV_BYTES)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        return jsonEnvelope(
            "format" to FORMAT,
            "version" to VERSION,
            "algorithm" to TRANSFORMATION,
            "kdf" to KDF,
            "iterations" to iterations,
            "salt" to salt.base64(),
            "iv" to iv.base64(),
            "ciphertext" to ciphertext.base64(),
        )
    }

    override fun decrypt(envelope: String, password: String): ByteArray {
        try {
            val json = parseJsonEnvelope(envelope)
            if (json["format"] != FORMAT || json["version"]?.toIntOrNull() != VERSION) {
                throw BackupCryptoException("Unsupported backup format")
            }

            val salt = json.requiredString("salt").fromBase64()
            val iv = json.requiredString("iv").fromBase64()
            val ciphertext = json.requiredString("ciphertext").fromBase64()
            val envelopeIterations = json["iterations"]?.toIntOrNull() ?: iterations
            val key = deriveKey(password, salt, envelopeIterations)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            return cipher.doFinal(ciphertext)
        } catch (e: AEADBadTagException) {
            throw BackupCryptoException("Invalid backup password or corrupted backup", e)
        } catch (e: BackupCryptoException) {
            throw e
        } catch (e: Exception) {
            throw BackupCryptoException("Failed to decrypt backup", e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterationCount: Int = iterations): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterationCount, KEY_BITS)
        val keyBytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun ByteArray.base64(): String = Base64.getEncoder().encodeToString(this)

    private fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)

    private fun jsonEnvelope(vararg fields: Pair<String, Any>): String = fields.joinToString(
        prefix = "{",
        postfix = "}",
    ) { (key, value) ->
        val jsonValue = when (value) {
            is Number -> value.toString()
            else -> "\"${value.toString().escapeJson()}\""
        }
        "\"${key.escapeJson()}\":$jsonValue"
    }

    private fun parseJsonEnvelope(envelope: String): Map<String, String> {
        val trimmed = envelope.trim()
        if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) {
            throw BackupCryptoException("Invalid backup format")
        }
        val content = trimmed.substring(1, trimmed.length - 1).trim()
        if (content.isEmpty()) {
            return emptyMap()
        }

        return content.split(',').associate { field ->
            val separator = field.indexOf(':')
            if (separator <= 0) {
                throw BackupCryptoException("Invalid backup format")
            }
            val key = field.substring(0, separator).trim().trimJsonString()
            val value = field.substring(separator + 1).trim().trimJsonString()
            key to value
        }
    }

    private fun Map<String, String>.requiredString(key: String): String = this[key] ?: throw BackupCryptoException("Missing backup field: $key")

    private fun String.trimJsonString(): String = if (startsWith('"') && endsWith('"') && length >= 2) {
        substring(1, length - 1).unescapeJson()
    } else {
        this
    }

    private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"")

    private fun String.unescapeJson(): String = replace("\\\"", "\"").replace("\\\\", "\\")

    companion object {
        private const val FORMAT = "connectbot-webdav-backup"
        private const val VERSION = 1
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KDF = "PBKDF2WithHmacSHA256"
        private const val DEFAULT_ITERATIONS = 150_000
        private const val KEY_BITS = 256
        private const val TAG_BITS = 128
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
    }
}

class BackupCryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)
