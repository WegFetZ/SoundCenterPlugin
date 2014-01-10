package com.soundcenter.soundcenter.plugin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.udp.UdpOpcodes;
import com.soundcenter.soundcenter.lib.udp.UdpPacket;
import com.soundcenter.soundcenter.plugin.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.plugin.messages.Messages;
import com.soundcenter.soundcenter.plugin.plugin.util.IntersectionDetection;

public class PositionsUpdater implements Runnable {

	private Thread thread = null;
	private boolean exit = false;

	public void run() {
		thread = Thread.currentThread();
		thread.setName("PositionsUpdater");

		while (!exit) {
			long timeA = System.currentTimeMillis();

			int i = 0;
			List<ServerUser> userList = new ArrayList<ServerUser>(SoundCenter.userList.acceptedUsers.values());
			for (ServerUser user : userList) {
				Player player = user.getPlayer();
				if (player != null && user.isInitialized()) {
					Location loc = player.getLocation();
					UdpPacket packet = new UdpPacket(GlobalConstants.LOCATION_PACKET_SIZE);
					packet.setType(UdpOpcodes.INFO_LOCATION);
					packet.setLocation(loc);
					SoundCenter.udpServer.send(packet, user);

					// update the list of listeners
					for (int k = i; k < userList.size(); k++) {
						ServerUser onlineUser = userList.get(k);
						if (!onlineUser.equals(user) && (user.isSpeaking() || onlineUser.isSpeaking())) {
							Player onlineUserPlayer = onlineUser.getPlayer();
							if (onlineUserPlayer != null && onlineUser.isInitialized()) {
								short distance = IntersectionDetection.playerCanHear(onlineUserPlayer,
										user.getPlayer(), SoundCenter.config.voiceRange());
								if (distance >= 0) {
									byte volumePercent = (byte) (100 - ((double)distance / (double)SoundCenter.config.voiceRange())*100 );
									if (onlineUser.isVoiceActive()) {
										user.addListener(onlineUser, volumePercent);
									}
									if (user.isVoiceActive()) {
										onlineUser.addListener(user, volumePercent);
									}
								} else {
									user.removeListener(onlineUser);
									onlineUser.removeListener(user);
								}
							}
						} else {
							user.removeListener(onlineUser);
							onlineUser.removeListener(user);
						}
					}

					if (user.isSpeaking()) {
						// remind player that he has still activated voice chat
						if (SoundCenter.config.voiceRemindTime() > 0
								&& timeA - user.getSpeakingStartTime() >= SoundCenter.config.voiceRemindTime() * 1000) {
							player.sendMessage(Messages.INFO_STILL_SPEAKING);
							user.setSpeakingStartTime(timeA);
						}
					}
				}
				i++;
			}

			long timeB = System.currentTimeMillis();
			int delay = (33) - (int) (timeB - timeA);
			
			if (delay < 0)
				delay = 0;

			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
			}
		}
	}

	public void shutdown() {
		exit = true;
		if (thread != null) {
			thread.interrupt();
		}
	}

}
