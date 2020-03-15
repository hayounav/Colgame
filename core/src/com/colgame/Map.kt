package com.colgame

import kotlin.math.abs
import kotlin.random.Random

class Map {

    private val width = MapParameters.size.width
    private val height = MapParameters.size.height
    private val centerIndex = width * height / 2 + width / 2

    val tiles: Array<Tile> = Array(width * height) { i -> Tile(this, i % width, i / width) }
    val centerTile: Tile = tiles[centerIndex]

    private val rng = Random(System.currentTimeMillis())

    fun generate() {
        val maxWater = ((1 - MapParameters.landMass.mass) * width * height).toInt()
        var waterCount = 0
        val edgePrcnt = (centerTile.x - MapParameters.minEdgeDistance).toDouble() / centerTile.x

        for (tile in tiles) {
            val possibleTiles = TerrainType.values()
                    .filter { tile.humidity in it.minHumidity..it.maxHumidity }
                    .filter { tile.temperature in it.minTemp..it.maxTemp }
            val forested = possibleTiles.filter { it.forested }
            val unforested = possibleTiles.filter { !it.forested }
            val water = possibleTiles.filter { it.isWater() }

            tile.type =
                    // have to have sealanes all along the eastern edge
                    if (tile.x > width - MapParameters.minEdgeDistance / 2) {
                        waterCount++
                        TerrainType.SeaLane
                    }
                    // ensure both the atlantic and the pacific are present
                    else if (abs(tile.longitude) > edgePrcnt) {
                        waterCount++
                        water.random()
                    }
                    // prevent too much ocean/sea tiles
                    else if (waterCount < maxWater && rng.nextDouble() > MapParameters.landMass.mass) {
                        waterCount++
                        water.random()
                    } else if (rng.nextDouble() < MapParameters.forestCover && forested.isNotEmpty())
                        forested.random()
                    else
                        unforested.random()
        }
    }

    fun containsCoordinates(x: Int, y: Int): Boolean = (x in 0..width) && (y in 0..height)

    fun coordsToTileIndex(x: Int, y: Int): Int = x % width + y * width
}

