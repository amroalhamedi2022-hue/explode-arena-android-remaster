package com.explodearena.remaster.core

import org.junit.Assert.*
import org.junit.Test

class StageCatalogTest {
    private fun catalog(count: Int): String {
        val entries = (0 until count).joinToString(",") { id ->
            """{"id":$id,"image":"assets/maps/map_000.png","w":16,"h":16,"obstacle":{"x":0,"y":0,"w":2,"h":2,"grid":[[0,0],[0,751]]},"spawns":[{"x":4,"y":4}]}"""
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
}
