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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun encryptWrapsPlaintextInVersionedJsonEnvelope() {
        val crypto = BackupCrypto(
            randomBytes = { size -> ByteArray(size) { index -> index.toByte() } },
            iterations = 1_000,
        )

        val envelope = crypto.encrypt("secret-json".toByteArray(), "backup password")

        assertTrue(envelope.contains("\"format\":\"connectbot-webdav-backup\""))
        assertTrue(envelope.contains("\"version\":1"))
        assertFalse(envelope.contains("secret-json"))
    }

    @Test
    fun decryptRestoresEncryptedPlaintext() {
        val crypto = BackupCrypto(
            randomBytes = { size -> ByteArray(size) { index -> (index * 3).toByte() } },
            iterations = 1_000,
        )

        val envelope = crypto.encrypt("secret-json".toByteArray(), "backup password")
        val plaintext = crypto.decrypt(envelope, "backup password")

        assertEquals("secret-json", plaintext.toString(Charsets.UTF_8))
    }

    @Test(expected = BackupCryptoException::class)
    fun decryptWithWrongPasswordThrows() {
        val crypto = BackupCrypto(
            randomBytes = { size -> ByteArray(size) { index -> index.toByte() } },
            iterations = 1_000,
        )

        val envelope = crypto.encrypt("secret-json".toByteArray(), "backup password")

        crypto.decrypt(envelope, "wrong password")
    }
}
