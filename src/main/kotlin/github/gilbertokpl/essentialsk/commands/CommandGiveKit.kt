package github.gilbertokpl.essentialsk.commands

import github.gilbertokpl.essentialsk.EssentialsK
import github.gilbertokpl.essentialsk.configs.GeneralLang
import github.gilbertokpl.essentialsk.data.DataManager
import github.gilbertokpl.essentialsk.manager.ICommand
import github.gilbertokpl.essentialsk.util.ItemUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CommandGiveKit : ICommand {
    override val consoleCanUse: Boolean = true
    override val commandName = "givekit"
    override val timeCoolDown: Long? = null
    override val permission: String = "essentialsk.commands.givekit"
    override val minimumSize = 2
    override val maximumSize = 2
    override val commandUsage = listOf("/givekit <playerName> <kitName>")

    override fun kCommand(s: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        //check length of kit name
        if (args[0].length > 16) {
            s.sendMessage(GeneralLang.getInstance().kitsNameLength)
            return false
        }

        val dataInstance = DataManager.getInstance().kitCacheV2[args[1]]

        //check if not exist
        if (dataInstance == null) {
            s.sendMessage(GeneralLang.getInstance().kitsNotExist)
            return false
        }

        //check if player not exist
        val p = EssentialsK.instance.server.getPlayer(args[0]) ?: run {
            s.sendMessage(GeneralLang.getInstance().generalPlayerNotOnline)
            return false
        }

        //give kit
        ItemUtil.getInstance().giveKit(p, dataInstance.items, true, drop = true)
        p.sendMessage(GeneralLang.getInstance().kitsGiveKitMessage.replace("%kit%", dataInstance.fakeName))
        return false
    }
}