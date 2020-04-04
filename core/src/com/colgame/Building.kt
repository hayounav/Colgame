package com.colgame

class Building(val type: BuildingType) {

    private val workers = mutableListOf<Unit>()
    var productionModifier: Bonus = None()

    var totalResourceAmount = 0
        get() = productionModifier.compute(field)

    fun addWorker(u: Unit) {
        workers.add(u)
        totalResourceAmount += type.productionAmount() * (if (u.type == type.specialist()) 2 else 1)
    }

    fun removeWorker(u: Unit) {
        assert(workers.contains(u))
        if (workers.remove(u)) {
            totalResourceAmount -= type.productionAmount() * (if (u.type == type.specialist()) 2 else 1)
        }
    }
}

interface Bonus {
    fun compute(baseAmount: Int): Int
}

class None : Bonus {
    override fun compute(baseAmount: Int): Int {
        return baseAmount
    }
}

class Add(private val amount: Int) : Bonus {
    override fun compute(baseAmount: Int): Int {
        return amount + baseAmount
    }
}

class Mult(private val amount: Int) : Bonus {
    override fun compute(baseAmount: Int): Int {
        return amount * baseAmount
    }
}

enum class BuildingType(val hammers: Int, tools: Int, val minPopulation: Int, val upgradesFrom: BuildingType?) {
    TownHall(0, 0, 1, null),

    CarpentersShop(64, 20, 4, null),
    LumberMill(0, 0, 1, CarpentersShop),

    BlacksmithHouse(0, 0, 1, null),
    BlacksmithShop(64, 20, 4, BlacksmithHouse),
    IronWorks(240, 100, 8, BlacksmithShop),

    TobacconistHouse(0, 0, 1, null),
    TobacconistShop(64, 20, 4, TobacconistHouse),
    CigarFactory(160, 100, 8, TobacconistShop),

    WeaverHouse(0, 0, 1, null),
    WeaverShop(64, 20, 4, WeaverHouse),
    TextileMill(160, 100, 8, WeaverShop),

    FurTraderHouse(0, 0, 1, null),
    FurTradingPost(64, 20, 4, FurTraderHouse),
    FurFactory(160, 100, 8, FurTradingPost),

    RumDistillerHouse(0, 0, 1, null),
    RumDistillery(64, 20, 1, RumDistillerHouse),
    RumFactory(160, 100, 8, RumDistillery),

    Armory(52, 0, 1, null),
    Magazine(120, 50, 8, Armory),
    Arsenal(240, 100, 8, Magazine),

    Stockade(64, 0, 3, null),
    Fort(120, 100, 3, Stockade),
    Fortress(320, 200, 8, Fort),

    Church(64, 0, 3, null),
    Cathedral(176, 100, 8, Church),

    Schoolhouse(64, 0, 4, null),
    College(160, 50, 8, Schoolhouse),
    University(200, 100, 10, College),

    Docks(52, 0, 1, null),
    Drydock(80, 50, 4, Docks),
    Shipyard(240, 100, 8, Drydock),

    PrintingPress(52, 20, 1, null),
    Newspaper(120, 50, 4, PrintingPress);


    fun specialist(): UnitType? {
        return when (this) {
            LumberMill, CarpentersShop -> UnitType.MasterCarpenter
            IronWorks, BlacksmithShop, BlacksmithHouse -> UnitType.MasterBlacksmith
            CigarFactory, TobacconistShop, TobacconistHouse -> UnitType.MasterTobacconist
            TextileMill, WeaverShop, WeaverHouse -> UnitType.MasterWeaver
            FurTraderHouse, FurTradingPost, FurFactory -> UnitType.MasterFurTrader
            RumDistillerHouse, RumDistillery, RumFactory -> UnitType.MasterDistiller
            Armory, Magazine, Arsenal -> UnitType.MasterGunsmith
            else -> null
        }
    }

    fun consumes(): Resource? {
        return when (this) {
            LumberMill, CarpentersShop -> Resource.Lumber
            IronWorks, BlacksmithShop, BlacksmithHouse -> Resource.Ore
            CigarFactory, TobacconistShop, TobacconistHouse -> Resource.Tobacco
            TextileMill, WeaverShop, WeaverHouse -> Resource.Cotton
            FurTraderHouse, FurTradingPost, FurFactory -> Resource.Furs
            RumDistillerHouse, RumDistillery, RumFactory -> Resource.Sugar
            Armory, Magazine, Arsenal -> Resource.Tools
            else -> null
        }
    }

    fun produces(): Resource? {
        return when (this) {
            LumberMill, CarpentersShop -> Resource.Hammers
            IronWorks, BlacksmithShop, BlacksmithHouse -> Resource.Tools
            CigarFactory, TobacconistShop, TobacconistHouse -> Resource.Cigars
            TextileMill, WeaverShop, WeaverHouse -> Resource.Cloth
            FurFactory, FurTradingPost, FurTraderHouse -> Resource.Coats
            RumDistillerHouse, RumDistillery, RumFactory -> Resource.Rum
            Arsenal, Magazine, Armory -> Resource.Muskets
            else -> null
        }
    }

    fun productionAmount(): Int {
        return when (this) {
            CarpentersShop, BlacksmithHouse, TobacconistHouse, WeaverHouse, FurTraderHouse, RumDistillerHouse, Armory -> 3
            LumberMill, BlacksmithShop, TobacconistShop, WeaverShop, FurTradingPost, RumDistillery, Magazine -> 6
            IronWorks, CigarFactory, TextileMill, FurFactory, RumFactory -> 9
            Arsenal -> 12
            else -> 0
        }
    }
}


