package me.Datatags.CommandBlockPermissions;

import org.bukkit.ChatColor;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

public class CommandBlockListener extends PacketAdapter {
	private CommandBlockPermissions cbp;
	private static final PacketType[] LISTENING_PACKETS = new PacketType[] {PacketType.Play.Client.SET_COMMAND_BLOCK, PacketType.Play.Client.SET_COMMAND_MINECART};
	public CommandBlockListener(CommandBlockPermissions cbp) {
		super(cbp, LISTENING_PACKETS);
		this.cbp = cbp;
	}
	@Override
	public void onPacketReceiving(PacketEvent e) {
		if (!isCorrectPacketType(e.getPacketType())) { // just in case
			cbp.getLogger().warning("Received a wrong packet type: " + e.getPacketType());
			return;
		}
		if (e.getPlayer().isOp() || (cbp.getConfig().getBoolean("enable-bypass") && e.getPlayer().hasPermission(CommandBlockPermissions.BYPASS_PERMISSION))) {
			return;
		}
		String fullCommand = e.getPacket().getStrings().read(0);
		if (fullCommand.trim().equals("")) {
			return; // I don't see any reason why blanking a command block shouldn't be allowed
		}
		String commandName = fullCommand.split(" ")[0];
		if (commandName.startsWith("/")) {
			commandName = commandName.substring(1); // remove slash if there is one
		}
		boolean whitelist = cbp.getConfig().getBoolean("whitelist");
		boolean blacklistFail = false;
		for (String testCommand : cbp.getConfig().getStringList("commands")) {
			if (commandName.equalsIgnoreCase(testCommand)) {
				if (whitelist) {
					return; // success
				} else {
					blacklistFail = true;
					break; // failed the blacklist
				}
			}
		}
		if (cbp.getConfig().getBoolean("block-colon-commands") && commandName.contains(":")) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + "You cannot use colon commands in command blocks. Please contact an administrator if you need a command override.");
			return;
		}
		if (!whitelist && !blacklistFail) {
			return; // success, we reached the end of the blacklist without any matches 
		}
		// either we failed the blacklist or we failed the whitelist
		e.setCancelled(true);
		e.getPlayer().sendMessage(ChatColor.RED + "You cannot use that command in a command block.");
	}
	private boolean isCorrectPacketType(PacketType type) {
		for (PacketType testType : LISTENING_PACKETS) {
			if (type == testType) return true;
		}
		return false;
	}
}
