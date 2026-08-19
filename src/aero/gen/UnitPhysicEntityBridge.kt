package aero.gen

import aero.entity.dyn4j.PhysicEntitySupport
import aero.entity.dyn4j.UnitPhysicEntitySupport
import arc.math.geom.Vec2
import arc.struct.Seq
import mindustry.entities.units.StatusEntry
import mindustry.gen.UnitEntity
import org.dyn4j.dynamics.Body

/**
 * Aeronautics entity with physics support.
 * Replaces EntityAnno-generated UnitPhysicEntityBridge.
 *
 * Extends Mindustry's UnitEntity (which already implements all standard interfaces)
 * and adds the custom component interfaces.
 */
class UnitPhysicEntityBridge : UnitEntity(), PhysicEntityc, UnitPhysicEntityc {
    @Transient
    var body: Body? = null

    override fun body(): Body? = body
    override fun body(b: Body?) { body = b }

    override fun initBody() {
        body = PhysicEntitySupport.initBody(this, this, body, createBody())
    }

    override fun createBody(): Body {
        return UnitPhysicEntitySupport.createBody(this)
    }

    override fun syncFromBody() {
        PhysicEntitySupport.syncFromBody(this, this, body)
    }

    override fun disposeBody() {
        body = PhysicEntitySupport.disposeBody(body)
    }

    override fun add() {
        super.add()
        initBody()
    }

    override fun update() {
        // Physics driven by AeroWorld
    }

    override fun moveAt(vector: Vec2, acceleration: Float) {
        UnitPhysicEntitySupport.moveAtBody(vector, acceleration, speed(), body)
    }

    override fun lookAt(angle: Float) {
        UnitPhysicEntitySupport.lookAtBody(angle, body, type().rotateSpeed, speedMultiplier())
    }

    override fun prefRotation(): Float = rotation()

    override fun rotateMove(vec: Vec2) {
        moveAt(arc.util.Tmp.v2.trns(rotation(), vec.len()))
    }

    override fun remove() {
        disposeBody()
        super.remove()
    }

    // MindustryXUnitCompat overrides
    override fun statuses(): Seq<StatusEntry> = statuses
    override fun healthBalance(): Float = 0f
    override fun healthChanged() { }

    /** Entity class ID — set during EntityRegistry.register() */
    override fun classId(): Int = entityId

    override fun serialize(): Boolean = true

    companion object {
        @JvmField
        var entityId: Int = 0
    }
}