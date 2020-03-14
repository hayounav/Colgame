package com.colgame

import kotlin.math.*
import kotlin.random.Random

class Map(val width: Int, val height: Int, private val type: MapType? = null) {

    val tiles: Array<Tile> = Array(width * height) { i -> Tile(this, i % width, i / height) }
    val rng = Random(System.currentTimeMillis())

    val centerIndex = width * height / 2 + width / 2
    val centerTile: Tile = tiles[centerIndex]

    private val waterThreshold = 0.5
    private val temperatureExtremeness = 0.6
    private val tilesPerBiomeArea = 6
    private val elevationExponent = 0.8
    private val vegetationRichness = 0.4

    init {
        generateLand()
        raiseMountainsAndHills()
        applyHumidityAndTemperature()
        spawnLakesAndCoasts()
        spreadAncientRuins()
    }

    fun containsCoordinates(x: Int, y: Int): Boolean = (x in 0 until width) && (y in 0 until height)

    private fun generateLand() {
        when (type) {
            MapType.Continents -> createTwoContinents()
            MapType.Archipelago -> createArchipelago()
            else -> createPerlin()
        }
    }

    private fun createPerlin() {
        val elevationSeed = rng.nextInt().toDouble()
        for (tile in tiles)
            tile.baseTerrain = landOrWater(getPerlinNoise(tile, elevationSeed), waterThreshold)
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

    private fun landOrWater(elevation: Double, threshold: Double): TerrainType {
        return when {
            elevation >= threshold -> TerrainType.Grassland
            else -> TerrainType.Grassland
        }
    }

    private fun createTwoContinents() {
        val elevationSeed = rng.nextInt().toDouble()
        for (tile in tiles) {
            var elevation = getPerlinNoise(tile, elevationSeed)
            elevation = (elevation + getTwoContinentsTransform(tile)) / 2.0
            tile.baseTerrain = landOrWater(elevation, waterThreshold)
        }
    }

    private fun getTwoContinentsTransform(tile: Tile): Double {
        val randomScale = rng.nextDouble()
        val longitudeFactor = 2.0 * abs(tile.longitude) / width

        return min(0.2, -1.0 + (5.0 * longitudeFactor.pow(0.6) + randomScale) / 3.0)
    }

    private fun createArchipelago() {
        val elevationSeed = rng.nextInt().toDouble()
        for (tile in tiles) {
            val elevation = getRidgedPerlinNoise(tile, elevationSeed)
            tile.baseTerrain = landOrWater(elevation, 0.25 + waterThreshold)
        }
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

    private fun spawnLakesAndCoasts() {
        //define lakes
        val waterTiles = tiles.filter { it.isWater() }.toMutableList()

        val tilesInArea = ArrayList<Tile>()
        val tilesToCheck = ArrayList<Tile>()

        while (waterTiles.isNotEmpty()) {
            val initialWaterTile = waterTiles.random(rng)
            tilesInArea += initialWaterTile
            tilesToCheck += initialWaterTile
            waterTiles -= initialWaterTile

            // Floodfill to cluster water tiles
            while (tilesToCheck.isNotEmpty()) {
                val tileWeAreChecking = tilesToCheck.random(rng)
                for (neighbor in tileWeAreChecking.neighbors
                        .filter { !tilesInArea.contains(tiles[it]) && waterTiles.contains(tiles[it]) }) {
                    tilesInArea += tiles[neighbor]
                    tilesToCheck += tiles[neighbor]
                    waterTiles -= tiles[neighbor]
                }
                tilesToCheck -= tileWeAreChecking
            }

            if (tilesInArea.size <= 10) {
                for (tile in tilesInArea) {
                    tile.baseTerrain = TerrainType.Lakes
//                    tile.setTransients()
                }
            }
            tilesInArea.clear()
        }

        //Coasts
        for (tile in tiles.filter { it.baseTerrain == TerrainType.Ocean }) {
            if (tile.neighbors.any { tiles[it].isLand() })
                tile.coast = true
//                tile.setTransients()
        }
    }

    private fun spreadAncientRuins() {
        val suitableTiles = tiles.filter { it.isLand() }
        val locations = chooseSpreadOutLocations(suitableTiles.size / 100,
                suitableTiles, 10)
        for (tile in locations)
            tile.lostCityRumors = true
    }

    private fun chooseSpreadOutLocations(numberOfResources: Int, suitableTiles: List<Tile>, initialDistance: Int): ArrayList<Tile> {

        for (distanceBetweenResources in initialDistance downTo 1) {
            var availableTiles = suitableTiles.toList()
            val chosenTiles = ArrayList<Tile>()

            // If possible, we want to equalize the base terrains upon which
            //  the resources are found, so we save how many have been
            //  found for each base terrain and try to get one from the lowerst
            val baseTerrainsToChosenTiles = HashMap<TerrainType, Int>()
            for (tileInfo in availableTiles) {
                if (tileInfo.baseTerrain !in baseTerrainsToChosenTiles)
                    baseTerrainsToChosenTiles[tileInfo.baseTerrain] = 0
            }

            for (i in 1..numberOfResources) {
                if (availableTiles.isEmpty()) break
                val orderedKeys = baseTerrainsToChosenTiles.entries
                        .sortedBy { it.value }.map { it.key }
                val firstKeyWithTilesLeft = orderedKeys
                        .first { availableTiles.any { tile -> tile.baseTerrain == it } }
                val chosenTile = availableTiles.filter { it.baseTerrain == firstKeyWithTilesLeft }.random()
                availableTiles = availableTiles.filter { it.getDistance(chosenTile) > distanceBetweenResources }
                chosenTiles.add(chosenTile)
                baseTerrainsToChosenTiles[firstKeyWithTilesLeft] = baseTerrainsToChosenTiles[firstKeyWithTilesLeft]!! + 1
            }
            // Either we got them all, or we're not going to get anything better
            if (chosenTiles.size == numberOfResources || distanceBetweenResources == 1) return chosenTiles
        }
        throw Exception("Couldn't choose suitable tiles for $numberOfResources resources!")
    }

    private fun raiseMountainsAndHills() {
        val elevationSeed = rng.nextInt().toDouble()
        for (tile in tiles.filter { !it.isWater() }) {
            var elevation = getPerlinNoise(tile, elevationSeed, scale = 3.0)
            elevation = abs(elevation).pow(1.0 - elevationExponent) * elevation.sign

            when {
                elevation <= 0.5 -> tile.baseTerrain = TerrainType.Plains
                elevation <= 0.7 -> tile.baseTerrain = TerrainType.Hills
                elevation <= 1.0 -> tile.baseTerrain = TerrainType.Mountains
            }
        }
    }

    private fun applyHumidityAndTemperature() {
        val humiditySeed = rng.nextInt().toDouble()
        val temperatureSeed = rng.nextInt().toDouble()

        val scale = tilesPerBiomeArea.toDouble()

        for (tile in tiles) {
            if (tile.isWater() || tile.baseTerrain in arrayOf(TerrainType.Mountains, TerrainType.Hills))
                continue

            val humidity = (getPerlinNoise(tile, humiditySeed, scale = scale, nOctaves = 1) + 1.0) / 2.0

            val randomTemperature = getPerlinNoise(tile, temperatureSeed, scale = scale, nOctaves = 1)
            val latitudeTemperature = 1.0 - 2.0 * 2 * abs(tile.latitude) / height
            var temperature = ((5.0 * latitudeTemperature + randomTemperature) / 6.0)
            temperature = abs(temperature).pow(1.0 - temperatureExtremeness) * temperature.sign

            tile.baseTerrain = when {
                temperature < -0.4 -> {
                    when {
                        humidity < 0.5 -> TerrainType.Arctic
                        else -> TerrainType.Tundra
                    }
                }
                temperature < 0.8 -> {
                    when {
                        humidity < 0.5 -> TerrainType.Plains
                        else -> TerrainType.Grassland
                    }
                }
                temperature <= 1.0 -> {
                    when {
                        humidity < 0.7 -> TerrainType.Desert
                        else -> TerrainType.Plains
                    }
                }
                else -> {
                    println(temperature)
                    TerrainType.Lakes
                }

            }
        }
    }

//    private fun spawnVegetation() {
//        val vegetationSeed = rng.nextInt().toDouble()
//        val candidateTerrains = TerrainType.values().sliceArray(0..7)
//        val forrestedTerrain = TerrainType.values().sliceArray(8..16)
//        for (tile in tiles.asSequence().filter { it.isLand() && it in candidateTerrains}) {
//            val vegetation = (getPerlinNoise(tile, vegetationSeed, scale = 3.0, nOctaves = 1) + 1.0) / 2.0
//
//            if (vegetation <= vegetationRichness)
//                tile.baseTerrain = tile.baseTerrain.forested()
//        }
//    }

}

enum class MapType {
    Continents,
    Perlin,
    Archipelago
}

