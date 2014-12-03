package postprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import utils.UtilMethods;

public class ClusterInfo {
	
	ClusterInfo(){
		
	}
	
	public HashMap<String, Boolean> importPredictions(String path){
		HashMap<String, Boolean> predictions = 
				new HashMap<String, Boolean>();
		File inputFile =  new File(path);
		
		if(!inputFile.exists()) return predictions;
		
		try {
			BufferedReader bf = new BufferedReader(new FileReader(inputFile));
			String line =bf.readLine();
			
			while( line!=null) {
				
				if(line.isEmpty()) continue;
				String[] splitline = line.split(" ");
				
				boolean predict = splitline[splitline.length-1].equals("2") ;
				
				line = "";
				for(int index =0; index < splitline.length-1; index ++) {
					line = UtilMethods.mergeStrings(line, splitline[index]);
				}
				
				predictions.put(line, predict);
				
				line = bf.readLine();
			}
			
			bf.close();
		}catch(Exception e) {
			
		}
		
		return predictions;
	}

}
