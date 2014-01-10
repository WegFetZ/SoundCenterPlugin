package com.soundcenter.soundcenter.plugin.plugin.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import com.soundcenter.soundcenter.lib.data.Area;
import com.soundcenter.soundcenter.lib.data.Box;
import com.soundcenter.soundcenter.lib.data.SCLocation;
import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.plugin.messages.Messages;
import com.soundcenter.soundcenter.plugin.plugin.network.StreamManager;
import com.soundcenter.soundcenter.plugin.plugin.network.tcp.ConnectionManager;
import com.soundcenter.soundcenter.plugin.plugin.util.IntersectionDetection;

public class SCCommandExecutor implements CommandExecutor{

	private SoundCenter plugin;
	
	public SCCommandExecutor(SoundCenter plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		Player player = null;
		
		if (args.length < 1)
			return false;
		
		/* sc help */
		if (args[0].equalsIgnoreCase("help")) {
			sender.sendMessage(Messages.INFO_HELP);
			return true;
		}
		
		if (!(sender instanceof Player)) { //the following commands are only available for online players
			sender.sendMessage(Messages.ERR_SENDER_NO_PLAYER);
			return true;
		}
		
		player = (Player) sender;
		
		/* sc init */
		if (args[0].equalsIgnoreCase("init")) {
			if (sender instanceof Player)
				ConnectionManager.initialize((Player) sender);
			return true;
		
		/* sc mute */
		} else if (args[0].equalsIgnoreCase("mute")) {
			ServerUser user = SoundCenter.userList.getAcceptedUserByName(player.getName());
			if (user == null || !user.isInitialized()) {
				ConnectionManager.sendStartClientMessage(player);
				return true;
			}
			
			/* sc mute <name> */
			if (args.length > 1) {
				ServerUser userToMute = SoundCenter.userList.getAcceptedUserByName(args[1]);
				if (userToMute == null) {
					player.sendMessage(Messages.ERR_MUTE_PT1 + args[1] + Messages.ERR_MUTE_PT2);
					return true;
				}
				
				user.addMutedUser(userToMute.getId());
				player.sendMessage(Messages.INFO_USER_MUTED + args[1]);
			}
			
			player.sendMessage(Messages.CMD_USAGE_MUTE);
			
		/* sc unmute */
			} else if (args[0].equalsIgnoreCase("unmute")) {
				ServerUser user = SoundCenter.userList.getAcceptedUserByName(player.getName());
				if (user == null || !user.isInitialized()) {
					ConnectionManager.sendStartClientMessage(player);
					return true;
				}
				
				/* sc unmute <name> */
				if (args.length > 1) {
					ServerUser userToUnmute = SoundCenter.userList.getAcceptedUserByName(args[1]);
					if (userToUnmute == null) {
						player.sendMessage(Messages.ERR_MUTE_PT1 + args[1] + Messages.ERR_MUTE_PT2);
						return true;
					}
					
					user.removeMutedUser(userToUnmute.getId());
					player.sendMessage(Messages.INFO_USER_UNMUTED + args[1]);
				}
				
				player.sendMessage(Messages.CMD_USAGE_UNMUTE);
			
		/* sc toggle */
		} else if (args[0].equalsIgnoreCase("toggle")) {
			ServerUser user = SoundCenter.userList.getAcceptedUserByName(player.getName());
			if (user == null || !user.isInitialized()) {
				ConnectionManager.sendStartClientMessage(player);
				return true;
			}
			
			/* sc toggle <music | voice> */
			if (args.length > 1) {
				/* sc toggle music */
				if (args[1].equalsIgnoreCase("music")) {
					user.setMusicActive(!user.isMusicActive());
					if (user.isMusicActive()) {
						SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_UNMUTE_MUSIC, null, null, user);
						player.sendMessage(Messages.INFO_MUSIC_UNMUTED);
					} else {
						SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_MUTE_MUSIC, null, null, user);
						player.sendMessage(Messages.INFO_MUSIC_MUTED);
					}
					return true;
					
				/* sc toggle voice */
				} else if(args[1].equalsIgnoreCase("voice")) {
					user.setVoiceActive(!user.isVoiceActive());
					if (user.isVoiceActive()) {
						SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_UNMUTE_VOICE, null, null, user);
						player.sendMessage(Messages.INFO_VOICE_UNMUTED);
					} else {
						SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_MUTE_VOICE, null, null, user);
						player.sendMessage(Messages.INFO_VOICE_MUTED);
					}
					return true;
				}
			}
					
			player.sendMessage(Messages.CMD_USAGE_TOGGLE);
			return true;
		
		/* sc volume <1-100>*/
		} else if (args[0].equalsIgnoreCase("volume")) {
			if (args.length > 1) {
				ServerUser user = SoundCenter.userList.getAcceptedUserByName(player.getName());
				if (user == null || !user.isInitialized()) {
					ConnectionManager.sendStartClientMessage(player);
					return true;
				}
				
				try {
					int volume = Integer.parseInt(args[1]);
					if (volume > 0 && volume <= 100) {
						SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_CHANGE_VOLUME, (byte) volume, null, user);
						player.sendMessage(Messages.INFO_VOLUME_CHANGED + volume + " %.");
						return true;
					}
				} catch (NumberFormatException e) {}
			}
			
			player.sendMessage(Messages.CMD_USAGE_VOLUME);
			return true;
			
		/* sc status */
		} else if(args[0].equalsIgnoreCase("status")) {
			SCLocation loc = new SCLocation(player.getLocation());
			HashMap<Short, Double> boxes = IntersectionDetection.inRangeOfBox(loc);
			HashMap<Short, Double> areas = IntersectionDetection.isInArea(loc);
			
			// boxes
			player.sendMessage(Messages.prefix + "You are in range of " + boxes.size() + " boxes:");
			for (Entry<Short, Double> entry : boxes.entrySet()) {
				Box box = (Box) SoundCenter.database.getStation(GlobalConstants.TYPE_BOX, entry.getKey());
				if (box != null) {
					player.sendMessage(Messages.prefix + "- ID: " + box.getId() + ", owner: " + box.getOwner()
							+ ", distance: " + entry.getValue());
				}
			}
			
			//areas
			player.sendMessage(Messages.prefix + "You are located in " + areas.size() + " areas:");
			for (Entry<Short, Double> entry : areas.entrySet()) {
				Area area = (Area) SoundCenter.database.getStation(GlobalConstants.TYPE_AREA, entry.getKey());
				if (area != null) {
					player.sendMessage(Messages.prefix + "- ID: " + area.getId() + ", owner: " + area.getOwner()
							+ ", distance to border: " + entry.getValue());
				}
			}
			return true;
		
		/* sc set */
		} else if (args[0].equalsIgnoreCase("set")) {
			
			/* sc set area */
			if (args.length >= 2 && args[1].equalsIgnoreCase("area")) {
				
				if (!player.hasPermission("sc.set.area")) {
					player.sendMessage(Messages.ERR_PERMISSION_SET_AREA);
					return true;
				}
				
				int maxAreas = SoundCenter.config.maxAreas();
				if (maxAreas > 0 && SoundCenter.database.getStationCount(GlobalConstants.TYPE_AREA, player.getName()) >= maxAreas 
						&& !player.hasPermission("sc.nolimits")) {
					player.sendMessage(Messages.ERR_MAX_AREAS + maxAreas + ChatColor.RED + "areas.");
					return true;
				}
				
				Location corner0 = (Location) getMetadata(player, "com.github.wegfetz.custommusic.corner0", plugin);
				Location corner1 = (Location) getMetadata(player, "com.github.wegfetz.custommusic.corner1", plugin);
				// check if corners are set
				if (corner0 == null || corner1 == null) {
					player.sendMessage(Messages.ERR_NO_CORNERS);
					return true;
				}
				
				Area newArea = new Area(SoundCenter.database.getAvailableId(GlobalConstants.TYPE_AREA), player.getName(), new SCLocation(corner0),
						new SCLocation(corner1), SoundCenter.config.defaultFadeout());
				// check if area overlaps with existing boxes or areas
				if (IntersectionDetection.areaOverlaps(newArea) && !player.hasPermission("sc.set.overlap")) {
					player.sendMessage(Messages.ERR_PERMISSION_SET_OVERLAP);
					return true;
				}
				
				player.removeMetadata("com.github.wegfetz.custommusic.corner0", plugin);
				player.removeMetadata("com.github.wegfetz.custommusic.corner1", plugin);
				
				SoundCenter.database.addStation(GlobalConstants.TYPE_AREA, newArea);
				player.sendMessage(Messages.INFO_AREA_CREATED + newArea.getId());
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_STATION, newArea, null, null);				
				
				return true;
				
			/* sc set box */
			} else if (args.length >= 2 && args[1].equalsIgnoreCase("box")) {
				
				if (!player.hasPermission("sc.set.box")) {
					player.sendMessage(Messages.ERR_PERMISSION_SET_BOX);
					return true;
				}
				
				int maxBoxes = SoundCenter.config.maxBoxes();
				if (maxBoxes > 0 && SoundCenter.database.getStationCount(GlobalConstants.TYPE_BOX, player.getName()) >= maxBoxes 
						&& !player.hasPermission("sc.nolimits")) {
					player.sendMessage(Messages.ERR_MAX_BOXES + maxBoxes + ChatColor.RED + "boxes.");
					return true;
				}
				
				int radius = SoundCenter.config.defaultBoxRange();
				/* sc set box [range] */
				if (args.length >= 3) {
					try {
						radius = Integer.parseInt(args[2]);
					} catch (NumberFormatException e) {}
				}
				int maxRange = SoundCenter.config.maxBoxRange();
				// check if radius exceeds the limits
				if (radius <= 0) {
					player.sendMessage(Messages.ERR_MIN_BOX_RANGE);
					radius = SoundCenter.config.defaultBoxRange();
				}
				if (radius > maxRange && maxRange > 0 && !player.hasPermission("sc.nolimits")) {
					player.sendMessage(Messages.ERR_MAX_BOX_RANGE + maxRange);
					radius = maxRange;
				}
				
				Box newBox = new Box(SoundCenter.database.getAvailableId(GlobalConstants.TYPE_BOX), player.getName(), 
						new SCLocation(player.getLocation()), radius);
				// check if box overlaps withexisting boxes or areas
				if (IntersectionDetection.boxOverlaps(newBox) && !player.hasPermission("sc.set.overlap")) {
					player.sendMessage(Messages.ERR_PERMISSION_SET_OVERLAP);
					return true;
				}
				
				SoundCenter.database.addStation(GlobalConstants.TYPE_BOX, newBox);
				player.sendMessage(Messages.INFO_BOX_CREATED + newBox.getId());
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_STATION, newBox, null, null);
				
				return true;
			
			/* sc set corner */
			} else if (args.length >= 2 && args[1].equalsIgnoreCase("corner")) {
				
				if (!player.hasPermission("sc.set.area")) {
					player.sendMessage(Messages.ERR_PERMISSION_SET_AREA);
					return true;
				}
				
				/* sc set corner <1|2> */
				if (args.length >= 3 && (args[2].equalsIgnoreCase("1") || args[2].equalsIgnoreCase("2"))) {
					
					int arg = 0;
					int b = 1;
					
					if (args[2].equalsIgnoreCase("2")) {
						arg = 1;
						b = 0;
					}
					
					Location loc = player.getLocation();
					Location otherCorner = (Location) getMetadata(player, "com.github.wegfetz.custommusic.corner" + b, plugin);
					
					if (otherCorner !=null) {
						// check if corners are in same world
						if (!otherCorner.getWorld().equals(loc.getWorld())) {
							player.sendMessage(Messages.ERR_CORNERS_DIFFERENT_WORLDS);
							return true;
						}
					
						// check if area exceeds size limit
						int maxSize = SoundCenter.config.maxAreaSize();
						if (maxSize > 0 && !player.hasPermission("sc.nolimits")
								&& (maxSize < Math.abs(loc.getBlockX() - otherCorner.getBlockX()) 
								|| maxSize < Math.abs(loc.getBlockY() - otherCorner.getBlockY()) 
								|| maxSize < Math.abs(loc.getBlockZ() - otherCorner.getBlockZ()))) {
							player.sendMessage(Messages.ERR_MAX_AREA_SIZE + maxSize);
							return true;
						}
						
						// Make sure we have a cuboid and not a plane
						if (otherCorner.getBlockX() == loc.getBlockX() 
								|| otherCorner.getBlockY() == loc.getBlockY() || otherCorner.getBlockZ() == loc.getBlockZ()) {
							sender.sendMessage(Messages.ERR_CORNERS_IN_LINE);
							return true;
						}
					}
					
					setMetadata(player, "com.github.wegfetz.custommusic.corner" + arg, loc, plugin);
					player.sendMessage(Messages.prefix + ChatColor.GREEN + "Corner " + (int) (arg+1) + " set.");
					
					return true;
				} 	

				player.sendMessage(Messages.CMD_USAGE_CORNER);
				return true;
			}
			
			player.sendMessage(Messages.CMD_USAGE_SET);
			return true;
			
		/* sc speak */
		} else if (args[0].equalsIgnoreCase("speak")) {
			
			if (!SoundCenter.config.voiceEnabled()) {
				player.sendMessage(Messages.ERR_VOICE_CHAT_DISABLED);
				return true;
			}
			
			if (!player.hasPermission("sc.speak")) {
				player.sendMessage(Messages.ERR_PERMISSION_SPEAK);
				return true;
			}
			
			ServerUser user = SoundCenter.userList.getAcceptedUserByName(player.getName());
			if (user == null || !user.isInitialized()) {
				ConnectionManager.sendStartClientMessage(player);
				return true;
			}
			
			/* sc speak global */
			if (args.length >= 2 && args[1].equalsIgnoreCase("global")) {
				
				if (!player.hasPermission("sc.speak.global")) {
					player.sendMessage(Messages.ERR_PERMISSION_SPEAK_GLOBAL);
					return true;
				}
				
				if (!user.isSpeaking() && !user.isSpeakingGlobally()) {
					user.setSpeakingGlobally(true);
					if (StreamManager.getTotalRate() + (GlobalConstants.VOICE_DATA_RATE
							*SoundCenter.userList.acceptedUsers.size()*0.8) 
							<= SoundCenter.config.maxStreamBandwidth()*1024) { //assuming 80% of accepted users are listening to 
																				//global voice chat 
																				//TODO replace with exact user count
						SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_START_RECORDING, null, null, user);
						StreamManager.totalVoiceRate += GlobalConstants.VOICE_DATA_RATE*SoundCenter.userList.acceptedUsers.size()*0.8;
						player.sendMessage(Messages.INFO_SPEAKING_GLOBALLY);
					} else {
						player.sendMessage(Messages.ERR_SERVER_LOAD);
						user.setSpeaking(false);
					}
				} else {
					user.setSpeaking(false);
					user.setSpeakingGlobally(false);
					SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_STOP_RECORDING, null, null, user);
					StreamManager.totalVoiceRate -= GlobalConstants.VOICE_DATA_RATE;
					player.sendMessage(Messages.INFO_NOT_SPEAKING);
				}
				return true;
			
			} else {
			
				/* sc speak */
				if (!user.isSpeaking() && !user.isSpeakingGlobally()) {
					user.setSpeaking(true);
					//check server load
					if (StreamManager.getTotalRate() + GlobalConstants.VOICE_DATA_RATE*user.listeners.size() <= SoundCenter.config.maxStreamBandwidth()*1024) {
						StreamManager.totalVoiceRate += GlobalConstants.VOICE_DATA_RATE*user.listeners.size();
						SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_START_RECORDING, null, null, user);
						player.sendMessage(Messages.INFO_SPEAKING);
					} else {
						player.sendMessage(Messages.ERR_SERVER_LOAD);
						user.setSpeaking(false);
					}
				} else {
					user.setSpeaking(false);
					user.setSpeakingGlobally(false);
					SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_STOP_RECORDING, null, null, user);
					StreamManager.totalVoiceRate -= GlobalConstants.VOICE_DATA_RATE;
					player.sendMessage(Messages.INFO_NOT_SPEAKING);
				}
				return true;
			}
		}
		
		sender.sendMessage(Messages.CMD_USAGE_SC);
		return true;
	}
	
	
	public void setMetadata(Player player, String key, Object value, Plugin plugin){
		  player.setMetadata(key,new FixedMetadataValue(plugin,value));
	}
	
	public Object getMetadata(Player player, String key, Plugin plugin){
		  List<MetadataValue> values = player.getMetadata(key);  
		  for(MetadataValue value : values){
		     if(value.getOwningPlugin().getDescription().getName().equals(plugin.getDescription().getName())){
		        return value.value();
		     }
		  }
		  return null;
	}
	
}
