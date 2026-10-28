package com.explodearena.remaster.core

import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import org.junit.Assert.*
import org.junit.Test

/**
 * N30: compare the production blast ray against an independently denser
 * one-pixel segment sampler at every valid 16px bomb cell in all 41 real maps.
 * The geometry remains provisional JS-port behavior, not Symbian/EKA2L1 proof.
 */
class NativeArenaBlastGeometryIntegrationTest {
    private fun originalStages(): List<StageData> {
        val file = File("../../web/assets/stages.js")
        assertTrue("Missing real stage catalog: ${file.absolutePath}", file.isFile)
        return StageCatalog.parse(file.readText(Charsets.UTF_8))
    }

    // Deliberately independent of StageCollision.blocked: read the raw stage grid
    // and enumerate every tile overlapped by the reference radius. If production
    // collision indexing regresses, the ray oracle must not repeat that bug.
    private fun referenceBlocked(stage: StageData, x: Double, y: Double, radius: Double): Boolean {
        if (!x.isFinite() || !y.isFinite() || !radius.isFinite() || radius < 0.0) return true
        if (x - radius < 0.0 || y - radius < 0.0 ||
            x + radius >= stage.width || y + radius >= stage.height) return true

        val left = floor((x - radius - stage.obstacleX) / 8.0).toInt()
        val right = floor((x + radius - stage.obstacleX) / 8.0).toInt()
        val top = floor((y - radius - stage.obstacleY) / 8.0).toInt()
        val bottom = floor((y + radius - stage.obstacleY) / 8.0).toInt()
        if (left < 0 || top < 0 || right >= stage.gridWidth || bottom >= stage.gridHeight) return true
        for (row in top..bottom) for (column in left..right) {
            if (stage.grid[row][column] != 0) return true
        }
        return false
    }

    private fun referenceFootprint(stage: StageData, bombX: Double, bombY: Double): Set<Spawn> {
        // Local constants intentionally avoid production ray geometry constants.
        val bombCell = 16.0
        val halfCell = 8.0
        val cx = floor(bombX / bombCell).toInt()
        val cy = floor(bombY / bombCell).toInt()
        val center = Spawn(cx * bombCell + halfCell, cy * bombCell + halfCell)
        val cells = linkedSetOf(center)
        val directions = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        for ((dx, dy) in directions) {
            for (distance in 1..3) {
                val targetX = (cx + dx * distance) * bombCell + halfCell
                val targetY = (cy + dy * distance) * bombCell + halfCell
                if (targetX < 0 || targetY < 0 ||
                    targetX >= stage.width || targetY >= stage.height) break

                val fromX = (cx + dx * (distance - 1)) * bombCell + halfCell
                val fromY = (cy + dy * (distance - 1)) * bombCell + halfCell
                // Independent reference samples every pixel instead of using
                // the production implementation's four substeps per ray cell.
                val steps = ceil(kotlin.math.hypot(targetX - fromX, targetY - fromY)).toInt()
                val blocked = (0..steps).any { step ->
                    val fraction = step.toDouble() / steps
                    referenceBlocked(
                        stage,
                        fromX + (targetX - fromX) * fraction,
                        fromY + (targetY - fromY) * fraction,
                        3.0,
                    )
                }
                if (blocked) break
                cells.add(Spawn(targetX, targetY))
            }
        }
        return cells
    }

    @Test fun everyValidBombCellOnAll41ArenasMatchesDenseIndependentRayReference() {
        val stages = originalStages()
        assertEquals((0..40).toList(), stages.map { it.id })
        assertEquals(mapOf(1 to 18, 2 to 2, 4 to 21),
            stages.groupingBy { it.spawns.size }.eachCount())

        var checkedCells = 0
        for (stage in stages) {
            for (y in 8 until stage.height step 16) {
                for (x in 8 until stage.width step 16) {
                    if (referenceBlocked(stage, x.toDouble(), y.toDouble(), 3.0)) continue

                    val simulation = NativeBombSimulation(stage)
                    assertTrue("arena ${stage.id}: remote test power",
                        simulation.setRemoteDetonator("player", true))
                    assertTrue("arena ${stage.id} bomb cell ($x,$y) should accept placement",
                        simulation.place(Actor(x.toDouble(), y.toDouble()), "player"))
                    assertTrue("arena ${stage.id} bomb cell ($x,$y) should detonate",
                        simulation.detonateRemote("player"))

                    val flame = simulation.flames.single()
                    val actual = flame.cells.toSet()
                    assertEquals("arena ${stage.id} bomb cell ($x,$y): duplicate flame cells",
                        flame.cells.size, actual.size)
                    assertEquals(
                        "arena ${stage.id} bomb cell ($x,$y): blast ray differs from dense reference",
                        referenceFootprint(stage, x.toDouble(), y.toDouble()),
                        actual,
                    )
                    checkedCells++
                }
            }
        }
        assertTrue("Expected a broad real-map sweep, checked only $checkedCells cells", checkedCells > 1000)
    }
}
