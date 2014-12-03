package classifier;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import corpus.MedicalRecord;

import preprocess.parser.SentenceContent;
import preprocess.parser.WordTagMap;

import cc.mallet.util.ArrayListUtils;
import classifier.vector.AttributeVector;
import classifier.vector.VectorCreator;
import classifier.vector.VectorCreator.TagType;

import utils.NLPSystem;

/**
 * This class handles the creation of the training and test files
 * @author ab
 *
 */
public class TrainTestCreator {
	VectorCreator vectorCreator ;
	ClassifierBase classifier ;

	ArrayList<String> positiveTrainInstances;
	int numberTrainFiles = 115;
	int numberParamFiles = 30;
	int allowedNonSentenceCount = 300;
	int positiveSentences = 0;
	int negativeSentences = 0;

	ArrayList<String> outputComments;

	/** The type of vector to generate*/
	public enum VectorType{CRF,  CLASSIFIER, MAXENT, HMM, CODL, CODL_CONTEXT, CODL_MEDICATION};
	public enum InfoToExtract{CONCEPT, RELATION, BOTH};
	public enum CorpusType{I2B2, I2B2RELATION, ACE};

	public static final List<String> incorrectReasonCategs = Arrays.asList("phsu", "ftcn",
			"inpr", "qlco", "qnco", "cnce", "dora", "hlca", "medd", "tisu", "bodm",
			"food", "inbe", "idcn", "clna", "bodm" );
	//"blor", "tmco", "bacs", "diap"

	/**
	 * 
	 * @param classifier
	 */
	public TrainTestCreator(ClassifierBase classifier){
		vectorCreator = new VectorCreator(classifier);
		this.classifier = classifier;
		positiveTrainInstances = new ArrayList<String>();

		outputComments = new ArrayList<String>();

		numberTrainFiles --;
	}

	/**
	 * Create the supervised test set for concept prediction
	 * @param vector
	 * @param crftype
	 * 
	 * @return
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws NoSuchMethodException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 */
	ArrayList<String> createConceptSupervisedTestSetMallet(VectorType vector) 
			throws SecurityException, IllegalArgumentException, 
			NoSuchMethodException, IllegalAccessException, 
			InvocationTargetException {
		ArrayList<String> outputLines = new ArrayList<String>();

		for(String file : classifier.testData.medicalRecords.keySet()) {
			// check the file was not used in training
			//			if(this.trainFiles.contains(file)) continue;
			//			testFiles.add(file);

			// go through the sentences and send each one to the instance creator
			int sentenceCount = 1;
			MedicalRecord record = classifier.testData.medicalRecords.get(file);
			vectorCreator.init(file, record);

			for(SentenceContent sentence : record.sentences) {

				ArrayList<AttributeVector> instances = 
						vectorCreator.createSentenceVectors(sentence,
								sentenceCount -1, record);

				outputLines.addAll(AttributeVector.printVectorList(instances, vector,
						classifier.infoType, classifier.attributeNames,
						classifier.normalizeText));

				sentenceCount ++;
			}
		}

		return outputLines;
	}


	/**
	 * 
	 * @param vector
	 * @param crftype
	 * 
	 * @return
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws NoSuchMethodException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 */
	ArrayList<String> createContextVectors(VectorType vector, 
			NLPSystem system, boolean isTrain) 
					throws SecurityException, IllegalArgumentException, 
					NoSuchMethodException, IllegalAccessException, 
					InvocationTargetException {
		vectorCreator.totalCount = 0;

		ArrayList<String> outputLines = new ArrayList<String>();

		for(String file : system.medicalRecords.keySet()) {
			System.out.println("Processing file: " + file);
			MedicalRecord record = system.medicalRecords.get(file);
			vectorCreator.init(file, record);

			int sentenceCount = 0;
			for(SentenceContent sentence : record.sentences){
				
				ArrayList<AttributeVector> instances = 
						vectorCreator.createSentenceRelationVectors(sentence, 
								record, sentenceCount);
				
				///////////////////////////////////
				boolean found = false;

				// check if we have a relation in the given sentence
				for(AttributeVector word : instances)
					if(word.tag != TagType.O){
						found = true;
						break;
					}

				if(!found){
					allowedNonSentenceCount -= 1;
					this.negativeSentences += 1;
				}else
					this.positiveSentences += 1;

				if(allowedNonSentenceCount <= 0 && isTrain && !found){
					continue;
				}
				///////////////////////////////////


				if(!isTrain)
					classifier.testAnnotations.addAll(instances);

				// store the attribute vector
				outputLines.addAll(AttributeVector.printVectorList(instances, vector,
						classifier.infoType, classifier.attributeNames,
						classifier.normalizeText));

				
				sentenceCount ++;
			}
		}

		return outputLines;
	}


