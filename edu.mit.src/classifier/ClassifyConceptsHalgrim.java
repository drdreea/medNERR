
/**
 * 
 */
package classifier;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import classifier.vector.AttributeVector;

import utils.UtilMethods;

/**
 * This class generates the train and test files to be used by the PGM
 * The output format is 
 * Configuration:
 * 	1. how many concepts to be set as target class inside the vector files
 *  2. the vector type : use VectorType.CRF for this class
 *  3. the crf type : CRFType.CONCEPTLINEAR
 *  
 * @author ab
 *
 */
public class ClassifyConceptsHalgrim extends ClassifierBase{

	final static Integer MINIMUM_FREQUENCY = 2;

	String outputFile ;

	private ArrayList<String> trainInstances;
	private ArrayList<String> testInstances;

	ClassificationResults classificationResults;

	/**
	 * 
	 * @param config
	 * @throws Exception 
	 */
	public ClassifyConceptsHalgrim(String config) throws Exception {
		super(config, false);

		trainTestCreator = new TrainTestCreator(this);
		classificationResults = new ClassificationResults(configs, this);

		gsContext = configs.classifierOutputPath + "/gsContext";

		resultsMap = new ArrayList<ArrayList<AttributeVector>>();
	}


	public void readPredictedResultsMallet(){
		findPatternFrequency();
		findContextGS();

		System.out.println("Reading results");
		classificationResults.readConceptResultsMalletSupervisedHalgrim(configs.vector);
//
//		store.storeCache(trainTestCreator.vectorCreator.genericAttributes.
//				pcmQuery.previousQueries);

	}


	/**
	 * just prepare data for mallet crf
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws NoSuchMethodException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 */
	public void executeCRF() throws SecurityException, 
	IllegalArgumentException, NoSuchMethodException, IllegalAccessException, 
	InvocationTargetException{
		findPatternFrequency();
		findContextGS();

		// decide if the crf should clasify other concepts except for the reasons
		boolean randomFileSelection = false;
		boolean trainTestProvided = true;

		trainInstances = trainTestCreator.createConceptSupervisedTrainSetMallet(configs.vector,
				randomFileSelection, 
				trainTestProvided);
		store.storeTrainMallet(trainInstances, trainFile);

		testInstances = trainTestCreator.createConceptSupervisedTestSetMallet(configs.vector);

		store.storeTestMallet(testInstances, testFile);

	}



	/**
	 * Find patterns that have a certain frequency
	 */
	private void findPatternFrequency() {
		patternCounts = 
				new HashMap<String, Integer>();

		for(String pattern : this.patternQueries.keySet()) {
			HashMap<String, Double> values = this.patternQueries.get(pattern);
			boolean found = true;
			ArrayList<String> words = new ArrayList<String>();

			for(String word : values.keySet()) {
				if(values.get(word) == 0.0) {
					found = false;
				}

				word = Utils.removePunctuation(word);

				words.add(word);

				if(word == null) {
					found = false; 
					continue;
				}
			}

			if(found) {
				if(patternCounts.containsKey(UtilMethods.mergeStrings(words)))
					patternCounts.put(UtilMethods.mergeStrings(words), 
							patternCounts.get(UtilMethods.mergeStrings(words)) +1);
				else
					patternCounts.put(UtilMethods.mergeStrings(words), 1);
			}
		}

		for(String val : patternCounts.keySet())
			if(patternCounts.get(val) > MINIMUM_FREQUENCY ) {
				medToReason.add(val);
			}

		System.out.println(String.valueOf(medToReason.size()));

	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println(ClassifierBase.errorMessage);
			System.exit(-1);
		}

		try{
			ClassifyConceptsHalgrim classifier = new ClassifyConceptsHalgrim(args[0]);
			classifier.readPredictedResultsMallet();
		}catch(Exception e){
			e.printStackTrace();
			System.out.println(ClassifierBase.errorMessage);

		}
	}
}