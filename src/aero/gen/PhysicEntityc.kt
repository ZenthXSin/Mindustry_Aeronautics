package aero.gen

import mindustry.gen.Entityc
import mindustry.gen.Posc
import mindustry.gen.Rotc
import org.dyn4j.dynamics.Body

/**
 * Component interface for entities with physics body.
 */
interface PhysicEntityc : Entityc, Posc, Rotc {
    fun body(): Body?
    fun body(body: Body?)
    fun createBody(): Body
    fun initBody()
    fun syncFromBody()
    fun disposeBody()
}