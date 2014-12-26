package com.soundcenter.soundcenter.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.data.SCLocation;
import com.soundcenter.soundcenter.lib.data.SCLocation2D;
import com.soundcenter.soundcenter.lib.data.Station;
import com.soundcenter.soundcenter.lib.data.WGRegion;
import com.soundcenter.soundcenter.lib.util.FileOperation;
import com.soundcenter.soundcenter.plugin.commands.SCCommandExecutor;
import com.soundcenter.soundcenter.plugin.data.Database;
import com.soundcenter.soundcenter.plugin.data.UserList;
import com.soundcenter.soundcenter.plugin.network.tcp.TcpServer;
import com.soundcenter.soundcenter.plugin.network.udp.UdpServer;
import com.soundcenter.soundcenter.plugin.util.SCLogger;

public class SoundCenter extends JavaPlugin {

	public static final double MIN_CL_VERSION = 0.300;
	public static final double MAX_CL_VERSION = 0.399;

	public static String musicDataFolder = "";
	public static Configuration config = null;
	public static Database database = null;
	public static UserList userList = null;
	public static TcpServer tcpServer = null;
	public static UdpServer udpServer = null;
	public static SCLogger logger = null;

	public static File dataFile = new File("plugins" + File.separator + "SoundCenter" + File.separator + "data.scdb");

	private MainLoop mainLoop = new MainLoop();
	
	private static WorldGuardPlugin worldGuard = null;

	@Override
	public void onEnable() {
		Thread.currentThread().setName("SoundCenter Plugin");

		musicDataFolder = this.getDataFolder().getPath() + File.separator + "musicdata" + File.separator;
		File file = (new File(musicDataFolder));
		if (!file.exists()) {
			try {
				file.mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				logger.w("Error while creating Music Data Folder.", e);
			}
		}

		config = new Configuration(this);
		logger = new SCLogger(this.getLogger(), config.debug());
		userList = new UserList();

		// register commands
		getCommand("sc").setExecutor(new SCCommandExecutor(this));

		// register listener
		getServer().getPluginManager().registerEvents(new SCPlayerListener(), this);

		// load data
		if (dataFile.exists()) {
			try {
				database = (Database) FileOperation.loadObject(dataFile);
				
				if (getWorldGuard() == null) {
					logger.i("WorldGuard not loaded.", null);
				} else {
					for (Entry<Short, Station> entry : database.wgRegions.entrySet()) {
						WGRegion wgRegion = (WGRegion) entry.getValue();
						World world = getServer().getWorld(wgRegion.getPoints().get(0).getWorld());
						ProtectedRegion region = getWorldGuard().getRegionManager(world).getRegion(wgRegion.getName());
						if (region == null) {
							database.removeStation(wgRegion);
						} else {
							SCLocation min = new SCLocation(region.getMinimumPoint().getX(), 
									region.getMinimumPoint().getY(), region.getMinimumPoint().getZ(), 
									world.getName(), "null");
							SCLocation max = new SCLocation(region.getMaximumPoint().getX(), 
									region.getMaximumPoint().getY(), region.getMaximumPoint().getZ(), 
									world.getName(), "null");
							wgRegion.setMin(min);
							wgRegion.setMax(max);
							
							List<SCLocation2D> points = new ArrayList<SCLocation2D>();
							for (BlockVector2D point : region.getPoints()) {
								points.add(new SCLocation2D(point.getX(), point.getZ(), world.getName()));
							}
							wgRegion.setPoints(points);
						}
					}
				}
				
				logger.i(
						"Database loaded: " + database.areas.size() + " areas, " + database.boxes.size() + " boxes, "
								+ database.getStationCount(GlobalConstants.TYPE_BIOME) + " biome settings, "
								+ database.getStationCount(GlobalConstants.TYPE_WORLD) + " world settings and " 
								+ database.getStationCount(GlobalConstants.TYPE_WGREGION) + " WorldGuard regions.", null);				
			} catch (InvalidClassException e) {
				database = new Database();
				logger.w("Found Database of incompatible SoundCenter Version. Old Database will be backed up to data.old.scdb", null);
				try {
					Files.move(dataFile, new File("plugins" + File.separator + "SoundCenter" + File.separator + "data.old.scdb"));
				} catch(Exception e2){}
			} catch (Exception e) {
				logger.w("Error while loading data.", e);
			}
		} else
			database = new Database();		

		// start server
		tcpServer = new TcpServer(config.port(), config.serverBindAddr());
		udpServer = new UdpServer(config.port(), config.serverBindAddr());
		new Thread(tcpServer).start();
		new Thread(udpServer).start();
		new Thread(mainLoop).start();

	}

	public void onDisable() {
		tcpServer.shutdown();
		udpServer.shutdown();
		mainLoop.shutdown();

		try {
			FileOperation.saveObject(dataFile, database);
			SoundCenter.logger.i("Database saved: " + database.areas.size() + " areas, " + database.boxes.size()
					+ " boxes, " + database.biomes.size() + " biome settings and " + database.worlds.size()
					+ " world settings + " + database.wgRegions.size() + " WorldGuard regions.", null);
		} catch (IOException e) {
			SoundCenter.logger.w("Error while saving data.", e);
		}

		while (tcpServer.active || udpServer.active) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		logger.i("SoundCenter disabled!", null);
	}
	
	public static WorldGuardPlugin getWorldGuard() {
		if (worldGuard == null) {
			Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
			 
		    // WorldGuard may not be loaded
		    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
		        return null;
		    }
		    
		    worldGuard = (WorldGuardPlugin) plugin;
		}
		
		return worldGuard;
	}
}
