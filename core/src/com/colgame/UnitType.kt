package com.colgame

enum class UnitType {
    PettyCriminal,
    FreeColonist,
    IndenturedServants,
    ExpertFarmer,
    ExpertFisherman,
    ExpertFurTrapper,
    ExpertSilverMiner,
    ExpertLumbarJack,
    ExpertOreMiner,
    MasterSugarPlanter,
    MasterCottonPlanter,
    MasterTobaccoPlanter,
    FirebrandPreacher,
    ElderStatesman,
    MasterCarpenter,
    MasterDistiller,
    MasterWeaver,
    MasterTobacconist,
    MasterFurTrader,
    MasterBlacksmith,
    MasterGunsmith,
    SeasonedScout,
    HardyPioneer,
    VeteranSoldier,
    JesuitMissionary;

    fun isSpecialized(): Boolean{
        return this != PettyCriminal &&
                this != FreeColonist &&
                this != IndenturedServants
    }
}