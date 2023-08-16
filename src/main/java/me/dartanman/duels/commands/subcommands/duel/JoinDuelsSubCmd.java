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
import com.crafteconomy.blockchain.utils.Util;

import java.util.Objects;
import java.util.Optional;
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

        // ensure the user is not already pending for another arena
        if(plugin.getArenaManager().getPendingArena(player) != null) {
            player.sendMessage(Util.color("\n&c&l[!] Error&7: &fYou are already pending for another arena.\n"));            
            Util.clickableCommand(sender, "/wallet clearpending", "\n&7&o&nClick to clear pending Transactions");
            return true;
        }

        // if args.length != 1, error
        if(args.length != 1) {
            incorrectArgs(sender, "/duels join [arena-name]");
            return true;
        }

        // check if arena name is valid
        Arena a = plugin.getArenaManager().getArena(args[0]);
        if(a == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInvalid arena name: " + args[0]));
            return true;
        }
        
        Optional<String> errorRes = a.addPendingSigner(player);
        if(errorRes.isPresent()) {
            player.sendMessage(errorRes.get());
            return true;
        }

        payForArenaPurchase(plugin, api, player, args[0]);
        return true;
    }

    // TODO: Variable bets here
    private static void payForArenaPurchase(Duels plugin, IntegrationAPI api, Player player, String arenaName) {
        Tx txinfo = new Tx(); // getTxID() -> auto generated. just a UUID [/wallet pending shows all]
        txinfo.setFromUUID(player.getUniqueId());
        txinfo.setToWalletAsServer(); // contract in the future?        
        txinfo.setUCraftAmount(Duels.BET_AMOUNT); // bets of just 1 JUNOX (1mil ujunox)        
        txinfo.setDescription("Purchased 1 bet for arena " + arenaName);
        txinfo.setFunction(purchaseSingleBet(plugin, plugin.getArenaManager(), player, arenaName));
                
        txinfo.setRedisMinuteTTL((int) Math.ceil(Duels.EXPIRE_SECONDS/60));
        txinfo.setConsumerOnExpire(expiredTime(plugin, txinfo, plugin.getArenaManager(), arenaName));

        txinfo.setIncludeTxClickable(true);
        txinfo.setSendDescMessage(true);
        txinfo.setSendWebappLink(true);

        // save txinfo.getTxID() to a pending array after submit is successful. This way we can clear all later on reload & msg the members

        player.sendMessage("");
        ErrorTypes error = txinfo.submit();
        if(error == ErrorTypes.SUCCESS) {
            // api.sendTxIDClickable(player, txinfo.getTxID().toString(), "\n&eBetting Transaction created successfully\n%value%");
            plugin.pendingTxs.add(txinfo);

            // let the player know they have Duels.addPendingSigner seconds to sign the Tx
            player.sendMessage(Util.color("\n&c[PENDING] &fYou have &e" + Duels.EXPIRE_SECONDS + " seconds &fto sign the Tx. &7&o(or it expires)"));            

        } else {            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "Error: " + error.toString()));
        }        
    }

    public static Consumer<UUID> expiredTime(Duels plugin, Tx txinfo, ArenaManager am, String arenaName) {
        // send a player that their bid for arenaName expired. Then remove them from pending
        Consumer<UUID> expired = (uuid) -> {
            String name = getNameIfOnline(uuid);
            if(name.isEmpty()) {
                return;
            }
            Util.colorMsg(uuid, "\n&c&l[!] Error&7: &fYour bet purchased for arena: " + arenaName + " expired.\n");
            am.getArena(arenaName).removePendingSigner(uuid); 
                        
            plugin.pendingTxs.remove(txinfo);
        };
        return expired;
    }
    
    
    public static Consumer<UUID> purchaseSingleBet(Duels plugin, ArenaManager am, Player player, String arenaName) {
        Consumer<UUID> purchase = (uuid) -> {  

            // String name = getNameIfOnline(uuid);                               
            Util.colorMsg(uuid, "&aSuccess&7: &fYour bet purchase was processed!\n");

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
}
