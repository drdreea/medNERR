
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
 * Use params :
 *         VectorType.CODL;
 *         CRFType.CONCEPTLINEAR
 * @author ab
 *
 */
public class ClassifyMedicalConditions extends ClassifierBase{

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
	public ClassifyMedicalConditions(String config) throws Exception {
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
		classificationResults.readNERresults(configs.vector, 
				testFile, true);

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
	public void generateTrainTestFiles() throws SecurityException, 
	IllegalArgumentException, NoSuchMethodException, 
	IllegalAccessException, InvocationTargetException{
		findPatternFrequency();
		findContextGS();

		// decide if the crf should clasify other concepts except for the reasons
		boolean randomFileSelection = false;
		boolean trainTestProvided = true;

		trainInstances = trainTestCreator.createConceptSupervisedTrainSetMallet(vectorType,
				randomFileSelection, 
				trainTestProvided);
		store.storeTrainMallet(trainInstances, trainFile);

		testInstances = trainTestCreator.createConceptSupervisedTestSetMallet(vectorType);

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
			ClassifyMedicalConditions classifier = new ClassifyMedicalConditions(args[0]);

			classifier.generateTrainTestFiles();
			//       classifier.readPredictedResultsMallet();
		}catch(Exception e){
			e.printStackTrace();
			System.out.println(ClassifierBase.errorMessage);

		}
	}
}