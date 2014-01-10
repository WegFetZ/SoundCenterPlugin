package com.soundcenter.soundcenter.plugin.plugin.network;

import java.io.File;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.data.Song;
import com.soundcenter.soundcenter.lib.data.Station;
import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.plugin.network.udp.StreamSession;

public class StreamManager {
		
	private static ConcurrentHashMap<Short, StreamSession> areaSessions = new ConcurrentHashMap<Short, StreamSession>();
	private static ConcurrentHashMap<Short, StreamSession> boxSessions = new ConcurrentHashMap<Short, StreamSession>();
	private static ConcurrentHashMap<Short, StreamSession> biomeSessions = new ConcurrentHashMap<Short, StreamSession>();
	private static ConcurrentHashMap<Short, StreamSession> worldSessions = new ConcurrentHashMap<Short, StreamSession>();
	public static StreamSession globalSession = null;
	
	public static long totalVoiceRate = 0;
	
	public static void setGlobalSession(Song song) {
		//do nothing if there is already a global session
		if (globalSession == null) {
			StreamSession session = new StreamSession(GlobalConstants.TYPE_GLOBAL, (short) 1, song);
			for(Entry<Short, ServerUser> entry : SoundCenter.userList.acceptedUsers.entrySet()) {
				ServerUser user = entry.getValue();
				if (user.isInitialized() && user.isMusicActive()) {
					session.addListener(user);
				}
			}
			new Thread(session).start();
		}
	}
	
	public static void addUserToSession(byte type, Short id, ServerUser user) {
			
		//check server load TODO: more precise
		if(getTotalRate() + 20000 > SoundCenter.config.maxStreamBandwidth()*1024) {
			SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_STREAM_SERVER_LOAD, null, null, user);
			return;
		}
		
		StreamSession session = getStreamSession(type, id);
		Station station = SoundCenter.database.getStation(type, id);
		if (station == null) {
			SoundCenter.logger.d("Station type " + type + " ID: " + id + " not found.", null);
			return;
		}
		
		boolean startNewSession = false;
		if (session != null) {
			//add the user as listener to the existing session
			if (!session.addListener(user)) {
				//session is exiting... start a new one
				startNewSession = true;
			}
		} else {
			startNewSession = true;
		}
		
		if (startNewSession) {
			if (!station.getSongs().isEmpty()) {
				//start a new session and add the user as listener
				session = new StreamSession(type, id);
				session.addListener(user);
				new Thread(session).start();
			} else {
				return;
			}
		}
	}
	
	public static void removeUserFromSession(byte type, short id, ServerUser user) {
		StreamSession session = getStreamSession(type, id);
		
		if (session != null) {
			session.removeListener(user);
		}
	}
	
	public static void shutdownSession(byte type, short id) {
		StreamSession session = getStreamSession(type, id);
		
		if (session != null) {
			session.shutdown();
		}
	}
	
	public static void shutdownAll() {
		for (Entry<Short, StreamSession> entry : areaSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.shutdown();
		}
		for (Entry<Short, StreamSession> entry : boxSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.shutdown();
		}
		for (Entry<Short, StreamSession> entry : biomeSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.shutdown();
		}
		for (Entry<Short, StreamSession> entry : worldSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.shutdown();
		}
		if (globalSession != null) {
			globalSession.shutdown();
		}
	}
	
	public static void removeUserFromAllSessions(ServerUser user) {
		for (Entry<Short, StreamSession> entry : areaSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.removeListener(user);
		}
		for (Entry<Short, StreamSession> entry : boxSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.removeListener(user);
		}
		for (Entry<Short, StreamSession> entry : biomeSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.removeListener(user);
		}
		for (Entry<Short, StreamSession> entry : worldSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.removeListener(user);
		}
		if (globalSession != null) {
			globalSession.removeListener(user);
		}
	}
	
	public static void addSessionToList(byte type, short id, StreamSession session) {
		ConcurrentHashMap<Short, StreamSession> map = getMap(type);
		if (map != null) {
			map.put(id, session);
		}
		
		if (type == GlobalConstants.TYPE_GLOBAL) {
			globalSession = session;
		}
	}
	
	public static void removeSessionFromList(byte type, short id) {
		ConcurrentHashMap<Short, StreamSession> map = getMap(type);
		if (map != null) {
			map.remove(id);
		}
		
		if (type == GlobalConstants.TYPE_GLOBAL) {
			globalSession = null;
		}
	}
	
	public static void releaseFile(File file) {
		for (Entry<Short, StreamSession> entry : areaSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.releaseFile(file);
		}
		for (Entry<Short, StreamSession> entry : boxSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.releaseFile(file);
		}
		for (Entry<Short, StreamSession> entry : biomeSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.releaseFile(file);
		}
		for (Entry<Short, StreamSession> entry : worldSessions.entrySet()) {
			StreamSession session = entry.getValue();
			session.releaseFile(file);
		}
		if (globalSession != null) {
			globalSession.releaseFile(file);
		}
	}
	
	private static StreamSession getStreamSession(byte type, short id) {
		ConcurrentHashMap<Short, StreamSession> map = getMap(type);
		if (map != null) {
			return map.get(id);
		}
		
		if (type == GlobalConstants.TYPE_GLOBAL) {
			return globalSession;
		}
		
		return null;
	}
	
	private static ConcurrentHashMap<Short, StreamSession> getMap(byte type) {
		switch (type) {
		case GlobalConstants.TYPE_AREA:
			return areaSessions;
			
		case GlobalConstants.TYPE_BOX:
			return boxSessions;
			
		case GlobalConstants.TYPE_BIOME:
			return biomeSessions;
			
		case GlobalConstants.TYPE_WORLD:
			return worldSessions;
		default:
			return null;
		}
	}
	
	public static long getTotalRate() {
		long totalRate = totalVoiceRate;
		for (Entry<Short, StreamSession> entry : areaSessions.entrySet()) {
			StreamSession session = entry.getValue();
			totalRate += session.getTotalRate();
		}
		for (Entry<Short, StreamSession> entry : boxSessions.entrySet()) {
			StreamSession session = entry.getValue();
			totalRate += session.getTotalRate();
		}
		for (Entry<Short, StreamSession> entry : biomeSessions.entrySet()) {
			StreamSession session = entry.getValue();
			totalRate += session.getTotalRate();
		}
		for (Entry<Short, StreamSession> entry : worldSessions.entrySet()) {
			StreamSession session = entry.getValue();
			totalRate += session.getTotalRate();
		}
		//TODO globalSession
		
		return totalRate;
	}
}
