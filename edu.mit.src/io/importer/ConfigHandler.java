package io.importer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import utils.UtilMethods;

public abstract class ConfigHandler {
    public HashMap<String, Boolean> cachedData;
    public String cachePath;
    public String metamapPath;

    public void importCache() {
	// first check whether the data is correct
	try {
	    File inputFile = new File(this.cachePath);
	    if (!inputFile.exists()) {
		boolean success = inputFile.createNewFile();
		if (!success) {
		    System.out.println("Incorrect path " + this.cachePath);
		    System.exit(-1);
		}
	    }
	}
	catch (Exception e) {
	    System.out.println("Incorrect path " + this.cachePath);
	    System.exit(-1);
	}

	// read the file lines
	ArrayList<String> fileLines = UtilMethods.readFileLines(this.cachePath);

	for (String line : fileLines) {
	    if (line == null || line.isEmpty())
		continue;
	    String[] splitLine = line.split(" ");
	    if (splitLine.length < 2)
		continue;

	    try {
		this.cachedData.put(line.replace(
			splitLine[splitLine.length - 1], "").trim(), Boolean
			.valueOf(splitLine[splitLine.length - 1]));
	    }
	    catch (Exception e) {
		System.out.println("Could not parse: " + line);
	    }
	}

    }

    public void storeCache(HashMap<String, Boolean> cachedData2) {

	try {
	    // Create file
	    FileWriter fstream = new FileWriter(this.cachePath, true);
	    BufferedWriter out = new BufferedWriter(fstream);

	    for (String key : cachedData2.keySet()) {
		out.write(key + " " + String.valueOf(this.cachedData.get(key))
			+ "\n");
	    }
	    // Close the output stream
	    out.close();
	}
	catch (Exception e) {// Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	}

    }

    public void storeCacheUnique(HashMap<String, Boolean> cachedData2) {
	try {
	    // Create file
	    FileWriter fstream = new FileWriter(this.cachePath);
	    BufferedWriter out = new BufferedWriter(fstream);

	    for (String key : cachedData2.keySet()) {
		out.write(key + " " + String.valueOf(this.cachedData.get(key))
			+ "\n");
	    }
	    // Close the output stream
	    out.close();
	}
	catch (Exception e) {// Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	}

    }

    /**
     * @param meansPath
     * @param means
     */
    public void storeCache(String meansPath, HashSet<String> means) {
	try {
	    // Create file
	    FileWriter fstream = new FileWriter(meansPath);
	    BufferedWriter out = new BufferedWriter(fstream);

	    for (String key : means) {
		out.write(key + "\n");
	    }
	    // Close the output stream
	    out.close();
	}
	catch (Exception e) {// Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	}
    }
}
