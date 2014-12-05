package com.soundcenter.soundcenter.plugin.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.plugin.SoundCenter;

public class ServerUser {

	/* tcp */
	private Socket socket = null;
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;
	private InetAddress ip = null;
	private String name = null;
	private boolean disconnect = false;
	private boolean accepted = false;
	private boolean joinRequested = false;
	private boolean versionOK = false;
	private boolean nameOK = false;
	private boolean initialized = false;
	private String quitReason = "Unknown.";
	
	private UserUploadManager uploadManager = null;
	
	/* udp */
	private int udpPort = 0;
	private short sequenceNr = Short.MIN_VALUE;
	
	/* misc */
	private short id = 0;
	private boolean musicActive = true;//TODO: do i really need this?
	private boolean voiceActive = true;
	private boolean speaking = false;
	private boolean speakingGlobally = false;
	private long speakingStartTime = 0;
	public ConcurrentHashMap<ServerUser, Byte> listeners = new ConcurrentHashMap<ServerUser, Byte>();
	private List<Short> mutedUsers = Collections.synchronizedList(new ArrayList<Short>());
	
	
	public ServerUser(Socket socket) {
		this.id = SoundCenter.userList.getAvailableId();
		this.socket = socket;
		if (socket != null) {
			ip = socket.getInetAddress();
			try {
				OutputStream out = socket.getOutputStream();
				InputStream in = socket.getInputStream();
				oos = new ObjectOutputStream(out);			
				ois = new ObjectInputStream(in);
			} catch (IOException e) {
				SoundCenter.logger.d("Error while opening TCP-streams for user:" + ip + " (IP).", e);
				disconnect();
			}
		} else {
			SoundCenter.logger.d("Cannot establish TCP-connection to new user, due to invalid socket.", null);
			disconnect();
		}
	}
	
	/*########################## TCP #######################*/
	public Socket getSocket() {	return socket; }
	
	public ObjectOutputStream getOos() { return oos; }
	
	public ObjectInputStream getOis() { return ois; }
	
	public InetAddress getIp() { return ip;	}
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public boolean getDisconnect() { return disconnect;	}
	
	public boolean isAccepted() { return accepted;	}
	public void setAccepted(Boolean accepted) { this.accepted = accepted; }
	
	public boolean hasJoinRequested() { return joinRequested; }
	public void setJoinRequested(boolean joinRequested) { this.joinRequested = joinRequested; }
	
	public boolean isVersionOK() { return versionOK; }
	public void setVersionOK(boolean versionOK) { this.versionOK = versionOK; }
	
	public boolean isNameOK() { return nameOK; }
	public void setNameOK(boolean nameOK) { this.nameOK = nameOK; }
	
	public boolean isInitialized() { return initialized; }
	public void setInitialized(boolean initialized) { this.initialized = initialized; }

	public String getQuitReason() { return quitReason; }
	public void setQuitReason(String quitReason) { this.quitReason = quitReason; }
	
	public void disconnect() { 
		SoundCenter.tcpServer.send(TcpOpcodes.CL_CON_INFO_DISCONNECT, "Disconnect by server.", null, this);
		
		//wait for tcp packet to be sent before disconnecting
		try { Thread.sleep(100); } catch(InterruptedException e ){}
		
		this.disconnect = true; 
		if (uploadManager != null) {
			uploadManager.shutdown();
		}
		try {
			socket.close();
		} catch (IOException e) {}
	}
	
	public void receiveSongChunk(byte[] chunk) {
		if (uploadManager != null && uploadManager.isActive()) {
			uploadManager.feed(chunk);
		}
	}
	
	public void receiveSong(File partFile, File file, long size) {
		if (uploadManager == null || !uploadManager.isActive()) {
			uploadManager = new UserUploadManager(partFile, file, size, this);
			new Thread(uploadManager).start();
		}
	}
	
	public void uploadEnded() {
		if (uploadManager != null) {
			uploadManager.uploadEnded();
		}
	}
	
	public boolean isUploadActive() {
		return (uploadManager != null && uploadManager.isActive());
	}
	
	/*########################## UDP #######################*/
	public int getUdpPort() { return udpPort; }
	public void setUdpPort(int value) { this.udpPort = value; }
	
	public short getSequenceNr(){ return sequenceNr; }
	public void incSequenceNr() { sequenceNr ++; }
	
	
	/*########################## MISC #######################*/
	public short getId() { return id; }
	public void setId(short id) { this.id = id; }
	
	public Player getPlayer() { 
		if (name !=null) {
			return Bukkit.getPlayer(name); 
		}
		return null;
	}
	
	public boolean isMusicActive() { return musicActive; }
	public void setMusicActive(boolean value) { this.musicActive = value; }
	
	public boolean isVoiceActive() { return voiceActive; }
	public void setVoiceActive(boolean value) { this.voiceActive = value; }
	
	public boolean isSpeaking() { return speaking; }
	public void setSpeaking(boolean speaking) {
		speakingStartTime = System.currentTimeMillis();
		this.speaking = speaking; 
	}
	
	public boolean isSpeakingGlobally() { return speakingGlobally; }
	public void setSpeakingGlobally(boolean speakingGlobally) { 
		speakingStartTime = System.currentTimeMillis();
		this.speakingGlobally = speakingGlobally; 
	}
	
	public long getSpeakingStartTime() {
		return speakingStartTime;
	}
	public void setSpeakingStartTime(long time) {
		this.speakingStartTime = time;
	}
	
	public void addListener(ServerUser user, byte volume) {
		listeners.put(user, volume);
	}
	public void removeListener(ServerUser user) {
		listeners.remove(user);
	}
	
	public void addMutedUser(short userId) {
		mutedUsers.add(userId);
	}
	public void removeMutedUser(short userId) {
		mutedUsers.remove(userId);
	}
	public boolean hasMuted(short userId) {
		return mutedUsers.contains(userId);
	}
}
