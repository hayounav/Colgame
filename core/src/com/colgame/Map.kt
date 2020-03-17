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
        growOceans()
        spawnPoles()
        findLakes()
        distributeBiomes()
        addRivers()
        scatterBonusResources()
    }

    private data class Seed(val tile: Tile, var energy: Double)

    private fun growContinents() {
        // seed the continents
        val maxEnergy = MapParameters.size.landmasses * MapParameters.landMass.multiplier
        val edgeLeft = (MapParameters.minEdge * width).toInt()
        val validLandXRange = edgeLeft until (width - edgeLeft)
        val validLandYRange = 0 until height
        val seedList = (0 until MapParameters.size.landmasses * MapParameters.landMass.multiplier)
                .map {
                    val continent = coordsToTileIndex(validLandXRange.random(), validLandYRange.random())
                    val tile = tiles[continent]
                    tile.continentID = continent
                    Seed(tile, maxEnergy - maxEnergy * rng.nextDouble())
                } as MutableList<Seed>

        val maxLandTiles = (width * (1 - MapParameters.minEdge)) * height * MapParameters.landMass.maxLand
        val stepCost = 1 - MapParameters.landMass.decay
        var numLand = seedList.size
        while (numLand < maxLandTiles && seedList.isNotEmpty()) {
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

            numLand += 1
        }
    }

    private fun growOceans() {
        val noiseSeed = rng.nextDouble()
        val edgePrcnt = (1 - MapParameters.minEdge)

        for (tile in tiles) {
            val waterNoise = getPerlinNoise(tile, noiseSeed)

            // force oceans at edges:
            when {
                // have to have sealanes all along the eastern edge
                tile.x == (width - 1) -> {
                    tile.type = TerrainType.SeaLane
                    tile.continentID = 0
                    tile.ocean = true
                }
                // only the eastern edge can have protruding sealanes
                tile.longitude >= (edgePrcnt * (1 - waterNoise)) -> {
                    tile.type = if (rng.nextDouble() < 0.5) TerrainType.Ocean else TerrainType.SeaLane
                    tile.continentID = 0
                    tile.ocean = true
                }
                // ensure both the atlantic and the pacific are present
                abs(tile.longitude) >= (edgePrcnt * min(1.0, (1 - waterNoise))) -> {
                    tile.type = TerrainType.Ocean
                    tile.continentID = 0
                    tile.ocean = true
                }
            }
        }
    }

    private fun spawnPoles() {
        val minOceanWidth = (MapParameters.minEdge * width).toInt()

        // North Pole
        val northEdge = tiles.sliceArray(0 until width)
        var northPoleWestCoast = northEdge.indices.first { tiles[it].type.isLand() }
        var northPoleEastCoast = northEdge.indices.last { tiles[it].type.isLand() }

        if (northPoleWestCoast <= minOceanWidth)
            northPoleWestCoast = minOceanWidth + 1

        if (northPoleEastCoast >= width - minOceanWidth)
            northPoleEastCoast = width - minOceanWidth - 1

        for (i in northPoleWestCoast until northPoleEastCoast) tiles[i].type = TerrainType.Plains


        // South Pole
        val southEdge = tiles.sliceArray(width * (height - 1) until width * height)
        var southPoleWestCoast = southEdge.indices.first { tiles[it].type.isLand() }
        var southPoleEastCoast = southEdge.indices.last { tiles[it].type.isLand() }

        if (southPoleWestCoast <= minOceanWidth)
            southPoleWestCoast = minOceanWidth + 1

        if (southPoleEastCoast > width - minOceanWidth)
            southPoleEastCoast = width - minOceanWidth - 1

        for (i in southPoleWestCoast until southPoleEastCoast) tiles[i].type = TerrainType.Plains

    }

    private fun findLakes() {
        var oceans = tiles.indices.filter { tiles[it].ocean } as MutableList
        val unsortedWater = HashSet(tiles.indices.filter { tiles[it].type.isWater() && it !in oceans })

        var changed = true
        while (changed) {
            changed = false
            val newOceans = mutableListOf<Int>()
            for (i in oceans) {
                for (n in tiles[i].neighbors) {
                    if (n in unsortedWater) {
                        changed = true
                        tiles[n].ocean = true
                        newOceans.add(n)
                        unsortedWater.remove(n)
                    } else if (tiles[n].type.isLand()) {
                        tiles[n].coast = true
                    }
                }
            }
            oceans = newOceans
        }

        // shrink lakes a bit
        for (n in unsortedWater) {
            val tile = tiles[n]
            tile.ocean = false
            tile.type = if (rng.nextDouble() > tile.humidity) TerrainType.Plains else TerrainType.Lakes
        }
    }

    private fun distributeBiomes() {
        val continentNoise = HashMap<Int, Double>()

        // only spawn biomes on land tiles
        for (tile in tiles.filter { it.type.isLand() }) {

            if (!continentNoise.containsKey(tile.continentID))
                continentNoise[tile.continentID] = rng.nextDouble()

            var elevation = getPerlinNoise(tile, continentNoise[tile.continentID]!!, scale = 3.0)
            elevation = abs(elevation).pow(1.0 - 0.8f) * elevation.sign

            when {
                elevation > 0.9 -> {
                    tile.type = TerrainType.Mountains
                }
                elevation > 0.8 -> {
                    tile.type = if (rng.nextDouble() < 0.6) TerrainType.Mountains else TerrainType.Hills
                }
                elevation > 0.7 -> {
                    tile.type = if (rng.nextDouble() < 0.3) TerrainType.Mountains else TerrainType.Hills
                }
                else -> {
                    val possibleTiles = TerrainType.values()
                            .filter { tile.humidity in it.minHumidity..it.maxHumidity }
                            .filter { tile.temperature in it.minTemp..it.maxTemp }
                            .filter { it != TerrainType.Lakes }
                            .filter { if (elevation < MapParameters.minMountainsElevation) it != TerrainType.Mountains else true }
                            .filter { if (elevation < MapParameters.minHillsElevation) it != TerrainType.Hills else true }
                    val forested = possibleTiles.filter { it.forested }
                    val unforested = possibleTiles.filter { it.isLand() && !it.forested }

                    tile.type = if (rng.nextDouble() < MapParameters.forestCover && forested.isNotEmpty())
                        forested.random()
                    else if (unforested.isNotEmpty())
                        unforested.random()
                    else
                        throw Exception("Terrain matching constraints unavailable: Tile -- ${tile}, elevation -- $elevation")
                }
            }
        }
    }

    private fun addRivers() {
        val numRiverSpawns = MapParameters.baseRiverAmount * tiles.count { it.type.isLand() }
        val numMountains = tiles.filter {
            it.type == TerrainType.Mountains &&
                    it.neighbors.none { tiles[it].type == TerrainType.Ocean }
        }
        val numHills = tiles.filter {
            it.type == TerrainType.Hills &&
                    it.neighbors.none { tiles[it].type == TerrainType.Ocean }
        }
        val numLakes = tiles.filter { it.type == TerrainType.Lakes }
        println("$numRiverSpawns ${numMountains.size} ${numHills.size} ${numLakes.size}")

        // river source chances affected by temperature and humidity
        // make rivers from some non-ocean-adjoining mountains to nearest lake (snow melt)
        // make rivers from some non-ocean-adjoining mountains to nearest ocean (snow melt)
        // make rivers from some non-ocean-adjoining hills to nearest lake (springs)
        // make rivers from some non-ocean-adjoining hills to nearest ocean (springs)
        // make rivers from some non-ocean-adjoining tile to nearest lake (rain)
        // make rivers from some non-ocean-adjoining tile to nearest ocean (rain)
    }

    private fun scatterBonusResources() {
//        TODO("Not yet implemented")
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

