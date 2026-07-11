package aero.entity.dyn4j;

import aero.core.AeroVars;
import arc.math.Mathf;
import ent.anno.Annotations.EntityComponent;
import ent.anno.Annotations.Import;
import ent.anno.Annotations.NoSerialize;
import ent.anno.Annotations.NoSync;
import mindustry.gen.Posc;
import mindustry.gen.Rotc;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

@EntityComponent
abstract class PhysicEntityComp implements Posc, Rotc {
    @Import float x, y, rotation;

    @NoSerialize
    @NoSync
    transient Body body = new Body();

    void initBody() {
        Transform transform = new Transform();
        transform.translate(x, y);
        transform.rotate(Math.toRadians(-rotation));

        body.setTransform(transform);
        body.setUserData(this);
        AeroVars.INSTANCE.getAeroWorld().addBody(body);
    }

    void syncFromBody() {
        Vector2 center = body.getWorldCenter();
        set((float)center.x, (float)-center.y);
        rotation((float)(-body.getTransform().getRotationAngle() * Mathf.radDeg));
    }

    @Override
    public void remove() {
        AeroVars.INSTANCE.getAeroWorld().removeBody(body);
    }
}
