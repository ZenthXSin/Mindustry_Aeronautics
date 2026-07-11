package aero.entity.dyn4j

import aero.core.AeroVars.aeroWorld
import arc.math.Mathf
import ent.anno.Annotations
import ent.anno.Annotations.*
import mindustry.gen.Posc
import mindustry.gen.Rotc
import org.dyn4j.dynamics.Body
import org.dyn4j.geometry.Transform

@EntityComponent
internal abstract class PhysicEntityComp : Posc, Rotc {
    @NoSerialize
    @NoSync
    @Transient
    lateinit var body: Body

    open fun initBody() {
        body = createBody()!!

        val transform = Transform()
        transform.translate(x.toDouble(), y.toDouble())
        transform.rotate(Math.toRadians(-rotation().toDouble()))

        body.setTransform(transform)
        body.setUserData(this)
        aeroWorld.addBody(body)
    }

    open fun syncFromBody() {
        val center = body.worldCenter
        set(center.x.toFloat(), -center.y.toFloat())
        rotation((-body.getTransform().getRotationAngle() * Mathf.radDeg).toFloat())
    }

    open fun createBody(): Body? = null
}