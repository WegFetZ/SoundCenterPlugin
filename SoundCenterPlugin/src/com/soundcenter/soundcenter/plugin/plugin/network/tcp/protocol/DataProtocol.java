package com.soundcenter.soundcenter.plugin.plugin.network.tcp.protocol;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.data.Song;
import com.soundcenter.soundcenter.lib.data.Station;
import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.lib.tcp.TcpPacket;
import com.soundcenter.soundcenter.lib.util.FileOperation;
import com.soundcenter.soundcenter.lib.util.StringUtil;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.plugin.network.SongManager;
import com.soundcenter.soundcenter.plugin.plugin.network.StreamManager;

public class DataProtocol {

	public static boolean processPacket(byte cmd, TcpPacket receivedPacket, ServerUser user) {

		//SoundCenter.logger.d("Processing packet in DataProtocol.", null);//TODO
		
		/* client sends a chunk of songdata */
		if (cmd == TcpOpcodes.SV_DATA_SONG_CHUNK) {
			byte[] chunk = (byte[]) receivedPacket.getKey();

			user.receiveSongChunk(chunk);

			return true;

		/* client wants to edit a station */
		} else if (cmd == TcpOpcodes.SV_DATA_CMD_EDIT_STATION) {
			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "Could not get the Bukkit player instance.", null,
						user);
				return true;
			}

			byte type = (Byte) receivedPacket.getKey();
			Station newStation = (Station) receivedPacket.getValue();
			short id = newStation.getId();
			Station oldStation = SoundCenter.database.getStation(type, id);

