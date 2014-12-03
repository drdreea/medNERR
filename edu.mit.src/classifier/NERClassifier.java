
/**
 * 
 */
package classifier;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import corpus.Annotation.AnnotationType;

import classifier.vector.AttributeVector;

import probabModel.ccm_code.Experiments.HMMConstraintsMedicationSupervised;
import probabModel.ccm_code.Experiments.Parameters.Constraints;

import utils.UtilMethods;

/**
 * This class generates the train and test vector files
 * for creating and testing a named entity recognizer model
 * 
 * Use VectorType.CODL_MEDICATION;
 * @author ab
 *
 */
public class NERClassifier extends ClassifierBase{

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
	public NERClassifier(String config) throws Exception {
		super(config, false);

		trainTestCreator = new TrainTestCreator(this);
		classificationResults = new ClassificationResults(configs, this);

		gsContext = configs.classifierOutputPath + "/gsContext";

		resultsMap = new ArrayList<ArrayList<AttributeVector>>();
	}


	public void readPredictedResultsMallet(){
		//		findPatternFrequency();
		//		findContextGS();

		System.out.println("Reading results");
		classificationResults.readNERresults(configs.vector, testFile, true);
		//
//				store.storeCache(trainTestCreator.vectorCreator.genericAttributes.
//						pcmQuery.previousQueries);

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
	IllegalArgumentException, NoSuchMethodException, IllegalAccessException, 
	InvocationTargetException{
		findPatternFrequency();
		findContextGS();

		// decide if the crf should classify other concepts except for the reasons
		//		boolean randomFileSelection = false;
		//		boolean trainTestProvided = true;

		trainInstances = trainTestCreator.createNERVectors(vectorType,
				this.trainData, true);
		store.storeTrainMallet(trainInstances, trainFile);

		testInstances = trainTestCreator.createNERVectors(vectorType, 
				this.testData, false);

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
			NERClassifier classifier = new NERClassifier(args[0]);

			// 1. generate the train and test files
			classifier.generateTrainTestFiles();

			// 2. create a prediction model and classify given instances
			boolean useFeatures =true;
			boolean useConstraints = true;

			Constraints givenConstraints = null;
			AnnotationType toAnnotate = classifier.nerToAnnotate.get(0);

			switch(toAnnotate){
			case M:
				givenConstraints = Constraints.MEDICATION;
				break;
			case R:
			default:
				givenConstraints = Constraints.MEDICAL;
				break;
			}

			if(classifier.nerToAnnotate.size() > 1)
				givenConstraints = Constraints.REASONANDRELATION;

			HMMConstraintsMedicationSupervised hmmClassifier = new 
					HMMConstraintsMedicationSupervised(classifier.attributeNames.size() + 2,
							classifier.configs.classifierOutputPath + "/" +
									ClassifierBase.resultsFile, 
									givenConstraints, useFeatures,
									useConstraints);

			String[] classifierArguments = {classifier.configs.classifierOutputPath + "/" +
					ClassifierBase.trainFile, 
					classifier.configs.classifierOutputPath + "/" +
							ClassifierBase.testFile,
							classifier.configs.classifierOutputPath + "/" +
									ClassifierBase.unlabelledFile,
									"0", "0", "0.9"} ; // these are additional program params
			hmmClassifier.testBed(classifierArguments);

//			// 3. read the prediction results
			classifier.readPredictedResultsMallet();

		}catch(Exception e){
			e.printStackTrace();
			System.out.println(ClassifierBase.errorMessage);

		}
	}
}