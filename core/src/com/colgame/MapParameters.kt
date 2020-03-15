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
        VerySmall(40,50, 1),
        Small(48,60, 1),
        Medium(56,70, 2),
        Large(64,80, 3),
        VeryLarge(72,90, 4)
    }

    enum class Landmass(val multiplier: Int, val decay : Double){
        Islands(15, 0.25),
        Archipelago(20, 0.075),
        Continent(50, 0.01)
    }

    val forestCover : Double by lazy { humidity.prcnt * 0.5 }
    const val minEdgeDistance : Double = 0.15

    // default map settings
    val size = Size.VerySmall
    val landMass = Landmass.Archipelago
    var climate = Climate.Temperate
    var humidity = Humidity.Normal

}