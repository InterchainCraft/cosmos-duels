package me.dartanman.duels.commands.subcommands.duel;

import me.dartanman.duels.Duels;
import me.dartanman.duels.commands.subcommands.DuelsSubCommand;
import me.dartanman.duels.game.GameState;
import me.dartanman.duels.game.arenas.Arena;
import me.dartanman.duels.game.arenas.ArenaManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import com.crafteconomy.blockchain.CraftBlockchainPlugin;
import com.crafteconomy.blockchain.api.IntegrationAPI;
import com.crafteconomy.blockchain.core.types.ErrorTypes;
import com.crafteconomy.blockchain.transactions.Tx;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class JoinDuelsSubCmd extends DuelsSubCommand
{

    IntegrationAPI api;

    public JoinDuelsSubCmd(Duels plugin)
    {
        super(plugin, "join");
        api = CraftBlockchainPlugin.getAPI();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("duels.join"))
        {
            noPerm(sender);
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // get their wallet
        String wallet = api.getWallet(uuid);
        if(wallet == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull("You do not have a wallet set. Please set one with /wallet set <wallet address>")));
            return true;
        }


        // if args.length != 1, error
        if(args.length != 1) {
            incorrectArgs(sender, "/duels join [arena-name]");
            return true;
        }

        // check if arena name is valid
        if(plugin.getArenaManager().getArena(args[0]) == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInvalid arena name: " + args[0]));
            return true;
        }

        payForArenaPurchase(plugin, api, player, args[0]);
        return true;
    }

    private static void payForArenaPurchase(Duels plugin, IntegrationAPI api, Player player, String arenaName) {
        Tx txinfo = new Tx(); // getTxID() -> auto generated. just a UUID [/wallet pending shows all]
        txinfo.setFromUUID(player.getUniqueId());
        txinfo.setToWalletAsServer(); // maybe a contract in the future?
        txinfo.setUCraftAmount(1_000_000); // bets of just 1 JUNOX (1mil ujunox)
        txinfo.setDescription("Purchase bet for 1JUNOX");
        txinfo.setFunction(purchaseSingleBet(plugin, plugin.getArenaManager(), player, arenaName));
        
        txinfo.setRedisMinuteTTL(15);

        txinfo.setIncludeTxClickable(true);
        txinfo.setSendDescMessage(true);
        txinfo.setSendWebappLink(true);

        player.sendMessage("");
        ErrorTypes error = txinfo.submit();
        if(error == ErrorTypes.SUCCESS) {
            // api.sendTxIDClickable(player, txinfo.getTxID().toString(), "\n&eBetting Transaction created successfully\n%value%");
        } else {            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "Error: " + error.toString()));
        }
    }
    
    public static Consumer<UUID> purchaseSingleBet(Duels plugin, ArenaManager am, Player player, String arenaName) {
        Consumer<UUID> purchase = (uuid) -> {  

            String name = getNameIfOnline(uuid);                
            // put logic here for business license
            Bukkit.broadcastMessage("[COMPLETE] Purchase bet for: " + name + " == " + uuid.toString() + "\n"); 

            if(am.getArena(player) != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou have already joined an arena"));                
                return;
            }            

            Arena arena = am.getArena(arenaName);            
            if(arena == null)
            {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInvalid arena id: " + arenaName));
                return;
            }
            
            if(arena.getGameState() != GameState.IDLE)
            {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cArena is not available"));
                return;
            }

            // required due to teleport
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    arena.addPlayer(player);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aJoined arena: " + arena.getName()));
                }
            }, 1L);

            return;

        };
        return purchase;
    }

    private static String getNameIfOnline(UUID uuid) {
        String playername = "";
        // check if the UUID is online
        Player player = Bukkit.getPlayer(uuid);
        if(player != null) {
            playername = player.getName();
        }
        return playername;
    }

    // private Arena findFirstArena()
    // {
    //     for(Arena arena : plugin.getArenaManager().getArenaList())
    //     {
    //         if(arena.getGameState() == GameState.IDLE)
    //         {
    //             return arena;
    //         }
    //     }
    //     return null;
    // }
}
