package com.explodearena.remaster.core

import com.badlogic.gdx.utils.JsonReader

/**
 * Reads the existing 41 original stage records as DATA, not executable JS.
 * No HTML/WebView/JavaScript interpreter is used by the native engine.
 *
 * Bound and validate catalog input before allocating per-stage collision grids.
 * These limits are defensive native-loader invariants, NOT original Symbian
 * map-size or spawn rules.
 */
object StageCatalog {
    private const val PREFIX = "window.EXPLODE_STAGES="
    private const val STAGE_COUNT = 41
    private const val MAX_CATALOG_CHARS = 1_000_000
    private const val MAX_STAGE_SIDE = 4096
    private const val MAX_GRID_CELLS = 20_000L
    private val mapPath = Regex("assets/maps/map_[0-9]{3}\\.png")

    fun parse(source: String): List<StageData> {
        require(source.length <= MAX_CATALOG_CHARS) { "Stage catalog exceeds bounded native input size" }
        val trimmed = source.trim()
        require(trimmed.startsWith(PREFIX)) { "Unexpected stage data header" }
        val json = trimmed.removePrefix(PREFIX).trim().removeSuffix(";")
        val root = JsonReader().parse(json)
        require(root.isArray && root.size == STAGE_COUNT) {
            "Expected exactly $STAGE_COUNT original stage records"
        }
        val stages = mutableListOf<StageData>()
        for (entry in root) {
            val id = entry.getInt("id")
            val width = entry.getInt("w")
            val height = entry.getInt("h")
            val image = entry.getString("image")
            require(id == stages.size && id in 0 until STAGE_COUNT) {
                "Stage IDs must be sequential from zero"
            }
            require(width in 1..MAX_STAGE_SIDE && height in 1..MAX_STAGE_SIDE) {
                "Stage $id has unsupported dimensions"
            }
            require(mapPath.matches(image)) { "Stage $id has invalid map asset path" }
            val expectedImage = "assets/maps/map_${id.toString().padStart(3, '0')}.png"
            require(image == expectedImage) {
                "Stage $id must reference its matching map asset"
            }

            val obstacle = entry.get("obstacle")
            val obstacleX = obstacle.getInt("x")
            val obstacleY = obstacle.getInt("y")
            val gridWidth = obstacle.getInt("w")
            val gridHeight = obstacle.getInt("h")
            require(gridWidth > 0 && gridHeight > 0 &&
                gridWidth.toLong() * gridHeight <= MAX_GRID_CELLS &&
                gridWidth.toLong() * StageCollision.CELL == width.toLong() &&
                gridHeight.toLong() * StageCollision.CELL == height.toLong() &&
                obstacleX >= 0 && obstacleY >= 0 &&
                obstacleX.toLong() + gridWidth.toLong() * StageCollision.CELL <= width.toLong() &&
                obstacleY.toLong() + gridHeight.toLong() * StageCollision.CELL <= height.toLong()) {
                "Stage $id has invalid native collision geometry"
            }
            val sourceGrid = obstacle.get("grid")
            require(sourceGrid.isArray && sourceGrid.size == gridHeight) {
                "Stage $id grid row count does not match source"
            }
            val grid = mutableListOf<IntArray>()
            for (row in sourceGrid) {
                require(row.isArray && row.size == gridWidth) {
                    "Stage $id grid column count does not match source"
                }
                val values = IntArray(gridWidth)
                var index = 0
                for (cell in row) {
                    require(cell.isNumber) { "Stage $id has a non-numeric collision cell" }
                    val value = cell.asDouble()
                    require(value.isFinite() && value >= 0.0 &&
                        value <= Int.MAX_VALUE.toDouble() && value == kotlin.math.floor(value)) {
                        "Stage $id has an invalid collision cell value"
                    }
                    values[index++] = value.toInt()
                }
                grid.add(values)
            }

            val sourceSpawns = entry.get("spawns")
            require(sourceSpawns.isArray && sourceSpawns.size in setOf(1, 2, 4)) {
                "Stage $id has unsupported native opponent spawn count"
            }
            val spawns = mutableListOf<Spawn>()
            for (point in sourceSpawns) {
                val x = point.getDouble("x")
                val y = point.getDouble("y")
                require(x.isFinite() && y.isFinite() &&
                    x >= 0.0 && x < width.toDouble() &&
                    y >= 0.0 && y < height.toDouble()) {
                    "Stage $id has invalid or out-of-bounds spawn coordinates"
                }
                spawns.add(Spawn(x, y))
            }
            stages.add(StageData(
                id = id, image = image, width = width, height = height,
                obstacleX = obstacleX, obstacleY = obstacleY,
                grid = grid, spawns = spawns,
            ))
        }
        return stages
    }
}
