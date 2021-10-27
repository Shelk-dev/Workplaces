package com.shelk;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.CropState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.material.Crops;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.shelk.utils.Utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;

@SuppressWarnings("deprecation")
public class Workplaces extends JavaPlugin implements Listener, CommandExecutor {

	private static Economy econ = null;

	public static HashMap<String, Location> workplaces = new HashMap<>();

	public static HashMap<Player, PlayerData> playerWorkplaces = new HashMap<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		Bukkit.getPluginManager().registerEvents(this, this);
		getCommand("arbete").setExecutor(this);
		if (!setupEconomy()) {
			Logger.getLogger("Minecraft").severe(
					String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		workplaces.put("Farming", Utils.getLocationString(getConfig().getString("Workplaces.Farming.Circle-Location")));
		workplaces.put("Fishing", Utils.getLocationString(getConfig().getString("Workplaces.Fishing.Circle-Location")));
		workplaces.put("Mining", Utils.getLocationString(getConfig().getString("Workplaces.Mining.Circle-Location")));
		workplaces.put("Mob-Hunting",
				Utils.getLocationString(getConfig().getString("Workplaces.Mob-Hunting.Circle-Location")));

		new BukkitRunnable() {
			public void run() {
				for (Player p : playerWorkplaces.keySet())
					updateActionBar(p);
			}
		}.runTaskTimer(this, 0, 20);

	}

	@Override
	public void onDisable() {
		for (Player p : playerWorkplaces.keySet()) {
			leaveWorkplace(p);
		}

	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	@EventHandler
	public void onSneak(PlayerToggleSneakEvent e) {
		Player p = e.getPlayer();
		for (Entry<String, Location> workplace : workplaces.entrySet()) {

			if (p.getLocation().distance(workplace.getValue()) < 3 && !playerWorkplaces.containsKey(p)) {
				enterWorkplace(p, workplace.getKey());
			}

		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		for (Player player : playerWorkplaces.keySet()) {
			e.getPlayer().hidePlayer(this, player);
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		leaveWorkplace(e.getPlayer());

	}

	public void updateActionBar(Player p) {
		if (playerWorkplaces.containsKey(p)) {
			PlayerData data = playerWorkplaces.get(p);
			int timeSpent = (int) Math.ceil((System.currentTimeMillis() - data.getTimeJoined()) / 1000);
			double moneyMade = data.getMoneyMade();

			String actionBar = getConfig().getString("Messages.Money-Earned-Actionbar").replace("&", "§");
			LocalTime time =  LocalTime.ofSecondOfDay(timeSpent);
			String addSecond = "";
			if (time.getSecond() == 0) addSecond = ":00";
			actionBar = actionBar.replace("{time}", LocalTime.ofSecondOfDay(timeSpent).toString() + addSecond);
			actionBar = actionBar.replace("{amount}", moneyMade + "");

			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBar));

		}
	}

	public void enterWorkplace(Player p, String workplace) {
		// Setup invisibility
		playerWorkplaces.put(p, new PlayerData(workplace, System.currentTimeMillis()));
		p.sendMessage(getConfig().getString("Messages.Entered-" + workplace + "-Workplace").replace("&", "§"));
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.hidePlayer(this, p);
		}
		p.spigot().setCollidesWithEntities(false);
		updateActionBar(p);
	}

	public void leaveWorkplace(Player p) {
		for (Player player : Bukkit.getOnlinePlayers())
			player.showPlayer(p);
		p.spigot().setCollidesWithEntities(true);
		playerWorkplaces.remove(p);
	}

	@EventHandler
	public void onKill(EntityDeathEvent e) {
		if (playerWorkplaces.containsKey(e.getEntity().getKiller())) {
			PlayerData data = playerWorkplaces.get(e.getEntity().getKiller());
			if (data.getWorkplace().equals("Mob-Hunting")) {
				int price = getConfig().getInt("Workplaces.Mob-Hunting." + e.getEntity().getType().name() + ".Price");
				if (price != 0) {
					data.setMoneyMade(data.getMoneyMade() + price);
					econ.depositPlayer(e.getEntity().getKiller(), price);
				}
			}
		}

	}

	@EventHandler
	public void onFish(PlayerFishEvent e) {
		if (playerWorkplaces.containsKey(e.getPlayer())) {
			PlayerData data = playerWorkplaces.get(e.getPlayer());
			if (data.getWorkplace().equals("Fishing")) {
				if (e.getCaught() != null) {
					Item item = (Item) e.getCaught();
					item.remove();
					e.setExpToDrop(0);

					String caught = getConfig().getStringList("Workplaces.Fishing.Fish-Names").get(
							new Random().nextInt(getConfig().getStringList("Workplaces.Fishing.Fish-Names").size()));
					e.getPlayer()
							.sendMessage((getConfig().getString("Messages.Got-Fish-Message").replace("{fish}", caught))
									.replace("&", "§"));
					data.setAmountCollected(data.getAmountCollected() + 1);
					if (data.getAmountCollected() == getConfig().getInt("Workplaces.Fishing.Amount")) {
						data.setAmountCollected(0);
						data.setMoneyMade(data.getMoneyMade() + getConfig().getDouble("Workplaces.Fishing.Price"));
						econ.depositPlayer(e.getPlayer(), getConfig().getDouble("Workplaces.Fishing.Price"));
					}
					updateActionBar(e.getPlayer());
				}
			}
		}
	}

	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		if (playerWorkplaces.containsKey(e.getPlayer())) {
			PlayerData data = playerWorkplaces.get(e.getPlayer());
			if (data.getWorkplace().equals("Mining")) {
				if (e.getPlayer().getInventory().getItemInMainHand().getType().name()
						.contains(getConfig().getString("Workplaces.Mining.Pickaxe-To-Receive"))) {
					data.setAmountCollected(data.getAmountCollected() + 1);
					if (data.getAmountCollected() == getConfig().getInt("Workplaces.Mining.Amount")) {
						data.setAmountCollected(0);
						data.setMoneyMade(data.getMoneyMade() + getConfig().getDouble("Workplaces.Mining.Price"));
						econ.depositPlayer(e.getPlayer(), getConfig().getDouble("Workplaces.Mining.Price"));
						updateActionBar(e.getPlayer());
					}

				}
			}
			if (data.getWorkplace().equals("Farming")) {
				if (e.getBlock().getType() == Material.WHEAT) {
					Crops crop = (Crops) e.getBlock().getState().getData();
					if (crop.getState() == CropState.RIPE) {
						e.getBlock().setType(Material.AIR);

						new BukkitRunnable() {
							public void run() {
								e.getBlock().setType(Material.WHEAT);
							}
						}.runTaskLater(this, 1);

						data.setAmountCollected(data.getAmountCollected() + 1);
						if (data.getAmountCollected() == getConfig().getInt("Workplaces.Farming.Amount")) {
							data.setAmountCollected(0);
							data.setMoneyMade(data.getMoneyMade() + getConfig().getDouble("Workplaces.Farming.Price"));
							econ.depositPlayer(e.getPlayer(), getConfig().getDouble("Workplaces.Farming.Price"));
							updateActionBar(e.getPlayer());
						}
					}
				}
			}

		}
	}

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (arg0 instanceof Player) {
			if (arg3.length > 1) {
				if (arg3[0].equals("join")) {
					Player p = (Player) arg0;
					String workplace = arg3[1];
					if (workplace.equals("Farming") || workplace.equals("Mining") || workplace.equals("Fishing")
							|| workplace.equals("Mob-Hunting")) {
						if (playerWorkplaces.containsKey(p)) {
							if (playerWorkplaces.get(p).getWorkplace().equals(workplace)) {
								p.sendMessage(getConfig().getString("Command.Already-In").replace("&", "§"));
								return false;
							}
							leaveWorkplace(p);
						}
						p.teleport(Utils.getLocationString(
								getConfig().getString("Workplaces." + workplace + ".Circle-Location")));
						enterWorkplace(p, workplace);
					} else
						arg0.sendMessage(getConfig().getString("Command.Workplace-Not-Found").replace("&", "§"));
				} else
					arg0.sendMessage(getConfig().getString("Command.Usage").replace("&", "§"));

			} else
				arg0.sendMessage(getConfig().getString("Command.Usage").replace("&", "§"));
		}
		return false;
	}

}
