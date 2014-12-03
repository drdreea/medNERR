package classifier;

import io.importer.ClassifierConfigHandler;
import io.importer.DataImport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.MedicalRecord;
import corpus.Annotation.AnnotationType;
import corpus.Relation.RelationType;

import classifier.TrainTestCreator.InfoToExtract;
import classifier.TrainTestCreator.CorpusType;
import classifier.TrainTestCreator.VectorType;
import classifier.vector.AttributeVector;

import patternExtraction.FindContextFromEMR;
import preprocess.parser.ImportSyntacticTags;
import preprocess.parser.SentenceContent;
import utils.NLPSystem;
import utils.UtilMethods;


/**
 * Abstract class for classifier functionality
 * @author ab
 *
 */
public abstract class ClassifierBase {
	public final static String errorMessage = "To run this program you need to pass" +
			" as argument the path of the config file. \n Include the following " +
			"parameters in the config file \n" +
			"vector = the vector type; possible values CRF,  CLASSIFIER, MAXENT, HMM, " +
			"CODL, CODL_CONTEXT, CODL_MEDICATION \n" +
			"infoToExtract = the information to extract " +
			"CONCEPT, RELATION \n" +
			"corpusType = the corpus type given for processing " +
			"ACE, i2b2";

	public ClassifierConfigHandler configs ;
	public DataImport importer;

	public NLPSystem trainData;
	public NLPSystem testData;

	public HashSet<String> patterns;
	public HashMap<String, ArrayList<String>> conceptSemanticTypes;

	public HashMap<String, HashMap<Annotation, Integer>> sortedConceptPerFile;

	public HashMap<String, HashMap<String, Double>> patternQueries;
	public HashSet<String> medToReason ;
	public HashMap<String, Integer> frequencies;
	public HashSet<String> abbreviations;
	public HashMap<String, Integer> patternCounts;
	public TrainTestCreator trainTestCreator;
	public ArrayList<ArrayList<AttributeVector>> resultsMap;

	ArrayList<Boolean> results;
	public ArrayList<String> resultLabels;
	public ArrayList<AttributeVector> testAnnotations;

	public HashSet<String> dailyMedications;

	public String outputFile ;
	public Storage store;
	public HashMap<String, ArrayList<SentenceContent>> taggedContentOtherCorpus;
	public HashMap<String, ArrayList<SentenceContent>> sentencesOtherCorpus;

	String gsContext ;
	public VectorType vectorType ;
	public CorpusType corpusType;
	public InfoToExtract infoType;

	public ArrayList<String> attributeNames;
	public ArrayList<AnnotationType> nerToAnnotate ;
	public ArrayList<RelationType> relationsToAnnotate ;

	public boolean normalizeText;

	public final static String trainFile = "trainSamples_mallet.txt";
	public final static String testFile = "testSamples_mallet.txt";
	public final static String unlabelledFile = "unlabelled_codl.txt";

	public final static String resultsFile = "results.txt";

	public ClassifierBase(){}

	/**
	 * 
	 * @param config
	 * @throws Exception 
	 */
	public ClassifierBase(String config, boolean isRelation) throws Exception{
		configs = new ClassifierConfigHandler(config);

		outputFile = configs.classifierOutputPath + "/" + resultsFile;
		// parse the system options
		vectorType = configs.vector;
		corpusType = configs.corpusType;
		infoType = configs.infoToExtract;

		resultsMap = new ArrayList<ArrayList<AttributeVector>>();
		store = new Storage(configs, this);

		importer = new DataImport(null);
		patternQueries = importer.loadPatternQueries(configs.patterns);
		medToReason = new HashSet<String>();

		// read in the attribute names
		System.out.println("\nLoading system feature names from " + configs.featuresListFilePath);
		this.attributeNames = importer.parseAttributeNames(
				configs.featuresListFilePath) ;

		// read the ner to annotate
		switch(infoType){
		case CONCEPT:	
			this.nerToAnnotate = importer.parseNERtoAnnotate(
					configs.nerToAnnotateFilePath);
			this.relationsToAnnotate = new ArrayList<RelationType>();
			break;
		case RELATION:
			this.relationsToAnnotate = importer.parseRelationToAnnotate(
					configs.nerToAnnotateFilePath);
			this.nerToAnnotate = new ArrayList<AnnotationType>();
			break;
		case BOTH:
			this.relationsToAnnotate = importer.parseRelationToAnnotate(
					configs.nerToAnnotateFilePath);
			this.nerToAnnotate = importer.parseNERtoAnnotate(
					configs.nerToAnnotateFilePath);
			break;
		}

		results = new ArrayList<Boolean>();
		testAnnotations = new ArrayList<AttributeVector>();
		resultLabels = new ArrayList<String>();       
		// import the parsed for the current record and for the other corpus
		//        importParsedContentOtherCorpus();
		
		// Import terminologies and data resources
		System.out.println("\nLoading data resources");
		importDailyMed();
		importSemanticTypes();
		loadFrequencies();
		loadAbbreviations();
		
		ImportSyntacticTags tagsImporter = new ImportSyntacticTags(configs.resourcesPath,
				corpusType);

		// import the train meds and attributes
		System.out.println("\nLoading training data...");
		trainData = new NLPSystem(configs.trainConceptsPath, 
				configs.trainRelationsPath,
				configs.trainSectionsPath,
				configs.trainRawFilesPath, 
				configs.trainParsedDataPath,
				conceptSemanticTypes, configs.templatePath, 
				tagsImporter, configs.trainPhonemes,
				corpusType);

		System.out.println("\nLoading test data...");
		testData = new NLPSystem(configs.testConceptsPath,
				configs.testRelationsPath,
				configs.testSectionsPath,
				configs.testRawFilesPath, configs.testParsedDataPath,
				conceptSemanticTypes,
				configs.templatePath, 
				tagsImporter, configs.testPhonemes, corpusType ); 


		// import the merged meds for the
		patterns = importer.importDailyMedNames(configs.textPatternsPath, false);
	}


