package worldBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Script {

	public Script(String filename) {
		BufferedReader r = null;

		try {
			r = new BufferedReader(new FileReader(filename));
			String line;
			int linenum = 0;
			String delimiters = "[ ]+";
			while((line = r.readLine()) != null) {
				linenum++;
				
				// ignore comments and blank lines
				if (line.equals("") || line.startsWith("#"))
					continue;
				String[] tokens = line.split(delimiters);
				process(tokens);
			}
		} catch (IOException e) {
			System.err.println("Unable to open/read script: " + filename);
			return;
		}
	}
	
	private boolean process(String[] tokens) {
		System.out.println(tokens[0]);
		return true;
	}
}
