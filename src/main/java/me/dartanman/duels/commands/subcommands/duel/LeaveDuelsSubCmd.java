package me.dartanman.duels.commands.subcommands.duel;

import me.dartanman.duels.Duels;
import me.dartanman.duels.commands.subcommands.DuelsSubCommand;
import me.dartanman.duels.game.GameState;
import me.dartanman.duels.game.arenas.Arena;
import me.dartanman.duels.utils.PlayerRestoration;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.crafteconomy.blockchain.utils.Util;

import java.util.Objects;

public class LeaveDuelsSubCmd extends DuelsSubCommand
{
    public LeaveDuelsSubCmd(Duels plugin)
    {
        super(plugin, "leave");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        // if(!sender.hasPermission("duels.leave"))
        // {
        //     noPerm(sender);
        //     return true;
        // }
        if(sender instanceof Player player)
        {

            // if player is spectator, move them to lobby and set as gamemode survival
            if(player.getGameMode() == GameMode.SPECTATOR)
            {
                player.setGameMode(GameMode.SURVIVAL);                
                Bukkit.dispatchCommand(player, "spawn");                
                Util.colorMsg(sender, "&aYou have left spectating!");
                return true;
            }

            if(args.length == 0)
            {
                Arena arena = plugin.getArenaManager().getArena(player);
                if(arena == null)
                {                    
                    player.sendMessage(ChatColor.RED + "You are not currently in a Duel!");
                    return true;
                }
                else
                {
                    PlayerRestoration.restorePlayer(player, true);

                    if(arena.getGameState() == GameState.PLAYING)
                    {
                        arena.getGame().kill(player);
                    }
                    else
                    {
                        arena.removePlayer(player);
                    }
                }
                return true;
            }
            else
            {
                incorrectArgs(sender, "/duels leave");
                return true;
            }
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "You must be a Player to do that!");
            return false;
        }
    }
}
