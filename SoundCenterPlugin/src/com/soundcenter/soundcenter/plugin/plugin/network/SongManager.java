package com.soundcenter.soundcenter.plugin.plugin.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.data.Song;
import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.lib.util.FileOperation;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.plugin.data.ServerUser;

public class SongManager {
	
	public static void sendMidi(Song song, ServerUser user) {
		File file = new File(SoundCenter.musicDataFolder + song.getPath());
		if (!file.exists() || !GlobalConstants.supportedExtensions.contains(FileOperation.getExtension(song.getTitle()))) {
			SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_FILE_NOT_EXISTANT, song.getPath(), null, user);
			return;
		}
		
		//send file
		SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_SONG_TRANSFER_START, song , null, user);
		SoundCenter.logger.d("Sending " + song.getPath() + " to user: " + user.getName() + "...", null);
		try {
			FileInputStream fileIn = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			int bytesRead = 0;
			try {
				while(bytesRead != -1) {
					bytesRead = fileIn.read(buffer, 0, buffer.length);
					if (bytesRead > 0) {
						byte[] chunk = new byte[bytesRead];
						System.arraycopy(buffer, 0, chunk, 0, bytesRead);
						SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_SONG_CHUNK, chunk, null, user);
					}
				}
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_SONG_TRANSFER_END, song, null, user);
				SoundCenter.logger.d("Song " + song.getPath() + " successfully sent to user: " + user.getName(), null);
			} finally {
				fileIn.close();
			}
		} catch(IOException e) {
			SoundCenter.logger.i("Error while sending Song to user " + user.getName()
					+ ". Maybe user disconnected?", e);
			return;
		}
		
	}
	
	
	//TODO: bandwidth limitation
	public static void receiveSong(String fileName, long size, ServerUser uploadingUser) {
		String extension = FileOperation.getExtension(fileName).toLowerCase();
		Player player = uploadingUser.getPlayer();
		if (!GlobalConstants.supportedExtensions.contains(extension) || player == null) {
			SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, null, null, uploadingUser);
			return;
		}
		
		if (extension.equalsIgnoreCase("mid")) {
			extension = "midi";
		}
		
		if (!player.hasPermission("sc.upload." + extension)) { //player has no permission to upload these files
			SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UPLOAD_PERMISSION, extension, null, uploadingUser);
			return;
		}
		
		File file = new File(SoundCenter.musicDataFolder + uploadingUser.getName() + File.separator + fileName);
		File partFile = new File (SoundCenter.musicDataFolder + uploadingUser.getName() 
				+ File.separator + fileName + ".part");
		
		if (file.exists()) {
			file.delete();
		}
		
		long requiredSize = FileOperation.getDirectorySize(file.getParentFile()); 
		requiredSize += size;
		if (partFile.exists()) {
			partFile.delete();
		} else {
			try {
				partFile.getParentFile().mkdirs();
				partFile.createNewFile();
			} catch (IOException e) {
				SoundCenter.logger.i("Error while creating .part file for song-upload.", e);
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, null, null, uploadingUser);
				return;
			}
		}
		
		long maxStorage = SoundCenter.config.maxStorage()*1024*1024;
		if (requiredSize > maxStorage && maxStorage > 0 && !player.hasPermission("sc.nolimits")) {
			SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UPLOAD_MAX_STORAGE, (long) (maxStorage - (requiredSize-size))/1024, null,
					uploadingUser);
			partFile.delete();
			return;
		}
		
		//start receiving the file
		if (!uploadingUser.isUploadActive()) {
			uploadingUser.receiveSong(partFile, file, size);
			SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_SONG_UPLOAD_START, null, null, uploadingUser);
		}
	}
	
	public static void deleteSong(Song song, ServerUser deletingUser) {
		File file = new File(SoundCenter.musicDataFolder + song.getPath());
		Player player = deletingUser.getPlayer();
		if (!file.exists() || player == null) {
			SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, null, null, deletingUser);
			return;
		}
		
		if (player.getName().equals(song.getOwner()) || player.hasPermission("sc.others.delete")) {
			
			//delete the song from all stations
			SoundCenter.database.removeSongFromStations(song.getPath());
			
			//stop StreamSession for this file if active
			StreamManager.releaseFile(file);
			
			//delete file
			file.delete();
			SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_CMD_DELETE_SONG, song, null, null);
		} else {
			SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_OTHERS_DELETE_PERMISSION, null, null, deletingUser);
		}
		SoundCenter.logger.d("User " + deletingUser.getName() + " has deleted song: " + song.getPath(), null);
		return;
	}
	
}
