package com.soundcenter.soundcenter.plugin.network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.udp.UdpOpcodes;
import com.soundcenter.soundcenter.lib.udp.UdpPacket;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;

public class UdpServer implements Runnable {

	public static long totalVoiceDataRate = 0;
	
	public boolean exit = false;
	public boolean active = false;
	public DatagramSocket datagramSocket = null;
	private UdpSender udpSender = null;
	private int udpPort = 4224;
	private String serverBindAddr = "0.0.0.0";
	
	public UdpServer(int port, String serverBindAddr) {
		this.udpPort = port;
		this.serverBindAddr = serverBindAddr;
	}
	
	public void run() {
		active = true;
		try {
			InetAddress addr = InetAddress.getByName(serverBindAddr);
			datagramSocket = new DatagramSocket(udpPort, addr);	
			
			udpSender = new UdpSender(datagramSocket);
			
			SoundCenter.logger.i("UDP-Server started on " + addr.getHostAddress() + ":" + udpPort + ".", null);
		} catch (SocketException e) {
			SoundCenter.logger.s("Error while starting UDP-Server on port" + udpPort, e);
			exit = true;
		} catch (UnknownHostException e) {
			SoundCenter.logger.s("Error while trying to resolve server-ip: " + serverBindAddr, e);
			exit = true;
		}
		
		while(!exit) {
			byte[] data = new byte[GlobalConstants.STREAM_PACKET_SIZE];
			try {
				DatagramPacket receivedPacket = new DatagramPacket(data, data.length);
				datagramSocket.receive(receivedPacket);				
				UdpPacket packet = new UdpPacket(receivedPacket.getData());
				
				//SoundCenter.logger.d("Received Udp-Message: Type: " + packet.getType() + " from: " + packet.getID(), null);
				
				if(packet.getIdent() == GlobalConstants.UDP_IDENT) {
					
					ServerUser user = SoundCenter.userList.getAcceptedUserById(packet.getID());
					if (packet.getType() == UdpOpcodes.TYPE_VOICE && SoundCenter.config.voiceEnabled()) {
						if (user != null && user.getIp().equals(receivedPacket.getAddress())) {
							//send the packet to all people that can hear him (because they are in range)
							if (user.isSpeaking()) {
								sendVoiceLocally(packet, user);
							} else if (user.isSpeakingGlobally()) {
								sendVoiceGlobally(packet, user);
							}
						}
					} else if (packet.getType() == UdpOpcodes.TYPE_HEARTBEAT) {
						if (user != null) {
							user.setUdpPort(receivedPacket.getPort());
						}
					}
				}
			} catch (IOException e) {
				if (!exit)
					SoundCenter.logger.i("Error while receiving UDP-Packet:", e);
			}
		}
		
		if (!exit)
			shutdown();
		SoundCenter.userList.resetServerUsers();
		datagramSocket = null;
		active = false;
		SoundCenter.logger.i("UDP-Server was shut down!", null);
	}
	
	public void sendVoiceLocally(UdpPacket packet, ServerUser sourceUser) {
		udpSender.sendVoiceLocally(packet, sourceUser);
	}
	
	public void sendVoiceGlobally(UdpPacket packet, ServerUser sourceUser) {
		udpSender.sendVoiceGlobally(packet, sourceUser);
	}
	
	public void send(UdpPacket packet, ServerUser receptor) {
		udpSender.send(packet, receptor);
	}
	
	public void send(UdpPacket packet, List<ServerUser> receptors) {
		udpSender.send(packet, receptors);
	}
	
	public static long getTotalDataRate() {
		return totalVoiceDataRate + (SoundCenter.userList.getInitializedUserCount() 
				* GlobalConstants.LOCATION_DATA_RATE);
	}
	
	public void shutdown() {
		SoundCenter.logger.i("Shutting down UDP-Server...", null);
		exit = true;
		udpSender.shutdown();
		datagramSocket.close();
	}
}
