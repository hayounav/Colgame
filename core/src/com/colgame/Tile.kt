package com.colgame

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class Tile(map: Map, val x: Int, val y: Int) {
    val neighbors: Array<Int>

    val longitude: Double by lazy { x.toDouble() / map.centerTile.x - 1.0 }
    val latitude: Double by lazy { y.toDouble() / map.centerTile.y - 1.0 }

    // temperature decays linearly from the equator to the poles
    val temperature: Int by lazy {
        val tempRange = MapParameters.climate.equatorTemperature - MapParameters.climate.poleTemperature
        val baseTemp = MapParameters.climate.poleTemperature + tempRange * (1 - abs(latitude))
        val temp = baseTemp + (-1 * MapParameters.temperatureDeviation..MapParameters.temperatureDeviation).random()
        if (temp > 40) 40 else if (temp < -20) -20 else temp.toInt()
    }

    // humidity variation should behave like a downward parabola centered at 0.5 the way to the pole
    // the parabola for that is 4x-4x^2, where x=latitude
    val humidity: Double by lazy {
        val abslat = abs(latitude)
        val baseHum = MapParameters.humidity.prcnt * (4 * abslat - 4 * abslat * abslat)
        val hum = baseHum + Random.Default.nextDouble() * MapParameters.humidityDeviation - MapParameters.humidityDeviation / 2
        if (hum > 1.0) 1.0 else if (hum < 0.0) 0.0 else hum
    }

    var coast = false
    var type = TerrainType.Ocean
    var ocean = false
    var lostCityRumors = false
    var continentID = 0
    var elevation = 0.0

    var river: River? = null

    init {
        val tmpNeighbors = ArrayList<Int>()
        for (xx in -1..1) {
            for (yy in -1..1) {
                if (xx == 0 && yy == 0) {
                    continue  // You are not neighbor to yourself
                }
                if (map.containsCoordinates(x + xx, y + yy)) {
                    tmpNeighbors.add(map.coordsToTileIndex(x + xx, y + yy))
                }
            }
        }

        neighbors = Array(tmpNeighbors.size) { i -> tmpNeighbors[i] }

    }

    // Chebyshev distance is 2D
    // Similar to manhattan distance, only allows diagonal movements as well
    fun getDistance(destination: Tile): Int {
        return max(abs(x - destination.x), abs(y - destination.y))
    }
}

data class River (val from : Direction, val to : Direction)

enum class Direction {
    E, N, W, S,
    NE, NW, SE, SW
}

enum class TerrainType(val movement: Int, val defensive: Int, val improvement: Int, val value: Int,
                       val grain: Int, val sugar: Int, val tobacco: Int, val cotton: Int,
                       val furs: Int, val lumber: Int, val ore: Int, val silver: Int, val fish: Int,
                       val minTemp: Int, val maxTemp: Int, val minHumidity: Double, val maxHumidity: Double,
                       val forested: Boolean = false) {

    // UNFORESTED
    Tundra(1, 0, 4, 2, 2, 0, 0, 0, 0, 0, 2, 0, 0, -5, 5, 0.0, 1.0),
    Desert(1, 0, 3, 2, 1, 0, 0, 1, 0, 0, 2, 0, 0, 10, 40, 0.0, 0.25),
    Plains(1, 0, 3, 4, 4, 0, 0, 2, 0, 0, 1, 0, 0, 0, 15, 0.0, 0.6),
    Prairie(1, 0, 3, 4, 2, 0, 0, 3, 0, 0, 0, 0, 0, 15, 30, 0.2, 0.5),
    Grassland(1, 0, 3, 4, 2, 0, 3, 0, 0, 0, 0, 0, 0, 10, 25, 0.25, 0.7),
    Savannah(1, 0, 3, 4, 3, 3, 0, 0, 0, 0, 0, 0, 0, 30, 40, 0.25, 0.7),
    Marsh(2, 1, 5, 2, 2, 0, 2, 0, 0, 0, 2, 0, 0, 5, 15, 0.5, 1.0),
    Swamp(2, 1, 7, 2, 2, 2, 0, 0, 0, 0, 2, 0, 0, 10, 40, 0.5, 1.0),

    // FORESTED
    Boreal(2, 2, 4, 3, 1, 0, 0, 0, 3, 2, 1, 0, 0, -5, 5, 0.0, 1.0, true),
    Scrub(1, 2, 4, 1, 1, 0, 0, 1, 2, 1, 1, 0, 0, 10, 40, 0.0, 0.25, true),
    Mixed(2, 2, 4, 3, 2, 0, 0, 1, 3, 3, 0, 0, 0, 0, 15, 0.0, 0.6, true),
    Broadleaf(2, 2, 4, 3, 1, 0, 0, 1, 2, 2, 0, 0, 0, 15, 30, 0.2, 0.5, true),
    Conifer(2, 2, 4, 3, 1, 0, 1, 0, 2, 3, 0, 0, 0, 10, 25, 0.25, 0.7, true),
    Tropical(2, 2, 6, 3, 2, 1, 0, 0, 2, 2, 0, 0, 0, 30, 40, 0.25, 0.7, true),
    Wetland(3, 2, 6, 1, 1, 0, 1, 0, 2, 2, 1, 0, 0, 5, 15, 0.5, 1.0, true),
    Rain(3, 3, 7, 1, 1, 1, 0, 0, 1, 2, 1, 0, 0, 10, 40, 0.5, 1.0, true),

    // Special Land
    Arctic(2, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -20, 0, 0.0, 1.0),
    Lakes(1, 0, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 3, -20, 40, 0.0, 1.0),
    Mountains(3, 6, 7, 2, 0, 0, 0, 0, 0, 0, 4, 1, 0, -20, 40, 0.0, 1.0),
    Hills(2, 4, 4, 2, 1, 0, 0, 0, 0, 0, 4, 0, 0, -20, 40, 0.0, 1.0),

    // Ocean
    Ocean(1, 0, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 3, -20, 40, 0.0, 1.0),
    SeaLane(1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -20, 40, 0.0, 1.0);

    fun isWater(): Boolean = this == Ocean || this == SeaLane || this == Lakes
    fun isLand(): Boolean = !this.isWater()

    fun deforest(): TerrainType {
        return when (this) {
            Boreal -> Tundra
            Scrub -> Desert
            Mixed -> Plains
            Broadleaf -> Prairie
            Conifer -> Grassland
            Tropical -> Savannah
            Wetland -> Marsh
            Rain -> Swamp
            else -> throw Exception("cannot make forest on ${this.name}")
            // else -> this // if we think silent failure is better
        }
    }
}