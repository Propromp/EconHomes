package net.propromp.econhomes.econhomes

import net.milkbowl.vault.economy.Economy
import net.propromp.economypro.EHLang
import net.propromp.util.CustomConfig
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.HashMap
import org.bukkit.ChatColor.RED as red
import org.bukkit.ChatColor.GOLD as gold
import org.bukkit.ChatColor.AQUA as aqua


class EconHomes : JavaPlugin() {
    var homes = HashMap<UUID, MutableMap<String, Location>>()
    var lang = EHLang(this)
    var econ: Economy? = null
    var tpPrice = 0.0
    var setPrice = 0.0
    var limit = 0
    lateinit var homeConfig: CustomConfig
    override fun onEnable() {
        if (!setupEconomy()) {
            logger.info(String.format("[%s] - no Vault dependency found!", getDescription().getName()));
        }
        saveDefaultConfig()
        tpPrice = config.getDouble("price.tp")
        setPrice = config.getDouble("price.set")
        limit=config.getInt("limit")


        //configのロード
        homeConfig = CustomConfig(this, "homes.yml")
        homeConfig.saveDefaultConfig()
        homeConfig.config.getKeys(false).forEach { uuid ->
            homeConfig.config.getConfigurationSection(uuid)!!.getKeys(false).forEach { name ->
                val loc = homeConfig.config.getLocation("$uuid.$name")
                if (homes[UUID.fromString(uuid)] == null) {
                    homes[UUID.fromString(uuid)] = mutableMapOf(Pair(name!!, loc!!))
                } else {
                    homes[UUID.fromString(uuid)]!![name] = loc!!
                }
            }
        }
    }

    override fun onDisable() {
        //configのセーブ
        homes.keys.forEach {
            homeConfig.config.set(it.toString(), homes[it])
        }
        homeConfig.saveConfig()
    }

    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        val rsp = server.servicesManager.getRegistration(
            Economy::class.java) ?: return false
        econ = rsp.provider
        return econ != null
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            when (label) {
                "home" -> {
                    if (econ?.has(sender, tpPrice) == true) {
                        var homename = if (args.isEmpty()) {
                            "default"
                        } else {
                            args[0]
                        }
                        if (homes[sender.uniqueId] == null || homes[sender.uniqueId]?.get(homename) == null) {
                            sender.sendMessage(red.toString()+lang.get(sender, "not_found"))
                            return true
                        }
                        sender.teleport(homes[sender.uniqueId]!![homename]!!)
                        sender.playSound(sender.location, Sound.ENTITY_ENDERMAN_TELEPORT,1f,1f)
                        econ?.withdrawPlayer(sender, tpPrice)
                        sender.sendMessage(aqua.toString()+lang.get(sender, "home.1").replace("%val1%", gold.toString()+homename+aqua.toString()))
                        econ?.let {
                            sender.sendMessage("(${it.format(tpPrice)})")
                        }
                        return true
                    } else {
                        sender.sendMessage(lang.get(sender, "no_money").replace("%val1%", tpPrice.toString()))
                        return true
                    }
                }
                "sethome" -> {
                    var homename = if (args.isEmpty()) {
                        "default"
                    } else {
                        args[0]
                    }
                    if (limit != 0 && (homes[sender.uniqueId]?.size ?: 0) >= limit) {
                        sender.sendMessage(red.toString()+lang.get(sender, "toomuchhome"))
                        return true
                    }
                    if (econ?.has(sender, setPrice) == true) {
                        if (homes[sender.uniqueId] == null) {
                            homes[sender.uniqueId] = mutableMapOf(Pair(homename, sender.location))
                        } else {
                            homes[sender.uniqueId]!![homename] = sender.location
                        }
                        econ?.withdrawPlayer(sender, setPrice)
                        sender.sendMessage(aqua.toString()+
                            lang.get(sender, "sethome.1")
                                .replace("%val1%", gold.toString()+homename+aqua.toString())
                                .replace("%val2%", "${gold}${sender.location.x},${sender.location.y},${sender.location.z}${aqua}")
                        )
                        econ?.let {
                            sender.sendMessage("(${it.format(setPrice)})")
                        }
                        return true
                    } else {
                        sender.sendMessage(lang.get(sender, "no_money").replace("%val1%", setPrice.toString()))
                        return true
                    }
                }
                "delhome" -> {
                    var homename = if (args.isEmpty()) {
                        "default"
                    } else {
                        args[0]
                    }
                    if (homes[sender.uniqueId] == null || homes[sender.uniqueId]?.get(homename) == null) {
                        sender.sendMessage(red.toString()+lang.get(sender, "not_found"))
                        return true
                    }
                    homes[sender.uniqueId]!!.remove(homename)
                    sender.sendMessage(aqua.toString()+lang.get(sender, "delhome.1").replace("%val1%", gold.toString()+homename+aqua.toString()))
                }
            }
        }
        return super.onCommand(sender, command, label, args)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): MutableList<String> {
        return when (command.label) {
            "home", "delhome" -> {
                if (sender is Player) {
                    homes[sender.uniqueId]?.let { return it.keys.toMutableList() }
                    return mutableListOf()
                } else {
                    mutableListOf()
                }
            }
            else -> mutableListOf()
        }
    }
}