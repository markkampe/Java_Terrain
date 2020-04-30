package worldBuilder;

import java.io.FileWriter;
import java.io.IOException;

/*
 * this class encapsulates writes to a debug log file
 * which has (sadly) often been necessary to analyze
 * complex anomalous results in erosion and deposition. 
 */
class DebugLog {
	
	private FileWriter log;
	private String filename;
	boolean failed = false;
	
	public DebugLog(String logType, String filename) {
		try {
			log = new FileWriter(filename);
			this.filename = filename;
			System.out.println(logType + " tracing enabled to " + filename);
		} catch (IOException e) {
			System.err.println("Unable to create log file " + filename);
			failed = true;
		}
	}
	
	public void write(String message) {
		if (!failed)
			try {
				log.write(message);
			} catch (IOException e) {
				System.err.println("Write error to log file " + filename);
				failed = true;
			}
	}
	
	public void flush() {
		if (!failed)
			try {
				log.flush();
			} catch (IOException e) {
				System.err.println("Write error to log file " + filename);
				failed = true;
			}	
	}
	
	public void close() {
		if (!failed)
			try {
				log.close();
			} catch (IOException e) {
				System.err.println("Close error on log file " + filename);
				failed = true;
			}	
	}
}
