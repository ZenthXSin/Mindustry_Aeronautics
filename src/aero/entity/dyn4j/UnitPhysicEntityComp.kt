package aero.entity.dyn4j

import arc.util.Log
import ent.anno.Annotations.*
import mindustry.gen.Unitc
import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Geometry
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Rectangle
import org.dyn4j.geometry.Vector2
import aero.gen.PhysicEntityc

@EntityComponent
internal abstract class UnitPhysicEntityComp : PhysicEntityc, Unitc {

    override fun add() {
        initBody()
    }

    override fun createBody(): Body = Body().apply {
        // 矩形（hitSize 为 Mindustry 的碰撞半径，转 dyn4j 单位）
        val w = hitSize() / 1.0
        val h = hitSize() / 1.0
        addFixture(BodyFixture(Geometry.createRectangle(w,h))
        )
    }

    override fun update() {
        syncFromBody()
    }

    override fun remove() {
        Log.info("111")
    }
}