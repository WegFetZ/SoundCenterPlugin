package com.soundcenter.soundcenter.plugin.network.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;




import java.net.UnknownHostException;

import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;

public class TcpServer implements Runnable {
	
	public boolean exit = false;
	public boolean active = false;
	public ServerSocket serverSocket = null;
	private TcpSender tcpSender = null;
	private Thread tcpSenderThread = null;
	private int tcpPort = 0;
	private String serverBindAddr = "";
	
	public TcpServer(int port, String serverIp) {
		this.tcpPort = port;
		this.serverBindAddr = serverIp;
	}
	
	@Override
	public void run() {
		active = true;
		try {
			InetAddress addr = InetAddress.getByName(serverBindAddr);
			serverSocket = new ServerSocket(tcpPort, 50, addr);
			
			tcpSender = new TcpSender();
			tcpSenderThread = new Thread(tcpSender);
			tcpSenderThread.start();
			
			SoundCenter.logger.i("TCP-Server started on " + addr.getHostAddress() + ":" + tcpPort + ".", null);
		} catch (UnknownHostException e) {
			SoundCenter.logger.s("Error while trying to resolve server-ip: " + serverBindAddr, e);
			exit = true;
		} catch (IOException e) {
			SoundCenter.logger.s("Error while starting TCP-Server on port " + tcpPort, e);
			exit = true;
		}
		
		while (!exit) {
			try {
				new Thread(new TcpUserConnection(serverSocket.accept())).start();
			} catch (IOException e) {
				if (!exit)
					SoundCenter.logger.i("Error while accepting new TCP-Connection:", e);
			}
		}
		
		if (!exit)
			shutdown();
		SoundCenter.userList.resetServerUsers();
		serverSocket = null;
		active = false;
		SoundCenter.logger.i("TCP-Server was shut down!", null);
	}
	
	public void shutdown() {
		SoundCenter.logger.i("Shutting down TCP-Server...", null);
		
		exit = true;		
		if (tcpSenderThread != null)
			tcpSenderThread.interrupt();
		
		while(tcpSender.isActive()) {
			try { Thread.sleep(100); } catch(InterruptedException e) {}
		}
	}
	
	public void send(Byte opCode, Object key, Object value, ServerUser user) {
		tcpSender.send(opCode, key, value, user);
	}
}
