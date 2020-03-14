package com.colgame

import kotlin.math.abs
import kotlin.math.max

class Tile(map: Map, val x: Int, val y: Int) {

    val neighbors: Array<Int>
    var coast: Boolean = false

    lateinit var baseTerrain: TerrainType
    var resource: Resources = Resources.None

    var lostCityRumors: Boolean = false

    // |(x - width) / (width/2)| => |2(x - width) / width| => |2(x / width - 1)|
    val longitude: Int by lazy { x - map.centerTile.x }

    // |(x - width) / (width/2)| => |2(x - width) / width| => |2(x / width - 1)|
    val latitude: Int by lazy { y - map.centerTile.y }

    init {
        val tmpNeighbors = ArrayList<Int>()

        if (y > 0) {
            if (x > 0)
                tmpNeighbors.add((x - 1) * map.width + y - 1)
            tmpNeighbors.add(x * map.width + y - 1)
            if (x < map.width)
                tmpNeighbors.add((x + 1) * map.width + y - 1)
        }
        if (x > 0)
            tmpNeighbors.add((x - 1) * map.width + y)
        if (x < map.width)
            tmpNeighbors.add((x + 1) * map.width + y)
        if (y < map.height) {
            if (x > 0)
                tmpNeighbors.add((x - 1) * map.width + y + 1)
            tmpNeighbors.add(x * map.width + y + 1)
            if (x < map.width)
                tmpNeighbors.add((x + 1) * map.width + y + 1)
        }

        neighbors = Array(tmpNeighbors.size) { i -> tmpNeighbors[i] }

    }

    fun isWater(): Boolean = baseTerrain == TerrainType.Ocean || baseTerrain == TerrainType.SeaLane
    fun isLand(): Boolean = !isWater()

    // Chebyshev distance is 2D
    // Similar to manhattan distance, only allows diagonal movements as well
    fun getDistance(destination: Tile): Int {
        return max(abs(x - destination.x), abs(y - destination.y))
    }
}

enum class TerrainType(movement: Int, defensive: Int, improvement: Int, value: Int,
                       grain: Int, sugar: Int, tobacco: Int, cotton: Int,
                       furs: Int, lumber: Int, ore: Int, silver: Int, fish: Int) {

    // UNFORESTED
    Tundra(1, 0, 4, 2, 2, 0, 0, 0, 0, 0, 2, 0, 0),
    Desert(1, 0, 3, 2, 1, 0, 0, 1, 0, 0, 2, 0, 0),
    Plains(1, 0, 3, 4, 4, 0, 0, 2, 0, 0, 1, 0, 0),
    Prairie(1, 0, 3, 4, 2, 0, 0, 3, 0, 0, 0, 0, 0),
    Grassland(1, 0, 3, 4, 2, 0, 3, 0, 0, 0, 0, 0, 0),
    Savannah(1, 0, 3, 4, 3, 3, 0, 0, 0, 0, 0, 0, 0),
    Marsh(2, 1, 5, 2, 2, 0, 2, 0, 0, 0, 2, 0, 0),
    Swamp(2, 1, 7, 2, 2, 2, 0, 0, 0, 0, 2, 0, 0),

    // FORESTED
    Boreal(2, 2, 4, 3, 1, 0, 0, 0, 3, 2, 1, 0, 0),
    Scrub(1, 2, 4, 1, 1, 0, 0, 1, 2, 1, 1, 0, 0),
    Mixed(2, 2, 4, 3, 2, 0, 0, 1, 3, 3, 0, 0, 0),
    Broadleaf(2, 2, 4, 3, 1, 0, 0, 1, 2, 2, 0, 0, 0),
    Conifer(2, 2, 4, 3, 1, 0, 1, 0, 2, 3, 0, 0, 0),
    Tropical(2, 2, 6, 3, 2, 1, 0, 0, 2, 2, 0, 0, 0),
    Wetland(3, 2, 6, 1, 1, 0, 1, 0, 2, 2, 1, 0, 0),
    Rain(3, 3, 7, 1, 1, 1, 0, 0, 1, 2, 1, 0, 0),

    // OTHER
    Arctic(2, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    Ocean(1, 0, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 3),
    Lakes(1, 0, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 3),
    SeaLane(1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3),
    Mountains(3, 6, 7, 2, 0, 0, 0, 0, 0, 0, 4, 1, 0),
    Hills(2, 4, 4, 2, 1, 0, 0, 0, 0, 0, 4, 0, 0);

    fun forested(): TerrainType {
        return when (this) {
            Tundra -> Boreal
            Desert -> Scrub
            Plains -> Mixed
            Prairie -> Broadleaf
            Grassland -> Conifer
            Savannah -> Tropical
            Marsh -> Wetland
            Swamp -> Rain
            else -> throw Exception("cannot make forest on ${this.name}")
        }
    }
}

enum class Resources(value: Int) {
    None(0),
    DepletedMine(6),
    Oasis(3),
    Wheat(4),
    PrimeCotton(6),
    PrimeTobacco(6),
    PrimeSugar(7),
    Minerals(4),
    Fishery(5),
    Beaver(6),
    Game(6),
    PrimeTimber(6),
    SilverDeposit(12),
    OreDeposit(6)
}