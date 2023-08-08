package me.dartanman.duels;

import me.dartanman.duels.commands.DuelTabCompleter;
import me.dartanman.duels.game.kits.KitManager;
import me.dartanman.duels.listeners.ArenaListener;
import me.dartanman.duels.game.arenas.Arena;
import me.dartanman.duels.game.arenas.ArenaManager;
import me.dartanman.duels.commands.DuelCmd;
import me.dartanman.duels.listeners.GameListener;
import me.dartanman.duels.listeners.StatsListener;
import me.dartanman.duels.stats.StatisticsManager;
import me.dartanman.duels.stats.db.DatabaseType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.crafteconomy.blockchain.CraftBlockchainPlugin;
import com.crafteconomy.blockchain.api.IntegrationAPI;
import com.crafteconomy.blockchain.transactions.Tx;
import com.crafteconomy.blockchain.utils.Util;

public class Duels extends JavaPlugin
{

    public static final int BET_AMOUNT = 1_000_000; // save this in the arena itself in the future, so people can set bets.
    public static final int EXPIRE_SECONDS = 60;

    private ArenaManager arenaManager;
    private KitManager kitManager;
    private StatisticsManager statisticsManager;

    public List<Tx> pendingTxs;

    IntegrationAPI api;

    @Override
    public void onEnable()
    {
        api = CraftBlockchainPlugin.getAPI();
        pendingTxs = new ArrayList<>();
        
        int pluginId = 12801;
        new Metrics(this, pluginId);

        getConfig().options().copyDefaults(true);
        saveConfig();

        this.arenaManager = new ArenaManager(this);
        this.kitManager = new KitManager(this);
        setupStatisticsManager();

        getCommand("duel").setExecutor(new DuelCmd(this));
        getCommand("duel").setTabCompleter(new DuelTabCompleter(this));

        getServer().getPluginManager().registerEvents(new ArenaListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new StatsListener(this), this);
    }

    // on disable, clear all pending signers from games
    @Override
    public void onDisable()
    {        
        for(Tx tx : pendingTxs)
        {            
            api.expireTransaction(tx.getTxID());            
            Player player = Bukkit.getPlayer(tx.getFromUUID());
            if(player != null)
            {
                player.sendMessage("A pending transaction has been force expired due to a plugin reload.");                
                player.sendMessage(tx.getDescription());
            }
        }

        for(Arena arena : arenaManager.getArenaList())
        {                        
            for(UUID uuid : arena.getPlayers()) {
                api.faucetUCraft(uuid, "returning funds from reloaded plugin", BET_AMOUNT);

                Util.colorMsg(uuid, "\n&c&l[!] &fThe plugin has reloaded. Since you were in a game, your funds are being returned to you within 15 seconds.\n");

            }            

            arena.reset();
        }
    }

    private void setupStatisticsManager()
    {
        String storageType = getConfig().getString("Statistics.Storage-Type");
        assert storageType != null;
        if(storageType.equalsIgnoreCase("sql") || storageType.equalsIgnoreCase("mysql"))
        {
            this.statisticsManager = new StatisticsManager(this, DatabaseType.SQL);
        }
        else if (storageType.equalsIgnoreCase("yaml") || storageType.equalsIgnoreCase("yml"))
        {
            this.statisticsManager = new StatisticsManager(this, DatabaseType.YAML);
        }
        else
        {
            Bukkit.getLogger().warning("Duels does not recognize the Storage Option '" + storageType + "'. Opting for YAML (flat-file) storage.");
            this.statisticsManager = new StatisticsManager(this, DatabaseType.YAML);
        }
    }

    public ArenaManager getArenaManager()
    {
        return arenaManager;
    }

    public KitManager getKitManager()
    {
        return kitManager;
    }

    public StatisticsManager getStatisticsManager()
    {
        return statisticsManager;
    }

}

