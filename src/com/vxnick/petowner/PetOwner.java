package com.vxnick.petowner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class PetOwner extends JavaPlugin implements Listener {
	public static Permission perms = null;
	public static HashMap<String, HashMap<String, String>> petAction = new HashMap<String, HashMap<String, String>>();
	
	@Override
	public void onEnable() {
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable() {

	}
	
	@EventHandler
	public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {		
		if (!petAction.containsKey(event.getPlayer().getName())) {
			return;
		}
		
		List<EntityType> validPets = Arrays.asList(EntityType.HORSE, EntityType.OCELOT, EntityType.WOLF);
		
		if (validPets.contains(event.getRightClicked().getType())) {
			Player player = event.getPlayer();
			HashMap<String, String> options = petAction.get(player.getName());
			Tameable pet = (Tameable) event.getRightClicked();
			
			// Check pet ownership
			if (options.get("type").equals("check")) {
				if (pet.getOwner() == null) {
					player.sendMessage(ChatColor.GOLD + "This animal is untamed");
				} else {
					player.sendMessage(ChatColor.GOLD + String.format("This pet belongs to %s", pet.getOwner().getName()));
				}
				
				petAction.remove(player.getName());
			}
			
			// Transfer pet ownership
			if (options.get("type").equals("set") && options.get("new-owner") != null) {
				if (pet.getOwner() == player || perms.has(player, "petowner.set.any")) {
					OfflinePlayer newOwner = getServer().getOfflinePlayer(options.get("new-owner"));
					
					pet.setOwner(newOwner);
					
					player.sendMessage(ChatColor.GOLD + String.format("This pet has been transferred to %s", newOwner.getName()));
				} else {
					player.sendMessage(ChatColor.RED + "You don't have permission to set ownership for other pets");
				}
				
				petAction.remove(player.getName());
			}
			
			// Remove pet ownership
			if (options.get("type").equals("unset")) {
				if (pet.getOwner() == player || perms.has(player, "petowner.unset.any")) {
					if (pet.getOwner() == null) {
						player.sendMessage(ChatColor.YELLOW + "This animal is already untamed");
					} else {
						String oldOwner = pet.getOwner().getName();
						
						pet.setOwner(null);
						
						player.sendMessage(ChatColor.GOLD + String.format("This animal is no longer owned by %s", oldOwner));
					}
				} else {
					player.sendMessage(ChatColor.RED + "You don't have permission to remove ownership from other pets");
				}
				
				petAction.remove(player.getName());
			}
			
			event.setCancelled(true);
		}
	}
	
	public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("petowner")) {
			String command;
			
			if (args.length > 0) {
				command = args[0].toLowerCase();
			} else {
				return true;
			}
			
			if (perms.has(sender, "petowner.check") && command.equals("check")) {
				sender.sendMessage(ChatColor.GOLD + "Right click a pet within 10 seconds to check ownership");
				
				HashMap<String, String> options = new HashMap<String, String>();
				options.put("type", "check");
				
				petAction.put(sender.getName(), options);
				
				getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						if (petAction.containsKey(sender.getName())) {
							HashMap<String, String> options = petAction.get(sender.getName());
							
							if (options.get("type").equals("check")) {
								petAction.remove(sender.getName());
								sender.sendMessage(ChatColor.YELLOW + "Pet check expired");
							}
						}
					}
				}, 10 * 20L);
			}
			
			if ((perms.has(sender, "petowner.set.own") || perms.has(sender, "petowner.set.any")) && command.equals("set")) {
				if (args.length != 2) {
					sender.sendMessage(ChatColor.RED + "Please specify a player to transfer ownership to");
					return true;
				}
				
				sender.sendMessage(ChatColor.GOLD + "Right click a pet within 10 seconds to transfer ownership");
				
				HashMap<String, String> options = new HashMap<String, String>();
				options.put("type", "set");
				options.put("new-owner", args[1]);
				
				petAction.put(sender.getName(), options);
				
				getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						if (petAction.containsKey(sender.getName())) {
							HashMap<String, String> options = petAction.get(sender.getName());
							
							if (options.get("type").equals("set")) {
								petAction.remove(sender.getName());
								sender.sendMessage(ChatColor.YELLOW + "Pet ownership transfer expired");
							}
						}
					}
				}, 10 * 20L);
			}
			
			if ((perms.has(sender, "petowner.unset.own") || perms.has(sender, "petowner.unset.any")) && command.equals("unset")) {
				sender.sendMessage(ChatColor.GOLD + "Right click a pet within 10 seconds to remove ownership");
				
				HashMap<String, String> options = new HashMap<String, String>();
				options.put("type", "unset");
				
				petAction.put(sender.getName(), options);
				
				getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						if (petAction.containsKey(sender.getName())) {
							HashMap<String, String> options = petAction.get(sender.getName());
							
							if (options.get("type").equals("unset")) {
								petAction.remove(sender.getName());
								sender.sendMessage(ChatColor.YELLOW + "Pet ownership removal expired");
							}
						}
					}
				}, 10 * 20L);
			}
		}
		
		return true;
	}
}