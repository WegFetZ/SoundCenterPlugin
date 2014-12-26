package com.soundcenter.soundcenter.plugin;

import org.bukkit.World;
import com.soundcenter.soundcenter.lib.data.Song;
import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.plugin.data.ServerUser;

public class PlaybackManager {

	public static void playSong(Song song, ServerUser user) {
		SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_PLAY_SONG, song, null, user);
	}
	
	public static void playWorldSong(Song song, World world) {
		SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_PLAY_SONG, song, world.getName(), null);
	}
	
	public static void playGlobalSong(Song song) {
		SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_PLAY_SONG, song, null, null);
	}
	
	public static void stopSong(Song song, ServerUser user) {
		SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_STOP_SONG, song, null, user);
	}
	
	public static void stopWorldSong(Song song, World world) {
		SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_STOP_SONG, song, world.getName(), null);
	}
	
	public static void stopGlobalSong(Song song) {
		SoundCenter.tcpServer.send(TcpOpcodes.CL_CMD_STOP_SONG, song, null, null);
	}
	
}
