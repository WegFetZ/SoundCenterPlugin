package com.soundcenter.soundcenter.plugin.data;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.soundcenter.soundcenter.lib.data.Area;
import com.soundcenter.soundcenter.lib.data.Box;
import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.data.Station;

public class Database implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1872505392008447869L;

	public ConcurrentHashMap<Short, Station> boxes = new ConcurrentHashMap<Short, Station>();
	public ConcurrentHashMap<Short, Station> areas = new ConcurrentHashMap<Short, Station>();
	public ConcurrentHashMap<Short, Station> biomes = new ConcurrentHashMap<Short, Station>();
	public ConcurrentHashMap<Short, Station> worlds = new ConcurrentHashMap<Short, Station>();
	private ConcurrentHashMap<String, Integer> boxCounts = new ConcurrentHashMap<String, Integer>();
	private ConcurrentHashMap<String, Integer> areaCounts = new ConcurrentHashMap<String, Integer>();

	public void addStation(byte type, Station station) {
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			map.put(station.getId(), station);
			incrementCounter(station);
		}
	}

	public Station getStation(byte type, short id) {
		Station station = null;
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			station = (Station) map.get(id);
		}

		return station;
	}

	public Station getStationByName(byte type, String name) {
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			for (Entry<Short, Station> entry : map.entrySet()) {
				Station station = entry.getValue();
				if (station.getName().equals(name)) {
					return station;
				}
			}
		}

		return null;
	}

	public int getStationCount(byte type) {
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			return map.size();
		}

		return 0;
	}

	public int getStationCount(byte type, String player) {
		int count = 0;
		ConcurrentHashMap<Short, Station> map = getMap(type);

		if (map != null) {
			for (Entry<Short, Station> entry : map.entrySet()) {
				Station station = entry.getValue();
				if (station.getOwner().equals(player)) {
					count++;
				}
			}
		}

		return count;
	}

	public Station removeStation(byte type, short id) {
		Station station = null;

		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			station = map.remove(id);
			if (station != null) {
				decrementCounter(station);
			}
		}

		return station;
	}

	public void removeStation(Station station) {
		removeStation(station.getType(), station.getId());
	}

	public short getAvailableId(byte type) {
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			Random rand = new Random();
			short id = (short) (rand.nextInt(Short.MAX_VALUE + Math.abs(Short.MIN_VALUE)) - Math.abs(Short.MIN_VALUE));
			if (map.get(id) == null && id != 0) {
				return id;
			} else
				return getAvailableId(type);
		} else
			return 0;
	}

	public void removeSongFromStations(String path) {
		for (Entry<Short, Station> entry : areas.entrySet()) {
			entry.getValue().removeSong(path);
		}
		for (Entry<Short, Station> entry : boxes.entrySet()) {
			entry.getValue().removeSong(path);
		}
		for (Entry<Short, Station> entry : biomes.entrySet()) {
			entry.getValue().removeSong(path);
		}
		for (Entry<Short, Station> entry : worlds.entrySet()) {
			entry.getValue().removeSong(path);
		}
	}

	private void incrementCounter(Station station) {
		ConcurrentHashMap<String, Integer> map = null;

		if (station instanceof Area) {
			map = areaCounts;
		} else if (station instanceof Box) {
			map = boxCounts;
		}

		if (map != null) {
			String owner = station.getOwner();
			int count = 0;
			if (map.containsKey(owner)) {
				count = map.get(owner);
			}
			map.put(owner, ++count);
		}
	}

	private void decrementCounter(Station station) {
		ConcurrentHashMap<String, Integer> map = null;

		if (station instanceof Area) {
			map = areaCounts;
		} else if (station instanceof Box) {
			map = boxCounts;
		}

		if (map != null) {
			String owner = station.getOwner();
			int count = 0;
			if (map.containsKey(owner)) {
				count = map.get(owner);
			}
			if (count > 0) {
				map.put(owner, --count);
			}
		}
	}

	private ConcurrentHashMap<Short, Station> getMap(byte type) {
		switch (type) {
		case GlobalConstants.TYPE_AREA:
			return areas;
		case GlobalConstants.TYPE_BOX:
			return boxes;
		case GlobalConstants.TYPE_BIOME:
			return biomes;
		case GlobalConstants.TYPE_WORLD:
			return worlds;
		default:
			return null;
		}
	}
}
