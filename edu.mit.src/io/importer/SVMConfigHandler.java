package io.importer;
/**
 * Created on Nov 1, 2011 2011
 * @author: Andreea Bodnari
 * Contact: andreeab at mit dot edu
 */

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author ab
 *
 */
public class SVMConfigHandler {
	public Properties configFile ;
	public String outputPath;
	public String svmPath;
	public String nounsPath;

	public Boolean verbose;
	public Boolean listOnly;
	public Boolean medOnly;
	public Boolean reasonOnly;

	final String appRandOutputFilePath = "resultsFile";

	static enum RequiredConfigs { svmPath, nounsPath, outputPath};

		static enum ConfigOptions { verbose ,
			listOnly,
			medOnly, 
			reasonOnly
		};

		public SVMConfigHandler(String configFileName){
			configFile = new java.util.Properties();

			try {	
				FileInputStream stream = new FileInputStream(configFileName);
				configFile.load(stream);			
			}catch(Exception eta){
				System.out.println("Could not parse the config file.");
				eta.printStackTrace();
				System.exit(-1);
			}

			this.reasonOnly = false;
			this.medOnly = true;

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
				case svmPath:
					this.svmPath = value;
					break;
				case nounsPath:
					this.nounsPath = value;
					break;
				case outputPath:
					if (value.endsWith("/"))
						this.outputPath = value;
					else{
						this.outputPath = value + "/";
					}
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
				case listOnly:
					this.listOnly = result;
					break;
				case medOnly:
					this.medOnly = result;
				case reasonOnly:
					this.reasonOnly = result;
				}

			}

			// check whether the user gave a specific output file path
			String value = this.getProperty(this.appRandOutputFilePath);
			if (value != null ){
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

		public String getProperty(String key){
			String value = this.configFile.getProperty(key);

			return value;
		}
		
}
