package aero.world.physics

import aero.core.AeroVars.aeroWorld
import arc.Core
import arc.Events
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Lines
import arc.math.geom.Rect
import mindustry.Vars
import mindustry.game.EventType
import mindustry.graphics.Layer
import org.dyn4j.dynamics.Body
import org.dyn4j.geometry.AABB
import org.dyn4j.geometry.Circle
import org.dyn4j.geometry.Convex
import org.dyn4j.geometry.Polygon
import org.dyn4j.geometry.Segment
import org.dyn4j.geometry.Transform
import org.dyn4j.geometry.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Client-only renderer for inspecting the shapes used by the dyn4j world. */
object AeroPhysicsDebugRenderer {
    private const val settingName = "aero-dyn4j-collision-shapes"
    private const val sampledShapeSegments = 24

    private val cameraBounds = Rect()
    private val supportDirection = Vector2()

    fun load() {
        if (Vars.headless) return

        Events.run(EventType.Trigger.drawOver) {
            if (Core.settings.getBool(settingName, false) && Vars.state.isGame) {
                draw()
            }
        }
    }

    private fun draw() {
        Draw.draw(Layer.overlayUI) {
            Core.camera.bounds(cameraBounds)
            Lines.stroke(1f)
            Draw.color(Color.cyan, 0.9f)

            aeroWorld.world.bodies.forEach { body ->
                if (!overlapsCamera(body.createAABB())) return@forEach

                body.fixtures.forEach { fixture ->
                    drawShape(fixture.shape, body.transform)
                }

                // The orange marker is the actual center of mass used by dyn4j.
                val center = body.worldCenter
                Draw.color(Color.orange, 0.9f)
                Lines.circle(center.x.toFloat(), center.y.toFloat(), 1.5f)
                Draw.color(Color.cyan, 0.9f)
            }

            Draw.reset()
        }
    }

    private fun drawShape(shape: Convex, transform: Transform) {
        when (shape) {
            is Polygon -> drawPolygon(shape, transform)
            is Circle -> drawCircle(shape, transform)
            is Segment -> drawSegment(shape, transform)
            else -> drawSampledShape(shape, transform)
        }
    }

    private fun drawPolygon(shape: Polygon, transform: Transform) {
        val vertices = shape.vertices
        for (index in vertices.indices) {
            val start = vertices[index]
            val end = vertices[(index + 1) % vertices.size]
            Lines.line(
                transform.getTransformedX(start).toFloat(),
                transform.getTransformedY(start).toFloat(),
                transform.getTransformedX(end).toFloat(),
                transform.getTransformedY(end).toFloat(),
            )
        }
    }

    private fun drawCircle(shape: Circle, transform: Transform) {
        val center = shape.center
        Lines.circle(
            transform.getTransformedX(center).toFloat(),
            transform.getTransformedY(center).toFloat(),
            shape.radius.toFloat(),
        )
    }

    private fun drawSegment(shape: Segment, transform: Transform) {
        val start = shape.point1
        val end = shape.point2
        Lines.line(
            transform.getTransformedX(start).toFloat(),
            transform.getTransformedY(start).toFloat(),
            transform.getTransformedX(end).toFloat(),
            transform.getTransformedY(end).toFloat(),
        )
    }

    /**
     * Capsule, ellipse, slice and other convex shapes are approximated through
     * their world-space support points so new fixture types remain visible.
     */
    private fun drawSampledShape(shape: Convex, transform: Transform) {
        var firstX = 0f
        var firstY = 0f
        var previousX = 0f
        var previousY = 0f

        for (index in 0 until sampledShapeSegments) {
            val angle = 2.0 * PI * index / sampledShapeSegments
            supportDirection.set(cos(angle), sin(angle))
            val point = shape.getFarthestPoint(supportDirection, transform)
            val x = point.x.toFloat()
            val y = point.y.toFloat()

            if (index == 0) {
                firstX = x
                firstY = y
            } else {
                Lines.line(previousX, previousY, x, y)
            }

            previousX = x
            previousY = y
        }

        Lines.line(previousX, previousY, firstX, firstY)
    }

    private fun overlapsCamera(bounds: AABB): Boolean {
        return bounds.maxX >= cameraBounds.x &&
            bounds.minX <= cameraBounds.x + cameraBounds.width &&
            bounds.maxY >= cameraBounds.y &&
            bounds.minY <= cameraBounds.y + cameraBounds.height
    }
}
