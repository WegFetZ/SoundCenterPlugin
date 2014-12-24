package com.soundcenter.soundcenter.plugin.network.tcp.protocol;

import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.data.Song;
import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.lib.tcp.TcpPacket;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.messages.Messages;

public class StreamProtocol {

	public static boolean processPacket(byte cmd, TcpPacket receivedPacket, ServerUser user) {
		
		//SoundCenter.logger.d("Processing packet in StreamProtocol.", null);
			
		/* client wants to play a global song */
		if (cmd == TcpOpcodes.SV_STREAM_CMD_PLAY_GLOBAL) {
			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "Could not get the Bukkit player instance.", null, user);	
				return true;
			}
			
			if (!player.hasPermission("sc.play.global")) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_PLAY_GLOBAL_PERMISSION, null, null, user);
				return true;
			}
			
			Song song = (Song) receivedPacket.getKey();
			SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_PLAY_GLOBAL, song, null, null);
			return true;			
			
		/* client wants to stop the global song */
		} else if (cmd == TcpOpcodes.SV_STREAM_CMD_STOP_GLOBAL) {
			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "stop global song", null, user);	
				return true;
			}
			
			if (!player.hasPermission("sc.play.global")) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_PLAY_GLOBAL_PERMISSION, null, null, user);
				return true;
			}
			
			SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_STOP_GLOBAL, null, null, null);
			return true;
		
		/* client has muted voice chat */
		} else if (cmd == TcpOpcodes.SV_STREAM_CMD_MUTE_VOICE) {
			user.setVoiceActive(false);
			
			Player player = user.getPlayer();
			if (player != null) {
				player.sendMessage(Messages.INFO_VOICE_MUTED);
			}
			return true;
			
		
		/* client has unmuted voice chat */
		} else if (cmd == TcpOpcodes.SV_STREAM_CMD_UNMUTE_VOICE) {
			user.setVoiceActive(true);
			
			Player player = user.getPlayer();
			if (player != null) {
				player.sendMessage(Messages.INFO_VOICE_UNMUTED);
			}
			return true;
		}
		
		return true;
	}
	
}
