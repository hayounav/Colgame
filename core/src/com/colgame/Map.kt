package com.colgame

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
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

        for (t in tiles) {
            t.continentID = seedList.minBy { it.tile.getDistance(t) }!!.tile.continentID
        }

        val maxLandTiles = (width * (1 - MapParameters.minEdge)) * height * MapParameters.landMass.maxLand
        val stepCost = 1 - MapParameters.landMass.decay
        var numLand = seedList.size
        while (numLand < maxLandTiles && seedList.isNotEmpty()) {
            val seedTile = seedList.removeAt(0)

            var next: Int? = null
            for (n in seedTile.tile.neighbors.asList().shuffled()) {
                if (tiles[n].type != TerrainType.Plains &&
                        tiles[n].continentID == seedTile.tile.continentID) {
                    next = n
                    break
                }
            }

            if (next == null)
                continue

            tiles[next].type = TerrainType.Plains
            if (seedTile.energy > 1)
                seedList.add(Seed(tiles[next], seedTile.energy * stepCost))
            seedTile.energy *= stepCost
            if (seedTile.energy > 1)
                seedList.add(seedTile)

            numLand += 1
        }

        // single-tile island have a 50-50 chance to disappear or grow
        for (t in tiles.filter { it.type.isLand() && it.neighbors.none { n -> tiles[n].type.isLand() } }) {
            val p = rng.nextDouble()
            if (rng.nextDouble() > 0.5) {
                t.neighbors.toList().shuffled().take((p * 8).toInt())
                        .forEach { tiles[it].type = TerrainType.Plains }
            } else {
                t.type = TerrainType.Ocean
            }
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
                    tile.ocean = true
                }
                // only the eastern edge can have protruding sealanes
                tile.longitude >= (edgePrcnt * (1 - waterNoise)) -> {
                    tile.type = if (rng.nextDouble() < 0.5 && tile.neighbors.none { tiles[it].type.isLand() })
                        TerrainType.SeaLane else TerrainType.Ocean
                    tile.ocean = true
                }
                // ensure both the atlantic and the pacific are present
                abs(tile.longitude) >= (edgePrcnt * min(1.0, (1 - waterNoise))) -> {
                    tile.type = TerrainType.Ocean
                    tile.ocean = true
                }
            }
        }
    }

    private fun spawnPoles() {
        val minOceanWidth = (MapParameters.minEdge * width).toInt()

        // North Pole
        val northEdge = tiles.indices.take(width).filter { tiles[it].type.isLand() }
        var northPoleWestCoast = if (northEdge.isEmpty()) minOceanWidth + 1 else northEdge.first()
        var northPoleEastCoast = if (northEdge.isEmpty()) width - minOceanWidth - 1 else northEdge.last()

        if (northPoleWestCoast <= minOceanWidth)
            northPoleWestCoast = minOceanWidth + 1

        if (northPoleEastCoast >= width - minOceanWidth)
            northPoleEastCoast = width - minOceanWidth - 1

        for (i in northPoleWestCoast until northPoleEastCoast) {
            tiles[i].type = TerrainType.Plains
        }


        // South Pole
        val lastRow = width * (height - 1)
        val southEdge = tiles.indices.drop(lastRow).filter { tiles[it].type.isLand() }
        var southPoleWestCoast = if (southEdge.isEmpty()) minOceanWidth + 1 else southEdge.first()
        var southPoleEastCoast = if (southEdge.isEmpty()) width - minOceanWidth - 1 else southEdge.last()

        if (southPoleWestCoast <= minOceanWidth)
            southPoleWestCoast = lastRow + minOceanWidth + 1

        if (southPoleEastCoast > width - minOceanWidth)
            southPoleEastCoast = lastRow + width - minOceanWidth - 1

        for (i in southPoleWestCoast until southPoleEastCoast) {
            tiles[i].type = TerrainType.Plains
        }

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

            var elevation = getPerlinNoise(tile, continentNoise[tile.continentID]!!)*(1+rng.nextDouble())
            elevation = abs(elevation).pow(0.25)

            tile.elevation = elevation

            when {
                elevation > 0.95 -> {
                    tile.type = TerrainType.Mountains
                }
                elevation > 0.85 -> {
                    tile.type = if (rng.nextDouble() < 0.6) TerrainType.Mountains else TerrainType.Hills
                }
                elevation > 0.75 -> {
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

        // river source chances affected by temperature and humidity
        val eligibleTiles = tiles.filter {
            it.type.isLand() &&
                    it.humidity > 0.1 &&
                    it.neighbors.none { n -> tiles[n].type.isWater() }
        }
        val eligibleMountains = eligibleTiles.filter { it.type == TerrainType.Mountains }
        val eligibleHills = eligibleTiles.filter { it.type == TerrainType.Hills }

        val numMountainRivers = min((0.7 * numRiverSpawns).toInt(), eligibleMountains.size)
        val numHillRivers = min((0.15 * numRiverSpawns).toInt(), eligibleHills.size)
        var numFlatRivers = numRiverSpawns.toInt() - numMountainRivers - numHillRivers

        // make rivers from some non-ocean-adjoining mountains
        eligibleMountains.shuffled().take(numMountainRivers).forEach {
            val river = flowFromSource(it)

            if (river.isEmpty())
                numFlatRivers += 1
            else
                applyRiver(river)
        }

        // make rivers from some non-ocean-adjoining hills
        eligibleHills.shuffled().take(numHillRivers).forEach {
            val river = flowFromSource(it)

            if (river.isEmpty())
                numFlatRivers += 1
            else
                applyRiver(river)
        }

        // make rivers from some non-ocean-adjoining tile
        eligibleTiles.filter {
            it.type != TerrainType.Mountains &&
                    it.type != TerrainType.Hills &&
                    it.type != TerrainType.Arctic &&
                    it.neighbors.none { n -> tiles[n].type.isWater() } &&
                    it.elevation >= it.neighbors.map { n -> tiles[n].elevation }.min()!!
        }.shuffled().take(numFlatRivers).forEach {
            val river = flowFromSource(it)

            if (river.isNotEmpty())
                applyRiver(river)
        }
    }

    private fun applyRiver(river: List<Tile>) {
        for (it in river) {
            print("{")
            print(it.x)
            print(" ")
            print(it.y)
            print("} => ")
        }
        println("end")

        for (i in 1 until river.size-1) {
            val current = river[i]
            val fromDelta = Pair(river[i-1].x - current.x, river[i-1].y - current.y)
            val toDelta = Pair(river[i+1].x - current.x, river[i+1].y - current.y)
            current.river = River(fromDelta, toDelta)
        }
    }

    private fun flowFromSource(from: Tile): List<Tile> {
        var current = from

        val river = mutableListOf<Tile>()

        while (true) {
            river.add(current)

            // reached water, so river ends
            if (current.type.isWater()) break

            // merged with another river
            // TODO: make the other river bigger
            if (current.river != null) break

            val neighbors = current.neighbors.map {tiles[it]}.sortedBy { it.elevation }
            val downhill =  neighbors[0]
            if (downhill.elevation <= current.elevation ){
                current = downhill
            } else return mutableListOf<Tile>()
        }

        return river
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

