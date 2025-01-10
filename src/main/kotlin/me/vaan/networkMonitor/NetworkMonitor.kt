package me.vaan.networkMonitor

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction
import me.vaan.networkMonitor.data.MachineMap
import me.vaan.networkMonitor.data.MultipleMachineData
import me.vaan.networkMonitor.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*


class NetworkMonitor(
    itemGroup: ItemGroup,
    item: SlimefunItemStack,
    recipeType: RecipeType,
    recipe: Array<ItemStack?>
) : SlimefunItem(itemGroup, item, recipeType, recipe), EnergyNetComponent {

    companion object {
        val BORDER: IntArray = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
        val END_BORDER: IntArray = intArrayOf(45, 46, 48, 49, 50, 52, 53)
        const val PREV: Int = 47
        const val NEXT: Int = 51
        const val AMOUNT: Int = 36
    }

    override fun preRegister() {
        val handler = BlockUseHandler { blockUserHandler: PlayerRightClickEvent ->
            val location: Location
            val player = blockUserHandler.player

            try {
                location = blockUserHandler.clickedBlock.orElseThrow().location
            } catch (e: NoSuchElementException) {
                blockUserHandler.cancel()
                return@BlockUseHandler
            }

            val network: EnergyNet = location.energyNetwork ?: return@BlockUseHandler
            getGUI(player, 0, network).open(player)
        }

        addItemHandler(handler)

    }

    private fun processNetworkMachines(supply: Map<Location, EnergyNetComponent>) : MachineMap {
        val print = MachineMap()

        for ((location, cmp) in supply) {
            if (cmp !is SlimefunItem) {
                continue
            }

            val entry = print.getOrDefault(cmp, MultipleMachineData())
            entry.amount++
            entry.active += cmp.isMachineActive(location)
            entry.storedEnergy += cmp.getCharge(location)
            print[cmp] = entry
        }

        return print
    }

    private fun getDisplayMachines(network: EnergyNet) : List<ItemStack> {
        val itemList = LinkedList<ItemStack>()

        val consumers = processNetworkMachines(network.consumers)
        for ((sf, data) in consumers) {
            val item = sf.item.clone()
            val consumer = sf as EnergyNetComponent
            val lore = LinkedList<Component>()

            lore += text("")
            lore += "<white><b>Amount:</b> ${data.amount} </white>".mini()
            lore += "<white><b>Active:</b> ${data.active} </white>".mini()
            lore += "<white><b>Capacity:</b> ${data.amount * consumer.capacity} </white>".mini()
            lore += "<white><b>Stored Energy:</b> ${data.storedEnergy} </white>".mini()

            val consumption = sf.energyConsumption
            if (consumption != null) {
                lore += "<white><b>Max Consumption:</b> ${data.amount * consumption} </white>".mini()
                lore += "<white><b>Present Consumption:</b> ${data.active * consumption} </white>".mini()
            }

            lore += text("")
            val actualLore = lore.map { it.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE) }
            item.editMeta {
                it.lore(actualLore)
            }

            itemList += item
        }

        return itemList
    }

    private fun getGUI(p: Player, page: Int, network: EnergyNet): ChestMenu {
        val chestMenu = ChestMenu(itemName)

        val blank = ChestMenuUtils.getEmptyClickHandler()
        val background = ChestMenuUtils.getBackground()

        for (i in BORDER) {
            chestMenu.addItem(i, background, blank)
        }

        val displayMachines = getDisplayMachines(network)
        val pages: Int = displayMachines.size / AMOUNT

        val start: Int = page * AMOUNT
        val end: Int = start + AMOUNT

        for (i in start until end) {
            val slot: Int = i % AMOUNT + 9

            if (displayMachines.size - 1 <= i) break

            chestMenu.addItem(slot, displayMachines[i], blank)
        }

        chestMenu.addItem(
            PREV, ChestMenuUtils.getPreviousButton(p, page, pages)
        ) { p1: Player, _: Int, _: ItemStack?, _: ClickAction? ->
            val prev = page - 1
            if (prev == -1) return@addItem false

            getGUI(p1, prev, network).open(p1)
            true
        }

        chestMenu.addItem(
            NEXT, ChestMenuUtils.getNextButton(p, page, pages)
        ) { p1: Player, _: Int, _: ItemStack?, _: ClickAction? ->
            val next = page + 1
            if (next > pages) return@addItem false

            getGUI(p1, next, network).open(p1)
            true
        }

        for (i in END_BORDER) {
            chestMenu.addItem(i, background, blank)
        }

        return chestMenu
    }

    override fun getEnergyComponentType(): EnergyNetComponentType {
        return EnergyNetComponentType.CONNECTOR
    }

    override fun getCapacity(): Int {
        return 0
    }
}
