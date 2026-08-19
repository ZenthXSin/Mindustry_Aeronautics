package aero.gen

import aero.entity.dyn4j.MindustryXUnitCompat
import mindustry.gen.Builderc
import mindustry.gen.Drawc
import mindustry.gen.Healthc
import mindustry.gen.Hitboxc
import mindustry.gen.Itemsc
import mindustry.gen.Minerc
import mindustry.gen.Physicsc
import mindustry.gen.Shieldc
import mindustry.gen.Statusc
import mindustry.gen.Syncc
import mindustry.gen.Teamc
import mindustry.gen.Unitc
import mindustry.gen.Velc
import mindustry.gen.Weaponsc

/**
 * Component interface for physics-enabled units.
 */
interface UnitPhysicEntityc : PhysicEntityc, MindustryXUnitCompat, Builderc, Drawc, Healthc,
    Hitboxc, Itemsc, Minerc, Physicsc, Shieldc, Statusc, Syncc, Teamc, Unitc, Velc, Weaponsc