package aero

import aero.content.AeroUnitTypes
import aero.core.AeroVars
import aero.gen.EntityRegistry
import arc.util.Log
import mindustry.mod.Mod

class Aero : Mod() {
    init {
        Log.info("Loaded Aero constructor.")
        AeroVars.aeroWorld.load()
    }

    override fun loadContent() {
        EntityRegistry.register()
        AeroUnitTypes.load()
        Log.info("Loading some Aeronautics content.")
    }
}
