package me.dartanman.duels.commands.subcommands.duel;

import me.dartanman.duels.Duels;
import me.dartanman.duels.commands.subcommands.DuelsSubCommand;
import me.dartanman.duels.game.arenas.Arena;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.crafteconomy.blockchain.utils.Util;

public class SpectateDuelsSubCmd extends DuelsSubCommand {        

    public SpectateDuelsSubCmd(Duels plugin)
    {        
        super(plugin, "spectate");        
    }

        @Override
    public boolean execute(CommandSender sender, String[] args)
    {        
        if(!(sender instanceof Player)) {            
            Util.colorMsg(sender, "&cOnly players can use this command!");
            return true;
        }
        
        if(args.length != 1) {
            incorrectArgs(sender, "/duels spectate <arena>");
            return true;
        }
                
        Arena arena = plugin.getArenaManager().getArena(args[0]);
        if(arena == null) {            
            Util.colorMsg(sender, "&cInvalid arena name: " + args[0]);
            return true;
        }
        
        Player player = (Player) sender;
        if(plugin.getArenaManager().getArena(player) != null) {
            Util.colorMsg(sender, "&cYou are already in a game!");
            return true;
        }        
        
        player.teleport(arena.getLobby());    
        Util.colorMsg(sender, "&aSpectating arena: " + arena.getName());

        return true;
    }
}