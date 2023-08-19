package me.dartanman.duels.commands.subcommands.arena;

import me.dartanman.duels.Duels;
import me.dartanman.duels.commands.subcommands.DuelsSubCommand;
import me.dartanman.duels.game.arenas.ArenaConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SetArenaBetAmount extends DuelsSubCommand
{
    public SetArenaBetAmount(Duels plugin)
    {
        super(plugin, "setbet");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("duels.createarena"))
        {
            noPerm(sender);
            return true;
        }
        if(sender instanceof Player player)
        {

            if(args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /duels setbet <amount>");
                return true;
            }
            
            UUID uuid = player.getUniqueId();
            ArenaConfig arenaConfig = ArenaConfig.creationMap.get(uuid);
            if(arenaConfig == null) {
                player.sendMessage(ChatColor.RED + "You must do " + ChatColor.YELLOW + "/duels createarena <arena name> " + ChatColor.RED + "before doing that!");
                return true;
            }

            // get arg[1] and convert it to a long if possible
            long betAmount = 0;
            try {
                betAmount = Long.parseLong(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "You must enter a valid number!");
                return true;
            }

            arenaConfig.setuTokenBetAmount(betAmount);
            player.sendMessage(ChatColor.GREEN + "Bet amount set to" + betAmount + " uTokens!");
            return true;
            
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "You must be a Player to do that!");
            return false;
        }
    }
}
