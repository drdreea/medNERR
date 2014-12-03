package postprocess.svm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import utils.UtilMethods;

public class SVMPredictions {

    public SVMPredictions() {

    }

    public static HashMap<String, ArrayList<Mapping>> importMappings(String path) {
	HashMap<String, ArrayList<Mapping>> mappings = 
		new HashMap<String, ArrayList<Mapping>>();

	// check if the file exists
	File newFile = new File(path);
	if(!newFile.exists())
	    return mappings;

	try {
	    BufferedReader read = new BufferedReader(new FileReader(newFile));

	    String line = read.readLine();

	    while(line!= null) {
		if(line.trim().isEmpty()) continue;
		ArrayList<Mapping> values = new ArrayList<Mapping>();

		String[] splitLine = line.split("\\|");
		String key = splitLine[0];

		for(int index = 1; index < splitLine.length; index ++) {
		    String value = splitLine[index];
		    String[] splitValue = value.split(" ");
		    value = "";
		    for(int tmpIndex = 0; tmpIndex < splitValue.length-1; tmpIndex++) {
			value = UtilMethods.mergeStrings(value, splitValue[tmpIndex]);
		    }

		    Double quant = 0.0;

		    try {
			quant = Double.parseDouble(splitValue[splitValue.length-1]);
		    }catch(Exception e) {
			System.out.println("Could not parse double");
		    }

//					if(quant >= .5)
		    values.add(new Mapping(value, quant));
		}

		mappings.put(key, values);
		line = read.readLine();
	    }

	    read.close();
	}catch(Exception e) {
	    System.out.println("Could not process mappings file.");
	}


	return mappings;
    }

    public ArrayList<ArrayList<String>> getConnectedComponents(
	    HashMap<String, ArrayList<Mapping>> mappings){
	HashMap<String, ArrayList<String>> connected = 
		new HashMap<String, ArrayList<String>>();

	for(String key : mappings.keySet()) {
	    ArrayList<Mapping> values = mappings.get(key);
	    ArrayList<String> comp = new ArrayList<String>();

	    for(Mapping val : values) {
		comp.add(val.map);
	    }

	    connected.put(key, comp);
	}

	boolean toModify = true;

	while(toModify) {
	    toModify = false;
	    for(String key : connected.keySet()) {
		ArrayList<String> values = connected.get(key);

		for(String val : values) {
		    if(connected.containsKey(val)) {
			if(!connected.get(val).contains(key)) {
			    ArrayList<String> update = connected.get(val);
			    update.add(key);
			    connected.put(val, update);
			}
		    }else {
			ArrayList<String> update = new ArrayList<String>();
			update.add(key);
			connected.put(val, update);
			toModify = true;
		    }
		}

		if(toModify)
		    break;
	    }
	}

	HashMap<String, Boolean> visited = new HashMap<String, Boolean>();
	ArrayList<ArrayList<String>> connectedComponents = 
		new ArrayList<ArrayList<String>>();

	for(String key : connected.keySet()) {
	    if(visited.containsKey(key)) continue;
	    ArrayList<String> components = new ArrayList<String>();

	    ArrayList<String> toVisit = new ArrayList<String>();
	    toVisit.add(key);

	    while(!toVisit.isEmpty()) {

		String newKey = toVisit.get(0);
		toVisit.remove(0);

		if(visited.containsKey(newKey)) continue;

		components.add(newKey);
		visited.put(newKey, true);

		ArrayList<String> values = connected.get(newKey);

		if(values == null) {
		    continue;
		}

		for(String vl : values) {
		    if(visited.containsKey(vl)) continue;
		    toVisit.add(vl);
		}
	    }

	    connectedComponents.add(components);
	}

	return connectedComponents;
    }

    /**
     * Compute the cosine between two vectors of Mappings
     * @param value
     * @param value2
     * @return
     */
    public double calcCosine(List<Mapping> value, List<Mapping> value2) {
	double cosine = 0.0;

	for(int index = 0; index<value.size(); index ++) {
	    cosine += value.get(index).score * value2.get(index).score;
	}

	cosine = cosine/ (calcNorm(value) * calcNorm(value2));

	return cosine;
    }

    /**
     * Compute the norm of a vector of mappings
     * @param value
     * @return
     */
    private double calcNorm(List<Mapping> value) {
	double norm = 0;

	for(Mapping map : value) {
	    norm += map.score*map.score;
	}

	return Math.sqrt(norm);
    }
}
