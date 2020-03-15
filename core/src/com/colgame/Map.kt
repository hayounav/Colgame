package com.colgame

import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

class Map {

    private val width = MapParameters.size.width
    private val height = MapParameters.size.height
    private val centerIndex = width * height / 2 + width / 2

    val tiles: Array<Tile> = Array(width * height) { i -> Tile(this, i % width, i / width) }
    val centerTile: Tile = tiles[centerIndex]

    private val rng = Random(System.currentTimeMillis())

    fun containsCoordinates(x: Int, y: Int): Boolean = (x in 0 until width) && (y in 0 until height)

    fun coordsToTileIndex(x: Int, y: Int): Int = x % width + y * width


    fun generate() {
        growContinents()

        val noiseSeed = rng.nextDouble()
        val edgePrcnt = (1 - MapParameters.minEdgeDistance)

        for (tile in tiles) {
            val possibleTiles = TerrainType.values()
                    .filter { tile.humidity in it.minHumidity..it.maxHumidity }
                    .filter { tile.temperature in it.minTemp..it.maxTemp }
            val forested = possibleTiles.filter { it.forested }
            val unforested = possibleTiles.filter { it.isLand() && !it.forested }
            val water = possibleTiles.filter { it.isWater() }

            val waterNoise = getPerlinNoise(tile, noiseSeed)

            tile.longitude
            tile.type = when {
                // have to have sealanes all along the eastern edge
                tile.x == (width - 1) -> TerrainType.SeaLane
                // only the eastern edge can have protruding sealanes
                tile.longitude >= (edgePrcnt * (1 - waterNoise)) -> water.random()
                // ensure both the atlantic and the pacific are present
                abs(tile.longitude) >= (edgePrcnt * min(1.0, (1 - waterNoise))) -> TerrainType.Ocean
                else -> tile.type
            }

            // only spawn biomes on land tiles
            if (tile.type.isLand()) {
                tile.type = if (rng.nextDouble() < MapParameters.forestCover && forested.isNotEmpty())
                    forested.random()
                else if (tile.neighbors.count { tiles[it].type.isLand() } == tile.neighbors.size)
                    unforested.random()
                else
                    unforested.filter { it != TerrainType.Lakes }.random()
            }
        }
    }

    private data class Seed(val tile: Tile, var energy: Double)

    fun growContinents() {
        // seed the continents
        val maxEnergy = MapParameters.size.landmasses * MapParameters.landMass.multiplier
        var seedList = (0 until MapParameters.size.landmasses * MapParameters.landMass.multiplier)
                .map { Seed(tiles[tiles.indices.random()], maxEnergy - maxEnergy * rng.nextDouble()) } as MutableList<Seed>

        val stepCost = 1 - MapParameters.landMass.decay
        while (seedList.isNotEmpty()) {
            val seedTile = seedList.removeAt(0)
            val next = seedTile.tile.neighbors.random()
            if (tiles[next].type == TerrainType.Plains) continue
            tiles[next].type = TerrainType.Plains
            if (seedTile.energy > 1)
                seedList.add(Seed(tiles[next], seedTile.energy * stepCost))
            seedTile.energy *= stepCost
            if (seedTile.energy > 1)
                seedList.add(seedTile)
        }
    }

    /**
     * Generates a perlin noise channel combining multiple octaves
     *
     * [nOctaves] is the number of octaves
     * [persistence] is the scaling factor of octave amplitudes
     * [lacunarity] is the scaling factor of octave frequencies
     * [scale] is the distance the noise is observed from
     */
    private fun getPerlinNoise(tile: Tile, seed: Double,
                               nOctaves: Int = 6,
                               persistence: Double = 0.5,
                               lacunarity: Double = 2.0,
                               scale: Double = 10.0): Double {
        return Perlin.noise3d(tile.x.toDouble(), tile.y.toDouble(), seed, nOctaves, persistence, lacunarity, scale)
    }
}

