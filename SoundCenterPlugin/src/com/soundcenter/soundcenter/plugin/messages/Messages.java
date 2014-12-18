package com.soundcenter.soundcenter.plugin.messages;

import org.bukkit.ChatColor;

public class Messages {
	
	//colors	
	public static final ChatColor RED = ChatColor.RED;
	public static final ChatColor GREEN = ChatColor.GREEN;
	public static final ChatColor BLUE = ChatColor.BLUE;
	public static final ChatColor WHITE = ChatColor.WHITE;
	
	public static final String prefix = "[" + GREEN + "Sound" 
										+ BLUE + "Center" + WHITE + "] ";
	
	
	public static final String INFO_START_AUDIOCLIENT_PT1 = prefix + GREEN 
													+ "Vist " + BLUE + ChatColor.UNDERLINE ;
	public static final String INFO_START_AUDIOCLIENT_PT2 =  "" + ChatColor.RESET + GREEN + " to use ingame-music and voice chat.";
	public static final String INFO_INIT_SUCCESS = prefix + GREEN + "AudioClient initialized!";
	public static final String INFO_USERS = prefix + GREEN + "Players using SoundCenter: ";
	public static final String INFO_USER_MUTED = prefix + GREEN + "Muted user: ";
	public static final String INFO_USER_UNMUTED = prefix + GREEN + "Unmuted user: ";
	public static final String INFO_MUSIC_MUTED = prefix + GREEN + "Music muted!";
	public static final String INFO_MUSIC_UNMUTED = prefix + GREEN + "Music unmuted!";
	public static final String INFO_VOICE_MUTED = prefix + GREEN + "Voice chat muted!";
	public static final String INFO_VOICE_UNMUTED = prefix + GREEN + "Voice chat unmuted!";
	public static final String INFO_VOLUME_CHANGED = prefix + GREEN + "Volume set to ";
	public static final String INFO_AREA_CREATED = prefix + GREEN + "Area created! Use the audioclient to change its settings. ID: ";
	public static final String INFO_BOX_CREATED = prefix + GREEN + "Box created! Use the audioclient to change its settings. ID: ";
	public static final String INFO_WGREGION_CREATED = prefix + GREEN + "WorldGuard region set as SoundCenter station! "
															+ "Use the audioclient to change its settings. ID: ";
	public static final String INFO_SPEAKING = prefix + GREEN + "Voice chat enabled!";
	public static final String INFO_SPEAKING_GLOBALLY = prefix + GREEN + "Global voice chat enabled!";
	public static final String INFO_STILL_SPEAKING = prefix + "Your voice chat is still enabled! You can turn it off by using " 
													+ GREEN + "/sc speak " + WHITE + "again";
	public static final String INFO_NOT_SPEAKING = prefix + GREEN + "Voice chat disabled!";
	
	public static final String INFO_HELP = prefix + "Available commands: ( < > = required [ ] = optional )\n"
											+ "- /sc init\n"
											+ "- /sc status\n"
											+ "- /sc users\n"
											+ "- /sc toggle <music|voice>\n"
											+ "- /sc mute <name>\n"
											+ "- /sc unmute <name>\n"
											+ "- /sc volume <1-100>\n"
											+ "- /sc set <box> [range]\n"
											+ "- /sc set corner <1|2>\n"
											+ "- /sc set area\n"
											+ "- /sc set wgregion <name>\n"
											+ "- /sc speak [global]\n";
	
