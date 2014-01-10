package com.soundcenter.soundcenter.plugin.plugin.network.tcp;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.lib.tcp.TcpPacket;
import com.soundcenter.soundcenter.lib.tcp.TcpPacketContainer;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.plugin.data.ServerUser;

public class TcpSender implements Runnable {

	private BlockingQueue<TcpPacketContainer> container = new LinkedBlockingQueue<TcpPacketContainer>();
	
	private TcpPacketContainer cont = null;
	private TcpPacket packet = null;
	private ObjectOutputStream oos = null;
	
	private boolean active = true;
	
	public void run() {
		active = true;
		Thread.currentThread().setName("TcpSender");
		
		while (!SoundCenter.tcpServer.exit) {
			try {	
				cont = container.take();
				packet = cont.getPacket();
				oos = cont.getOos();
				if (packet != null) {
					try {
						if (oos == null) {
							/* send packet to all users */
							for (ServerUser user: SoundCenter.userList.connectedUsers) {										
								oos = user.getOos();
								if (oos != null) {
									oos.writeObject(packet);
								}
							}
						} else {
							oos.writeObject(packet);
						}
					} catch (IOException e) {
						if (!!SoundCenter.tcpServer.exit)
						SoundCenter.logger.d("Error while sending TCP-packet", e);
					}
				}			
			} catch (InterruptedException e) {}	
		}	
		
		/* tell users to disconnect and close the connection */
		packet = new TcpPacket(TcpOpcodes.CL_CON_INFO_DISCONNECT, null, null);
		for (ServerUser user: SoundCenter.userList.connectedUsers) {										
			oos = user.getOos();
			try {										
				oos.writeObject(packet);
			} catch (IOException e) {}
			try{
				oos.close();
			} catch (IOException e) {}
		}		
		
		try {
			SoundCenter.tcpServer.serverSocket.close();
		} catch (IOException e) {}
		
		reset();
		
		active = false;
	}
	
	private void reset() {
		container.clear();
		cont = null;
		packet = null;
		oos = null;
	}
	
	private void addContainer(TcpPacketContainer cont) {
		container.add(cont);
	}
	
	public void send(Byte opCode, Object key, Object value, ServerUser user) {
		TcpPacket packet = new TcpPacket(opCode, key, value);
		ObjectOutputStream oos = null;
		if (user != null) {
			oos = user.getOos();
		}
		addContainer(new TcpPacketContainer(packet, oos));
	}
	
	public boolean isActive() {
		return active;
	}
}
