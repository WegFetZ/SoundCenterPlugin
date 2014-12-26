package com.soundcenter.soundcenter.plugin.network.tcp.protocol;

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
import com.soundcenter.soundcenter.lib.util.StringUtil;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;

public class DataProtocol {

	public static boolean processPacket(byte cmd, TcpPacket receivedPacket, ServerUser user) {

		//SoundCenter.logger.d("Processing packet in DataProtocol.", null);
		
		/* client wants to edit a station */
		if (cmd == TcpOpcodes.SV_DATA_CMD_EDIT_STATION) {
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
			} else if (type == GlobalConstants.TYPE_WGREGION) {
				// new world must have the same name
				if (!oldStation.getName().equals(newStation.getName())) {
					SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "edit wgregion", null, user);
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

		/* client wants to add a song */
		} else if (cmd == TcpOpcodes.SV_DATA_CMD_ADD_SONG) {
			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "Could not get the Bukkit player instance.", null,
						user);
				return true;
			}

			Song song = (Song) receivedPacket.getKey();

			if (!player.hasPermission("sc.add.song")) {	
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_CREATE_PERMISSION, "sc.add.song", null, user);
				return true;
			}
			
			if (SoundCenter.database.getSong(song.getTitle()) != null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_ALREADY_EXISTS, "Song", null, user);
				return true;
			}
			
			song.setOwner(player.getName());
			SoundCenter.database.addSong(song);
			SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_SONG, song, null, null);
		
			return true;

		/* client wants to delete a song */
		} else if (cmd == TcpOpcodes.SV_DATA_CMD_DELETE_SONG) {
			Player player = user.getPlayer();
			if (player == null) {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_UNKNOWN, "Could not get the Bukkit player instance.", null,
						user);
				return true;
			}

			Song song = (Song) receivedPacket.getKey();

			if (song.getOwner().equalsIgnoreCase(player.getName()) || player.hasPermission("sc.delete.others")) {
				SoundCenter.database.removeSong(song);
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_CMD_DELETE_SONG, song, null, null);
			} else {
				SoundCenter.tcpServer.send(TcpOpcodes.CL_ERR_CREATE_PERMISSION, "sc.delete.others", null, user);
			}
		
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

			// send list of wgregions
			if (SoundCenter.getWorldGuard() != null) {
				for (Entry<Short, Station> entry : SoundCenter.database.wgRegions.entrySet()) {
					Station wgregion = entry.getValue();
					SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_STATION, wgregion, null, user);
				}
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
			for (Entry<String, Song> entry : SoundCenter.database.songs.entrySet()) {
				Song song = entry.getValue();
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_SONG, song, null, user);
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
