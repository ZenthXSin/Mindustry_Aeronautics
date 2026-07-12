package aero.entity.dyn4j

import arc.struct.Seq
import mindustry.entities.units.StatusEntry

/**
 * MindustryX 添加到单位组件接口的方法的二进制兼容性合约。
 * 仅引用原版 Mindustry 类型，因此同一个 mod jar 在 v158 上仍然可用。
 */
interface MindustryXUnitCompat {
    fun statuses(): Seq<StatusEntry>

    fun healthBalance(): Float

    fun healthChanged()
}
