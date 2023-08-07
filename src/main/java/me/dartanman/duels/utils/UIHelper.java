package me.dartanman.duels.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.crafteconomy.blockchain.utils.Util;

public class UIHelper {
    public static void createDisplay(Inventory inv, Material material, int Slot, String name, List<String> lore) {
		ArrayList<String> Lore = new ArrayList<String>();
		
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Util.color(name));
				
		String DefaultLoreColor = "&7";
		
		for(String l : lore) {			
			if(!DefaultLoreColor.equalsIgnoreCase("")) {
				l = DefaultLoreColor + l;
			}
						
			Lore.add(Util.color(l));
		}        
		
		meta.setLore(Lore);
		item.setItemMeta(meta);
		 
		inv.setItem(Slot, item); 
	}
}
