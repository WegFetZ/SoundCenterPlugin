package com.soundcenter.soundcenter.plugin;

import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.soundcenter.soundcenter.plugin.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.plugin.network.StreamManager;
import com.soundcenter.soundcenter.plugin.plugin.network.tcp.ConnectionManager;

public class SCPlayerListener implements Listener{
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		ConnectionManager.initialize(player);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		onPlayerDisconnect(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		onPlayerDisconnect(event.getPlayer());
	}
	
	private void onPlayerDisconnect(Player player) {
		ServerUser user = SoundCenter.userList.getAcceptedUserByName(player.getName());
		if (user != null) {
			//remove user from all streams
			StreamManager.removeUserFromAllSessions(user);
			//remove user from all voice chat listener lists
			for (Entry<Short, ServerUser> entry : SoundCenter.userList.acceptedUsers.entrySet()) {
				ServerUser onlineUser = entry.getValue();
				onlineUser.removeListener(user);
			}
			//disconnect user
			user.disconnect();
		}
	}

}
