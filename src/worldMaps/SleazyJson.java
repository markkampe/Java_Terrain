package worldMaps;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class SleazyJson {
	
	private BufferedReader reader;
	private static final int MAX_READAHEAD = 1024;

	public SleazyJson(String filename) {
		FileReader fr;
		try {
			fr = new FileReader(filename);
			reader = new BufferedReader(fr);
		} catch (FileNotFoundException e) {
			System.err.println("unable to open input file " + filename);
		}
	}

	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean enter(String string) {
		// FIX implement SleasyJson.enter
		return true;
	}

	public String property(String string) {
		// FIX implement SleazyJson.property
		return null;
	}
	
	public boolean nextObject() {
		// FIX implement SleazyJson.nextObject
		return false;
	}

	public void push() {	
		try {
			reader.mark(MAX_READAHEAD);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void pop() {
		try {
			reader.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
