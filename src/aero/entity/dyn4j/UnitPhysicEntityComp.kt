package aero.entity.dyn4j

import arc.math.geom.Vec2
import mindustry.Vars
import mindustry.gen.Unitc
import mindustry.gen.Velc
import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Geometry
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Vector2
import java.util.IdentityHashMap
import kotlin.math.sqrt

/** 保留在 Kotlin 中的特定于单位的 dyn4j 物体构建，便于维护。 */
object UnitPhysicEntitySupport {
    // Mindustry 的速度单位是“世界单位/逻辑刻”，而 dyn4j 使用“世界单位/秒”。
    private const val ticksPerSecond = 60.0
    private const val fixedStep = 1.0 / ticksPerSecond
    private const val turnAccelerationTime = 0.1
    private const val warpDistance = 8.0

    /**
     * 一次 Mindustry 更新提交的移动意图。
     *
     * 这里只保存数值，不能保存传入的 Vec2：Mindustry 经常复用 Tmp.v*，下一行代码
     * 就可能修改同一个向量对象。
     */
    private data class MovementCommand(
        val velocityX: Double,
        val velocityY: Double,
        val acceleration: Float,
        val speed: Float,
    )

    /** 转向输入也要跨越同一渲染帧内的所有物理子步。 */
    private data class SteeringCommand(
        val targetAngle: Float,
        val rotateSpeed: Float,
        val speedMultiplier: Float,
    )

    // Body 没有稳定的业务 ID；按对象身份关联命令可避免 equals 语义带来的意外覆盖。
    private val movementCommands = IdentityHashMap<Body, MovementCommand>()
    private val steeringCommands = IdentityHashMap<Body, SteeringCommand>()

    @JvmStatic
    fun createBody(unit: Unitc): Body {
        //TODO 计划支持更多形状
        val size = unit.hitSize().toDouble()
        return Body().apply {
            addFixture(BodyFixture(Geometry.createRectangle(size, size)))
            setMass(MassType.NORMAL)
            linearDamping = unit.drag() * 60.0
            angularDamping = unit.drag() * 60.0
        }
    }

    @JvmStatic
    fun moveAtBody(vector: Vec2, acceleration: Float, speed: Float, body: Body?) {
        if (body == null) return

        // moveAt 跟随 Mindustry 更新调用，频率不一定等于 dyn4j 的固定 60 Hz。
        // 此处仅保留最新指令，由 AeroWorld 在每个物理子步前施加，避免慢帧的
        // 第二个子步没有推力、只受到阻尼，从而出现“一卡一卡”。
        movementCommands[body] = MovementCommand(
            // 将“每刻速度”转换成 dyn4j 使用的“每秒速度”。
            velocityX = vector.x * ticksPerSecond,
            velocityY = vector.y * ticksPerSecond,
            acceleration = acceleration,
            speed = speed,
        )
    }

    @JvmStatic
    fun applyPhysicsControls(bodies: List<Body>) {
        // 同一渲染帧可能补算多个固定子步；每个子步都要重新根据当前状态计算力。
        movementCommands.forEach { (body, command) ->
            applyMovementForce(body, command)
        }

        steeringCommands.forEach { (body, command) ->
            applySteeringTorque(body, command)
        }

        // 边界约束必须直接作用于权威的 Body。只修改 Unit.x/y 会在下一次
        // syncFromBody 时被 Body 覆盖，造成边界附近反复跳动。
        bodies.forEach(::applyWorldBounds)
    }

    @JvmStatic
    fun clearControlCommands() {
        movementCommands.clear()
        steeringCommands.clear()
    }

    private fun applyMovementForce(body: Body, command: MovementCommand) {
        val acceleration = command.acceleration
        val speed = command.speed

        val mass = body.getMass().getMass()
        if (mass <= 0.0 || acceleration <= 0.0001f || speed <= 0f) return

        if (
            command.velocityX * command.velocityX + command.velocityY * command.velocityY <=
            0.0001 * 0.0001
        ) return

        val currentVelocity = body.getLinearVelocity()
        val desiredVelocityX = command.velocityX
        val desiredVelocityY = command.velocityY

        // (目标速度 - 当前速度) / dt 得到本子步追上目标所需的加速度。
        // 再补偿 linearDamping * velocity，避免到达巡航速度后被阻尼反复拉低。
        var accelerationX =
            (desiredVelocityX - currentVelocity.x) / fixedStep +
                body.linearDamping * currentVelocity.x
        var accelerationY =
            (desiredVelocityY - currentVelocity.y) / fixedStep +
                body.linearDamping * currentVelocity.y

        val accelerationLength = sqrt(
            accelerationX * accelerationX + accelerationY * accelerationY,
        )
        // Mindustry 的 accel 表示“每逻辑刻增加多少速度”；两个 60 倍分别完成
        // 速度和时间单位换算，因此这里是 ticksPerSecond 的平方。
        val maximumAcceleration = speed * acceleration * ticksPerSecond * ticksPerSecond

        if (accelerationLength > maximumAcceleration) {
            val scale = maximumAcceleration / accelerationLength
            accelerationX *= scale
            accelerationY *= scale
        }

        // applyForce 会唤醒休眠 Body；直接修改累计力可能让单位收到输入却仍保持休眠。
        body.applyForce(Vector2(accelerationX * mass, accelerationY * mass))
    }

