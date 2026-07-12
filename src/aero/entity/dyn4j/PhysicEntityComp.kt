package aero.entity.dyn4j

import aero.core.AeroVars.aeroWorld
import mindustry.gen.Posc
import mindustry.gen.Rotc
import org.dyn4j.dynamics.Body
import org.dyn4j.geometry.Transform

/**
* Kotlin 实现背后是薄的 Java EntityAnno 组件声明
* EntityAnno 复制 Java AST 正体
* 所以实际工作Bridge.java委派到这里
 */
object PhysicEntitySupport {
    @JvmStatic
    fun initBody(position: Posc, rotation: Rotc, previousBody: Body?, newBody: Body): Body {
        disposeBody(previousBody)

        val transform = Transform().apply {
            translate(position.x().toDouble(), position.y().toDouble())
            rotate(Math.toRadians(-rotation.rotation().toDouble()))
        }

        newBody.transform = transform
        newBody.userData = position
        aeroWorld.addBody(newBody)
        return newBody
    }

    @JvmStatic
    fun syncFromBody(position: Posc, rotation: Rotc, body: Body?) {
        if (body == null) return

        val center = body.worldCenter
        position.set(center.x.toFloat(), center.y.toFloat())
        rotation.rotation((-Math.toDegrees(body.transform.rotationAngle)).toFloat())
    }

    @JvmStatic
    fun disposeBody(body: Body?): Body? {
        if (body != null) {
            aeroWorld.removeBody(body)
            body.userData = null
        }
        return null
    }
}
