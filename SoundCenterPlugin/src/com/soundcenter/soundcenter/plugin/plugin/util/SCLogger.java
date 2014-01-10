package com.soundcenter.soundcenter.plugin.plugin.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SCLogger {
	
	private Logger log = null;
	private boolean debug = false;
	
	public SCLogger(Logger log, boolean debug) {
		this.log = log;
		this.debug = debug;
		
		log.setLevel(Level.INFO);
	}
	
	public void d(String msg, Exception e) {
		if (debug) {
			log.info("[DEBUG] " + msg);
			if (e != null)
				log.log(Level.INFO, "[DEBUG] " + e.getMessage(), e);
		}
	}
	
	public void i(String msg, Exception e) {
		log.info(msg);
		if (debug && e != null)
			log.log(Level.INFO, "[DEBUG] " + e.getMessage(), e);
	}
	
	public void w(String msg, Exception e) {
		log.warning(msg);
		if (e != null)
			log.log(Level.WARNING, "[DEBUG] " + e.getMessage(), e);
	}
	
	public void s(String msg, Exception e) {
		log.severe(msg);
		if (e != null)
			log.log(Level.SEVERE, "[DEBUG] " + e.getMessage(), e);
	}
	
}
