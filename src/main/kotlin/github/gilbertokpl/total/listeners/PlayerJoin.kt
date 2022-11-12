package github.gilbertokpl.total.listeners

import github.gilbertokpl.total.TotalEssentials
import github.gilbertokpl.total.cache.local.LoginData
import github.gilbertokpl.total.config.files.MainConfig
import github.gilbertokpl.total.cache.local.PlayerData
import github.gilbertokpl.total.cache.local.SpawnData
import github.gilbertokpl.total.config.files.LangConfig
import github.gilbertokpl.total.discord.Discord
import github.gilbertokpl.total.util.*
import github.gilbertokpl.total.util.MainUtil
import github.gilbertokpl.total.util.PermissionUtil
import github.gilbertokpl.total.util.PlayerUtil
import kotlinx.coroutines.awaitCancellation
import org.bukkit.Bukkit

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class PlayerJoin : Listener {
    @EventHandler(priority = EventPriority.HIGH)
    fun event(e: PlayerJoinEvent) {
        e.joinMessage = null

        val address = e.player.address?.address.toString()

        LoginData.attempts[e.player] = 0
        LoginData.values[e.player] = 0

        if (LoginData.ip[e.player] == address) {
            e.player.sendMessage(LangConfig.authAutoLogin)
            LoginData.loggedIn[e.player] = true
        }
        else {
            LoginUtil.loginMessage(e.player)
        }


        TotalEssentials.basePlugin.getTask().async {
            val p = e.player

            waitFor(5)

            SpawnData.teleport(p)

            val limitHome: Int = PermissionUtil.getNumberPermission(
                p,
                "totalessentials.commands.sethome.",
                MainConfig.homesDefaultLimitHomes
            )


            if (!PlayerData.checkIfPlayerExist(p)) {
                PlayerData.createNewPlayerData(e.player.name)
            }

            PlayerData.homeLimitCache[p] = limitHome

            PlayerData.values(e.player)

            if (!p.hasPermission("*")) {
                if (MainConfig.messagesLoginMessage) {
                    MainUtil.serverMessage(
                        LangConfig.messagesEnterMessage
                            .replace("%player%", p.name)
                    )
                }
                if (MainConfig.discordbotSendLoginMessage) {
                    Discord.sendDiscordMessage(
                        LangConfig.discordchatDiscordSendLoginMessage.replace("%player%", p.name),
                        true
                    )
                }
            }

            if (MainConfig.generalAntiVPN) {
                if (PlayerUtil.checkPlayer(address)[3] == "true") {
                    println("bot entrou")
                }
            }

        }
    }
}
