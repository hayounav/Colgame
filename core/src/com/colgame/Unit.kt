package com.colgame

class Unit (var type: UnitType, var movementSpeed: Int, var toolds: Int, var horses: Boolean, var muskets: Boolean){

    fun isAggressive(): Boolean{
        return muskets || horses
    }
}