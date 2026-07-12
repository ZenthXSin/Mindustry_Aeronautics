package aero.entity.dyn4j

import arc.math.geom.Vec2
import mindustry.gen.Unitc
import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Geometry
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Vector2
import kotlin.math.sqrt

/** Unit-specific dyn4j body construction kept in Kotlin for maintainability. */
object UnitPhysicEntitySupport {
    private const val ticksPerSecond = 60.0
    private const val fixedStep = 1.0 / ticksPerSecond

    @JvmStatic
    fun createBody(unit: Unitc): Body {
        val size = unit.hitSize().toDouble()
        return Body().apply {
            addFixture(BodyFixture(Geometry.createRectangle(size, size)))
            setMass(MassType.NORMAL)
        }
    }

    @JvmStatic
    fun moveAtBody(vector: Vec2, acceleration: Float, speed: Float, body: Body) {
        val mass = body.getMass().getMass()
        if (mass <= 0.0 || acceleration <= 0f || speed <= 0f) return

        val desiredVelocityX = vector.x * ticksPerSecond
        val desiredVelocityY = vector.y * ticksPerSecond
        val currentVelocity = body.getLinearVelocity()

        var accelerationX = (desiredVelocityX - currentVelocity.x) / fixedStep
        var accelerationY = (desiredVelocityY - currentVelocity.y) / fixedStep

        val accelerationLength = sqrt(
            accelerationX * accelerationX + accelerationY * accelerationY,
        )
        val maximumAcceleration = speed * acceleration * ticksPerSecond * ticksPerSecond

        if (accelerationLength > maximumAcceleration) {
            val scale = maximumAcceleration / accelerationLength
            accelerationX *= scale
            accelerationY *= scale
        }

        body.applyForce(Vector2(accelerationX * mass, accelerationY * mass))
    }
}
