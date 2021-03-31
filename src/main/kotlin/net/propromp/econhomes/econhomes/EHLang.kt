package net.propromp.economypro

import net.propromp.econhomes.econhomes.EconHomes
import net.propromp.util.CustomConfig
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.lang.reflect.Method

class EHLang(plugin: EconHomes) {
    private var langFiles = HashMap<String, FileConfiguration>()
    private var hasLocaleSupport = hasLocaleMethod()
    private fun hasLocaleMethod():Boolean{
        Player::class.java.methods.forEach {
            if (it.name == "getLocale")
                return true
        }
        return false
    }

    init {
        langFiles["en_us"] = CustomConfig(plugin, "lang/en_US.yml").let { it.saveDefaultConfig();it.config }
        langFiles["ja_jp"] = CustomConfig(plugin, "lang/ja_JP.yml").let { it.saveDefaultConfig();it.config }
        langFiles["else"] = CustomConfig(plugin, "lang/else.yml").let { it.saveDefaultConfig();it.config }
    }

    fun get(sender: CommandSender, key: String): String {
        var config: FileConfiguration = langFiles["else"]!!
        if(hasLocaleSupport) {
            if (sender is Player) {
                langFiles[sender.locale]?.let {
                    config = it
                }
            }
        }
        return config.getString(key)!!.replace("&", "§")
    }
}