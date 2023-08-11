package me.dartanman.duels.commands.subcommands.arena;

import java.util.ArrayList;
import java.util.List;
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

    public ArenasUISubCmd(Duels plugin)
    {
        super(plugin, "arenas");

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
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
			
			Arena arena = plugin.getArenaManager().getArena(arenaName);
			if(arena == null) {
				event.getWhoClicked().sendMessage(Util.color("&cError: &fArena "+arenaName+" not found."));
				return;
			}

			final String subCmd;
			if(arena.getGameState() == GameState.IDLE) {
				subCmd = "duels join " + arenaName;
			} else {
				subCmd = "duels spectate " + arenaName;	
			}

			event.getWhoClicked().closeInventory();	
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				Bukkit.dispatchCommand(event.getWhoClicked(), subCmd);
			}, 1);
		}
	}

	private Inventory generateUI() {
		Inventory inv = Bukkit.createInventory(null, rows, InvName);

		for (Arena a : plugin.getArenaManager().getArenaList()) {
			Integer slot = a.getId() - 1;			
						
			Material statusMat = getArenaMaterialStatusBlock(a);	
			String title = getArenaTitle(a);
			List<String> lore = getArenaItemLore(a);	
			
			UIHelper.createDisplay(inv, statusMat, slot, title, lore);
		}

		return inv;
	}

	private String getArenaTitle(Arena a) {
		String itemName = "Error ";		
		switch (a.getGameState()) {
			case IDLE:
				itemName = "&a&lJoin Arena: ";
				break;
			case COUNTDOWN:
				itemName = "&e&lArena Starting: ";
				break;
			case PLAYING:
				itemName = "&c&lArena Live: ";
				break;
			default:
				break;
		}

		return itemName + a.getName();
	}

	private List<String> getArenaItemLore(Arena a) {
		List<String> lore = new ArrayList<String>();
		lore.add("&fArena: &f" + a.getName());		
		lore.add("&7");
		lore.add("&fPending Signers: &f" + getPendingSignersNames(a).toString());
		lore.add("&7");
		lore.add("&fPlayers: ");

		List<String> playerNames = new ArrayList<String>();
		for (UUID uuid : a.getPlayers()) {
			playerNames.add(getNameFromUUID(uuid));
		}

		for (int i = 0; i < 2; i++) {
			if (i < playerNames.size()) {
				lore.add("&7- &e" + playerNames.get(i));
			} else {
				lore.add("&7- &e");
			}
		}	

		lore.add("&7");
		lore.add("&7&o(( id: " + Integer.toString(a.getId()) + "&7&o ))");

		return lore;
	}

	private Material getArenaMaterialStatusBlock(Arena a) {
		switch (a.getGameState()) {
			case IDLE:
				if (a.getPlayers().size() == 0) {
					return Material.LIME_CONCRETE;
				} else {
					return Material.YELLOW_CONCRETE;
				}
			case COUNTDOWN:
				return Material.ORANGE_CONCRETE;
			case PLAYING:
				return Material.RED_CONCRETE;
			default:
				return Material.BARRIER;
		}
	}

	private List<String> getPendingSignersNames(Arena a) {
		List<String> names = new ArrayList<String>();
		for (UUID uuid : a.getPendingSigners()) {
			names.add(getNameFromUUID(uuid));
		}

		return names;
	}

	private String getNameFromUUID(UUID uuid) {
		Player p = Bukkit.getPlayer(uuid);
		if (p != null) {
			return p.getName();
		}

		return uuid.toString();
	}
}


