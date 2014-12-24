package com.soundcenter.soundcenter.plugin.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class UserList {
	
	public List<ServerUser> connectedUsers = Collections.synchronizedList(new ArrayList<ServerUser>());
	public ConcurrentHashMap<Short, ServerUser> acceptedUsers = new ConcurrentHashMap<Short, ServerUser>();
	public ConcurrentHashMap<String, Short> acceptedUsersName = new ConcurrentHashMap<String, Short>();

	private int initializedUserCount = 0;
	
	public void addConnectedUser(ServerUser user) {
		connectedUsers.add(user);
	}
	
	public void addAcceptedUser(ServerUser user) {
		short id = user.getId();
		acceptedUsersName.put(user.getName(), id);
		acceptedUsers.put(id, user);
	}
	
	public ServerUser getAcceptedUserByName(String name) {
		if (!acceptedUsersName.containsKey(name))
			return null;
		
		short id = acceptedUsersName.get(name);
		return acceptedUsers.get(id);
	}
	
	public ServerUser getAcceptedUserById(Short id) {
		return acceptedUsers.get(id);
	}
	
	public int serverUserCount() {
		return connectedUsers.size();
	}
	
	public int getInitializedUserCount() {
		return initializedUserCount;
	}
	public void incrementInitializedUserCount() {
		initializedUserCount++;
	}
	
	public void removeServerUser(ServerUser user) {
		connectedUsers.remove(user);
		if (user.isAccepted()) {
			acceptedUsersName.remove(user.getName());
			acceptedUsers.remove(user.getId());
			if (user.isInitialized()) {
				initializedUserCount --;
			}
		}
	}
	
	public void resetServerUsers() {
		connectedUsers.clear();
		acceptedUsersName.clear();
		acceptedUsers.clear();
	}
	
	public short getAvailableId() {
		Random rand = new Random();
		short id = (short) (rand.nextInt(Short.MAX_VALUE + Math.abs(Short.MIN_VALUE)) - Math.abs(Short.MIN_VALUE));
		if (id != 0 && !acceptedUsers.contains(id)) {
			return (short) id;
		} else {
			return getAvailableId();
		}
			
	}
}
