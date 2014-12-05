package com.soundcenter.soundcenter.plugin.network.tcp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.messages.Messages;

public class ConnectionManager {

	public static boolean initialize(Player player) {
		ServerUser tcpUser = SoundCenter.userList.getAcceptedUserByName(player.getName());

		if (tcpUser != null) {
			if (player.hasPermission("sc.init") && tcpUser.isAccepted() // initialize
					&& (tcpUser.getIp().equals(player.getAddress().getAddress()) || !SoundCenter.config.verifyIp())) {

				tcpUser.setInitialized(true);

				SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_INFO_INITIALIZED, null, null, tcpUser);
				player.sendMessage(Messages.INFO_INIT_SUCCESS);

				return true;

			} else if (!player.hasPermission("sc.init")) { // no permission
				player.sendMessage(Messages.ERR_PERMISSION_INIT);
				SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_PERMISSION_INIT, null, null, tcpUser);
				
				tcpUser.setQuitReason("Missing required permission: 'sc.init'");
				tcpUser.disconnect();

				return false;

			} else if (tcpUser.isAccepted()) { // ip-verification failed
				SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_IP, null, null, tcpUser);
				player.sendMessage(Messages.ERR_IP_VERIFICATION);
				
				tcpUser.setQuitReason("IP-Verification failed.");
				tcpUser.disconnect();


				return false;

			} else { // user not accepted
				player.sendMessage(Messages.ERR_NOT_ACCEPTED);
				return true;
			}

		} else { // audioclient not connected
			sendStartClientMessage(player);
			return true;
		}
	}

	public static void sendStartClientMessage(Player player) {
		player.sendMessage(Messages.INFO_START_AUDIOCLIENT_PT1 + "http://www.sound-center.com?n=" 
				+ player.getName() + "&i=" + Bukkit.getServer().getIp() + "&p=" 
				+ SoundCenter.config.port() + "&v=" + SoundCenter.MIN_CL_VERSION + " " 
				+ Messages.INFO_START_AUDIOCLIENT_PT2);
	}

}
