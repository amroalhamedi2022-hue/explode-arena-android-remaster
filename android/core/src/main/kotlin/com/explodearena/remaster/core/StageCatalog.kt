package com.explodearena.remaster.core

import com.badlogic.gdx.utils.JsonReader

/**
 * Reads the existing 41 original stage records as DATA, not executable JS.
 * No HTML/WebView/JavaScript interpreter is used by the native engine.
 */
object StageCatalog {
    private const val PREFIX = "window.EXPLODE_STAGES="

    fun parse(source: String): List<StageData> {
        val trimmed = source.trim()
        require(trimmed.startsWith(PREFIX)) { "Unexpected stage data header" }
        val json = trimmed.removePrefix(PREFIX).trim().removeSuffix(";")
        val root = JsonReader().parse(json)
        require(root.isArray) { "Stage catalog must be an array" }
        val stages = mutableListOf<StageData>()
        for (entry in root) {
            val obstacle = entry.get("obstacle")
            val grid = mutableListOf<IntArray>()
            for (row in obstacle.get("grid")) {
                val values = IntArray(row.size)
                var index = 0
                for (cell in row) values[index++] = cell.asInt()
                grid.add(values)
            }
            val spawns = mutableListOf<Spawn>()
            for (point in entry.get("spawns")) {
                spawns.add(Spawn(point.getDouble("x"), point.getDouble("y")))
            }
            val stage = StageData(
                id = entry.getInt("id"),
                image = entry.getString("image"),
                width = entry.getInt("w"),
                height = entry.getInt("h"),
                obstacleX = obstacle.getInt("x"),
                obstacleY = obstacle.getInt("y"),
                grid = grid,
                spawns = spawns,
            )
            require(stage.gridWidth == obstacle.getInt("w") && stage.gridHeight == obstacle.getInt("h")) {
                "Stage ${stage.id} grid dimensions do not match source"
            }
            stages.add(stage)
        }
        require(stages.size == 41 && stages.map { it.id } == (0..40).toList()) {
            "Expected exactly 41 original stage IDs in order"
        }
        return stages
    }
}
