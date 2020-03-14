package com.colgame

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class Colgame : Game() {

    lateinit var camera: OrthographicCamera
    lateinit var batch: Batch

    private val map: Map = Map(56, 70)

    private var viewEntireMapForDebug = false
    val simulateUntilTurnForDebug: Int = 0

    override fun create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)

        camera = OrthographicCamera()
        camera.setToOrtho(false, 800f, 480f)

        Gdx.graphics.isContinuousRendering = false

        initTextures()
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch = SpriteBatch()
//        batch.projectionMatrix = camera.combined
        batch.begin()
        for (tile in map.tiles) {
            println("${tile.x} ${tile.y}")
            batch.draw(tileTexture(tile.baseTerrain), tile.x * 20f, tile.y * 20f)
        }
        batch.end()

    }

    override fun dispose() {
        super.dispose()
        batch.dispose()
    }

    companion object {
        private val textures = Array<Texture?>(9) { null }

        fun initTextures() {
            textures[0] = Texture(Gdx.files.local("Desert.png"))
            textures[1] = Texture(Gdx.files.local("Arctic.png"))
            textures[2] = Texture(Gdx.files.local("Marsh.png"))
            textures[3] = Texture(Gdx.files.local("Mountains.png"))
            textures[4] = Texture(Gdx.files.local("Ocean.png"))
            textures[5] = Texture(Gdx.files.local("Plains.png"))
            textures[6] = Texture(Gdx.files.local("Savannah.png"))
            textures[7] = Texture(Gdx.files.local("Swamp.png"))
            textures[8] = Texture(Gdx.files.local("Grassland.png"))
        }

        fun tileTexture(type: TerrainType): Texture = when (type) {
            TerrainType.Desert, TerrainType.Scrub -> textures[0]!!
            TerrainType.Arctic, TerrainType.Tundra -> textures[1]!!
            TerrainType.Marsh, TerrainType.Wetland -> textures[2]!!
            TerrainType.Hills, TerrainType.Mountains -> textures[3]!!
            TerrainType.Ocean, TerrainType.SeaLane -> textures[4]!!
            TerrainType.Plains, TerrainType.Mixed -> textures[5]!!
            TerrainType.Savannah, TerrainType.Tropical -> textures[6]!!
            TerrainType.Swamp, TerrainType.Rain -> textures[7]!!
            else -> textures[8]!!
        }
    }
}