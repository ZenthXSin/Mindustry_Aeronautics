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
    // 当前仍在主线程同步；ThreadLocal 也为后续物理线程接入保留了隔离。
    private val renderTransform = ThreadLocal.withInitial { Transform() }

    @JvmStatic
    fun initBody(position: Posc, rotation: Rotc, previousBody: Body?, newBody: Body): Body {
        disposeBody(previousBody)

        val transform = Transform().apply {
            translate(position.x().toDouble(), position.y().toDouble())
            // Arc/Mindustry 与 dyn4j 都在 Y 轴向上的世界坐标中逆时针增加角度。
            rotate(Math.toRadians(rotation.rotation().toDouble()))
        }

        newBody.transform = transform
        // 新 Body 没有历史状态；否则第一帧会从 dyn4j 默认原点插值过来。
        newBody.previousTransform.set(transform)
        newBody.userData = position
        aeroWorld.addBody(newBody)
        return newBody
    }

    @JvmStatic
    fun syncFromBody(position: Posc, rotation: Rotc, body: Body?) {
        if (body == null) return

        val interpolated = renderTransform.get()
        body.previousTransform.lerp(
            body.transform,
            aeroWorld.interpolationAlpha,
            interpolated,
        )

        // 对局部质心做变换，兼容后续不以原点为质心的三角形等碰撞形状。
        val localCenter = body.localCenter
        position.set(
            interpolated.getTransformedX(localCenter).toFloat(),
            interpolated.getTransformedY(localCenter).toFloat(),
        )
        rotation.rotation(Math.toDegrees(interpolated.rotationAngle).toFloat())
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
