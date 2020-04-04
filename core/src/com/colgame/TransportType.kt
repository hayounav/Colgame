package com.colgame

enum class TransportType (val moves: Int, val armed: Boolean, val strength: Int, val cargo: Int){
    TreasureTrain(1,false,0,0),
    WagonTrain(2,false,1,2),
    Caravel(4,false,2,2),
    Merchantman(5,false,6,4),
    Galleon(6,false,10,6),
    Privateer(8,true,8,2),
    Frigate(6,true,16,4),
    ManOWar(6,true,24,6),
}