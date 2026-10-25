package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class StageCatalogTest {
    private fun catalog(count: Int): String {
        val entries = (0 until count).joinToString(",") { id ->
            """{"id":$id,"image":"assets/maps/map_${id.toString().padStart(3, '0')}.png","w":16,"h":16,"obstacle":{"x":0,"y":0,"w":2,"h":2,"grid":[[0,0],[0,751]]},"spawns":[{"x":4,"y":4}]}"""
        }
        return "window.EXPLODE_STAGES=[$entries];"
    }

    @Test fun readsAllOriginalStageRecordsAsData() {
        val stages = StageCatalog.parse(catalog(41))
        assertEquals(41, stages.size)
        assertEquals(751, stages.last().grid[1][1])
        assertEquals(4.0, stages.first().spawns.first().x, 0.0)
    }

    @Test fun rejectsTruncatedOrMalformedStageInventory() {
        assertThrows(IllegalArgumentException::class.java) { StageCatalog.parse(catalog(40)) }
        assertThrows(IllegalArgumentException::class.java) { StageCatalog.parse("alert('not data')") }
        val bad = catalog(41).replace("\"w\":2", "\"w\":3")
        assertThrows(IllegalArgumentException::class.java) { StageCatalog.parse(bad) }
    }

    @Test fun originalMainlineStageFileHas41ValidGeometryRecords() {
        // Gradle :core:test runs from android/core; never substitute mock arenas
        // for the original stage inventory when checking the native port.
        val file = java.io.File("../../web/assets/stages.js")
        assertTrue("Original stage catalog missing: ${file.absolutePath}", file.isFile)
        val stages = StageCatalog.parse(file.readText(Charsets.UTF_8))
        assertEquals(41, stages.size)
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount())
        assertTrue(stages.all { it.gridWidth * 8 == it.width && it.gridHeight * 8 == it.height })
        assertTrue(stages.all { it.spawns.isNotEmpty() })
        assertTrue(stages.all { it.image == "assets/maps/map_${it.id.toString().padStart(3, '0')}.png" })
    }

    @Test fun rejectsOversizedCatalogBeforeJsonAllocation() {
        val huge = catalog(41) + " ".repeat(1_000_000)
        assertThrows(IllegalArgumentException::class.java) { StageCatalog.parse(huge) }
    }

    @Test fun rejectsInvalidMapPathAndOutOfOrderStageIds() {
        val original = catalog(41)
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace("assets/maps/map_000.png", "../outside.png"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace("\"id\":0", "\"id\":40"))
        }
    }

    @Test fun rejectsMismatchedMapIdentityShiftedGridAndOutOfBoundsSpawn() {
        val original = catalog(41)
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace(
                "\"id\":1,\"image\":\"assets/maps/map_001.png\"",
                "\"id\":1,\"image\":\"assets/maps/map_000.png\"",
            ))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace(
                "\"obstacle\":{\"x\":0,\"y\":0",
                "\"obstacle\":{\"x\":1,\"y\":0",
            ))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace("\"x\":4,\"y\":4", "\"x\":16,\"y\":4"))
        }
    }

    @Test fun rejectsNonIntegerNegativeAndNonNumericCollisionCells() {
        val original = catalog(41)
        for (badGrid in listOf(
            "[[0,-1],[0,751]]",
            "[[0,0],[0,751.5]]",
            """[[0,0],[0,"bad"]]""",
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                StageCatalog.parse(original.replace("[[0,0],[0,751]]", badGrid))
            }
        }
    }

    @Test fun rejectsOversizedDimensionsAndGridBeforeAllocatingRows() {
        val original = catalog(41)
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace("\"w\":16", "\"w\":5000"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace("\"w\":2", "\"w\":1000000"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace("[[0,0],[0,751]]", "[[0,0],[0]]"))
        }
    }

    @Test fun rejectsUnsupportedOrNonfiniteNativeSpawns() {
        val original = catalog(41)
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace("\"spawns\":[{\"x\":4,\"y\":4}]", "\"spawns\":[]"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            StageCatalog.parse(original.replace("\"x\":4", "\"x\":\"NaN\""))
        }
    }
}
