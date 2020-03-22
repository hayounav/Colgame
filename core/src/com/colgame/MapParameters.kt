package com.colgame

object MapParameters {
    enum class Climate(val poleTemperature: Int, val equatorTemperature: Int) {
        Cold(-20, 25),
        Cool(-20, 30),
        Temperate(-10, 35),
        Warm(-5, 40),
        Hot(0, 40)
    }

    const val temperatureDeviation = 7

    enum class Humidity(val prcnt: Double) {
        VeryDry(0.25),
        Arid(0.35),
        Normal(0.4),
        Wet(0.55),
        VeryWet(0.65)
    }

    const val humidityDeviation = 0.1

    enum class Size(val width: Int, val height: Int, val landmasses: Int) {
        VerySmall(40, 50, 1),
        Small(48, 60, 1),
        Medium(56, 70, 2),
        Large(64, 80, 3),
        VeryLarge(72, 90, 4)
    }

    enum class Landmass(val multiplier: Int, val decay: Double, val maxLand : Double) {
        Islands(20, 0.3, 0.2),
        Archipelago(20, 0.01, 0.35),
        Continent(20, 0.001, 0.65)
    }

    val forestCover: Double by lazy { humidity.prcnt * 0.5 }
    const val minEdge: Double = 0.15
    const val minMountainsElevation = 0.75
    const val minHillsElevation = 0.65

    // default map settings
    val size = Size.VerySmall
    val landMass = Landmass.Continent
    var climate = Climate.Temperate
    var humidity = Humidity.Normal
    var baseRiverAmount = 0.015

}