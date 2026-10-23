package com.explodearena.remaster.core

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ensures the native Android asset mirror has not silently diverged from the
 * existing Mainline reference. This does NOT establish Symbian asset fidelity.
 */
class NativeAssetMirrorTest {
    private fun digest(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    @Test fun originalMainlineAssetsMatchEveryAndroidPackagedAssetByteForByte() {
        // :core:test executes from android/core in the free GitHub Actions job.
        val original = File("../../web/assets").canonicalFile
        val packaged = File("../app/src/main/assets/assets").canonicalFile
        assertTrue("Missing Mainline source assets", original.isDirectory)
        assertTrue("Missing native packaged assets", packaged.isDirectory)
        fun inventory(root: File): Map<String, File> = root.walkTopDown()
            .filter { it.isFile }
            .associateBy { it.relativeTo(root).invariantSeparatorsPath }
        val sourceFiles = inventory(original)
        val targetFiles = inventory(packaged)
        assertEquals("Android asset inventory must exactly mirror Mainline", sourceFiles.keys, targetFiles.keys)
        assertEquals(41, sourceFiles.keys.count { it.matches(Regex("maps/map_[0-9]{3}\\.png")) })
        assertEquals(15, sourceFiles.keys.count { it.startsWith("sfx/") && it.endsWith(".wav") })
        assertTrue("Stage JSON must be packaged", "stages.js" in sourceFiles)
        for ((relative, file) in sourceFiles) {
            assertEquals("SHA-256 mismatch for $relative", digest(file), digest(targetFiles.getValue(relative)))
        }
    }
}
