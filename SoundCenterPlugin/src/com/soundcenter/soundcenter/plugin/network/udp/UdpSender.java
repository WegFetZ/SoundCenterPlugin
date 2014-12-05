package com.soundcenter.soundcenter.plugin.network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.udp.UdpPacket;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;

public class UdpSender {
	
	private ExecutorService executor = null;
	private int nThreads = 1; //number of threads used for sending datagrampackets 
		/// TODO: change dynamically to fit server load
	private DatagramSocket datagramSocket = null;
	
	public UdpSender(DatagramSocket socket) {
		datagramSocket = socket;
		executor = Executors.newFixedThreadPool(nThreads);
	}
	
	public void send(UdpPacket packet, ServerUser receptor) {
		executor.execute(new Sender(packet, receptor));
	}
	
	public void send(UdpPacket packet, List<ServerUser> receptors) {
		executor.execute(new Sender(packet, receptors));
	}
	
	public void sendVoiceGlobally(UdpPacket packet, ServerUser sourceUser) {
		packet.setVolume((byte) 100);
		List<ServerUser> receptorList = new ArrayList<ServerUser>();
		for (Entry<Short, ServerUser> entry : SoundCenter.userList.acceptedUsers.entrySet()) {
			ServerUser user = entry.getValue();
			if(user.isInitialized() && !user.equals(sourceUser) && user.isVoiceActive() && !user.hasMuted(sourceUser.getId())) {
				receptorList.add(user);
			}
		}
		executor.execute(new Sender(packet, receptorList));
	}

	public void sendVoiceLocally(UdpPacket packet, ServerUser sourceUser) {
		for (Entry<ServerUser, Byte> entry : sourceUser.listeners.entrySet()) {
			ServerUser user = entry.getKey();
			if(user.isInitialized() && user.isVoiceActive() && !user.hasMuted(sourceUser.getId())) {
				byte volume = entry.getValue();
				packet.setVolume(volume);
				executor.execute(new Sender(packet, user));
			}
		}
	}
	
	public void shutdown() {
		executor.shutdownNow();
	}
	

	private class Sender implements Runnable {
		
		private UdpPacket dataPacket;
		private List<ServerUser> receptors;
		private ServerUser singleReceptor;
		
		private Sender(UdpPacket dataPacket, List<ServerUser> receptors) {
			this.dataPacket = dataPacket;
			this.receptors = receptors;
		}
		
		private Sender(UdpPacket dataPacket, ServerUser user) {
			this.dataPacket = dataPacket;
			this.singleReceptor = user;
		}
		
		public void run() {
			
			dataPacket.setIdent(GlobalConstants.UDP_IDENT);
			int receptorPort = 0;
			
			try {
				if (receptors != null) {
					for (ServerUser user : receptors) {
						receptorPort = user.getUdpPort();
						if (receptorPort != 0) {
							dataPacket.setDestUserID(user.getId());
							dataPacket.setSeq(user.getSequenceNr());
							user.incSequenceNr();
							DatagramPacket packet = new DatagramPacket(dataPacket.getData(), dataPacket.getLength()
									, user.getIp(), receptorPort);
							datagramSocket.send(packet);
						}
					}
				} else if (singleReceptor != null) {
					receptorPort = singleReceptor.getUdpPort();
					if (receptorPort != 0) {
						dataPacket.setDestUserID(singleReceptor.getId());
						dataPacket.setSeq(singleReceptor.getSequenceNr());
						singleReceptor.incSequenceNr();
						DatagramPacket packet = new DatagramPacket(dataPacket.getData(), dataPacket.getLength()
								, singleReceptor.getIp(), receptorPort);
						datagramSocket.send(packet);
					}
				}
			} catch (SecurityException e) {
				if (!SoundCenter.udpServer.exit) {
					SoundCenter.logger.w("SecurityException while sending UDP-Packet:", e);
				}
				SoundCenter.udpServer.shutdown();
			} catch (IOException e) {
				if (!SoundCenter.udpServer.exit) {
					SoundCenter.logger.i("Error while sending UDP-Packet:", e);
				}
				SoundCenter.udpServer.shutdown();
			}
		}
	}
	
}
