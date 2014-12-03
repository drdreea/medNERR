/**
 * Created on Jan 10, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package io.log;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author ab
 *
 */
public abstract class ExperimentLog {
	BufferedWriter out;
	String outputFilePath;
	
	public void startLogging() {
		if (out != null) {
			try {
				out.write("=====================================================\n");
				out.write("Medication extraction library\n");
				out.write("Author: Andreea Bodnari < andreeab at mit dot edu >\n");
				out.write("=====================================================\n\n");
				out.write("================== STARTING Experiment ==================\n");
				
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void writeError(String message) {
		if (out != null) {
			try {
				out.write(message + "\n");
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
