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

import com.crafteconomy.blockchain.api.IntegrationAPI;
import com.crafteconomy.blockchain.utils.Util;

import me.dartanman.duels.Duels;
import me.dartanman.duels.commands.subcommands.DuelsSubCommand;
import me.dartanman.duels.utils.UIHelper;

public class MainMenuSubCmd extends DuelsSubCommand implements Listener
{	
    private String InvName = Util.color("&8&lDuels Menu");
	private int rows = 6 * 9;

	private static IntegrationAPI api = IntegrationAPI.getInstance();

    private Map<Integer, String> slotToCommand;

    public MainMenuSubCmd(Duels plugin)
    {
        super(plugin, "menu");

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        // create an array which links the slot number to the command to run for the player on click
        slotToCommand = new HashMap<Integer, String>();
        slotToCommand.put(10, "spawn");
        slotToCommand.put(12, "duels arenas");
        slotToCommand.put(14, "duels kits list");
        slotToCommand.put(16, "gencode"); // from integration plugin		
        slotToCommand.put(28, "duels stats"); // from integration plugin		
    }

    @Override
    public boolean execute(CommandSender sender, String[] args)
    {        
        if(!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to use this command.");
			return true;
		}

        Player p = (Player) sender;
 		p.openInventory(generateMainUI(p));
 		 				
        return true;
    }

	private Inventory generateMainUI(Player p) {
		Inventory menuInv = Bukkit.createInventory(null, rows, InvName);

        UIHelper.createDisplay(menuInv, Material.GRASS_BLOCK, 10, "&e&lSpawn", new ArrayList<String>());
        UIHelper.createDisplay(menuInv, Material.DIAMOND_SWORD, 12, "&f&lArenas", new ArrayList<String>());        
        UIHelper.createDisplay(menuInv, Material.IRON_CHESTPLATE, 14, "&7&oKits Coming Soon...", new ArrayList<String>());
        UIHelper.createDisplay(menuInv, Material.BOOK, 16, "&e&lConnect Wallet", new ArrayList<String>());
        UIHelper.createDisplay(menuInv, Material.NAME_TAG, 28, "&e&lYour Duel Statistics", new ArrayList<String>());		

		api.getCraftBalance(p.getUniqueId()).thenAccept(balance -> {
			UIHelper.createDisplay(menuInv, Material.GOLD_INGOT, 34, "&e&l"+api.getTokenName()+" balance: " + balance, new ArrayList<String>());
		});

		return menuInv;
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


