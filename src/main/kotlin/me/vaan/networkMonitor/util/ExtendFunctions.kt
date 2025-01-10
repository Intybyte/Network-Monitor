package me.vaan.networkMonitor.util

import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import org.bukkit.Location
import java.util.*
import kotlin.NoSuchElementException

val Location.energyNetwork : EnergyNet? get() {
    val network: Optional<EnergyNet>

    try {
        network = Slimefun.getNetworkManager().getNetworkFromLocation(this, EnergyNet::class.java)
        if (network.isEmpty) return null
    } catch (e: NoSuchElementException) {
        return null
    }

    return network.get()
}

operator fun Int.plus(b: Boolean) : Int {
    return if (b) this + 1 else this
}