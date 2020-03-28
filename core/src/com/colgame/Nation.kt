package com.colgame

open class Nation (val name : String) {
    val cities = mutableListOf<Colony>()
    val units = mutableListOf<Unit>()
}

class EuropeanNation (name : String) : Nation(name) {
    val kingsTax = 0.0
}