package com.colgame

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
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
        val continentNoise = HashMap<Int, Double>()

        for (tile in tiles) {
            val waterNoise = getPerlinNoise(tile, noiseSeed)

            // force oceans at edges:
            when {
                // have to have sealanes all along the eastern edge
                tile.x == (width - 1) -> {
                    tile.type = TerrainType.SeaLane
                    tile.continentID = 0
                }
                // only the eastern edge can have protruding sealanes
                tile.longitude >= (edgePrcnt * (1 - waterNoise)) -> {
                    tile.type = if (rng.nextDouble() < 0.5) TerrainType.Ocean else TerrainType.SeaLane
                    tile.continentID = 0
                }
                // ensure both the atlantic and the pacific are present
                abs(tile.longitude) >= (edgePrcnt * min(1.0, (1 - waterNoise))) -> {
                    tile.type = TerrainType.Ocean
                    tile.continentID = 0
                }
            }

            // only spawn biomes on land tiles
            if (tile.type.isLand()) {
                if (!continentNoise.containsKey(tile.continentID))
                    continentNoise[tile.continentID] = rng.nextDouble()

                var elevation = getPerlinNoise(tile, continentNoise[tile.continentID]!!, scale = 3.0)
                elevation = abs(elevation).pow(1.0 - 0.8f) * elevation.sign

                if (elevation > 0.9) {
                    tile.type = TerrainType.Mountains
                } else if (elevation > 0.8) {
                    tile.type = if (rng.nextDouble() < 0.6) TerrainType.Mountains else TerrainType.Hills
                } else if (elevation > 0.7) {
                    tile.type = if (rng.nextDouble() < 0.3) TerrainType.Mountains else TerrainType.Hills
                } else {
                    val possibleTiles = TerrainType.values()
                            .filter { tile.humidity in it.minHumidity..it.maxHumidity }
                            .filter { tile.temperature in it.minTemp..it.maxTemp }
                            .filter { it != TerrainType.Lakes }
                            .filter { if (elevation < MapParameters.minMountainsElevation) it != TerrainType.Mountains else true }
                            .filter { if (elevation < MapParameters.minHillsElevation) it != TerrainType.Hills else true }
                    val forested = possibleTiles.filter { it.forested }
                    val unforested = possibleTiles.filter { it.isLand() && !it.forested }
                    val noLake = unforested.filter { it != TerrainType.Lakes }

                    tile.type = if (rng.nextDouble() < MapParameters.forestCover && forested.isNotEmpty())
                        forested.random()
                    else if (unforested.isNotEmpty() /*&& tile.neighbors.count { tiles[it].type.isLand() } == tile.neighbors.size*/)
                        unforested.random()
//                    else if (noLake.isNotEmpty())
//                        noLake.random()
                    else
                        TerrainType.SeaLane//tile.type
                }
            }
        }
    }

    private data class Seed(val tile: Tile, var energy: Double)

    private fun growContinents() {
        // seed the continents
        val maxEnergy = MapParameters.size.landmasses * MapParameters.landMass.multiplier
        val edgeLeft = (MapParameters.minEdgeDistance * width).toInt()
        val validLandXRange = edgeLeft until (width - edgeLeft)
        val validLandYRange = 0 until height
        val seedList = (0 until MapParameters.size.landmasses * MapParameters.landMass.multiplier)
                .map {
                    val continent = coordsToTileIndex(validLandXRange.random(), validLandYRange.random())
                    val tile = tiles[continent]
                    tile.continentID = continent
                    Seed(tile, maxEnergy - maxEnergy * rng.nextDouble())
                } as MutableList<Seed>

        val stepCost = 1 - MapParameters.landMass.decay
        while (seedList.isNotEmpty()) {
            val seedTile = seedList.removeAt(0)

            // lower probability of growing too far east or west
            var next = seedTile.tile.neighbors.random()
            if (tiles[next].x !in validLandXRange)
                next = seedTile.tile.neighbors.random()

            if (tiles[next].type == TerrainType.Plains) continue
            tiles[next].type = TerrainType.Plains
            tiles[next].continentID = seedTile.tile.continentID
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

