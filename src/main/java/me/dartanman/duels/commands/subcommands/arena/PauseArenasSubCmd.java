package me.dartanman.duels.commands.subcommands.arena;

import me.dartanman.duels.Duels;
import me.dartanman.duels.commands.subcommands.DuelsSubCommand;
import org.bukkit.command.CommandSender;

import com.crafteconomy.blockchain.utils.Util;

public class PauseArenasSubCmd extends DuelsSubCommand {
    public PauseArenasSubCmd(Duels plugin) {
        super(plugin, "pause");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("duels.pause")) {
            noPerm(sender);
            return true;
        }

        Duels.PAUSED = !Duels.PAUSED;
        Util.colorMsg(sender, "&7&o&nDuels are now " + (Duels.PAUSED ? "&cpaused" : "&aunpaused") + "&7&o&n.");

        return true;
    }
}