	ArrayList<String> createNERVectors(VectorType vector, 
			NLPSystem system, boolean isTrain) 
					throws SecurityException, IllegalArgumentException, 
					NoSuchMethodException, IllegalAccessException, 
					InvocationTargetException {
		ArrayList<String> outputLines = new ArrayList<String>();

		for(String file : system.medicalRecords.keySet()) {
			System.out.println("Processing file: " + file);
			MedicalRecord record = system.medicalRecords.get(file);
			vectorCreator.init(file, record);

			// go through the sentences and identify the annotations
			int sentenceCount = 0;
			for(SentenceContent sentence : record.sentences){
				ArrayList<AttributeVector> instances = 
						vectorCreator.createSentenceVectors(sentence, 
								sentenceCount, record);

				if(!isTrain)
					classifier.testAnnotations.addAll(instances);

				outputLines.addAll(AttributeVector.printVectorList(instances, vector,
						classifier.infoType, classifier.attributeNames,
						classifier.normalizeText));

				sentenceCount ++;
			}
		}

		return outputLines;
	}

	/**
	 * Create the supervised train set for concept prediction
	 * with 115 records
	 * @param vector
	 * @param setType
	 * @param crfType
	 * @param randomFileSelection
	 * @param trainTestProvided
	 * 
	 * @return  
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws NoSuchMethodException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 */
	ArrayList<String> createConceptSupervisedTrainSetMallet(VectorType vector,
			boolean randomFileSelection, 
			boolean trainTestProvided) throws SecurityException, 
			IllegalArgumentException, NoSuchMethodException, 
			IllegalAccessException, InvocationTargetException {
		ArrayList<String> outputLines = new ArrayList<String>();

		// first select the training files
		//		createSupervisedTrainSet(randomFileSelection, trainTestProvided);

		// make a list of the positives instances
		//		HashSet<String> positiveInstances = new HashSet<String>();
		//		for(String file : this.classifier.trainData.medicalRecords.keySet()) {
		//			for(Annotation med : classifier.trainData.medicalRecords.get(file).concepts) {
		//				if(med.reason == null || med.reason.content.equals("nm")) continue;
		//
		//				positiveInstances.add(AttributeVector.printComments(med.medication, file));
		//				positiveInstances.add(AttributeVector.printComments(med.reason, file));
		//			}
		//		}

		// include positive instances
		// go through the sentences and send in the annotations in each sentence

		for(String file : this.classifier.trainData.medicalRecords.keySet()) {
			MedicalRecord record = this.classifier.trainData.medicalRecords.get(file);
			vectorCreator.init( file, record);

			int sentenceCount = 0;

			for(SentenceContent sentence : record.sentences){

				ArrayList<AttributeVector> instances = 
						vectorCreator.createSentenceVectors(sentence,
								sentenceCount, record);

				outputLines.addAll(AttributeVector.printVectorList(instances, vector,
						classifier.infoType, classifier.attributeNames,
						classifier.normalizeText));

				sentenceCount ++;
			}
		}

		return outputLines;
	}

	/**
	 * Remove unwanted reason based on their umls semantic type
	 * @param reasonContent
	 * @return
	 */
	boolean filterSemanticType(String reasonContent){
		if(classifier.conceptSemanticTypes.containsKey(reasonContent)) {
			for(String el :  classifier.conceptSemanticTypes.get(reasonContent))
				if(TrainTestCreator.incorrectReasonCategs.contains(el))
					return true;

		}

		return false;
	}
}
