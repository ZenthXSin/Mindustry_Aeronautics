package aero.entity.dyn4j

import arc.struct.Seq
import mindustry.entities.units.StatusEntry

/**
 * Binary compatibility contract for methods added to unit component interfaces by MindustryX.
 * Only vanilla Mindustry types are referenced so the same mod jar remains usable on v158.
 */
interface MindustryXUnitCompat {
    fun statuses(): Seq<StatusEntry>

    fun healthBalance(): Float

    fun healthChanged()
}
