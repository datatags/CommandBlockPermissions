package me.Datatags.CommandBlockPermissions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
	private CommandBlockPermissions cbp;
	private Material listeningBlock = null;
	public PlayerListener(CommandBlockPermissions cbp) {
		this.cbp = cbp;
		String substitute = cbp.getConfig().getString("command-substitute");
		if (substitute.equalsIgnoreCase("disable")) {
			return;
		}
		Material block = Material.matchMaterial(substitute);
		if (block == null || !block.isBlock()) {
			cbp.getLogger().warning("Invalid substitution block in config, that feature will be disabled.");
			return;
		}
		cbp.getLogger().info("Will transform " + block.toString() + " into COMMAND_BLOCK when placed.");
		listeningBlock = block;
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (cbp.getConfig().getBoolean("auto-enable") && e.getPlayer().hasPermission(CommandBlockPermissions.USE_PERMISSION) && !e.getPlayer().isOp()) {
			cbp.enablePlayer(e.getPlayer());
		}
	}
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		// no errors happen if you remove something that doesn't exist anyway
		cbp.removePlayer(e.getPlayer().getUniqueId());
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlace(BlockPlaceEvent e) {
		if (!cbp.isPlayerEditing(e.getPlayer())) return;
		if (e.getBlock().getType() == listeningBlock) {
			BlockData oldData = e.getBlock().getState().getBlockData();
			e.getBlock().setType(Material.COMMAND_BLOCK);
			Directional data = (Directional) e.getBlock().getBlockData();
			if (oldData instanceof Directional) {
				data.setFacing(((Directional) oldData).getFacing());
				e.getBlock().setBlockData(data);
				return;
			}
			Location playerLoc = e.getPlayer().getEyeLocation();
			Location blockLoc = e.getBlock().getLocation();
			Location diff = playerLoc.subtract(blockLoc);
			// if the Y difference is bigger than both the X and Z difference
			if (Math.abs(diff.getY()) > Math.max(Math.abs(diff.getX()), Math.abs(diff.getZ()))) {
				data.setFacing(diff.getY() > 0 ? BlockFace.UP : BlockFace.DOWN);
			} else {
				data.setFacing(e.getPlayer().getFacing().getOppositeFace());
			}
			e.getBlock().setBlockData(data);
		}
	}
}
