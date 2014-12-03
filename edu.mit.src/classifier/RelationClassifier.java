
/**
 * 
 */
package classifier;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import classifier.TrainTestCreator.InfoToExtract;
import classifier.vector.AttributeVector;

import probabModel.ccm_code.Experiments.HMMConstraintsMedicationSupervised;
import probabModel.ccm_code.Experiments.Parameters.Constraints;

import utils.UtilMethods;

/**
 * This class generates the train and test vector files
 * for creating and testing a named relation recognizer model
 * 
 * Use VectorType.CODL_MEDICATION;
 * @author ab
 *
 */
public class RelationClassifier extends ClassifierBase{

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
	public RelationClassifier(String config) throws Exception {
		super(config, true);

		trainTestCreator = new TrainTestCreator(this);
		classificationResults = new ClassificationResults(configs, this);

		gsContext = configs.classifierOutputPath + "/gsContext";

		resultsMap = new ArrayList<ArrayList<AttributeVector>>();
	}


	public void readPredictedResults(){

		System.out.println("Reading results");
		if(this.infoType == InfoToExtract.BOTH)
			classificationResults.readContextAndConceptResults(configs.vector, testFile,
					this.testData);
		else
			classificationResults.readContextResults(configs.vector, testFile,
					this.testData);
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
//		findContextGS();

		trainInstances = trainTestCreator.createContextVectors(vectorType,
				this.trainData, true);
		store.storeTrainMallet(trainInstances, trainFile);

		testInstances = trainTestCreator.createContextVectors(vectorType, 
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
			RelationClassifier classifier = new RelationClassifier(args[0]);
			
			classifier.normalizeText = true;
			
			// 1. generate the train and test files
			classifier.generateTrainTestFiles();

			// 2. create a prediction model and classify given instances
			boolean useFeatures = true ;
			boolean useConstraints = true;
			Constraints chosenConstraints;

			switch(classifier.corpusType){
			case I2B2:
				chosenConstraints = Constraints.RELATION;
				if(classifier.infoType == InfoToExtract.BOTH)
					chosenConstraints = Constraints.REASONANDRELATION;

				break;
			default:
				chosenConstraints = Constraints.RELATION_GENERIC;


				break;
			}
			HMMConstraintsMedicationSupervised hmmClassifier = new 
					HMMConstraintsMedicationSupervised(classifier.attributeNames.size() + 2,
							classifier.configs.classifierOutputPath + "/" +
									ClassifierBase.resultsFile, 
									chosenConstraints, useFeatures,
									useConstraints);

			String[] classifierArguments = {classifier.configs.classifierOutputPath + "/" +
					ClassifierBase.trainFile, 
					classifier.configs.classifierOutputPath + "/" +
							ClassifierBase.testFile,
							classifier.configs.classifierOutputPath + "/" +
									ClassifierBase.unlabelledFile,
									"0", "0", "0.9"} ; // these are additional program params
			hmmClassifier.testBed(classifierArguments);

			// 3. read the prediction results
			classifier.readPredictedResults();
			
			System.out.println(classifier.trainTestCreator.positiveSentences);
			System.out.println(classifier.trainTestCreator.negativeSentences);

		}catch(Exception e){
			e.printStackTrace();
			System.out.println(ClassifierBase.errorMessage);

		}
	}
}