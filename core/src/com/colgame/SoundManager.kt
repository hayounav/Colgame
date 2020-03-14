package com.colgame

import com.badlogic.gdx.Gdx

enum class SoundManager(val value: String) {
    Click("click"),
    Fortify("fortify"),
    Promote("promote"),
    Upgrade("upgrade"),
    Setup("setup"),
    Chimes("chimes"),
    Coin("coin"),
    Choir("choir"),
    Policy("policy"),
    Paper("paper"),
    Whoosh("whoosh"),
    Silent("");

    val sound by lazy { Gdx.audio.newSound(Gdx.files.internal("drop.wav")) }
}