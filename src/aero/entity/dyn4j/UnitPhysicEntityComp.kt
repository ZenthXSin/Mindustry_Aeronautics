package aero.entity.dyn4j

import arc.math.Angles
import arc.math.geom.Vec2
import mindustry.gen.Unitc
import mindustry.gen.Velc
import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Geometry
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Vector2
import kotlin.math.sqrt

/** 保留在 Kotlin 中的特定于单位的 dyn4j 物体构建，便于维护。 */
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

        // moveAt 可能比固定物理步长运行得更频繁。将每次调用视为最新的推进命令，包括零输入和限速情况
        body.clearAccumulatedForce()

        val mass = body.getMass().getMass()
        if (mass <= 0.0 || acceleration <= 0.0001f || speed <= 0f) return

        val inputLength = vector.len().toDouble()
        if (inputLength <= 0.0001) return

        val currentVelocity = body.getLinearVelocity()
        val desiredVelocityX = vector.x * ticksPerSecond
        val desiredVelocityY = vector.y * ticksPerSecond

        // Track the complete target velocity, as vanilla moveAt does. Compensating
        // dyn4j damping here prevents cruise speed from oscillating every physics step.
        var accelerationX =
            (desiredVelocityX - currentVelocity.x) / fixedStep +
                body.linearDamping * currentVelocity.x
        var accelerationY =
            (desiredVelocityY - currentVelocity.y) / fixedStep +
                body.linearDamping * currentVelocity.y

        val accelerationLength = sqrt(
            accelerationX * accelerationX + accelerationY * accelerationY,
        )
        val maximumAcceleration = speed * acceleration * ticksPerSecond * ticksPerSecond

        if (accelerationLength > maximumAcceleration) {
            val scale = maximumAcceleration / accelerationLength
            accelerationX *= scale
            accelerationY *= scale
        }

        // applyForce also wakes a resting body. Writing body.force directly leaves a
        // sleeping unit asleep, so it can receive movement commands without moving.
        body.applyForce(Vector2(accelerationX * mass, accelerationY * mass))
    }

    @JvmStatic
    fun lookAtBody(targetAngle: Float, body: Body?, rotateSpeed: Float, speedMultiplier: Float) {
        if (body == null) return

        val currentDeg = -Math.toDegrees(body.transform.rotationAngle)
        val angleDiff = Angles.angleDist(currentDeg.toFloat(), targetAngle)
        val maxOmega = rotateSpeed * speedMultiplier * 60f
        val targetOmega = Math.toRadians(angleDiff.coerceIn(-maxOmega, maxOmega).toDouble())

        body.setAngularVelocity(targetOmega)
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
