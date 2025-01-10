package me.vaan.networkMonitor

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

object GuideStuff {
    val itemGroup = ItemGroup(NamespacedKey(NetworkMonitorPlugin.instance, "network_monitor"), CustomItemStack(Material.GLOWSTONE, "§6Network Monitor"))
    val monitor = SlimefunItemStack("NETWORK_MONITOR", HeadTexture.GEO_SCANNER, "Network Monitor")

    fun register() {
        NetworkMonitor(itemGroup, monitor, RecipeType.ENHANCED_CRAFTING_TABLE,
            arrayOf(
                null, ItemStack(Material.REDSTONE_BLOCK), null,
                null, SlimefunItems.ENERGY_REGULATOR, null,
                null, SlimefunItems.ENERGY_CONNECTOR, null
            )
        ).register(NetworkMonitorPlugin.instance)
    }
}