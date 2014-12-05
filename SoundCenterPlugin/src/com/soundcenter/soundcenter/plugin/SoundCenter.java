package com.soundcenter.soundcenter.plugin;

import java.io.File;
import java.io.IOException;
import java.util.TimerTask;

import org.bukkit.plugin.java.JavaPlugin;

import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.util.FileOperation;
import com.soundcenter.soundcenter.plugin.commands.SCCommandExecutor;
import com.soundcenter.soundcenter.plugin.data.Database;
import com.soundcenter.soundcenter.plugin.data.UserList;
import com.soundcenter.soundcenter.plugin.network.StreamManager;
import com.soundcenter.soundcenter.plugin.network.tcp.TcpServer;
import com.soundcenter.soundcenter.plugin.network.udp.UdpServer;
import com.soundcenter.soundcenter.plugin.util.SCLogger;

public class SoundCenter extends JavaPlugin {

	public static final double MIN_CL_VERSION = 0.100;
	public static final double MAX_CL_VERSION = 0.199;

	public static String musicDataFolder = "";
	public static Configuration config = null;
	public static Database database = null;
	public static UserList userList = null;
	public static TcpServer tcpServer = null;
	public static UdpServer udpServer = null;
	public static SCLogger logger = null;

	private File dataFile = new File("plugins" + File.separator + "SoundCenter" + File.separator + "data.scdb");
	private int saveDataTaskId = 0;

	private PositionsUpdater positionsUpdater = new PositionsUpdater();

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
				logger.i(
						"Database loaded: " + database.areas.size() + " areas, " + database.boxes.size() + " boxes, "
								+ database.getStationCount(GlobalConstants.TYPE_BIOME) + " biome settings and "
								+ database.getStationCount(GlobalConstants.TYPE_WORLD) + " world settings.", null);
			} catch (IOException e) {
				logger.w("Error while loading data.", e);
			} catch (ClassNotFoundException e) {
				logger.w("Error while loading data.", e);
			}
		} else
			database = new Database();

		// save the database every 5 minutes
		saveDataTaskId = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new TimerTask() {
			@Override
			public void run() {
				try {
					FileOperation.saveObject(dataFile, database);
					SoundCenter.logger.d("Database saved.", null);
				} catch (IOException e) {
					SoundCenter.logger.w("Error while saving data.", e);
				}
			}
		}, 20 * 60 * 2, 20 * 60 * 5); // 20 ticks = 1 second
		

		// start server
		tcpServer = new TcpServer(config.port());
		udpServer = new UdpServer(config.port());
		new Thread(tcpServer).start();
		new Thread(udpServer).start();
		new Thread(positionsUpdater).start();

	}

	public void onDisable() {
		StreamManager.shutdownAll();
		tcpServer.shutdown();
		udpServer.shutdown();
		positionsUpdater.shutdown();
		
		//stop saving the database
		this.getServer().getScheduler().cancelTask(saveDataTaskId);

		try {
			FileOperation.saveObject(dataFile, database);
			SoundCenter.logger.i("Database saved: " + database.areas.size() + " areas, " + database.boxes.size()
					+ " boxes, " + database.biomes.size() + " biome settings and " + database.worlds.size()
					+ " world settings.", null);
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
}
