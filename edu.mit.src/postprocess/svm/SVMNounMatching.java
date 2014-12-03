package postprocess.svm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import utils.UtilMethods;

import io.importer.DataImport;
import io.importer.SVMConfigHandler;

/**
 * This class parses throught the output of the sematic vector model
 * and matches the noun phrases to the noun phrases identified by the
 * medExtaction system
 * @author ab
 *
 */
public class SVMNounMatching {
	SVMConfigHandler configs;
	final String predSplitter = "|";
	DataImport importer;
	
	SVMNounMatching(String configsPath){
		configs = new SVMConfigHandler(configsPath);
		importer = new DataImport(null);
		
	}
	
	private void execute() {
		ArrayList<String> nouns = importer.readFile(configs.nounsPath);
		ArrayList<String> svmOut = importer.readFile(configs.svmPath);
		
		HashMap<String, HashMap<String, Double>> svmValues = 
				parseSVM(svmOut);
		HashMap<String, HashMap<String, Double>> finalResults = 
				new HashMap<String, HashMap<String, Double>>();
		
		for(String noun: nouns) {
			if(svmValues.containsKey(noun)) {
				finalResults.put(noun, svmValues.get(noun));
			}
		}
		
		storeResults(finalResults);
	}
	
	/**
	 * Store the svm to noun matching results
	 * @param finalResults
	 */
	private void storeResults(
			HashMap<String, HashMap<String, Double>> finalResults) {
		String filePath = configs.outputPath + "/" + "svmNoun.txt";
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filePath)));
			
			for(String key : finalResults.keySet()) {
				HashMap<String, Double> values = finalResults.get(key);
				String out = key;
				
				for(String val : values.keySet()) {
					out = UtilMethods.mergeStrings(out, val);
				}
				
				bw.write(out + "\n");
				bw.flush();
			}
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Go through the output of the SVM and create a hashmap of
	 * word similarities
	 * @param input
	 * @return 
	 */
	private HashMap<String, HashMap<String, Double>> parseSVM(ArrayList<String> input) {
		HashMap<String, HashMap<String, Double>> results = 
				new HashMap<String, HashMap<String, Double>>();
		
		for(String pred : input) {
			if (pred == null || pred.trim().isEmpty()) continue;
			String[] splitPred = pred.split(predSplitter);
			if(splitPred.length <=1) continue;
			
			String key = splitPred[0];
			HashMap<String, Double> values = new HashMap<String, Double>();
			
			for(int index =1; index < splitPred.length; index ++) {
				String[] splitValues = splitPred[index].split(" ");
				if(splitValues.length <=1 ) continue;
				
				double score = 0.;
				try {
					score =	Double.parseDouble(splitValues[1]);
				}catch(Exception e) {
					e.printStackTrace();
				}
				
				values.put(splitValues[0], score);
			}
			
			results.put(key, values);
		}
		
		return results;
	}
	
	
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Incorrect param length");
			System.exit(-1);
		}
		
		SVMNounMatching matcher = new SVMNounMatching(args[0]);
		matcher.execute();
		
	}

}
