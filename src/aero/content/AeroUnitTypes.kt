package aero.content

import aero.gen.EntityRegistry
import aero.gen.UnitPhysicEntityBridge
import mindustry.type.UnitType

object AeroUnitTypes {
    lateinit var smallPlane: UnitType
        private set

    fun load() {
        smallPlane = EntityRegistry.content(
            "small-plane",
            UnitPhysicEntityBridge::class.java
        ) { name ->
            UnitType(name).apply {
                flying = true
                lowAltitude = true

                health = 120f
                hitSize = 8f

                speed = 2.5f
                accel = 0.08f
                drag = 0.04f
                rotateSpeed = 5f

                engineOffset = 5f
                engineSize = 2f
                itemCapacity = 0
            }
        }
    }
}
