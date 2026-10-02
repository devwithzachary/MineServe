package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.repository.ServerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileManagerUnzipTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createZipFile(zipFile: File, entries: Map<String, String>) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for ((path, content) in entries) {
                val entry = ZipEntry(path)
                zos.putNextEntry(entry)
                if (!path.endsWith("/")) {
                    val bytes = content.toByteArray(Charsets.UTF_8)
                    zos.write(bytes)
                }
                zos.closeEntry()
            }
        }
    }

    @Test
    fun testExtractFlatZip() {
        val root = tempFolder.newFolder("flat_test")
        val zipFile = File(root, "plugins.zip")
        val targetDir = File(root, "plugins")

        val testEntries = mapOf(
            "EssentialsX.jar" to "dummy jar content 1",
            "Vault.jar" to "dummy jar content 2",
            "LuckPerms.jar" to "dummy jar content 3"
        )
        createZipFile(zipFile, testEntries)

        val result = ServerRepository.extractZip(zipFile, targetDir, deleteZipAfter = false)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
        assertTrue(zipFile.exists())

        assertEquals("dummy jar content 1", File(targetDir, "EssentialsX.jar").readText())
        assertEquals("dummy jar content 2", File(targetDir, "Vault.jar").readText())
        assertEquals("dummy jar content 3", File(targetDir, "LuckPerms.jar").readText())
    }

    @Test
    fun testExtractNestedZip() {
        val root = tempFolder.newFolder("nested_test")
        val zipFile = File(root, "bundle.zip")
        val targetDir = File(root, "server")

        val testEntries = mapOf(
            "plugins/" to "",
            "plugins/WorldEdit.jar" to "worldedit binary",
            "plugins/WorldEdit/config.yml" to "locale: en\nmax-blocks: 100000",
            "config/paper.yml" to "verbose: true"
        )
        createZipFile(zipFile, testEntries)

        val result = ServerRepository.extractZip(zipFile, targetDir, deleteZipAfter = false)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()) // 3 files (directory entry not counted in extracted files)

        assertTrue(File(targetDir, "plugins/WorldEdit.jar").exists())
        assertEquals("worldedit binary", File(targetDir, "plugins/WorldEdit.jar").readText())
        assertTrue(File(targetDir, "plugins/WorldEdit/config.yml").exists())
        assertEquals("locale: en\nmax-blocks: 100000", File(targetDir, "plugins/WorldEdit/config.yml").readText())
        assertTrue(File(targetDir, "config/paper.yml").exists())
        assertEquals("verbose: true", File(targetDir, "config/paper.yml").readText())
    }

    @Test
    fun testExtractZipWithDeleteAfter() {
        val root = tempFolder.newFolder("delete_after_test")
        val zipFile = File(root, "mods.zip")
        val targetDir = File(root, "mods")

        createZipFile(zipFile, mapOf("fabric-api.jar" to "fabric api contents"))

        val result = ServerRepository.extractZip(zipFile, targetDir, deleteZipAfter = true)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertFalse("Archive should be deleted after successful extraction", zipFile.exists())
        assertTrue(File(targetDir, "fabric-api.jar").exists())
    }

    @Test
    fun testExtractZipSlipSecurityException() {
        val root = tempFolder.newFolder("security_test")
        val zipFile = File(root, "malicious.zip")
        val targetDir = File(root, "target")

        val maliciousEntries = mapOf(
            "../../escaped.txt" to "dangerous content"
        )
        createZipFile(zipFile, maliciousEntries)

        val result = ServerRepository.extractZip(zipFile, targetDir, deleteZipAfter = false)

        assertTrue("ZipSlip should fail extraction", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertFalse(File(root, "escaped.txt").exists())
    }

    @Test
    fun testMacOSMetadataFiltered() {
        val root = tempFolder.newFolder("macos_test")
        val zipFile = File(root, "macos_archive.zip")
        val targetDir = File(root, "extracted")

        val entries = mapOf(
            "__MACOSX/._plugin.jar" to "resource fork",
            ".DS_Store" to "desktop services",
            "plugin.jar" to "actual plugin jar"
        )
        createZipFile(zipFile, entries)

        val result = ServerRepository.extractZip(zipFile, targetDir, deleteZipAfter = false)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertTrue(File(targetDir, "plugin.jar").exists())
        assertFalse(File(targetDir, "__MACOSX").exists())
        assertFalse(File(targetDir, ".DS_Store").exists())
    }

    @Test
    fun testNonExistentZipReturnsFailure() {
        val root = tempFolder.newFolder("missing_test")
        val missingZip = File(root, "does_not_exist.zip")
        val targetDir = File(root, "target")

        val result = ServerRepository.extractZip(missingZip, targetDir)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileNotFoundException)
    }
}
