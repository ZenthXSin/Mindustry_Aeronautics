package aero.world.physics

import arc.Core
import arc.Events
import mindustry.game.EventType
import org.dyn4j.dynamics.Body
import org.dyn4j.world.World

class AeroWorld {
    val world: World<Body> by lazy {
        World<Body>().apply {
            // Mindustry is a top-down plane; dyn4j defaults to Earth gravity on the Y axis.
            setGravity(0.0, 0.0)
        }
    }

    var accumulator = 0.0

    val step = 1.0 / 60.0

    // TODO: move the fixed-step update to its planned physics thread.
    fun update() {
        accumulator += Core.graphics.deltaTime.toDouble()
        while (accumulator >= step) {
            world.update(step)
            accumulator -= step
        }
    }

    fun addBody(body: Body) = world.addBody(body)

    fun removeBody(body: Body) = world.removeBody(body)

    fun load() {
        Events.on(EventType.WorldLoadBeginEvent::class.java) {
            // Clear bodies and future joints when switching maps.
            world.removeAllBodiesAndJoints()
            accumulator = 0.0
        }
        Events.run(EventType.Trigger.update) {
            update()
        }
    }
}
