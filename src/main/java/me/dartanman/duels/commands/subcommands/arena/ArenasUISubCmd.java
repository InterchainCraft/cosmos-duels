package me.dartanman.duels.commands.subcommands.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.crafteconomy.blockchain.utils.Util;

import me.dartanman.duels.Duels;
import me.dartanman.duels.commands.subcommands.DuelsSubCommand;
import me.dartanman.duels.game.GameState;
import me.dartanman.duels.game.arenas.Arena;
import me.dartanman.duels.utils.UIHelper;

public class ArenasUISubCmd extends DuelsSubCommand implements Listener
{

    private String InvName = Util.color("&8&lDuels Arena List");
	private int rows = 6 * 9;
    private Map<GameState, Material> stateToMaterial;

    public ArenasUISubCmd(Duels plugin)
    {
        super(plugin, "arenas");

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        stateToMaterial = new HashMap<GameState, Material>() {
			{
				put(GameState.IDLE, Material.LIME_CONCRETE);
				put(GameState.COUNTDOWN, Material.YELLOW_CONCRETE);
				put(GameState.PLAYING, Material.RED_CONCRETE);
			}
		};
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {        
        if(!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to use this command.");
			return true;
		}

        Player p = (Player) sender;
		p.openInventory(generateUI());
        return true;
    }

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack clicked = event.getCurrentItem();

		if (event.getView().getTitle().equalsIgnoreCase(Util.color(InvName))) {
			if (clicked == null) {
				return;
			}
			
			event.setCancelled(true);

			List<String> lore = clicked.getItemMeta().getLore();
			if(lore.size() < 1) {
				return;
			}
			String spliter = Util.color("&fArena: &f");

			String arenaName = lore.get(0).split(spliter)[1];			
			
			event.getWhoClicked().closeInventory();				
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				Bukkit.dispatchCommand(event.getWhoClicked(), "duels join " + arenaName);
			}, 1);
		
			
		}
	}

	private Inventory generateUI() {
		Inventory inv = Bukkit.createInventory(null, rows, InvName);

		for (Arena a : plugin.getArenaManager().getArenaList()) {
			Integer slot = a.getId() - 1;
			String name = a.getName();
			GameState state = a.getGameState();

			List<String> playerNames = new ArrayList<String>();
			for (UUID uuid : a.getPlayers()) {
				playerNames.add(getNameFromUUID(uuid));
			}

			// create the lore
			List<String> lore = new ArrayList<String>();
			lore.add("&fArena: &f" + name);
			lore.add("&fid: &e" + Integer.toString(slot));
			lore.add("&7");
			lore.add("&fPlayers: ");

			for (int i = 0; i < 2; i++) {
				if (i < playerNames.size()) {
					lore.add("&7- &e" + playerNames.get(i));
				} else {
					lore.add("&7- &e");
				}
			}

			String itemName = "Error";
			// switch case statement on state
			switch (state) {
				case IDLE:
					itemName = "&a&lJoin Arena: " + a.getId();
					break;
				case COUNTDOWN:
					itemName = "&e&lArena Starting: " + a.getId();
					break;
				case PLAYING:
					itemName = "&c&lArena Live: " + a.getId();
					break;
				default:
					break;
			}

			// create the value in the UI
			UIHelper.createDisplay(inv, stateToMaterial.get(state), slot, itemName, lore);
		}

		return inv;
	}

	private String getNameFromUUID(UUID uuid) {
		Player p = Bukkit.getPlayer(uuid);
		if (p != null) {
			return p.getName();
		}

		return uuid.toString();
	}
}


