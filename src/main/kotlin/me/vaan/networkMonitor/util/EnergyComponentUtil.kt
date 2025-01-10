package me.vaan.networkMonitor.util

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor
import org.bukkit.Location

fun EnergyNetComponent.isMachineActive(l: Location) : Boolean {
    //This isn't very reliable as end-game machines which are faster will show as offline if clicked when a recipe just completed
    if (this is MachineProcessHolder<*>) {
        val processor = this.machineProcessor as MachineProcessor<MachineOperation>
        val op = processor.getOperation(l)
        return op != null
    }

    return true
}

//It is time for some warcrimes.
val SlimefunItem.energyConsumption : Int? get() {
    //get the most specific possible class representation
    val actualClass = this.javaClass.cast(this)
    try {
        val getEnergy = this.javaClass.getDeclaredMethod("getEnergyConsumption") ?: return null
        return getEnergy.invoke(actualClass) as Int
    } catch (_: Exception) {
        return null
    }
}