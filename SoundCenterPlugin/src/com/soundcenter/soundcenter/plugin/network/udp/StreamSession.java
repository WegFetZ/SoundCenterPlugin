package com.soundcenter.soundcenter.plugin.network.udp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.sampled.AudioFileFormat;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import org.tritonus.share.sampled.file.TAudioFileFormat;

import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.data.Song;
import com.soundcenter.soundcenter.lib.data.Station;
import com.soundcenter.soundcenter.lib.tcp.MidiNotificationPacket;
import com.soundcenter.soundcenter.lib.tcp.TcpOpcodes;
import com.soundcenter.soundcenter.lib.udp.UdpPacket;
import com.soundcenter.soundcenter.lib.util.FileOperation;
import com.soundcenter.soundcenter.plugin.SoundCenter;
import com.soundcenter.soundcenter.plugin.data.ServerUser;
import com.soundcenter.soundcenter.plugin.network.StreamManager;

public class StreamSession implements Runnable {

	private static final int BUFFER_SIZE = GlobalConstants.STREAM_DATA_SIZE;

	byte type = -1;
	private short id;
	private Station station = null;
	private Song song = null;

	private boolean exit = false;
	private Thread thread = null;

	private List<ServerUser> listeners = Collections.synchronizedList(new ArrayList<ServerUser>());

	private FileInputStream fileIn;
	private File currentFile;
	private int rate = 17400;

	public StreamSession(byte type, short id) {
		this.type = type;
		this.id = id;
	}

	public StreamSession(byte type, short id, Song song) {
		this.type = type;
		this.id = id;
		this.song = song;
	}

	public void run() {
		thread = Thread.currentThread();
		thread.setName("StreamSession for: " + type + " (" + id + ")");
		SoundCenter.logger.d("Starting StreamSession for " + type + " (" + id + ")", null);

		// add session to StreamSession-list and update total rate
		StreamManager.addSessionToList(type, id, this);
		
		// we use only one song for global sessions
		if (song != null) {
			currentFile = new File(SoundCenter.musicDataFolder + File.separator + song.getPath());
			if (currentFile.exists()
					&& GlobalConstants.supportedExtensions.contains(FileOperation.getExtension(currentFile.getName()))) {
				if (FileOperation.fileIsMidi(currentFile)) {
					// tell all listeners to play the midi file and stop the session
					notifyAboutMidi(currentFile, 0);
				} else {
					// stream the song to all listeners
					streamSong(currentFile, 0, (byte) 0);
				}
			}
		} else {

			// get the list of songs to be played
			List<Song> songList = null;
			station = SoundCenter.database.getStation(type, id);
	
			if (station != null && !station.isRadio()) {
				songList = station.getSongs();
			}
	
			if (songList == null) {
				exit = true;
				sendTcpMessageToListeners(TcpOpcodes.CL_ERR_STREAM_NO_SONGS, type, id);
				return;
			}
	
			// get the offset for the playback
			long[] offset = getOffset(songList);
			int index = (int) offset[0];
			long byteOffset = offset[1];
			long timeOffset = offset[2];
	
			if (index == -1) {
				sendTcpMessageToListeners(TcpOpcodes.CL_ERR_STREAM_NO_SONGS, type, id);
				shutdown();
			}
	
			ListIterator<Song> iter = songList.listIterator();
			// set iterator to the calculated index
			while (iter.hasNext() && iter.nextIndex() < index) {
				iter.next();
			}
			
			while (!exit) {
				
				if (songList.isEmpty()) {
					sendTcpMessageToListeners(TcpOpcodes.CL_ERR_STREAM_NO_SONGS, type, id);
					break;
				}
				
				if (!iter.hasNext()) {
					iter = songList.listIterator();
				}
				
				index = iter.nextIndex();
				Song song = iter.next();
	
				currentFile = new File(SoundCenter.musicDataFolder + File.separator + song.getPath());
	
				if (currentFile.exists()) {
					if (FileOperation.fileIsMidi(currentFile)) {
						// tell all listeners to play the midi file and stop the
						// session
						notifyAboutMidi(currentFile, timeOffset);
						break;
					} else {
						// stream the song to all listeners
						streamSong(currentFile, byteOffset, (byte) index);
					}
				} else {
					songList.remove(currentFile);
				}
	
				byteOffset = 0;
				timeOffset = 0;
	
				index++;
			}
		}

		// remove the session from list
		StreamManager.removeSessionFromList(type, id);

		SoundCenter.logger.d("Stopped StreamSession for " + type + " (" + id + ")", null);
	}

	private void notifyAboutMidi(File file, long millisecondsPos) {

		SoundCenter.logger.d("Notifiy listeners of " + type + " (" + id + ") about midi: " + file.getName(), null);

		String path = file.getParentFile().getName() + File.separator + file.getName();
		MidiNotificationPacket notification = new MidiNotificationPacket(type, id, path, millisecondsPos);

		sendTcpMessageToListeners(TcpOpcodes.CL_CMD_PLAY_MIDI, notification, null);
	}

