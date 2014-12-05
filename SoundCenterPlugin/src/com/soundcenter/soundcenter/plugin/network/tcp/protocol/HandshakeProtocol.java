package com.soundcenter.soundcenter.plugin.network.tcp.protocol;

import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.lib.tcp.TcpPacket;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.network.tcp.ConnectionManager;

public class HandshakeProtocol {

	public static boolean processPacket(byte cmd, TcpPacket receivedPacket, ServerUser user) {
		
		//SoundCenter.logger.d("Processing packet in HandshakeProtocol.", null);//TODO
		
		if (cmd == TcpOpcodes.SV_CON_REQ_JOIN) {		/* join request */
			if (SoundCenter.userList.serverUserCount() < SoundCenter.config.serverCapacity()) {	
				
				/* request client version info */
				SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_REQ_VERSION, null, null, user);
				user.setJoinRequested(true);
				
				return true;
				
			} else {		/* server full */
				SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_USER_CAP, null, null, user);
				user.setQuitReason("Server reached its full capacity.");
				return false;
			}
			
		} else if (!user.hasJoinRequested()) {		/* protocol error */
			SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_PROTOCOL, null, null, user);
			user.setQuitReason("Client not following the protocol.");
			return false;
			
		} else if (cmd == TcpOpcodes.SV_CON_INFO_VERSION) {		/* version info */
				Double version = (Double) receivedPacket.getKey();
				if (version >= SoundCenter.MIN_CL_VERSION && version <= SoundCenter.MAX_CL_VERSION) {
					/* request user name */
					SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_REQ_NAME, null, null, user);
					user.setVersionOK(true);
					return true;
					
				} else {		/* wrong client version */
					SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_VERSION, SoundCenter.MIN_CL_VERSION, SoundCenter.MAX_CL_VERSION, user);						
					user.setQuitReason("Wrong client version.");
					return false;
				}	
			
		} else if (!user.isVersionOK()) {		/* protocol error */
			user.setQuitReason("Client not following the protocol.");
			SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_PROTOCOL, null, null, user);				
			return false;
			
		} else if (cmd == TcpOpcodes.SV_CON_INFO_NAME) {		/* user name info */
				final String name = (String) receivedPacket.getKey();
				if (name.length() <= 16) {
					if (SoundCenter.userList.getAcceptedUserByName(name) != null ) {		/* user with that name already connected */
						SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_ALREADY_CONNECTED, null, null, user);						
						user.setQuitReason("User with name " + name + " is already connected.");
						return false;
					}						
					/* accept user */
					SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_INFO_ACCEPTED, user.getId(), null, user);
					Thread.currentThread().setName("TcpUserConnection Name: " + name);
					user.setName(name);						
					user.setNameOK(true);
					user.setAccepted(true);
					SoundCenter.userList.addAcceptedUser(user);
					SoundCenter.logger.d("User " + user.getIp() + " is connected to SoundCenter with id " 
					+ user.getId() + ".", null);
					
					final ServerUser joinedUser = user;
					
					new Timer().schedule(new TimerTask() {		/* show warning if we don't receive udp heartbeat in 20s */
						@Override
						public void run() {
							if (joinedUser.getUdpPort() == 0) {
								SoundCenter.logger.d("Receiving no UDP-heartbeats from user " + name + " (" + joinedUser.getId() + ").", null);
							}
						}
						
					}, 20000);
					
					Player player = Bukkit.getServer().getPlayer(name);
					if (player == null || !player.getName().equals(name) || !player.isOnline()) {			/* player not online */
						
						new Timer().schedule(new TimerTask() {       /* give the user 2 minutes time to intitialize the client */   
						    @Override
						    public void run() {
						    	if (!joinedUser.isInitialized()) {
									SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_INIT_TIMEOUT, null, null, joinedUser);						
									joinedUser.setQuitReason("Initialization timeout.");
						    		joinedUser.disconnect();
						    	}
						    }
						}, 2* 60 * 1000);
						
						SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_INFO_NOT_ONLINE, null, null, user);
						return true;
					} else {
						return ConnectionManager.initialize(player);
					}
					
				} else {		/* invalid name */
					SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_NAME, null, null, user);						
					user.setQuitReason("Invalid name.");
					return false;
				}
				
		} else if (!user.isNameOK()) {		/* protocol error */
			user.setQuitReason("Client not following the protocol.");
			SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_DENY_PROTOCOL, null, null, user);				
			return false;
			
		} else {		/* unknown error */
			SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, null, null, user);				
			return false;
		}
	}
}
