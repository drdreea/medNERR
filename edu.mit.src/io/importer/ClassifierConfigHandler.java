package io.importer;

/**
 * Created on Nov 1, 2011 2011
 * 
 * @author: Andreea Bodnari Contact: andreeab at mit dot edu
 */

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;

import classifier.TrainTestCreator.InfoToExtract;
import classifier.TrainTestCreator.CorpusType;
import classifier.TrainTestCreator.VectorType;

/**
 * @author ab
 */
public class ClassifierConfigHandler extends ConfigHandler {
	public Properties configFile;
	public String outputPath;
	public String patterns;
	public String textPatternsPath;
	
	public String trainRawFilesPath;
	public String testRawFilesPath;
	
	public String testConceptsPath;
	public String trainConceptsPath;
	
	public String testRelationsPath;
	public String trainRelationsPath;

	public String trainParsedDataPath;
	public String testParsedDataPath;
	
	public String trainSectionsPath;
	public String testSectionsPath;

	public String testPhonemes;
	public String trainPhonemes;

	public String pcmPath;
	public String wordFrequenciesEnglishCorpusPath;

	public Boolean verbose;

	public String discourseConfigsPath;
	public String semanticTypesPath;
	public String frequenciesPath;
	public String abbeviationPath;
	public String dailyMedications;

	// paths to the other corpus
	public String otherCorpus;
	public String otherParsedCorpus;

	public String classifierOutputPath;

	public String resourcesPath;
	public String templatePath;
	public String wordNetPath;
//	public String labTopicPath;
//	public String medTopicPath;

	public String featuresListFilePath;
	public String nerToAnnotateFilePath;

	public VectorType vector;
	public InfoToExtract infoToExtract;
	public CorpusType corpusType;

	/**
	 * required config params
	 * @author ab
	 *
	 */
	static enum RequiredConfigs {
		outputPath, patternsPath, 
		trainRawFilesPath, trainSectionsPath, trainConceptsPath, 
			trainParsedDataPath, trainRelationsPath,
		testRawFilesPath, testConceptsPath,  testSectionsPath, 
			testParsedDataPath, testRelationsPath,
		textPatternsPath, pcmPath, 
		wordFrequenciesEnglishCorpusPath, 
		discourseConfigsPath, semanticTypesPath, frequenciesPath,
		abbeviationPath, classifierOutputPath , dailyMedications, 
		otherCorpus, otherParsedCorpus,  
		resourcesPath, wordNetPath, 
		featuresListFilePath, nerToAnnotate,
		trainPhonemesDataPath, testPhonemesDataPath,
//		labTopicPath, medTopicPath
	};

	static enum ConfigOptions {
		vector, infoToExtract, corpusType
	};

	/**
	 * 
	 * @param configFileName
	 * @throws Exception 
	 */
	public ClassifierConfigHandler(String configFileName) throws Exception {
		configFile = new java.util.Properties();
		this.cachedData = new HashMap<String, Boolean>();

		try {
			FileInputStream stream = new FileInputStream(configFileName);
			configFile.load(stream);
		}
		catch (Exception eta) {
			System.out.println("Could not parse the config file.");
			System.exit(-1);
		}

		// read all the configurations from the file
		parseConfigFile();

	}

