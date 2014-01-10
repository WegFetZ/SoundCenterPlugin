package com.soundcenter.soundcenter.plugin.plugin.network.tcp.protocol;

import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.lib.tcp.TcpPacket;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.plugin.data.ServerUser;

public class MainProtocol {

	public static boolean processPacket(TcpPacket receivedPacket, ServerUser user) {
		
		Byte cmd = receivedPacket.getType();
		
		//SoundCenter.logger.d("Received Tcp-Message: Type: " + cmd + " Key: " + receivedPacket.getKey() + " Value: " + receivedPacket.getValue(), null);
		
		if (!user.isAccepted()) {		/* user is not yet accepted, do the handshaking */
			
			return HandshakeProtocol.processPacket(cmd, receivedPacket, user);
			
		} else if (!user.isInitialized()) {			/* audioclient isn't intialized */
			SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_NOT_INITIALIZED, null, null, user);	
			return true;
			
		} else {		/* user is already accepted and initialized */
			
			if (isInGroup(cmd, TcpOpcodes.SV_GROUP_STREAM, TcpOpcodes.SV_GROUP_END_STREAM)) {
				return StreamProtocol.processPacket(cmd, receivedPacket, user);
						
			} else if (isInGroup(cmd, TcpOpcodes.SV_GROUP_DATA, TcpOpcodes.SV_GROUP_END_DATA)) {
				return DataProtocol.processPacket(cmd, receivedPacket, user);
			}
		}
		
		if(cmd == TcpOpcodes.SV_CON_INFO_DISCONNECT) {
			user.setQuitReason("User quit.");
			return false;
		}
		return true;
	}
	
	
	private static boolean isInGroup(byte cmd, byte start, byte end) {
		return (cmd >= start && cmd <= end);
	}
	
}
