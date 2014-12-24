package com.soundcenter.soundcenter.plugin.network.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;





import com.soundcenter.soundcenter.lib.tcp.TcpPacket;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.messages.Messages;
import com.soundcenter.soundcenter.plugin.network.tcp.protocol.MainProtocol;

public class TcpUserConnection implements Runnable {

	private ServerUser user = null;
	
	public TcpUserConnection(Socket socket) {
		this.user = new ServerUser(socket);
		SoundCenter.userList.addConnectedUser(user);
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("TcpUserConnection");
		
		ObjectOutputStream oos = user.getOos();
		ObjectInputStream ois = user.getOis();
		Socket socket = user.getSocket();
		
		while (!user.getDisconnect() && !socket.isClosed()) {
			try {
				/* receive and process packet */
				Object receivedPacket = ois.readObject();
				if ((receivedPacket instanceof TcpPacket) 
						&&!MainProtocol.processPacket((TcpPacket) receivedPacket, user)) {
					break;
				}
			} catch (IOException e) {
				if (!SoundCenter.tcpServer.exit && !user.getDisconnect() && !socket.isClosed()) {
					SoundCenter.logger.i("Lost connection to user " + user.getName() 
							+ " on " + user.getIp() + ".", e);
				}
				user.disconnect();
			} catch (ClassCastException e) {
				SoundCenter.logger.i("Error while restoring object from packet:", e);
				user.disconnect();
			} catch (ClassNotFoundException e) {
				SoundCenter.logger.i("Error while reading packet from user " + user.getName() + " on " 
			+ user.getIp() + ".", e);
				user.disconnect();
			}
		}
		SoundCenter.logger.i("Closed TCP-connection to user " + user.getName() 
				+ " on " + user.getIp() + ". \nReason: " + user.getQuitReason(), null);
		Player player = user.getPlayer();
		if (player != null) {
			player.sendMessage(Messages.ERR_CONNECTION_LOST);
		}
		
		user.disconnect();
		
		//remove user from userList and all voice chats
		List<ServerUser> userList = new ArrayList<ServerUser>(SoundCenter.userList.acceptedUsers.values());
		for (ServerUser onlineUser : userList) {
			onlineUser.removeListener(user);
		}
		
		if (!SoundCenter.tcpServer.exit && oos != null) {
			SoundCenter.userList.removeServerUser(user);
		}
		
		if (!SoundCenter.tcpServer.exit && socket != null && !socket.isClosed()) {
			try {
				if (oos!=null)
					oos.close();
				else
					socket.close();
			} catch (IOException e) {}
		}
	}
}
