package io.importer;
/**
 * Created on Nov 1, 2011 2011
 * @author: Andreea Bodnari
 * Contact: andreeab at mit dot edu
 */

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author ab
 *
 */
public class EMRPatternConfigHandler extends ConfigHandler{
    public Properties configFile ;
    public String outputPath;
    public String mergedPath;
    public String rawFilesPath;

    public Boolean verbose;


    static enum RequiredConfigs {   
	outputPath, mergedPath, rawFilesPath};
	static enum ConfigOptions { verbose };

	public EMRPatternConfigHandler(String configFileName){
	    configFile = new java.util.Properties();
	    this.cachedData = new HashMap<String, Boolean>();

	    try {	
		FileInputStream stream = new FileInputStream(configFileName);
		configFile.load(stream);			
	    }catch(Exception eta){
		System.out.println("Could not parse the config file.");
		eta.printStackTrace();
		System.exit(-1);
	    }

	    // read all the configurations from the file
	    parseConfigFile();


	}

	private void parseConfigFile(){
	    // read the required properties first
	    for (RequiredConfigs conf : RequiredConfigs.values()){
		String value = this.getProperty(conf.toString());
		if (value == null || value.isEmpty()){
		    System.out.println("ERROR: Conf property required for " +
			    conf.toString());
		    System.exit(-1);
		}

		if (!new File(value).exists() ){
		    if(conf != RequiredConfigs.outputPath){
			System.out.println("Incorrect path " + value);
			System.exit(-1);
		    }else{
			try{
			    File inputFile = new File(value);
			    if(!inputFile.exists()){
				boolean success = inputFile.createNewFile();
				if (! success){
				    System.out.println("Incorrect path " + value);
				    System.exit(-1);
				}
			    }
			}catch(Exception e){
			    System.out.println("Incorrect path " + value);
			    System.exit(-1);	
			}
		    }
		}

		switch(conf){
		case rawFilesPath:
		    this.rawFilesPath = value;
		    break;
		case outputPath:
		    this.outputPath = value;
		    break;
		case mergedPath:
		    this.mergedPath = value;
		    break;
		}
	    }

	    // read the program options
	    for (ConfigOptions options: ConfigOptions.values()){
		String value = this.getProperty(options.toString());
		Boolean result = false;

		if (value != null && !value.isEmpty()){
		    if (value.toLowerCase().contains("false")){
			result = false;
		    }
		    if (value.toLowerCase().contains("true")){
			result = true;
		    }
		}

		switch(options){
		case verbose:
		    this.verbose = result;
		    break;
		}

	    }


	}

	public String getProperty(String key){
	    String value = this.configFile.getProperty(key);

	    return value;
	}

}
