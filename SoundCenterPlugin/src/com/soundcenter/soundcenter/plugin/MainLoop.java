package com.soundcenter.soundcenter.plugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.udp.UdpOpcodes;
import com.soundcenter.soundcenter.lib.udp.UdpPacket;
import com.soundcenter.soundcenter.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.messages.Messages;
import com.soundcenter.soundcenter.plugin.util.IntersectionDetection;

public class MainLoop implements Runnable {

	private Thread thread = null;
	private boolean exit = false;

	public void run() {
		thread = Thread.currentThread();
		thread.setName("PositionsUpdater");

		
		long timeA;
		long timeB;
		int delay;
		
		ServerUser[] userList;
		int i;
		int k;
		Player player;
		ServerUser onlineUser;
		Location loc;
		while (!exit) {
			timeA = System.currentTimeMillis();
			
			if (!SoundCenter.userList.acceptedUsers.isEmpty()) {
				userList = SoundCenter.userList.acceptedUsers.values().toArray(new ServerUser[0]);
	
				i = 0;
				for (ServerUser user : userList) {
					player = user.getPlayer();
					if (player != null && user.isInitialized()) {
						loc = player.getLocation();
						UdpPacket packet = new UdpPacket(GlobalConstants.LOCATION_PACKET_SIZE);
						packet.setType(UdpOpcodes.INFO_LOCATION);
						packet.setLocation(loc);
						SoundCenter.udpServer.send(packet, user);
	
						// update the list of listeners
						for (k=i; k < userList.length; k++) {
							onlineUser = userList[k];
							if (!onlineUser.equals(user) && (user.isSpeaking() || onlineUser.isSpeaking())) {
								if (onlineUser.getPlayer() != null && onlineUser.isInitialized()) {
									short distance = IntersectionDetection.playerCanHear(onlineUser.getPlayer(),
											user.getPlayer(), SoundCenter.config.voiceRange());
									if (distance >= 0) {
										byte volumePercent = (byte) (100 - ((double)distance / (double)SoundCenter.config.voiceRange())*100.d );
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
			}

			timeB = System.currentTimeMillis();
			delay = (33) - (int) (timeB - timeA);
			
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