	private void importDailyMed(){
		System.out.println("Loading DailyMed data...");
		this.dailyMedications = new HashSet<String>();

		File path = new File(this.configs.dailyMedications);

		if(!path.exists()){
			System.out.println("Incorrect path " + this.configs.dailyMedications);
		}else{
			try {
				BufferedReader in = new BufferedReader(new FileReader(path));
				String line = in.readLine();

				while(line!= null){
					dailyMedications.add(line.trim());

					line = in.readLine();
				}

				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void loadAbbreviations(){
		System.out.println("Loading abbreviation data...");
		this.abbreviations = new HashSet<String>();

		File path = new File(this.configs.abbeviationPath);

		if(!path.exists()){
			System.out.println("Incorrect path " + this.configs.frequenciesPath);
		}else{
			try {
				BufferedReader in = new BufferedReader(new FileReader(path));
				String line = in.readLine();

				while(line!= null){
					abbreviations.add(line.trim());

					line = in.readLine();
				}

				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


	}

	private void loadFrequencies() {
		System.out.println("Loading word frequency data...");
		frequencies = new HashMap<String, Integer>();

		File path = new File(this.configs.frequenciesPath);
		if(!path.exists()){
			System.out.println("Incorrect path " + this.configs.frequenciesPath);
		}else{
			try {
				BufferedReader in = new BufferedReader(new FileReader(path));
				String line = in.readLine();

				while(line!= null){
					String[] splitline = line.split("\\:");
					if(splitline.length > 1){
						Integer value = Integer.parseInt(splitline[splitline.length-1].trim());
						String key = splitline[0].trim();

						frequencies.put(key, value);
					}

					line = in.readLine();
				}

				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Import the concept semantic types
	 * for both the reason and the medications
	 */
	private void importSemanticTypes(){
		System.out.println("Loading semantic type data...");

		this.conceptSemanticTypes = new HashMap<String, ArrayList<String>>();

		this.conceptSemanticTypes = importer.importSemanticType(configs.semanticTypesPath);
	}

	//	private void importMedReasonPairings() {
	//		medReasonPairing = new HashMap<String, HashMap<Annotation, ArrayList<Annotation>>>();
	//
	//		for(String file : cereSystem.medsPerFile.keySet()){
	//			MatchUtility matching = new MatchUtility();
	//			matching.getMatches(cereSystem.medsPerFile.get(file).annotations, 
	//					cereSystem.reasonsPerFile.get(file).annotations);
	//
	//			medReasonPairing.put(file, matching.medReasonPairing);
	//		}
	//	}

	/**
	 * 
	 */
	void findContextGS() {
		try{
			BufferedWriter out = new BufferedWriter(new FileWriter(gsContext));

			for(String file : trainData.medicalRecords.keySet()) {
				MedicalRecord record = trainData.medicalRecords.get(file);

				for(Annotation med : trainData.medicalRecords.get(file).concepts) {
					AnnotationDetail reason = med.annotationElements.get(AnnotationType.R);
					AnnotationDetail medication = med.annotationElements.get(AnnotationType.M);

					if(reason == null || medication.content == null) continue;

					String medContent = Utils.removePunctuation(medication.content);
					String reasonContent = reason.content;

					if(reasonContent == null || reasonContent.equals("nm")) continue;

					reasonContent = Utils.removePunctuation(reasonContent);
					String context = 
							FindContextFromEMR.getContent(record.rawFileLines, 
									medication, reason, false);
					if(this.medToReason.contains(
							UtilMethods.mergeStrings(medContent, reasonContent))) {
						//                                              System.out.println(medContent + " " + context + " " + reasonContent);
						out.write(medContent + " + " + context + " + " + reasonContent);
						out.write("\n");
					}
				}
			}

			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}



}
