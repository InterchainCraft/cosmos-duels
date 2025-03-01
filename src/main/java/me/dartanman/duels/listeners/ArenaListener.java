package me.dartanman.duels.listeners;

import me.dartanman.duels.Duels;
import me.dartanman.duels.game.GameState;
import me.dartanman.duels.utils.PlayerRestoration;
import me.dartanman.duels.game.arenas.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ArenaListener implements Listener
{

    private final Duels plugin;

    public ArenaListener(Duels plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        
        // if player is spectator, move them to lobby and set as gamemode survival
        if(player.getGameMode() == GameMode.SPECTATOR)
        {
            player.setGameMode(GameMode.SURVIVAL);                
            Bukkit.dispatchCommand(player, "spawn");            
        }

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if(pdc.has(new NamespacedKey(plugin, "world"), PersistentDataType.STRING))
        {
            String worldName = pdc.get(new NamespacedKey(plugin, "world"), PersistentDataType.STRING);
            int x = pdc.get(new NamespacedKey(plugin, "x"), PersistentDataType.INTEGER);
            int y = pdc.get(new NamespacedKey(plugin, "y"), PersistentDataType.INTEGER);
            int z = pdc.get(new NamespacedKey(plugin, "z"), PersistentDataType.INTEGER);
            assert worldName != null;
            World world = Bukkit.getWorld(worldName);
            player.teleport(new Location(world, x, y, z));
            pdc.remove(new NamespacedKey(plugin, "world"));
            pdc.remove(new NamespacedKey(plugin, "x"));
            pdc.remove(new NamespacedKey(plugin, "y"));
            pdc.remove(new NamespacedKey(plugin, "z"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArena(player);
        if(arena != null)
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
    }

}