			// check if the new station has the same owner as the old station
			if (oldStation == null || newStation == null || !oldStation.getOwner().equals(newStation.getOwner())) {

				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "edit area", null, user);
				return true;
			}

			// check if the user has permission to edit the station
			if (!oldStation.getOwner().equals(user.getName()) && !oldStation.isEditableByOthers()
					&& !player.hasPermission("sc.others.edit")) {

				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_OTHERS_EDIT_PERMISSION, null, null, user);
				return true;
			}

			// check type sepcific conditions
			if (type == GlobalConstants.TYPE_AREA) {
				// new area must have the same corners
				if (!oldStation.getMin().sameAs(newStation.getMin())
						|| !oldStation.getMax().sameAs(newStation.getMax())) {
					SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "edit area", null, user);
					return true;
				}
			} else if (type == GlobalConstants.TYPE_BOX) {
				// new box must have the same location
				if (oldStation == null || !oldStation.getLocation().sameAs(newStation.getLocation())) {
					SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "edit box", null, user);
					return true;
				}
				int maxRange = SoundCenter.config.maxBoxRange();
				if (newStation.getRange() > maxRange && maxRange > 0 && !player.hasPermission("sc.nolimits")) {
					SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_EDIT_RANGE, maxRange, null, null);
					return true;
				}
			} else if (type == GlobalConstants.TYPE_BIOME) {
				// new biome must have the same name
				if (!oldStation.getName().equals(newStation.getName())) {
					SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "edit biome", null, user);
					return true;
				}
			} else if (type == GlobalConstants.TYPE_WORLD) {
				// new world must have the same name
				if (!oldStation.getName().equals(newStation.getName())) {
					SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "edit world", null, user);
					return true;
				}
			}

			SoundCenter.database.addStation(type, newStation);
			StreamManager.shutdownSession(type, id); //session needs to be restarted
			SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_STATION, newStation, null, null);
			return true;

		/* client wants to delete a station */
		} else if (cmd == TcpOpcodes.SV_DATA_CMD_DELETE_STATION) {
			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "delete station", null, user);
				return true;
			}

			byte type = (Byte) receivedPacket.getKey();
			short id = (Short) receivedPacket.getValue();
			Station station = SoundCenter.database.getStation(type, id);
			if (station == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "delete station", null, user);
				return true;
			}

			if (station.getOwner().equals(user.getName()) || user.getPlayer().hasPermission("sc.others.delete")) {
				SoundCenter.database.removeStation(type, id);
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_CMD_DELETE_STATION, type, id, null);
			} else {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_OTHERS_DELETE_PERMISSION, null, null, user);
			}
			return true;

			/* client wants to create a new station (only biome or world) */
		} else if (cmd == TcpOpcodes.SV_DATA_CMD_CREATE_STATION) {
			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "Could not get the Bukkit player instance.", null,
						user);
				return true;
			}

			byte type = (Byte) receivedPacket.getKey();
			Station station = (Station) receivedPacket.getValue();

			if (SoundCenter.database.getStationByName(type, station.getName()) != null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_ALREADY_EXISTS, "Station", null, user);
				return true;
			}

			String perm = "null";
			byte opcode = TcpOpcodes.CL_DATA_STATION;
			if (type == GlobalConstants.TYPE_BIOME) {
				perm = "sc.set.biome";
			} else if (type == GlobalConstants.TYPE_WORLD) {
				perm = "sc.set.world";
			}

			if (player.hasPermission(perm)) {
				station.setOwner(player.getName());
				station.setId(SoundCenter.database.getAvailableId(type));
				SoundCenter.database.addStation(type, station);
				SoundCenter.tcpServer.send(opcode, station, null, null);
			} else {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_CREATE_PERMISSION, perm, null, user);
			}
			
			return true;

		/* client stopped upload */
		} else if (cmd == TcpOpcodes.SV_DATA_INFO_UPLOAD_ENDED) {
			user.uploadEnded();
			return true;

		/* client wants to upload a song */
		} else if (cmd == TcpOpcodes.SV_DATA_CMD_RECEIVE_SONG) {
			String songName = (String) receivedPacket.getKey();
			long size = (Long) receivedPacket.getValue();

			SongManager.receiveSong(songName, size, user);
			return true;

		/* client wants to delete a song */
		} else if (cmd == TcpOpcodes.SV_DATA_CMD_DELETE_SONG) {
			Song song = (Song) receivedPacket.getKey();

			SongManager.deleteSong(song, user);
			return true;

		/* client wants to download a midi file */
		} else if (cmd == TcpOpcodes.SV_DATA_REQ_SONG) {
			Song song = (Song) receivedPacket.getKey();

			SongManager.sendMidi(song, user);
			return true;

		/* client requests area/box/biome and world data */
		} else if (cmd == TcpOpcodes.SV_DATA_REQ_INFODATA) {

			// Tell the user that we will send a bunch of info data
			SoundCenter.tcpServer.send(TcpOpcodes.CL_INFODATA_START, null, null, user);

			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "Could not get the Bukkit player instance.", null,
						user);
				return true;
			}

			// send list of existing biomes
			for (Biome biome : Biome.values()) {
				// cut string to make it comparable to the udp version
				SoundCenter.tcpServer.send(TcpOpcodes.CL_INFODATA_AVAILABLE_BIOME, StringUtil.cutForUdp(biome.toString()),
						null, user);
			}

			// send list of existing worlds:
			for (World world : Bukkit.getServer().getWorlds()) {
				// cut string to make it comparable to the udp version
				SoundCenter.tcpServer.send(TcpOpcodes.CL_INFODATA_AVAILABLE_WORLD, StringUtil.cutForUdp(world.getName()),
						null, user);
			}

			// send list of areas
			for (Entry<Short, Station> entry : SoundCenter.database.areas.entrySet()) {
				Station area = entry.getValue();
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_STATION, area, null, user);
			}

			// send list of boxes
			for (Entry<Short, Station> entry : SoundCenter.database.boxes.entrySet()) {
				Station box = entry.getValue();
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_STATION, box, null, user);
			}

			// send list of biomes
			for (Entry<Short, Station> entry : SoundCenter.database.biomes.entrySet()) {
				Station biome = entry.getValue();
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_STATION, biome, null, user);
			}

			// send list of worlds
			for (Entry<Short, Station> entry : SoundCenter.database.worlds.entrySet()) {
				Station world = entry.getValue();
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_STATION, world, null, user);
			}

			// send list of songs
			// do not send songs of others if user has no persmission to use
			// them
			File musicDataFolder = new File(SoundCenter.musicDataFolder);
			;
			List<File> files = FileOperation.listAllFiles(musicDataFolder);
			for (File file : files) {
				if (!file.isDirectory() && GlobalConstants.supportedExtensions.contains(
						FileOperation.getExtension(file.getName()))) {
					Song song = new Song(file.getParentFile().getName(), file.getName(), file.length());
					SoundCenter.tcpServer.send(TcpOpcodes.CL_INFODATA_SONG, song, null, user);
				}
			}

			// send list of permissions
			for (String permission : GlobalConstants.permissions) {
				if (player.hasPermission(permission)) {
					SoundCenter.tcpServer.send(TcpOpcodes.CL_INFODATA_PERMISSION, permission, null, user);
				}
			}

			SoundCenter.tcpServer.send(TcpOpcodes.CL_INFODATA_END, null, null, user);

			return true;
		}

		return true;
		
	}

}
