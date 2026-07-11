package aero.entity.dyn4j;

import aero.gen.PhysicEntityc;
import aero.gen.UnitPhysicEntityc;
import ent.anno.Annotations.EntityComponent;
import ent.anno.Annotations.EntityDef;
import mindustry.gen.Unitc;

@EntityComponent
@EntityDef({Unitc.class, UnitPhysicEntityc.class})
abstract class UnitPhysicEntityComp implements PhysicEntityc, Unitc {
    @Override
    public void add() {
        initBody();
    }
}
