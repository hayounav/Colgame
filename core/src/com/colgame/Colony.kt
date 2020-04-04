package com.colgame

import kotlin.math.max
import kotlin.math.min

class Colony(val location: Tile) {
    var fortification = Fortification.None

    val resourceStorage = mutableMapOf<Resource, Storage>()
    val production = mutableMapOf<Resource, Int>()
    val consumption = mutableMapOf<Resource, Int>()

    init {
        for (r in Resource.values()) {
            if (r == Resource.Hammers || r == Resource.Horses)
                resourceStorage[r] = Storage(Int.MAX_VALUE)
            else
                resourceStorage[r] = Storage(100)
        }
    }

    fun advanceTurn() {
        for ((r, i) in resourceStorage) {
            if (production.containsKey(r))
                resourceStorage[r]!!.current += production[r]!!
            if (consumption.containsKey(r))
                resourceStorage[r]!!.current -= consumption[r]!!
        }
    }

    class Storage(var capacity: Int) {
        var current: Int = 0
            set(value) {
                field = max(0, min(capacity, value + field))

            }
    }
}

enum class Fortification {
    None,
    Stockade,
    Fort,
    Fortress
}
