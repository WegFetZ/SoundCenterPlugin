package com.soundcenter.soundcenter.plugin.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.soundcenter.soundcenter.lib.data.Song;
import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.plugin.SoundCenter;

public class UserUploadManager implements Runnable {

	private BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>();
	
	private File partFile = null;
	private File file = null;
	private long size = 0;
	private ServerUser user = null;
	
	private Thread thread = null;
	private boolean uploadEnded = false;
	private boolean active = false;
	private boolean exit = false;
	
	
	public UserUploadManager(File partFile, File file, long size, ServerUser user) {
		this.partFile = partFile;
		this.file = file;
		this.size = size;
		this.user = user;
	}
	
	@Override
	public void run() {
		thread = Thread.currentThread();
		thread.setName("UserUploadManager - " + user.getName());
		
		active = true;
		
		try {
			FileOutputStream fileOut = new FileOutputStream(partFile);
			try {
				while(!exit && !user.getDisconnect()) {
					fileOut.write(queue.take());
				}
		
			} finally {
				if (uploadEnded && !user.getDisconnect()) {
					for (byte[] data : queue) {
						fileOut.write(data);
					}
				}
				fileOut.close();
			}
		} catch (IOException e) {
			SoundCenter.logger.i("Error while receiving song from user " + user.getName(), e);
			return;
		} catch (InterruptedException e) {}
		
		if (partFile.length() >= size) { //if upload is complete
			partFile.renameTo(file);
			
			//inform all users about the new song
			Song song = new Song(user.getName(), file.getName(), size);
			SoundCenter.tcpServer.send(TcpOpcodes.CL_INFODATA_SONG, song, null, null);
			//inform the uploading user that the server is ready for the next song
			SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_SONG_UPLOAD_DONE, null, null, user);
			
			SoundCenter.logger.d("User " + user.getName() + " has uploaded song " + file.getName(), null);
		}
		partFile.delete();
		
		active = false;
		
	}
	
	public void feed(byte[] chunk) {
		queue.add(chunk);
	}
	
	public void uploadEnded() {
		uploadEnded = true;
		shutdown();
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void shutdown() {
		exit = true;
		thread.interrupt();
	}

}
