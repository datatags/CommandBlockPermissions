package me.Datatags.CommandBlockPermissions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

public class CommandListListener extends PacketAdapter {
	private static final PacketType ALLOWED_PACKET = PacketType.Play.Server.COMMANDS;
	private CommandBlockPermissions cbp;
	public CommandListListener(CommandBlockPermissions cbp) {
		super(cbp, ALLOWED_PACKET);
		this.cbp = cbp;
	}
	public void onPacketSending(PacketEvent e) {
		if (e.getPacketType() != ALLOWED_PACKET) {
			cbp.getLogger().warning("Invalid packet type on command listener: " + e.getPacketType());
			return;
		}
		cbp.savePlayerCommands(e.getPlayer(), e.getPacket());
	}
	
}
