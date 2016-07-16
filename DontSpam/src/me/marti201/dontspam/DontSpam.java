package me.marti201.dontspam;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import me.marti201.dontspam.Metrics.Graph;

public class DontSpam extends JavaPlugin implements Listener, CommandExecutor {

	public static boolean onlyToPlayer = true;
	public static boolean blockCaps = true;
	public static String dontSpamChat = ChatColor.RED + "Don't spam!";
	public static String dontSpamCommands = ChatColor.RED + "Don't spam!";
	public static int allowedRepeatsChat = 3;
	public static int allowedRepeatsCommands = 3;
	public static String capsMessage = ChatColor.RED + "DON'T SHOUT";

	HashMap<UUID, SpamPlayer> players = new HashMap<UUID, SpamPlayer>();

	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getCommand("dontspam").setExecutor(this);

		saveDefaultConfig();

		conf();
		
		//Metrics
		setupMetrics();
	}

	private void conf() {
		// Loading the configuration
		FileConfiguration conf = getConfig();

		onlyToPlayer = conf.getBoolean("sendMessageToPlayer");
		dontSpamChat = ChatColor.translateAlternateColorCodes('&', conf.getString("spamChatMessage"));
		dontSpamCommands = ChatColor.translateAlternateColorCodes('&', conf.getString("spamCommandsMessage"));
		allowedRepeatsChat = conf.getInt("allowedRepeatsChat");
		allowedRepeatsCommands = conf.getInt("allowedRepeatsCommands");
		blockCaps = conf.getBoolean("blockCaps");
		capsMessage = ChatColor.translateAlternateColorCodes('&', conf.getString("capsMessage"));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		boolean canReload = (sender instanceof ConsoleCommandSender || sender.isOp()
				|| sender.hasPermission("dontspam.reload"));
		if (canReload && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			reloadConfig();
			conf();
			sender.sendMessage(ChatColor.YELLOW + "Don't spam!" + ChatColor.GREEN + " configuration reloaded");
		} else {
			sender.sendMessage(
					ChatColor.YELLOW + "Don't spam!" + ChatColor.GREEN + " v1.0 by " + ChatColor.YELLOW + "Marti201");
			if (canReload)
				sender.sendMessage(ChatColor.GREEN + "Type " + ChatColor.YELLOW + "/dontspam reload" + ChatColor.GREEN
						+ " to reload the config.");
		}
		return true;
	}

	// Overriding other chat plugins to see if the event is cancelled
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(AsyncPlayerChatEvent e) {
		if (e.isCancelled())
			return; // No need to do anything

		Player p = e.getPlayer();

		if (p.isOp() || p.hasPermission("dontspam.bypass"))
			return; // Don't do anything if the player is OP or has the
		// permission

		//Block if the message is sent in all caps
		if(blockCaps && e.getMessage().equals(e.getMessage().toUpperCase())){
			p.sendMessage(capsMessage);
			e.setCancelled(true);
		}

		if (!players.containsKey(p.getUniqueId())) {
			// Adding the player to the HashMap
			players.put(p.getUniqueId(), new SpamPlayer());
		}

		// Getting the SpamPlayer instance
		SpamPlayer sp = players.get(p.getUniqueId());

		if (sp.processChat(e.getMessage().toLowerCase())) {
			if (onlyToPlayer) {
				// Send the message only to the player who sent it
				e.getRecipients().clear();
				e.getRecipients().add(p);
			} else {
				// If onlyToPlayer is disabled, just cancel the event
				p.sendMessage(dontSpamChat);
				e.setCancelled(true);
			}
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (e.isCancelled())
			return; // No need to do anything
		Player p = e.getPlayer();

		if (p.isOp() || p.hasPermission("dontspam.bypass"))
			return; // Don't do anything if the player is OP or has permission

		if (!players.containsKey(p.getUniqueId())) {
			// Adding the player to the HashMap
			players.put(p.getUniqueId(), new SpamPlayer());
		}

		// Getting the SpamPlayer instance
		SpamPlayer sp = players.get(p.getUniqueId());

		if (sp.processCommand(e.getMessage().toLowerCase())) {
			//Cancel the event
			p.sendMessage(dontSpamCommands);
			e.setCancelled(true);
		}

	}
	
	//mcstats.org
	private void setupMetrics(){
		try {
		    Metrics metrics = new Metrics(this);
		    
		    //onlyToPlayer graph

		    Graph onlyPlayerGraph = metrics.createGraph("When a message is blocked, the player who sent it still receives it");
		    
		    onlyPlayerGraph.addPlotter(new Metrics.Plotter("Enabled") {
				
				@Override
				public int getValue() {
					return onlyToPlayer ? 1 : 0;
				}
			});
		    
		    onlyPlayerGraph.addPlotter(new Metrics.Plotter("Disabled") {
				
				@Override
				public int getValue() {
					return onlyToPlayer ? 0 : 1;
				}
			});
		    
		    //capsMessage graph
		    
		    Graph capsGraph = metrics.createGraph("Block messages if written with caps lock enabled");
		    
		    capsGraph.addPlotter(new Metrics.Plotter("Enabled") {
				
				@Override
				public int getValue() {
					return blockCaps ? 1 : 0;
				}
			});
		    
		    capsGraph.addPlotter(new Metrics.Plotter("Disabled") {
				
				@Override
				public int getValue() {
					return blockCaps ? 0 : 1;
				}
			});

		    metrics.start();
		} catch (IOException e) {
		    getLogger().warning("Exception while sending metrics: "+e.getMessage());
		}
	}
	
	

}