	private void streamSong(File file, long bytesToSkip, byte index) {
		int sleepDuration = 10;
		int delay = 10;
		String fileName = file.getParentFile().getName() + File.separator + file.getName();

		try {
			// calculate the rate at which the stream packets should be send
			long seconds = getSongDuration(file) / 1000;
			if (seconds > 0) {
				rate = (int) (file.length() / seconds);
				rate += 0.1 * rate;
				delay = (int) 1000 / (rate / BUFFER_SIZE);
			} else {
				SoundCenter.logger.i(
						"Error while calculating bitrate of file: " + fileName + ". Sending on high rate.", null);
			}

			// start the stream
			SoundCenter.logger.d("Starting streaming of song: " + fileName + " in session " + type + " (" + id
					+ ")\n Rate: " + rate + " Offset: " + bytesToSkip, null);
			long timeA, timeB;
			fileIn = new FileInputStream(file);
			fileIn.skip(bytesToSkip);
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = 0;

			try {
				while (!exit && bytesRead != -1) {

					timeA = System.currentTimeMillis();

					// read the stream data into the buffer
					bytesRead = fileIn.read(buffer, 0, buffer.length);

					// create a new UdpPacket with the stream data
					UdpPacket packet = new UdpPacket(GlobalConstants.STREAM_PACKET_SIZE);
					packet.setID(id);
					packet.setType(type);
					packet.setSongIndex((byte) (index - 127));
					packet.setStreamData(buffer);

					// send the packet
					SoundCenter.udpServer.send(packet, listeners);

					timeB = System.currentTimeMillis();
					sleepDuration = delay - (int) (timeB - timeA);

					try {
						if (sleepDuration > 0) {
							Thread.sleep(sleepDuration);
						}
					} catch (InterruptedException e) {
					}
				}
			} finally {
				fileIn.close();
			}

		} catch (IOException e) {
			if (!exit && file.exists()) {
				SoundCenter.logger.i("Error while streaming song: " + file.getName() + " in session " + type + " ("
						+ id + "):", e);
			}
		}

		String stopReason = "Finished";
		if (exit) {
			stopReason = "Stopped";
		}
		SoundCenter.logger.d(stopReason + " streaming of file: " + file.getName() + " in session " + type + " (" + id
				+ ")", null);
	}

	private long[] getOffset(List<Song> songList) {

		long[] fileDurations = new long[songList.size()];
		long[] fileLengths = new long[songList.size()];
		long totalDuration = 0;

		// get duration of all files
		Iterator<Song> iter = songList.iterator();
		int loops = 0;
		while (iter.hasNext()) {
			Song song = iter.next();
			File file = new File(SoundCenter.musicDataFolder + File.separator + song.getPath());
			if (file.exists()
					&& GlobalConstants.supportedExtensions.contains(FileOperation.getExtension(file.getName()))) {
				fileDurations[loops] = getSongDuration(file);
				fileLengths[loops] = file.length();
				totalDuration += fileDurations[loops];

			} else {// remove song from list and notify users
				loops--; // do not increment
				//remove the song from our list using the iterator 
				//to avoid a concurrentmodificationexception
				iter.remove();
				SoundCenter.database.removeSongFromStations(song.getPath());
				SoundCenter.tcpServer.send(TcpOpcodes.CL_DATA_CMD_DELETE_SONG, song, null, null);
			}
			loops++;
		}

		if (totalDuration == 0) {
			return new long[] { -1, 0, 0 };
		}

		long currentTime = System.currentTimeMillis();
		long timeOffset = currentTime % totalDuration; // amount of time that
														// has to be skipped
														// over all files
		long partOffset = 0; // sum of all file durations until the file that is
								// currently played
		long restOffset = 0; // time offset of the file to be played
		int fileIndex = 0; // index of file to be played

		for (int i = 0; i < fileDurations.length; i++) {
			fileIndex = i;
			partOffset += fileDurations[i];
			if (partOffset >= timeOffset) {
				restOffset = fileDurations[i] - (partOffset - timeOffset);
				break;
			}
		}

		long byteOffset = (long) (((double) fileLengths[fileIndex] / (double) fileDurations[fileIndex]) * restOffset);

		return new long[] { fileIndex, byteOffset, restOffset };
	}

	private long getSongDuration(File file) {
		try {
			if (FileOperation.fileIsMidi(file)) {
				Sequence sequence = MidiSystem.getSequence(file);
				long mili = (long) (sequence.getMicrosecondLength() / 1000);

				return mili;

			} else {
				AudioFileFormat fileFormat = new MpegAudioFileReader().getAudioFileFormat(file);
				Map<?, ?> properties = ((TAudioFileFormat) fileFormat).properties();
				String key = "duration";
				Long microseconds = (Long) properties.get(key);
				long mili = (long) (microseconds / 1000);

				return mili;
			}
		} catch (Exception e) {
			SoundCenter.logger.i("Error while retrieving duration of file: " + file.getParentFile().getName()
					+ File.separator + file.getName() + ":", e);
		}
		return 0;
	}

	private void sendTcpMessageToListeners(byte opcode, Object key, Object value) {
		Iterator<ServerUser> iter = listeners.iterator();
		while (iter.hasNext()) {
			SoundCenter.tcpServer.send(opcode, key, value, iter.next());
		}
	}

	public boolean addListener(ServerUser user) {
		if (exit) {
			return false;
		}
		if (!listeners.contains(user)) {
			listeners.add(user);
		}
		return true;
	}

	public void removeListener(ServerUser user) {
		listeners.remove(user);
		if (listeners.isEmpty()) {
			shutdown();
		}
	}

	public int getRate() {
		return rate;
	}

	public int getTotalRate() {
		return rate * listeners.size();
	}

	public void releaseFile(File fileToStop) {
		if (currentFile != null && fileToStop.equals(currentFile)) {
			try {
				if (fileIn != null) {
					fileIn.close();
				}
			} catch (IOException e) {
			}
		}
	}

	public void shutdown() {
		exit = true;
		// close fileinputstream in order to make the file accessible (in case
		// it will be deleted)
		try {
			if (fileIn != null) {
				fileIn.close();
			}
		} catch (IOException e) {
		}
		if (thread != null) {
			thread.interrupt();
		}
	}

}
