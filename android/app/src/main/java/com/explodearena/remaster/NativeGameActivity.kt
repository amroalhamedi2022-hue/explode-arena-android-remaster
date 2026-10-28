package com.explodearena.remaster

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.explodearena.remaster.core.ExplodeArenaNativeGame

/** Native libGDX activity; remains non-exported until gameplay parity is verified. */
class NativeGameActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration()
        config.useImmersiveMode = true
        initialize(ExplodeArenaNativeGame(), config)
    }
}
