package aero.world.physics

import arc.Events
import mindustry.game.EventType
import org.dyn4j.dynamics.Body
import org.dyn4j.world.World

class AeroWorld {
    val world: World<Body> by lazy { World() }

    var accumulator = 0.0//进度

    val step = 1.0 / 60.0//步长

    //TODO 计划单开线程
    fun update() {
        accumulator += arc.Core.graphics.deltaTime.toDouble()
        while (accumulator >= step) {
            world.update(step)
            accumulator -= step
        }
    }

    fun addBody(body: Body) = world.addBody(body)

    fun removeBody(body: Body) = world.removeBody(body)

    fun load() {
        Events.on(EventType.WorldLoadBeginEvent::class.java) {
            // 切换地图时清空所有刚体，防止旧世界残留
            world.bodies.toList().forEach { world.removeBody(it) }
            accumulator = 0.0
        }
        Events.run(EventType.Trigger.update) {
            update()
        }
    }
}