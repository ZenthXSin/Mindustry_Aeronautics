package aero.world.physics

import arc.Core
import arc.Events
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
            // Mindustry is a top-down plane; dyn4j defaults to Earth gravity on the Y axis.
            setGravity(0.0, 0.0)
        }
    }

    var accumulator = 0.0

    val step = 1.0 / 60.0

    // TODO: move the fixed-step update to its planned physics thread.
    fun update() {
        // Do not allow a long frame (or a debugger pause) to create an ever-growing
        // physics backlog. Such a backlog causes the classic spiral of death where
        // every following frame spends more time catching up and movement stutters.
        val frameTime = Core.graphics.deltaTime.toDouble().coerceIn(0.0, maxFrameTime)
        accumulator = minOf(accumulator + frameTime, step * maxStepsPerFrame)

        var steps = 0
        while (accumulator >= step && steps < maxStepsPerFrame) {
            world.update(step)
            accumulator -= step
            steps++
        }

        // Discard any sub-step floating-point residue only when the safety limit was hit.
        if (steps == maxStepsPerFrame && accumulator >= step) accumulator %= step
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
