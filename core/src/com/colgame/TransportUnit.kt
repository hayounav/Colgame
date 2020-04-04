package com.colgame

class TransportUnit (val type: TransportType, override val nation: Nation, override var tile: Tile) : NationEntity(nation, tile){
}