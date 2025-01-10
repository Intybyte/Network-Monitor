package me.vaan.networkMonitor

import io.github.seggan.sf4k.AbstractAddon
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class NetworkMonitorPlugin : AbstractAddon() {

    companion object {
        lateinit var instance: NetworkMonitorPlugin
            private set
        lateinit var metrics: Metrics
            private set
        lateinit var pluginFolder: File
            private set
    }

    override suspend fun onEnableAsync() {
        instance = this
        pluginFolder = this.dataFolder
        metrics = Metrics(this, 0) //TODO: add service id
        saveDefaultConfig()

        GuideStuff.register()
    }

    override suspend fun onDisableAsync() {

    }

    override fun getJavaPlugin(): JavaPlugin {
        return this
    }

    override fun getBugTrackerURL(): String {
        return "https://github.com/Intybyte/NetworkMonitor/issues"
    }
}
