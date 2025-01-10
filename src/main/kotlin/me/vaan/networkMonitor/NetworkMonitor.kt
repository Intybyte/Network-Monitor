package me.vaan.networkMonitor

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.ReactorAccessPort
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.AbstractEnergyProvider
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction
import me.mrCookieSlime.Slimefun.api.BlockStorage
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

    override fun preRegister() = addItemHandler(
        BlockUseHandler { blockUserHandler: PlayerRightClickEvent ->
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
    )

    private fun processNetworkMachines(supply: Map<Location, EnergyNetComponent>) : MachineMap {
        val print = MachineMap()

        for ((l, cmp) in supply) {
            if (cmp !is SlimefunItem) {
                continue
            }

            val toProcess = if(cmp is ReactorAccessPort) {
                val reactorLocation = Location(l.world, l.x, l.y - 3, l.z)
                if (supply.containsKey(reactorLocation)) {
                    continue
                } else {
                    BlockStorage.check(reactorLocation) ?: cmp
                }
            } else {
                cmp
            }

            val netComponent = toProcess as EnergyNetComponent

            val entry = print.getOrDefault(toProcess, MultipleMachineData())
            entry.amount++

            val active = netComponent.isMachineActive(l)
            entry.active += active
            entry.storedEnergy += netComponent.getCharge(l)

            if (toProcess is EnergyNetProvider) {
                entry.energyTotal += toProcess.getGeneratedOutput(l, BlockStorage.getLocationInfo(l))
                print[cmp] = entry
                continue
            }

            if (active) {
                val value = toProcess.energyConsumption
                if (value != null) {
                    entry.energyTotal += value
                }
            }

            print[cmp] = entry
        }

        return print
    }

    private fun generalProcessing(sf: SlimefunItem, data: MultipleMachineData) : LinkedList<Component> {
        val consumer = sf as EnergyNetComponent
        val lore = LinkedList<Component>()

        lore += text("")
        lore += "<white><b>Amount:</b> ${data.amount} </white>".mini()
        lore += "<white><b>Active:</b> ${data.active} </white>".mini()

        if (0 < consumer.capacity) {
            lore += "<white><b>Capacity:</b> ${data.amount * consumer.capacity} </white>".mini()
            lore += "<white><b>Stored Energy:</b> ${data.storedEnergy} </white>".mini()
        }

        return lore
    }

    private fun getDisplayConsumers(network: EnergyNet) : List<ItemStack> {
        return getDisplayWhatever(network.consumers) { sf, data ->
            val lore = LinkedList<Component>()
            val consumption = sf.energyConsumption
            if (consumption != null) {
                lore += "<white><b>Max Consumption:</b> ${data.amount * consumption} </white>".mini()
                lore += "<white><b>Present Consumption:</b> ${data.energyTotal} </white>".mini()
            }

            lore
        }
    }

    private fun getDisplayGenerators(network: EnergyNet) : List<ItemStack> {
        return getDisplayWhatever(network.generators) { sf, data ->
            val lore = LinkedList<Component>()
            val energyProvider = sf as? AbstractEnergyProvider

            if (energyProvider != null) {
                lore += "<white><b>Max Production:</b> ${data.amount * energyProvider.energyProduction} </white>".mini()
                lore += "<white><b>Present Production:</b> ${data.active * energyProvider.energyProduction} </white>".mini()
            } else if (sf is EnergyNetProvider) {
                lore += "<white><b>Max Production:</b> Cannot calculate for this generator type. </white>".mini()
                lore += "<white><b>Present Production:</b> ${data.energyTotal} </white>".mini()
            }

            lore
        }
    }

    private fun getDisplayWhatever(
        map: Map<Location, EnergyNetComponent>,
        process: ((SlimefunItem, MultipleMachineData) -> List<Component>)? = null
    ) : List<ItemStack> {
        val itemList = LinkedList<ItemStack>()

        val consumers = processNetworkMachines(map)
        for ((sf, data) in consumers) {
            val item = sf.item.clone()
            val lore = generalProcessing(sf, data)

            if (process != null) {
                lore += process(sf, data) // specify if there is extra data to process
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

    private fun getDisplayCapacitors(network: EnergyNet) : List<ItemStack> {
        return getDisplayWhatever(network.capacitors)
    }

    private fun getDisplayMachines(network: EnergyNet) : List<ItemStack> {
        val consumerItems = getDisplayConsumers(network)
        val generatorItems = getDisplayGenerators(network)
        val capacitorItems = getDisplayCapacitors(network)

        return consumerItems + generatorItems + capacitorItems
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
