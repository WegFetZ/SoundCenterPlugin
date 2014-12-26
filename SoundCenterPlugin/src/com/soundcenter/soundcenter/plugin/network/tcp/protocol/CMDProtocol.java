package com.soundcenter.soundcenter.plugin.network.tcp.protocol;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.data.Song;
import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.lib.tcp.TcpPacket;
import com.soundcenter.soundcenter.plugin.PlaybackManager;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.messages.Messages;

public class CMDProtocol {

	public static boolean processPacket(byte cmd, TcpPacket receivedPacket, ServerUser user) {

		// SoundCenter.logger.d("Processing packet in StreamProtocol.", null);

		/* client has muted voice chat */
		if (cmd == TcpOpcodes.SV_CMD_MUTE_VOICE) {
			user.setVoiceActive(false);

			Player player = user.getPlayer();
			if (player != null) {
				player.sendMessage(Messages.INFO_VOICE_MUTED);
			}
			return true;

			/* client has unmuted voice chat */
		} else if (cmd == TcpOpcodes.SV_CMD_UNMUTE_VOICE) {
			user.setVoiceActive(true);

			Player player = user.getPlayer();
			if (player != null) {
				player.sendMessage(Messages.INFO_VOICE_UNMUTED);
			}
			return true;

			
		} else if (cmd == TcpOpcodes.SV_CMD_PLAY_SONG) {
			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "Could not get the Bukkit player instance.", null, user);
				return true;
			}

			Song song = (Song) receivedPacket.getKey();
			String recipants = "";
			if (receivedPacket.getValue() != null) {
				recipants = (String) receivedPacket.getValue();
			}

			if (recipants.isEmpty()) { // play only for user
				PlaybackManager.playSong(song, user);
				return true;
			}

			if (recipants.equals("/global")) { // play globally
				if (!player.hasPermission("sc.play.global")) {
					SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_PLAY_PERMISSION, "sc.play.global", null, user);
					return true;
				}
				PlaybackManager.playGlobalSong(song);
				return true;
			}

			// play for a world
			if (!player.hasPermission("sc.play.world")) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_PLAY_PERMISSION, "sc.play.world", null, user);
				return true;
			}
			World world = Bukkit.getServer().getWorld(recipants);
			if (world == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_NOT_EXISTS, "World: " + recipants, null, user);
				return true;
			}
			PlaybackManager.playWorldSong(song, world);
			return true;
		
			
			/* client wants to stop a song */
		} else if (cmd == TcpOpcodes.SV_CMD_STOP_SONG) {
			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "Could not get the Bukkit player instance.", null,
						user);
				return true;
			}
			
			Song song = (Song) receivedPacket.getKey();
			String recipants = "";
			if (receivedPacket.getValue() != null) {
				recipants = (String) receivedPacket.getValue();
			}
			
			if (recipants.isEmpty()) { //stop only for user
				PlaybackManager.stopSong(song, user);
				return true;
			} 
			
			if (recipants.equals("/global")) { //stop globally
				if (!player.hasPermission("sc.play.global")) {
					SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_PLAY_PERMISSION, "sc.play.global", null, user);
					return true;
				}
				PlaybackManager.stopGlobalSong(song);
				return true;
			}
			
			//stop for a world
			if (!player.hasPermission("sc.play.world")) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_PLAY_PERMISSION, "sc.play.world", null, user);
				return true;
			}
			World world = Bukkit.getServer().getWorld(recipants);
			if (world == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_NOT_EXISTS, "World: " + recipants, null, user);
				return true;
			}
			PlaybackManager.stopWorldSong(song, world);
			return true;
		}

		return true;
	}

}
