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

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WebDavBackupRepositoryTest {

    @Test
    fun backupExportsEncryptsAndUploadsLatestBackup() = runTest {
        val exporter = FakeBackupExporter(exportJson = "{\"hosts\":[]}")
        val crypto = FakeBackupCrypto(encrypted = "encrypted-backup")
        val client = FakeBackupTransport()
        val repository = WebDavBackupRepository(exporter, crypto, client)

        val result = repository.backup(
            WebDavBackupConfig(
                baseUrl = "https://dav.example.com/remote.php/dav/files/alice/",
                username = "alice",
                password = "dav-password",
                remotePath = "/ConnectBot/latest.cbbackup",
                encryptionPassword = "backup-password",
            ),
        )

        assertEquals(WebDavBackupResult.Success, result)
        assertEquals("backup-password", crypto.encryptPassword)
        assertEquals("{\"hosts\":[]}", crypto.encryptPlaintext)
        assertEquals("ConnectBot/latest.cbbackup", client.uploadPath)
        assertEquals("encrypted-backup", client.uploadBytes?.toString(Charsets.UTF_8))
    }

    @Test
    fun restoreDownloadsDecryptsAndImportsBackup() = runTest {
        val exporter = FakeBackupExporter()
        val crypto = FakeBackupCrypto(decrypted = "{\"hosts\":[]}")
        val client = FakeBackupTransport(downloadBytes = "encrypted-backup".toByteArray())
        val repository = WebDavBackupRepository(exporter, crypto, client)

        val result = repository.restore(
            WebDavBackupConfig(
                baseUrl = "https://dav.example.com/remote.php/dav/files/alice/",
                username = "alice",
                password = "dav-password",
                remotePath = "/ConnectBot/latest.cbbackup",
                encryptionPassword = "backup-password",
            ),
        )

        assertEquals(WebDavBackupResult.Success, result)
        assertEquals("ConnectBot/latest.cbbackup", client.downloadPath)
        assertEquals("encrypted-backup", crypto.decryptEnvelope)
        assertEquals("backup-password", crypto.decryptPassword)
        assertEquals("{\"hosts\":[]}", exporter.importJson)
    }

    @Test
    fun backupRejectsBlankRequiredFields() = runTest {
        val repository = WebDavBackupRepository(FakeBackupExporter(), FakeBackupCrypto(), FakeBackupTransport())

        val result = repository.backup(
            WebDavBackupConfig(
                baseUrl = "",
                username = "alice",
                password = "dav-password",
                remotePath = "ConnectBot/latest.cbbackup",
                encryptionPassword = "backup-password",
            ),
        )

        assertEquals(WebDavBackupResult.MissingConfiguration, result)
    }

    private class FakeBackupExporter(
        private val exportJson: String = "{}",
    ) : ConnectBotBackupExporter {
        var importJson: String? = null

        override fun exportJson(): String = exportJson

        override fun importJson(json: String) {
            importJson = json
        }
    }

    private class FakeBackupCrypto(
        private val encrypted: String = "encrypted",
        private val decrypted: String = "{}",
    ) : BackupCryptoEngine {
        var encryptPlaintext: String? = null
        var encryptPassword: String? = null
        var decryptEnvelope: String? = null
        var decryptPassword: String? = null

        override fun encrypt(plaintext: ByteArray, password: String): String {
            encryptPlaintext = plaintext.toString(Charsets.UTF_8)
            encryptPassword = password
            return encrypted
        }

        override fun decrypt(envelope: String, password: String): ByteArray {
            decryptEnvelope = envelope
            decryptPassword = password
            return decrypted.toByteArray()
        }
    }

    private class FakeBackupTransport(
        private val downloadBytes: ByteArray = ByteArray(0),
    ) : WebDavBackupTransport {
        var uploadPath: String? = null
        var uploadBytes: ByteArray? = null
        var downloadPath: String? = null

        override suspend fun upload(config: WebDavBackupConfig, path: String, bytes: ByteArray) {
            uploadPath = path
            uploadBytes = bytes
        }

        override suspend fun download(config: WebDavBackupConfig, path: String): ByteArray {
            downloadPath = path
            return downloadBytes
        }
    }
}
