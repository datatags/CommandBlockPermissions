package me.Datatags.CommandBlockPermissions;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

public class CommandBlockPermissions extends JavaPlugin implements CommandExecutor {
	public static final Permission USE_PERMISSION = new Permission("commandblockpermissions.use");
	public static final Permission BYPASS_PERMISSION = new Permission("commandblockpermissions.bypass");
	private Set<UUID> enabledPlayers = new HashSet<>(); // key is player uuid, value is previous gamemode
	private Map<UUID,PacketContainer> playerCommands = new HashMap<>();
	private ProtocolManager pm;
	private PacketContainer noCommandsPacket;
	@Override
	public void onEnable() {
		saveDefaultConfig();
		pm = ProtocolLibrary.getProtocolManager();
		Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
		pm.addPacketListener(new CommandBlockListener(this));
		if (getConfig().getBoolean("block-tab-complete")) {
			pm.addPacketListener(new CommandListListener(this));
		}
		noCommandsPacket = pm.createPacket(PacketType.Play.Server.COMMANDS);
	}
	public void removePlayer(UUID uuid) {
		enabledPlayers.remove(uuid);
	}
	public void unloadPlayer(UUID uuid) {
		removePlayer(uuid);
		playerCommands.remove(uuid);
	}
	public boolean enablePlayer(Player player) {
		boolean success = sendOpPacket(player, true);
		if (!success) {
			return false;
		}
		enabledPlayers.add(player.getUniqueId());
		if (getConfig().getBoolean("block-tab-complete")) {
			try {
				pm.sendServerPacket(player, noCommandsPacket);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	public boolean disablePlayer(Player player) {
		boolean success = sendOpPacket(player, false);
		if (!success) return false;
		if (getConfig().getBoolean("block-tab-complete")) {
			if (playerCommands.containsKey(player.getUniqueId())) {
				try {
					pm.sendServerPacket(player, playerCommands.get(player.getUniqueId()));
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					return false;
				}
			} else {
				getLogger().warning("Couldn't find cached commands list for player " + player.getName());
			}
		}
		player.setGameMode(player.getGameMode()); // resend correct
		removePlayer(player.getUniqueId());
		return true;
	}
	public boolean isPlayerEditing(Player player) {
		return enabledPlayers.contains(player.getUniqueId());
	}
	protected void savePlayerCommands(Player player, PacketContainer packet) {
		if (!playerCommands.containsKey(player.getUniqueId())) { // check first so not to overwrite with a blank value
			playerCommands.put(player.getUniqueId(), packet);
		}
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Only players can edit command blocks.");
			return true;
		}
		Player player = (Player) sender;
		if (player.isOp()) {
			sender.sendMessage(ChatColor.YELLOW + "You are already an OP, you don't need help editing command blocks.");
			return true;
		}
		if (!player.hasPermission(USE_PERMISSION)) {
			player.sendMessage(ChatColor.RED + "You cannot edit command blocks.");
			return true;
		}
		boolean enable = !enabledPlayers.remove(player.getUniqueId());
		boolean success;
		if (enable) {
			success = enablePlayer(player);
		} else {
			success = disablePlayer(player);
		}
		String prefix = enable ? "en" : "dis";
		if (success) {
			sender.sendMessage(ChatColor.GREEN + "Edit mode " + prefix + "abled");
		} else {
			sender.sendMessage(ChatColor.RED + "Failed to " + prefix + "able edit mode, please alert an administrator.");
		}
		return true;
	}
	protected ProtocolManager getProtocolManager() {
		return pm;
	}
	private boolean sendOpPacket(Player player, boolean op) {
		PacketContainer opPacket = pm.createPacket(PacketType.Play.Server.ENTITY_STATUS);
		opPacket.getIntegers().write(0, player.getEntityId());
		opPacket.getBytes().write(0, (byte)(op ? 26 : 24)); // 26 is OP L2, 24 is not OP
		try {
			pm.sendServerPacket(player, opPacket);
		} catch (InvocationTargetException ex) {
			getLogger().warning("Failed to send fake op packet to player:");
			ex.printStackTrace();
			return false;
		}
		return true;
	}
}