	/**
	 * parse the given params
	 * @throws Exception 
	 */
	private void parseConfigFile() throws Exception {
		// read the required properties first
		for (RequiredConfigs conf : RequiredConfigs.values()) {
			String value = this.getProperty(conf.toString());
			if (value == null || value.isEmpty()) {
				throw new Exception("ERROR: Conf property required for "
						+ conf.toString());

			}

			if (!new File(value).exists()) {
				if (conf != RequiredConfigs.outputPath) {
					throw new Exception("Incorrect path " + value);
				}
				else {
					try {
						File inputFile = new File(value);
						if (!inputFile.exists()) {
							boolean success = inputFile.createNewFile();
							if (!success) {
								throw new Exception("Incorrect path " + value);
							}
						}
					}
					catch (Exception e) {
						throw new Exception("Incorrect path " + value);
					}
				}
			}

			switch (conf) {
			case testRelationsPath:
				this.testRelationsPath = value;
				break;
			case trainRelationsPath:
				this.trainRelationsPath = value;
				break;
			case otherCorpus:
				this.otherCorpus = value;
				break;
			case otherParsedCorpus:
				this.otherParsedCorpus = value;
				break;
			case wordFrequenciesEnglishCorpusPath:
				this.wordFrequenciesEnglishCorpusPath = value;
				break;
			case dailyMedications:
				this.dailyMedications = value;
				break;
			case classifierOutputPath:
				this.classifierOutputPath = value;
				break;
			case abbeviationPath:
				this.abbeviationPath = value;
				break;
			case frequenciesPath:
				this.frequenciesPath = value;
				break;
			case semanticTypesPath:
				this.semanticTypesPath = value;
				break;
			case trainConceptsPath:
				this.trainConceptsPath = value;
				break;
			case discourseConfigsPath:
				this.discourseConfigsPath = value;
				break;
			case trainParsedDataPath:
				this.trainParsedDataPath = value;
				break;
			case testParsedDataPath:
				this.testParsedDataPath = value;
				break;
			case pcmPath:
				this.pcmPath = value;
				break;
			case textPatternsPath:
				this.textPatternsPath = value;
				break;
			case trainSectionsPath:
				this.trainSectionsPath = value;
				break;
			case testSectionsPath:
				this.testSectionsPath = value;
				break;
			case testConceptsPath:
				this.testConceptsPath = value;
				break;
			case trainRawFilesPath:
				this.trainRawFilesPath = value;
				break;
			case testRawFilesPath:
				this.testRawFilesPath = value;
				break;
			case patternsPath:
				this.patterns = value;
				break;
			case outputPath:
				this.outputPath = value;
				break;
			case resourcesPath:
				this.resourcesPath = value;
				this.templatePath = value +"/" + "template.xml";
				break;
			case wordNetPath:
				this.wordNetPath = value;
				break;
			case featuresListFilePath:
				this.featuresListFilePath = value;
				break;
			case nerToAnnotate:
				this.nerToAnnotateFilePath = value;
				break;
			case trainPhonemesDataPath:
				this.trainPhonemes = value;
				break;
			case testPhonemesDataPath:
				this.testPhonemes = value;
				break;
//			case labTopicPath:
//				this.labTopicPath = value;
//				break;
//			case medTopicPath:
//				this.medTopicPath = value;
//				break;
			}

		}

		// read the program options
		for (ConfigOptions options : ConfigOptions.values()) {
			String value = this.getProperty(options.toString());
			if (value == null || value.isEmpty()) {
				throw new Exception("ERROR: Conf property required for "
						+ options.toString());

			}

			switch (options) {
			case vector:
				vector = parseVectorType(value);
				break;
			case infoToExtract:
				this.infoToExtract = parseInfoType(value);
				break;
			case corpusType:
				corpusType = parseCorpusType(value);
				break;
			}

		}
	}

	/**
	 * retrieve the config file property
	 * @param key
	 * @return the property
	 */
	public String getProperty(String key) {
		String value = this.configFile.getProperty(key);

		return value;
	}

	/**
	 * the possible values for the vector type are
	 * CRF,  CLASSIFIER, MAXENT, HMM, CODL, CODL_CONTEXT, CODL_MEDICATION
	 * 
	 * these values are also specified in the log message
	 * @param value
	 * @return
	 */
	private VectorType parseVectorType(String value){
		VectorType newVT = VectorType.CLASSIFIER; // choose a default value

		if(value != null){
			newVT = null;

			if(value.equalsIgnoreCase("crf"))
				newVT = VectorType.CRF;
			else if (value.equalsIgnoreCase("classifier"))
				newVT = VectorType.CLASSIFIER;
			else if(value.equalsIgnoreCase("maxent"))
				newVT = VectorType.MAXENT;
			else if (value.equalsIgnoreCase("hmm"))
				newVT = VectorType.HMM;
			else if (value.equalsIgnoreCase("codl"))
				newVT = VectorType.CODL;
			else if (value.equalsIgnoreCase("codl_context"))
				newVT = VectorType.CODL_CONTEXT;
			else if (value.equalsIgnoreCase("codl_medication"))
				newVT = VectorType.CODL_MEDICATION;

			if(newVT != null){
				System.out.println("Classifier output set to " + value);
			}else{
				newVT = VectorType.CLASSIFIER;
				System.out.println("ERROR: Could not process parameter. " +
						"Classifier output set to default " + value);
			}
		}

		return newVT;
	}

	/**
	 * The different types of info to extract are
	 * 1. concepts
	 * 2. relations
	 * 
	 * @param value
	 * @return
	 */
	private InfoToExtract parseInfoType(String value){
		InfoToExtract info = InfoToExtract.CONCEPT;

		try{
			info = InfoToExtract.valueOf(value.toUpperCase());
		}catch(Exception e){
			System.out.println("ERROR: could not process info to extract " + value);
		}
		
		System.out.println("Information to extract set to " + info.toString());


		return info;
	}

	private CorpusType parseCorpusType(String value){
		CorpusType corpus = CorpusType.I2B2;
		
		try{
			corpus = CorpusType.valueOf(value.toUpperCase());
		}catch(Exception e){
			System.out.println("ERROR: could not process corpus type " + value);
		}

		System.out.println("Corpus type set to " + corpus.toString());

		
		return corpus;
	}
}
