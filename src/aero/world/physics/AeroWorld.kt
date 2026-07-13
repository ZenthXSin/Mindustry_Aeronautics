package aero.world.physics

import aero.entity.dyn4j.UnitPhysicEntitySupport
import arc.Events
import arc.util.Time
import mindustry.game.EventType
import org.dyn4j.dynamics.Body
import org.dyn4j.world.World

class AeroWorld {
    private companion object {
        const val maxFrameTime = 0.25
        const val maxStepsPerFrame = 8
    }

    val world: World<Body> by lazy {
        World<Body>().apply {
            // Mindustry 是俯视平面；dyn4j 默认在 Y 轴上应用地球重力。
            setGravity(0.0, 0.0)
        }
    }

    var accumulator = 0.0

    val step = 1.0 / 60.0

    /** 当前渲染帧位于前后两个固定物理状态之间的比例。 */
    val interpolationAlpha: Double
        get() = (accumulator / step).coerceIn(0.0, 1.0)

    // 待办：将固定步长更新移至其计划中的物理线程。
    fun update() {
        // 不允许长帧（或调试器暂停）造成不断增长的物理积压。
        // 这种积压会导致经典的"死亡螺旋"，即每一帧都花费更多时间来追赶，运动出现卡顿。
        // 使用 Mindustry 的逻辑时钟，保证物理与单位控制受到相同的暂停、变速和帧长限制。
        val frameTime = (Time.delta.toDouble() / 60.0).coerceIn(0.0, maxFrameTime)
        accumulator = minOf(accumulator + frameTime, step * maxStepsPerFrame)

        var steps = 0
        while (accumulator >= step && steps < maxStepsPerFrame) {
            // dyn4j 每完成一个 step 就会消费普通 Force；补算多个 step 时必须
            // 在每一步前重新施加本帧的移动指令。
            UnitPhysicEntitySupport.applyPhysicsControls()
            // 外层已经完成固定步长累加；这里必须保证恰好执行一次 dyn4j step。
            // 调用 world.update(step) 会启用 dyn4j 的第二套累加器，长期运行后两者
            // 一旦错相，就会出现 previousTransform 与显示插值前后跳动。
            world.step(1, step)

            // 硬边界必须在积分完成后约束最终位置。若在 step 前夹回，当前
            // step 的移动力会再次把 Body 推出去，形成逐帧出界/回拉的频闪。
            UnitPhysicEntitySupport.enforceWorldBounds(world.bodies)
            accumulator -= step
            steps++
        }

        UnitPhysicEntitySupport.syncBodies(world.bodies)

        // 指令只代表一次 Mindustry 更新。下一帧若仍有输入，moveAt 会重新提交；
        // 若控制器停止调用 moveAt，旧指令也不会永久推动单位。
        UnitPhysicEntitySupport.clearControlCommands()

        // 仅在达到安全限制时丢弃任何子步长浮点余数。
        if (steps == maxStepsPerFrame && accumulator >= step) accumulator %= step
    }

    fun addBody(body: Body) = world.addBody(body)

    fun removeBody(body: Body) = world.removeBody(body)

    fun load() {
        Events.on(EventType.WorldLoadBeginEvent::class.java) {
            // 切换地图时清除所有物体和未来的关节。
            world.removeAllBodiesAndJoints()
            UnitPhysicEntitySupport.clearControlCommands()
            accumulator = 0.0
        }
        Events.run(EventType.Trigger.afterGameUpdate) {
            update()
        }
    }
}
