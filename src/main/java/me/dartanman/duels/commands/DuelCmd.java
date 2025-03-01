package me.dartanman.duels.commands;

import me.dartanman.duels.Duels;
import me.dartanman.duels.commands.subcommands.*;
import me.dartanman.duels.commands.subcommands.arena.*;
import me.dartanman.duels.commands.subcommands.duel.JoinDuelsSubCmd;
import me.dartanman.duels.commands.subcommands.duel.KitsDuelsSubCmd;
import me.dartanman.duels.commands.subcommands.duel.LeaveDuelsSubCmd;
import me.dartanman.duels.commands.subcommands.duel.SpectateDuelsSubCmd;
import me.dartanman.duels.commands.subcommands.stats.GetStatsDuelsSubCmd;
import me.dartanman.duels.commands.subcommands.stats.LeaderboardDuelsSubCmd;
import me.dartanman.duels.commands.subcommands.stats.LoadStatsDuelsSubCmd;
import me.dartanman.duels.commands.subcommands.stats.StatsFileToSQLCmd;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DuelCmd implements CommandExecutor
{
    public DuelCmd(Duels plugin)
    {
        new JoinDuelsSubCmd(plugin);
        new ListArenasDuelsSubCmd(plugin);
        new HelpDuelsSubCmd(plugin);
        new CreateArenaDuelsSubCmd(plugin);
        new SetLobbyDuelsSubCmd(plugin);
        new SetSpawn1DuelsSubCmd(plugin);
        new SetSpawn2DuelsSubCmd(plugin);
        new FinishArenaDuelsSubCmd(plugin);
        new LeaveDuelsSubCmd(plugin);
        new KitsDuelsSubCmd(plugin);
        new LoadStatsDuelsSubCmd(plugin);
        new GetStatsDuelsSubCmd(plugin);
        new LeaderboardDuelsSubCmd(plugin);
        new StatsFileToSQLCmd(plugin);
        new ArenasUISubCmd(plugin);
        new MainMenuSubCmd(plugin);
        new SpectateDuelsSubCmd(plugin);
        new PauseArenasSubCmd(plugin);
        new SetArenaBetAmount(plugin);
    }

    // CommandExecutor
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if(args.length == 0)
        {
            Bukkit.dispatchCommand(sender, "duels menu");
            return true;
        }
        else
        {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
            for(DuelsSubCommand subCommand : DuelsSubCommand.getSubCommands())
            {
                if(subCommand.is(args[0]))
                {
                    return subCommand.execute(sender, subArgs);
                }
            }
            sender.sendMessage("Unknown duels command");
            return true;
        }
    }
}
