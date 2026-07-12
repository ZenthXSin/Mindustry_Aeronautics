package aero.entity.dyn4j;

import aero.gen.PhysicEntityc;
import aero.gen.UnitPhysicEntityc;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import mindustry.entities.units.StatusEntry;
import mindustry.gen.Posc;
import mindustry.gen.Rotc;
import mindustry.gen.Unitc;
import mindustry.gen.Velc;
import org.dyn4j.dynamics.Body;

import static ent.anno.Annotations.*;

@EntityComponent
abstract class PhysicEntityComp implements Posc, Rotc {
    @NoSerialize
    @NoSync
    transient Body body;

    public void initBody() {
        body = aero.entity.dyn4j.PhysicEntitySupport.initBody(this, this, body, createBody());
    }

    public void syncFromBody() {
        aero.entity.dyn4j.PhysicEntitySupport.syncFromBody(this, this, body);
    }

    public void disposeBody() {
        body = aero.entity.dyn4j.PhysicEntitySupport.disposeBody(body);
    }

    public abstract Body createBody();
}

@EntityComponent
abstract class UnitPhysicEntityComp implements PhysicEntityc, Unitc, MindustryXUnitCompat {
    @Import
    Seq<StatusEntry> statuses;

    @Override
    @MethodPriority(100f)
    public void add() {
        initBody();
    }

    @Override
    public Body createBody() {
        return aero.entity.dyn4j.UnitPhysicEntitySupport.createBody(this);
    }

    @Override
    @Remove(Velc.class)
    @MethodPriority(100f)
    public void update() {
        syncFromBody();
        aero.entity.dyn4j.UnitPhysicEntitySupport.syncToVel(body(), this);
    }

    @Override
    @Replace
    public void moveAt(Vec2 vector, float acceleration) {
        aero.entity.dyn4j.UnitPhysicEntitySupport.moveAtBody(vector, acceleration, speed(), body());
    }

    @Override
    @BypassGroupCheck
    public void remove() {
        disposeBody();
    }

    @Override
    public Seq<StatusEntry> statuses() {
        return statuses;
    }

    @Override
    public float healthBalance() {
        return 0f;
    }

    @Override
    public void healthChanged() {
        // MindustryX event support is optional; vanilla v158 has no matching event API.
    }
}

@EntityDef({Unitc.class, UnitPhysicEntityc.class})
abstract class UnitPhysicEntityBridgeComp { }
