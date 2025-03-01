package me.dartanman.duels.game.arenas;

import me.dartanman.duels.*;
import me.dartanman.duels.game.Countdown;
import me.dartanman.duels.game.Game;
import me.dartanman.duels.game.GameState;
import me.dartanman.duels.game.kits.KitManager;
import me.dartanman.duels.stats.db.StatisticsDatabase;
import me.dartanman.duels.utils.PlayerRestoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import com.crafteconomy.blockchain.CraftBlockchainPlugin;
import com.crafteconomy.blockchain.api.IntegrationAPI;
import com.crafteconomy.blockchain.utils.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Arena
{

    private final Duels plugin;

    private final int id;
    private final String name;

    private GameState gameState;

    private final List<UUID> players;
    private final Location spawnOne;
    private final Location spawnTwo;
    private final Location lobby;
    private Game game;
    private Countdown countdown;
    private int countdownSeconds;

    // TODO: show in UI as well
    private long uTokenBetAmount;

    private final HashSet<UUID> pendingSigns;

    private static final IntegrationAPI api = CraftBlockchainPlugin.getAPI();

    public Arena(Duels plugin, int id, String name, Location spawnOne, Location spawnTwo, Location lobby, int countdownSeconds, long uTokenBetAmount)
    {
        this.plugin = plugin;

        this.id = id;
        this.name = name;
        
        this.pendingSigns = new HashSet<>(2);

        this.gameState = GameState.IDLE;

        this.spawnOne = spawnOne;
        this.spawnTwo = spawnTwo;
        this.lobby = lobby;
        this.players = new ArrayList<>();
        this.game = new Game(this);
        this.countdownSeconds = countdownSeconds;
        this.countdown = new Countdown(plugin, this, countdownSeconds);
        this.uTokenBetAmount = uTokenBetAmount;
    }

    public Arena(Duels plugin, ArenaConfig arenaConfig)
    {
        this.plugin = plugin;

        this.id = arenaConfig.getId();
        this.name = arenaConfig.getName();

        this.gameState = GameState.IDLE;
        
        this.pendingSigns = new HashSet<>(2);

        this.spawnOne = arenaConfig.getSpawnOne();
        this.spawnTwo = arenaConfig.getSpawnTwo();
        this.lobby = arenaConfig.getLobby();
        this.players = new ArrayList<>();
        this.game = new Game(this);
        this.countdownSeconds = arenaConfig.getCountdownSeconds();
        this.countdown = new Countdown(plugin, this, arenaConfig.getCountdownSeconds());
    }

    /*
     * Gameplay
     */

    public void reset()
    {        
        this.game = new Game(this);
        
        this.countdown = new Countdown(plugin, this, countdownSeconds);
        for(UUID uuid : this.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if(p != null) {
                PlayerRestoration.restorePlayer(p, false);

                for (PotionEffect effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                } 
            }
        }
        
        players.clear();
        pendingSigns.clear();
        gameState = GameState.IDLE;
    }

    public void start()
    {
        gameState = GameState.COUNTDOWN;
        countdown.start();
    }

    /*
     * Arena Utilities
     */

    public void sendMessage(String message)
    {
        for(UUID uuid : players)
        {
            Player player = Bukkit.getPlayer(uuid);
            if(player != null)
            {
                player.sendMessage(message);
            }
        }
    }

    public KitManager getKitManager()
    {
        return plugin.getKitManager();
    }

    public StatisticsDatabase getStatisticsDatabase()
    {
        return plugin.getStatisticsManager().getStatsDB();
    }

    /*
     * Players
     */

    public void addPlayer(Player player)
    {
        PlayerRestoration.savePlayer(player);
        players.add(player.getUniqueId());
        player.teleport(lobby);

        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setMaxHealth(20);
        player.setHealth(20);
        player.setFoodLevel(20);

        // remove from pending
        pendingSigns.remove(player.getUniqueId());

        if(players.size() == 2)
        {
            start();
        }
    }

    public void removePlayer(Player player)
    {
        players.remove(player.getUniqueId());
        pendingSigns.remove(player.getUniqueId());
        if(gameState == GameState.COUNTDOWN)
        {            
            countdown.cancel();
            sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(plugin.getConfig().getString("Messages.Player-Left-Cancelled"))));
            gameState = GameState.IDLE;
        }

        // TODO: Refund player for leaving go here or just in countdown? (i think here since its not a kill)
        api.faucetUCraft(player.getUniqueId(), "refunding leaving payment", getuTokenBetAmount());
        
        Util.colorMsg(player.getUniqueId(), "&aSince you left early you will be getting back " + (getuTokenBetAmount()/1_000_000) + " coins.");

        this.countdown = new Countdown(plugin, this, countdownSeconds);
    }

    public long getuTokenBetAmount() {
        return uTokenBetAmount;
    }
    
    public Optional<String> addPendingSigner(Player player)
    {
        // check if there are already 2 pending signers, if so error
        if(pendingSigns.size() == 2) {            
            return Optional.of(Util.color("&cThere are already 2 pending signers for this arena"));
            // else if pending == 1 and players.size() is one already sgind
        } else if (pendingSigns.size() == 1 && players.size() == 1) {            
            return Optional.of(Util.color("&cThere is already 1 pending signer for this arena."));
            // else there are 0 signers and already 2 players
        } else if (players.size() == 2) {
            return Optional.of(Util.color("&cThere are already 2 players in this arena."));
        }

        pendingSigns.add(player.getUniqueId());

        // create a bukkit runnable which in X seconds removes the player from pending signers
        // this is incase the person does not sign, so it opens it up to someone else.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingSigns.remove(player.getUniqueId());
        }, 20 * Duels.EXPIRE_SECONDS);

        return Optional.empty();
    }

    public boolean removePendingSigner(UUID uuid)
    {
        return pendingSigns.remove(uuid);
    }

    public List<UUID> getPlayers()
    {
        return players;
    }

    public Location getSpawnOne()
    {
        return spawnOne;
    }

    public Location getSpawnTwo()
    {
        return spawnTwo;
    }

    public Location getLobby()
    {
        return lobby;
    }

    public UUID getPlayerOne()
    {
        return players.get(0);
    }

    public UUID getPlayerTwo()
    {
        return players.get(1);
    }
    public String getPlayerOneName()
    {
        return getPlayerName(getPlayerOne());
    }

    public String getPlayerTwoName()
    {
        return getPlayerName(getPlayerTwo());
    }

    public HashSet<UUID> getPendingSigners()
    {
        return pendingSigns;
    }

    /*
     * Arena Info
     */

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public Game getGame()
    {
        return game;
    }

    public GameState getGameState()
    {
        return gameState;
    }

    public void setGameState(GameState gameState)
    {
        this.gameState = gameState;
    }

    // static mewthod to get players in game name
    public static String getPlayerName(UUID uuid)
    {
        Player p = Bukkit.getPlayer(uuid);
        if(p != null)
        {
            return p.getName();
        }
        return uuid.toString();
    }
}
