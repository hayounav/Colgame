package com.colgame

object MapParameters {
    enum class Climate(val poleTemperature: Int, val equatorTemperature: Int) {
        Cold(-20, 25),
        Cool(-20, 30),
        Temperate(-10, 35),
        Warm(-5, 40),
        Hot(0, 40)
    }

    val tempuratureDeviation = 7

    enum class Humidity(val prcnt: Double) {
        VeryDry(0.25),
        Arid(0.35),
        Normal(0.4),
        Wet(0.55),
        VeryWet(0.65)
    }

    val humidityDeviation = 0.1

    enum class Size(val width: Int, val height: Int) {
        VerySmall(40,50),
        Small(48,60),
        Medium(56,70),
        Large(64,80),
        VeryLarge(72,70)
    }

    enum class Landmass(val mass: Double){
        Islands(0.1),
        Archipelago(0.2),
        Continent(0.5)
    }

    val forestCover : Double by lazy { humidity.prcnt * 0.5 }
    val minEdgeDistance : Double = 0.05

    // default map settings
    val size = Size.VeryLarge
    val landMass = Landmass.Islands
    var climate = Climate.Temperate
    var humidity = Humidity.Normal

}