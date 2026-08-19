package aero.gen

import arc.func.Func
import arc.func.Prov
import arc.struct.ObjectMap
import mindustry.gen.Entityc
import mindustry.gen.EntityMapping

/**
 * Entity registry for Aeronautics.
 * Replaces EntityAnno-generated EntityRegistry.
 *
 * Uses [EntityMapping.register] to assign class IDs and register
 * both idMap and nameMap entries.
 */
object EntityRegistry {
    private val entityNames = ObjectMap<Class<*>, String>()
    private val entityClasses = ObjectMap<String, Class<out Entityc>>()
    private val entityProviders = ObjectMap<String, Prov<out Entityc>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Entityc> get(type: Class<T>): Prov<T>? {
        val name = entityNames.get(type) ?: return null
        return entityProviders.get(name) as? Prov<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Entityc> get(name: String): Prov<T>? {
        return entityProviders.get(name) as? Prov<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Entityc> register(name: String, type: Class<T>, prov: Prov<out T>) {
        entityNames.put(type, name)
        entityClasses.put(name, type as Class<out Entityc>)
        entityProviders.put(name, prov)
        EntityMapping.register(name, prov as Prov<Entityc>)
    }

    fun getID(type: Class<out Entityc>): Int {
        return EntityMapping.idMap.indexOfFirst { it?.javaClass == type }
    }

    /**
     * Register an entity and create a UnitType via [factory].
     *
     * Registers with [EntityMapping.register] which assigns a unique class ID
     * and puts the provider in both idMap and nameMap.
     *
     * @param name the entity name (also used as the UnitType name)
     * @param entityClass the entity implementation class
     * @param factory function that creates the content object (typically a UnitType)
     * @return the result of [factory]
     */
    @Suppress("UNCHECKED_CAST")
    fun <T, E : Entityc> content(name: String, entityClass: Class<E>, factory: Func<String, T>): T {
        val prov = Prov<Entityc> {
            entityClass.getDeclaredConstructor().newInstance() as Entityc
        }
        val entityId = EntityMapping.register(name, prov)
        entityNames.put(entityClass, name)
        entityClasses.put(name, entityClass as Class<out Entityc>)
        entityProviders.put(name, prov)

        // Store the entity ID on the entity class so classId() returns the right value
        UnitPhysicEntityBridge.entityId = entityId

        return factory.get(name)
    }

    fun register() {
        // Placeholder — entities are registered lazily via content() calls in AeroUnitTypes
    }
}