	public static final String CMD_USAGE_SC = prefix + "Use " + GREEN + "/sc help " + WHITE + "to get a list of all commands.";
	public static final String CMD_USAGE_MUTE = prefix + "Use " + GREEN + "/sc mute <name> " + WHITE + ".";
	public static final String CMD_USAGE_UNMUTE = prefix + "Use " + GREEN + "/sc unmute <name> " + WHITE + ".";
	public static final String CMD_USAGE_TOGGLE = prefix + "Use " + GREEN + "/sc toggle <music | voice> " + WHITE + ".";
	public static final String CMD_USAGE_VOLUME = prefix + "Use " + GREEN + "/sc volume <1 - 100> " + WHITE + ".";
	public static final String CMD_USAGE_SET = prefix + "Use " + GREEN + "/sc set < box [range] | area | corner <1|2> >" 
												+ WHITE + ".";
	public static final String CMD_USAGE_SET_WGREGION = prefix + "Use " + GREEN + "/sc set wgregion <name>" 
														+ WHITE + ".";
	public static final String CMD_USAGE_SET_CORNERS = prefix + "Use " + GREEN + "/sc set corner <1|2>" 
														+ WHITE + ".";
	
	
	public static final String ERR_CONNECTION_LOST = prefix + RED + "Connection to AudioClient lost!";
	public static final String ERR_SERVER_LOAD = prefix + RED + "The audio-server is currently streaming at its maximum bandwidth, " 
			+ "therefore you cannot speak at the moment.";
	public static final String ERR_PERMISSION_INIT = prefix + RED + "Unfortunately you don't " 
													+ "have permission to use the audioclient.";
	public static final String ERR_PERMISSION_SET_AREA = prefix + RED + "Unfortunately you don't " 
														+ "have permission to set areas.";
	public static final String ERR_PERMISSION_SET_BOX = prefix + RED + "Unfortunately you don't " 
														+ "have permission to set boxes.";
	public static final String ERR_PERMISSION_SET_WGREGION = prefix + RED + "Unfortunately you don't " 
			+ "have permission to use WorldGuard regions.";
	public static final String ERR_PERMISSION_SET_WGREGION_OTHERS = prefix + RED + "Unfortunately you don't " 
			+ "have permission to use WorldGuard regions you are not a member of.";
	public static final String ERR_PERMISSION_SET_OVERLAP = prefix + RED + "Unfortunately you don't " 
														+ "have permission to set boxes/ areas that overlap "
														+ "with existing boxes/ areas/ wgregions.";
	public static final String ERR_PERMISSION_SPEAK = prefix + RED + "Unfortunately you don't " 
														+ "have permission to use voice chat.";
	public static final String ERR_PERMISSION_SPEAK_GLOBAL = prefix + RED + "Unfortunately you don't " 
														+ "have permission to use global voice chat.";
	public static final String ERR_LOAD_WORLDGUARD = prefix + RED + "Cannot load the WorldGuard plugin.";
	public static final String ERR_WGREGION_ALREADY_EXISTANT = prefix + RED + "This region is already a SoundCenter station.";
	public static final String ERR_WGREGION_NOT_EXISTANT = prefix + RED + "A WorldGuard region with this id doesn't exist.";
	public static final String ERR_VOICE_CHAT_DISABLED = prefix + RED + "Voice chat is disabled on this server.";
	
	public static final String ERR_MUTE_PT1 = prefix + RED + "Cannot (un)mute ";
	public static final String ERR_MUTE_PT2 = " because he is not using the audio-client.";
	public static final String ERR_MAX_AREAS = prefix + RED + "Too many areas! You cannot set more than ";
	public static final String ERR_MAX_AREA_SIZE = prefix + RED 
												+ "Area too large! You can set areas with a maximum edge-length of ";
	public static final String ERR_CORNERS_IN_LINE = prefix + RED + "The 2 corners of an area must have " 
													+ "different x, y, and z coordinates.";
	public static final String ERR_NO_CORNERS = prefix + RED + "Use " + GREEN + "/sc set corner <1|2> " 
												+ RED + "to set the two diagonally opposing corners of a cuboid.";
	public static final String ERR_CORNERS_DIFFERENT_WORLDS = prefix + RED + "Corners must be in the same world.";
	public static final String ERR_MAX_BOXES = prefix + RED + "Too many boxes! You cannot set more than ";
	public static final String ERR_MAX_BOX_RANGE = prefix + RED 
													+ "Range too high! Using maximum range of ";
	public static final String ERR_MIN_BOX_RANGE = prefix + RED 
													+ "Range must be higher than 0! Using default range.";
	
	public static final String ERR_SENDER_NO_PLAYER = prefix + RED 
															+ "You must be a player to use commands other than "
															+ GREEN + "/sc help" + RED + ".";
	public static final String ERR_IP_VERIFICATION = prefix + RED
													+ "IP-Verification failed! Cannot use audioclient.";
	public static final String ERR_NOT_ACCEPTED = prefix + "Oh noes! Something went wrong with initialization! "
													+ "Please try again.";
	
}
