package aero

import aero.content.AeroUnitTypes
import aero.core.AeroVars
import aero.core.extend.Settings
import aero.gen.EntityRegistry
import aero.world.physics.AeroPhysicsDebugRenderer
import arc.util.Log
import mindustry.mod.Mod

class Aero : Mod() {
    init {
        Log.info("Loaded Aero constructor.")
        AeroVars.aeroWorld.load()
        Settings().init()
        AeroPhysicsDebugRenderer.load()
    }

    override fun loadContent() {
        EntityRegistry.register()
        AeroUnitTypes.load()
        Log.info("Loading some Aeronautics content.")
    }
}
