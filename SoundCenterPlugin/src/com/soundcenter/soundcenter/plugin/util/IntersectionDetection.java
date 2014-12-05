package com.soundcenter.soundcenter.plugin.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.soundcenter.soundcenter.lib.data.Area;
import com.soundcenter.soundcenter.lib.data.Box;
import com.soundcenter.soundcenter.lib.data.SCLocation;
import com.soundcenter.soundcenter.lib.data.Station;
import com.soundcenter.soundcenter.plugin.SoundCenter;

public class IntersectionDetection {

	public static short playerCanHear(Player player1, Player player2, int range) {
		short distance = -1;
		
		Location loc1 = player1.getLocation();
		Location loc2 = player2.getLocation();
		
		if (loc1.getWorld().equals(loc2.getWorld())) {
			if (locIsNear(loc1, loc2, range)) {
				double dist = loc1.distance(loc2);
				if (dist <= range)
					distance = (short) dist;
			}
		}
		
		return distance;
	}
	
	public static boolean boxOverlaps(Box newBox) {
		
		SCLocation loc = newBox.getLocation();
		int radius = newBox.getRange();
		
		for (Map.Entry<Short, Station> entry : SoundCenter.database.boxes.entrySet()) {
			Station oldBox = entry.getValue();
            try {
            	if(locIsNear(oldBox.getLocation(), loc, oldBox.getRange()+radius)) {
		            double distance = oldBox.getLocation().distance(loc);
		            
		            // if boxes overlap, return true
		            if (!isNaN(distance) && distance < oldBox.getRange() + radius)
		          	  	return true;
            	}
            } catch (IllegalArgumentException e) {}
        }
		
		for (Map.Entry<Short, Station> entry : SoundCenter.database.areas.entrySet())
        {
			Station area = entry.getValue();
            double distance = distToAreaBorder(loc, area, true);

            // if box intersects with area, return true
            if ((distance > 0 && distance < radius) || (distance < 0 && Math.abs(distance) < radius))
            	return true;
        }		
		
		return false;
	}
	
	public static boolean areaOverlaps(Area newArea) {
		
		for (Map.Entry<Short, Station> entry : SoundCenter.database.boxes.entrySet())
        {
			Station box = entry.getValue();
            int radius = box.getRange();
            double distance = distToAreaBorder(box.getLocation(), newArea, true);
            
            // if area intersects with box, return true
            if ((distance > 0 && distance < radius) || (distance < 0 && Math.abs(distance) < radius))
            	return true;
        }
		
		for (Map.Entry<Short, Station> entry : SoundCenter.database.areas.entrySet())
        {
			Station oldArea = entry.getValue();
            
            // if areas intersect, return true;
            if (areasIntersect(newArea, oldArea))
            	return true;
        }
		
		return false;
	}
	
	public static HashMap<Short, Double> inRangeOfBox(SCLocation loc) {
		HashMap<Short, Double> matches = new HashMap<Short, Double>();
		
		for (Map.Entry<Short, Station> entry : SoundCenter.database.boxes.entrySet())
        {
            short id = entry.getKey();
            Station box = entry.getValue();
            SCLocation center = box.getLocation();
            try {
            	if (locIsNear(center, loc, box.getRange())) {
		            double distance = center.distance(loc);
		            
		            /* if in range of box, add box id and distance to list */
		            if (!isNaN(distance) && distance <= box.getRange()) {
		            	matches.put(id, distance);
		            }
            	}
            } catch (IllegalArgumentException e) {}
        }
		
		return matches;
	}
	
	public static HashMap<Short, Double> isInArea(SCLocation loc) {
		HashMap<Short, Double> matches = new HashMap<Short, Double>();
		
		for (Map.Entry<Short, Station> entry : SoundCenter.database.areas.entrySet())
        {
            short id = entry.getKey();
            Station area = entry.getValue();
            double distance = distToAreaBorder(loc, area, false);
            
            /* if in area, add area id and distance to border to list */
            if (distance > 0) {
            	matches.put(id, distance);
            }
        }
		return matches;
	}
	
	private static double distToAreaBorder(SCLocation loc, Station area, Boolean calcDistIfOutside) {
		
		if (!loc.getWorld().equals(area.getWorld()))
			return -1;
		
		double dist = 0;
		boolean contains = false;
		
		SCLocation min = area.getMin();
		SCLocation max = area.getMax();
		
		
		// check if cuboid contains loc
		boolean betweenX = false;
		boolean betweenY = false;
		boolean betweenZ = false;
		if ((loc.getX() <= max.getX()) && (loc.getX() >= min.getX()))
			betweenX = true;
		if ((loc.getY() <= max.getY()) && (loc.getY() >= min.getY()))
			betweenY = true;
		if ((loc.getZ() <= max.getZ()) && (loc.getZ() >= min.getZ()))
			betweenZ = true;
		if(betweenX && betweenZ && betweenY)
			contains = true;
		
		if (contains || calcDistIfOutside) {
			//calculate distance to borders
			double distX = Math.min(Math.abs(min.getX() - loc.getX()), Math.abs(max.getX() - loc.getX()));
			double distY = Math.min(Math.abs(min.getY() - loc.getY()), Math.abs(max.getY() - loc.getY()));
			double distZ = Math.min(Math.abs(min.getZ() - loc.getZ()), Math.abs(max.getZ() - loc.getZ()));
			
			//get minimum distance
			if (contains)
				dist = Math.min(Math.min(distX, distY), Math.min(distZ, Math.min(distX, distY)));
			
			else {
				if (betweenX && betweenY)
					dist = distZ;
				else if (betweenX && betweenZ)
					dist = distY;
				else if (betweenY && betweenZ)
					dist = distX;
				else if (betweenX)
					dist = Math.sqrt(Math.pow(distY, 2) + Math.pow(distZ, 2));
				else if (betweenY)
					dist = Math.sqrt(Math.pow(distX, 2) + Math.pow(distZ, 2));
				else if (betweenZ)
					dist = Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
				else
					dist = Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2) + Math.pow(distZ, 2));
				
				//use negative distance for points from outside
				dist = -dist;
			}
		} 
			 
		return dist;
	}
	
	private static boolean areasIntersect(Station area1, Station area2) {
		
		if (!area1.getWorld().equals(area2.getWorld()))
			return false;
		
		SCLocation min1 = area1.getMin();
		SCLocation max1 = area1.getMax();
		SCLocation min2 = area2.getMin();
		SCLocation max2 = area2.getMax();
		
		return (max1.getX() >= min2.getX() && min1.getX() <= max2.getX())
				&& (max1.getY() >= min2.getY() && min1.getY() <= max2.getY())
				&& (max1.getZ() >= min2.getZ() && min1.getZ() <= max2.getZ());
	}
	
	private static boolean locIsNear(Location loc1, Location loc2, int range) {
		if (loc1.getWorld().equals(loc2.getWorld())) {
			//check if location are near each other
			if ((Math.abs(loc1.getX()-loc2.getX()) <= range) && (Math.abs(loc1.getY()-loc2.getY()) <= range)
					&& (Math.abs(loc1.getZ()-loc2.getZ()) <= range)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean locIsNear(SCLocation loc1, SCLocation loc2, int range) {
		if (loc1.getWorld().equals(loc2.getWorld())) {
			//check if location are near each other
			if ((Math.abs(loc1.getX()-loc2.getX()) <= range) && (Math.abs(loc1.getY()-loc2.getY()) <= range)
					&& (Math.abs(loc1.getZ()-loc2.getZ()) <= range)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isNaN(double x) {
		return x != x;
	}
	
}
