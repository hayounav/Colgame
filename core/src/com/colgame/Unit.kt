package com.colgame

class Unit (var type: UnitType = UnitType.PettyCriminal,
            var movementSpeed: Int = 1,
            var tools: Int = 0,
            var horses: Boolean = false,
            var muskets: Boolean = false,
            override val nation: Nation, override var tile: Tile) : NationEntity(nation, tile){

    fun isHostile(): Boolean{
        return muskets || horses
    }

    fun acquireHorses(){
        this.movementSpeed = 4
        this.horses = true
    }

    fun disposeOfHorses(){
        this.movementSpeed = 1
        this.horses = false
    }

    fun acquireMuskets(){
        this.muskets = true
    }

    fun disposeOfMuskets(){
        this.muskets = false
    }
}