package com.soundcenter.soundcenter.plugin;

import java.util.HashMap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Configuration {
	
	private JavaPlugin plugin;
	
	//configuration variables
	private boolean debug;
	private boolean verifyIp;
	private boolean voiceEnabled;
	private String serverIp;
	private int port;
	private int serverCapacity;
	private int defaultBoxRange;
	private int defaultFadeout;
	private int maxBoxRange;
	private int maxAreaSize;
	private int maxBoxes;
	private int maxAreas;
	private int voiceRange;
	private int voiceRemindTime;
	private int maxBandwidth;
	
	public Configuration(JavaPlugin plugin) {
		this.plugin = plugin;
		load();
	}	
	
	public void load() {
        plugin.reloadConfig();

        FileConfiguration cfg = plugin.getConfig();
        cfg.options().copyDefaults(true);
        cfg.addDefaults(new HashMap<String,Object>(){
			{
            	put("Debug-Mode", false);
            	put("Server-IP", "");
            	put("Port", 4224);
            	put("Max-Users", 50);
            	put("Verify-By-IP-Address", true);
            	put("Enable-Voice-Chat", true);
            	put("Default-Box-Range", 25);
            	put("Default-Area-Fadeout", 10);
            	put("Voice-Range", 30);
            	put("Max-Box-Range", 50);
            	put("Max-Area-Edge-Length", 150);
            	put("Max-Boxes-Per-User", 5);
            	put("Max-Areas-Per-User", 3);
            	put("Remind-Of-Voice-Chat-Time", 60);
            	put("Limit-Streaming-Bandwidth-Kb/s", 2048);
            }
        });

        debug = cfg.getBoolean("Debug-Mode");
        verifyIp = cfg.getBoolean("Verify-By-IP-Address");
        voiceEnabled = cfg.getBoolean("Enable-Voice-Chat");
        serverIp = cfg.getString("Server-IP");
		port = cfg.getInt("Port");
		serverCapacity = cfg.getInt("Max-Users");
		defaultBoxRange = cfg.getInt("Default-Box-Range");
		defaultFadeout = cfg.getInt("Default-Area-Fadeout");
		voiceRange = cfg.getInt("Voice-Range");
		maxBoxRange = cfg.getInt("Max-Box-Range");
		maxAreaSize = cfg.getInt("Max-Area-Edge-Length");
		maxBoxes = cfg.getInt("Max-Boxes-Per-User");
		maxAreas = cfg.getInt("Max-Areas-Per-User");
		voiceRemindTime = cfg.getInt("Remind-Of-Voice-Chat-Time");
		maxBandwidth = cfg.getInt("Limit-Streaming-Bandwidth-Kb/s");
		
        plugin.saveConfig();
	}
	
	public boolean debug() { return debug; }
	public boolean verifyIp() { return verifyIp; }
	public boolean voiceEnabled() { return voiceEnabled; }
	public String serverIp() { return serverIp; }
	public int port() { return port; }
	public int serverCapacity() { return serverCapacity; }
	public int defaultBoxRange() { return defaultBoxRange; }
	public int defaultFadeout() { return defaultFadeout; }
	public int voiceRange() { return voiceRange; }
	public int maxBoxRange() { return maxBoxRange; }
	public int maxAreaSize() { return maxAreaSize; }
	public int maxBoxes() { return maxBoxes; }
	public int maxAreas() { return maxAreas; }
	public int voiceRemindTime() { return voiceRemindTime; }
	public int maxBandwidth() { return maxBandwidth; }
}
