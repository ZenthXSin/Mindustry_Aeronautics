package aero.entity.dyn4j

import arc.math.geom.Vec2
import mindustry.gen.Unitc
import mindustry.gen.Velc
import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Geometry
import org.dyn4j.geometry.MassType

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
            linearDamping = unit.drag() * 60.0
        }
    }

    @JvmStatic
    fun moveAtBody(vector: Vec2, acceleration: Float, speed: Float, body: Body?) {
        if (body == null) return

        // moveAt may run more often than the fixed physics step. Treat each call as
        // the latest propulsion command, including zero input and speed-limit cases.
        body.clearAccumulatedForce()

        val mass = body.getMass().getMass()
        if (mass <= 0.0 || acceleration <= 0.0001f || speed <= 0f) return

        val inputLength = vector.len().toDouble()
        if (inputLength <= 0.0001) return

        val directionX = vector.x / inputLength
        val directionY = vector.y / inputLength
        val currentVelocity = body.getLinearVelocity()
        val currentForwardSpeed =
            currentVelocity.x * directionX + currentVelocity.y * directionY
        val targetForwardSpeed = inputLength * ticksPerSecond
        val speedDeficit = targetForwardSpeed - currentForwardSpeed

        val maximumAcceleration = speed * acceleration * ticksPerSecond * ticksPerSecond
        // dyn4j applies linear damping every physics step. If thrust becomes zero as
        // soon as the target speed is reached, damping drops the speed on the next
        // step, thrust switches back on, and the unit alternates between the two
        // states. This is visible as periodic unit stutter after cruising for a while.
        // Compensate the damping continuously and only use positive propulsion here;
        // excess speed is still reduced naturally by damping.
        val dampingCompensation = body.linearDamping * maxOf(currentForwardSpeed, 0.0)
        val requestedAcceleration = speedDeficit / fixedStep + dampingCompensation
        val appliedAcceleration = requestedAcceleration.coerceIn(0.0, maximumAcceleration)

        if (appliedAcceleration <= 0.0001) return

        // The propulsion command replaces the previous command, so write into the
        // body's reusable force vector instead of allocating a Vector2/Force each frame.
        body.force.set(
            directionX * appliedAcceleration * mass,
            directionY * appliedAcceleration * mass,
        )
    }

    @JvmStatic
    fun syncToVel(body: Body?, velocity: Velc) {
        if (body == null) return

        velocity.vel().set(
            (body.linearVelocity.x / 60.0).toFloat(),
            (body.linearVelocity.y / 60.0).toFloat(),
        )
    }
}
