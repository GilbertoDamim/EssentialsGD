package github.gilbertokpl.essentialsk.commands

import github.gilbertokpl.essentialsk.configs.GeneralLang
import github.gilbertokpl.essentialsk.data.SpawnData
import github.gilbertokpl.essentialsk.manager.ICommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandSetSpawn  : ICommand {
    override val consoleCanUse: Boolean = false
    override val permission: String = "essentialsk.commands.setspawn"
    override val minimumSize = 0
    override val maximumSize = 0
    override val commandUsage = listOf("/setspawn")

    override fun kCommand(s: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        SpawnData("spawn").setSpawn((s as Player).location)
        s.sendMessage(GeneralLang.getInstance().spawnSendSetMessage)
        return false
    }
}