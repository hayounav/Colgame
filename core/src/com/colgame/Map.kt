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
        val elevationSeed = rng.nextDouble()

        val edgePrcnt = (1 - MapParameters.minEdgeDistance)

        for (tile in tiles) {
            val possibleTiles = TerrainType.values()
                    .filter { tile.humidity in it.minHumidity..it.maxHumidity }
                    .filter { tile.temperature in it.minTemp..it.maxTemp }
            val forested = possibleTiles.filter { it.forested }
            val unforested = possibleTiles.filter { !it.forested }
            val water = possibleTiles.filter { it.isWater() }

            val waterNoise =
                    if (MapParameters.landMass == MapParameters.Landmass.Archipelago)
                        getRidgedPerlinNoise(tile, elevationSeed)
                    else getPerlinNoise(tile, elevationSeed)

            tile.type =
                    // have to have sealanes all along the eastern edge
                    if (tile.x > width * (1 - MapParameters.minEdgeDistance / 2)) {
                        TerrainType.SeaLane
                    }
                    // ensure both the atlantic and the pacific are present
                    else if (abs(tile.longitude) > edgePrcnt * (1 - waterNoise)) {
                        water.random()
                    }
                    // consider landmass percentage
                    else if (waterNoise > MapParameters.landMass.mass) {
                        water.random()
                    } else if (rng.nextDouble() < MapParameters.forestCover && forested.isNotEmpty())
                        forested.random()
                    else if (tile.neighbors.filter { tiles[it].type.isLand() }.size == tile.neighbors.size)
                        unforested.random()
                    else
                        unforested.filter { it != TerrainType.Lakes }.random()
        }
    }

    fun containsCoordinates(x: Int, y: Int): Boolean = (x in 0 until width) && (y in 0 until height)

    fun coordsToTileIndex(x: Int, y: Int): Int = x % width + y * width

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

    /**
     * Generates ridged perlin noise. As for parameters see [getPerlinNoise]
     */
    private fun getRidgedPerlinNoise(tile: Tile, seed: Double,
                                     nOctaves: Int = 10,
                                     persistence: Double = 0.5,
                                     lacunarity: Double = 2.0,
                                     scale: Double = 15.0): Double {
        return Perlin.ridgedNoise3d(tile.x.toDouble(), tile.y.toDouble(), seed, nOctaves, persistence, lacunarity, scale)
    }
}

