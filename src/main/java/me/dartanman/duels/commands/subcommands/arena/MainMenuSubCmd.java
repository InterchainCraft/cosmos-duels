package me.dartanman.duels.commands.subcommands.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import me.dartanman.duels.utils.UIHelper;

public class MainMenuSubCmd extends DuelsSubCommand implements Listener
{
	private static Inventory menuInv;
    private String InvName = Util.color("&8&lDuels Menu");
	private int rows = 3 * 9;

    private Map<Integer, String> slotToCommand;

    public MainMenuSubCmd(Duels plugin)
    {
        super(plugin, "menu");

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

		menuInv = Bukkit.createInventory(null, rows, InvName);

        UIHelper.createDisplay(menuInv, Material.GRASS_BLOCK, 10, "&e&lSpawn", new ArrayList<String>());
        UIHelper.createDisplay(menuInv, Material.BOOK, 12, "&f&lArenas", new ArrayList<String>());
        // UIHelper.createDisplay(menuInv, Material.DIAMOND_SWORD, 14, "&a&lJoin Duel", new ArrayList<String>());
        UIHelper.createDisplay(menuInv, Material.IRON_CHESTPLATE, 14, "&7&oKits Coming Soon...", new ArrayList<String>());

        // create an array which links the slot number to the command to run for the player on click
        slotToCommand = new HashMap<Integer, String>();
        slotToCommand.put(10, "spawn");
        slotToCommand.put(12, "duels arenas");
        slotToCommand.put(14, "duels kits list");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {        
        if(!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to use this command.");
			return true;
		}

        Player p = (Player) sender;
 		p.openInventory(menuInv);
 		 				
        return true;
    }

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack clicked = event.getCurrentItem();		
						
		if(event.getView().getTitle().equalsIgnoreCase(Util.color(InvName))) {
			if(clicked == null) {
				return;
			}

			// if the slot is in slotToCommand, then close the UI and run said command for player
			if(slotToCommand.containsKey(event.getSlot()))
			{
				event.setCancelled(true);
				event.getWhoClicked().closeInventory();				
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					Bukkit.dispatchCommand(event.getWhoClicked(), slotToCommand.get(event.getSlot()));
				}, 1);
			}

			event.setCancelled(true);
		}
	}
}