    @JvmStatic
    fun lookAtBody(targetAngle: Float, body: Body?, rotateSpeed: Float, speedMultiplier: Float) {
        if (body == null) return

        // 不能在这里 setAngularVelocity：那会覆盖碰撞求解器刚产生的角冲量。
        // 与移动输入一样，只缓存目标并在每个固定子步前施加有限扭矩。
        steeringCommands[body] = SteeringCommand(targetAngle, rotateSpeed, speedMultiplier)
    }

    private fun applySteeringTorque(body: Body, command: SteeringCommand) {
        val inertia = body.mass.inertia
        if (inertia <= 0.0) return

        // Mindustry 与 dyn4j 的旋转正方向相反，目标角度先转换到 dyn4j 坐标系。
        val targetAngle = Math.toRadians(-command.targetAngle.toDouble())
        val currentAngle = body.transform.rotationAngle

        // atan2(sin, cos) 将误差归一化到 [-PI, PI]，得到带方向的最短角差。
        val rawError = targetAngle - currentAngle
        val angleError = kotlin.math.atan2(kotlin.math.sin(rawError), kotlin.math.cos(rawError))

        val maximumAngularSpeed = Math.toRadians(
            command.rotateSpeed.toDouble() * command.speedMultiplier * ticksPerSecond,
        ).coerceAtLeast(0.0)
        val desiredAngularVelocity =
            (angleError / fixedStep).coerceIn(-maximumAngularSpeed, maximumAngularSpeed)

        // 有限角加速度让控制器逐渐纠正碰撞造成的旋转，而不是瞬间抹掉碰撞结果。
        val maximumAngularAcceleration = maximumAngularSpeed / turnAccelerationTime
        val desiredAngularAcceleration =
            ((desiredAngularVelocity - body.angularVelocity) / fixedStep +
                body.angularDamping * body.angularVelocity)
                .coerceIn(-maximumAngularAcceleration, maximumAngularAcceleration)

        body.applyTorque(inertia * desiredAngularAcceleration)
    }

    private fun applyWorldBounds(body: Body) {
        val unit = body.userData as? Unitc ?: return
        if (!unit.type().bounded || (Vars.net.client() && !unit.isLocal)) return

        var left = 0.0
        var bottom = 0.0
        var right = Vars.world.unitWidth().toDouble()
        var top = Vars.world.unitHeight().toDouble()

        if (Vars.state.rules.limitMapArea && !unit.team().isAI) {
            left = Vars.state.rules.limitX * Vars.tilesize.toDouble()
            bottom = Vars.state.rules.limitY * Vars.tilesize.toDouble()
            right = left + Vars.state.rules.limitWidth * Vars.tilesize
            top = bottom + Vars.state.rules.limitHeight * Vars.tilesize
        }

        if (right - left < Vars.tilesize || top - bottom < Vars.tilesize) return

        val maximumX = right - Vars.tilesize
        val maximumY = top - Vars.tilesize
        val center = body.worldCenter
        var pushX = 0.0
        var pushY = 0.0

        if (center.x < left) pushX += (left - center.x) / warpDistance
        if (center.y < bottom) pushY += (bottom - center.y) / warpDistance
        if (center.x > maximumX) pushX -= (center.x - maximumX) / warpDistance
        if (center.y > maximumY) pushY -= (center.y - maximumY) / warpDistance

        val mass = body.mass.mass
        if (mass > 0.0 && (pushX != 0.0 || pushY != 0.0)) {
            // 原版每逻辑刻把 push 加进 vel；换算成 dyn4j 加速度需要乘 60^2。
            body.applyForce(Vector2(
                pushX * ticksPerSecond * ticksPerSecond * mass,
                pushY * ticksPerSecond * ticksPerSecond * mass,
            ))
        }

        val margin = if (unit.isGrounded) 0.0 else Vars.tilesize.toDouble()
        val clampedX = center.x.coerceIn(left - margin, maximumX + margin)
        val clampedY = center.y.coerceIn(bottom - margin, maximumY + margin)
        val translationX = clampedX - center.x
        val translationY = clampedY - center.y

        if (translationX != 0.0 || translationY != 0.0) {
            body.translate(translationX, translationY)

            // 硬边界处去掉继续朝外的速度，否则 Body 会每个子步冲出后再被拉回。
            val velocity = body.linearVelocity
            if (translationX > 0.0 && velocity.x < 0.0 || translationX < 0.0 && velocity.x > 0.0) {
                velocity.x = 0.0
            }
            if (translationY > 0.0 && velocity.y < 0.0 || translationY < 0.0 && velocity.y > 0.0) {
                velocity.y = 0.0
            }

            // 边界修正是瞬移；同步 previousTransform 可防止渲染插值跨越修正距离。
            body.previousTransform.set(body.transform)
        }
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
