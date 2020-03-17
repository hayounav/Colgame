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

    private val map: Map = Map()

    private var viewEntireMapForDebug = false
    val simulateUntilTurnForDebug: Int = 0

    override fun create() {
        map.generate()

        Gdx.input.setCatchKey(Input.Keys.BACK, true)

        camera = OrthographicCamera()
        camera.setToOrtho(false, 800f, 480f)

//        Gdx.graphics.isContinuousRendering = false

        initTextures()
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch = SpriteBatch()
        batch.setProjectionMatrix(camera.combined)
        batch.begin()
        for (tile in map.tiles) {
            batch.draw(terrainTextures[tile.type], tile.x * 25f, tile.y * 25f, 25f, 25f)

            val r = tile.river
            if (r != null) {
                if (!riverTextures.containsKey(r))
                    riverTextures[r] = Texture(Gdx.files.local("textures/River_${r.from.name}_${r.to.name}.png"))
                batch.draw(riverTextures[r], tile.x * 25f, tile.y * 25f, 25f, 25f)
            }
        }
        batch.end()

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
            camera.translate(-10f, 0f)
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            camera.translate(10f, 0f)
        if (Gdx.input.isKeyPressed(Input.Keys.UP))
            camera.translate(0f, 10f)
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
            camera.translate(0f, -10f)
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS))
            camera.zoom += 0.1f
        if (Gdx.input.isKeyPressed(Input.Keys.PLUS))
            camera.zoom -= 0.1f

        camera.update()
    }

    override fun dispose() {
        super.dispose()
        batch.dispose()
    }

    companion object {
        val terrainTextures = HashMap<TerrainType, Texture>()
        val riverTextures = HashMap<River, Texture>()

        fun initTextures() {
            for (type in TerrainType.values())
                terrainTextures[type] = Texture(Gdx.files.local("textures/${type.name}.png"))
        }
    }
}