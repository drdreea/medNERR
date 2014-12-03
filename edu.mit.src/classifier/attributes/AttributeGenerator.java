package classifier.attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import corpus.AnnotationDetail;
import corpus.MedicalRecord;
import corpus.Sectionizer;
import corpus.Annotation.AnnotationType;

import classifier.ClassifierBase;
import discourse.DiscourseProcessing;
import ds.tree.RadixTreeImpl;
import preprocess.parser.Parsing;
import preprocess.parser.WordTagMap;
import preprocess.wordnet.WordNetProcessing;

public abstract class AttributeGenerator {
	ClassifierBase classifier;
	public WordNetProcessing wordNetProcessor;
	public List<String> indexes = Arrays.asList("wmin2", "wmin1","w0" , "w1", "w2");
	List<String> subordinatingConj = Arrays.asList("after", "although", "as", "because", "before", "how", "if", 
			"once", "since", "than", "that", "though", "till", "until", "when", "where", "whether", "while");
	
	public AttributeGenerator(ClassifierBase classifier2){
		this.classifier = classifier2;

		wordNetProcessor = new WordNetProcessing(classifier.configs.wordNetPath);
	}

	/**
	 * Compute the section in which the given medication is located
	 * @param sections
	 * @param wordAnnotation
	 * @param medicalRecord
	 * @return
	 */
	String computeSection(AnnotationDetail wordAnnotation, MedicalRecord medicalRecord) {

		String sectionName = "other";
		if(medicalRecord != null)
			if(wordAnnotation.startLine < medicalRecord.rawFileLines.size() &&
					wordAnnotation.startLine > 0)
				sectionName = Sectionizer.findSection(medicalRecord.sections, 
						medicalRecord.rawFileLines.get(wordAnnotation.startLine-1),
						medicalRecord.rawFileLines.get(wordAnnotation.startLine), 
						wordAnnotation);

		if(sectionName != null && !sectionName.isEmpty()){
			;
		}else
			sectionName = "unk";

		return sectionName;
	}

	ArrayList<String> getCountsForConcept(
			HashMap<String, String> indexValues) {
		ArrayList<String> atts = new ArrayList<String>();

		// apparently only certain n-grams should be checked for here
		List<String> startKeys = Arrays.asList("w0", "w-1", "w-2-1", "w-10", "w01",
				"w-2-10", "w012");
		List<String> inKeys = Arrays.asList("w0", "w-1", "w1", "w2", "w-2-1", "w-10", "w01","w12",
				"w-2-10", "w-101", "w012");

		for(String key : indexValues.keySet()){
			String gram = indexValues.get(key);
			if(gram == null) continue;

			for(AnnotationType anntType : AnnotationType.values()){
				String anntString = getAnntString(anntType);

				if(classifier.trainData.otherStartGoldFrequencies.get(anntType).
						containsKey(gram)){
					int count = classifier.trainData.otherStartGoldFrequencies.
							get(anntType).get(gram);
					count = Math.min(15, count);
					if(count > 3 && startKeys.contains(key))
						atts.add(key + "StartGold=" + 
								anntString + String.valueOf(count));
				}

				if(classifier.trainData.otherInGoldFrequencies.get(anntType).
						containsKey(gram)){
					int count = classifier.trainData.otherInGoldFrequencies.
							get(anntType).get(gram);

					count = Math.min(15, count);
					if(count > 3 && inKeys.contains(key))
						atts.add(key + "InGold=" + 
								anntString + String.valueOf(count));
				}
			}
		}

		return atts; 
	}

	private String getAnntString(AnnotationType annotationType){
		String anntString= null;

		switch(annotationType){
		case R:
			anntString = "R";
			break;
		case MO:
			anntString = "M";
			break;
		case DU:
			anntString = "U";
			break;
		case DO:
			anntString = "D";
			break;
		case F:
			anntString = "F";
			break;
		default:
			break;
		}

		return anntString;
	}

	/**
	 * get the counts of the concept inside the given concept list
	 * @param conceptStrings
	 * @param indexValues
	 * @param type
	 * @return
	 */
	ArrayList<String> getCountsForConcept(
			HashMap<String, Integer> conceptStrings,
			RadixTreeImpl<?> conceptTrie,
			HashMap<String, String> indexValues, String type) {

		ArrayList<String> atts = new ArrayList<String>();
		HashMap<String, Integer> indexCountsStart= new HashMap<String, Integer>();
		HashMap<String, Integer> indexCountsContains = new HashMap<String, Integer>();

		for(String key: indexValues.keySet()){
			indexCountsStart.put(key, 0);
			indexCountsContains.put(key, 0);
		}

		// find the perfect match first
		for(String key : indexValues.keySet())
			if(indexValues.get(key) == null || indexValues.get(key).contains("null"))
				continue;
			else if(conceptStrings.containsKey(indexValues.get(key)))
				indexCountsContains.put(key, conceptStrings.get(indexValues.get(key))+ 
						indexCountsContains.get(key));

		// get the medication counts and add them to the attributes list
		for(String key : indexValues.keySet())
			if(indexValues.get(key) == null || indexValues.get(key).contains("null"))
				continue;
			else {
				int sizeFoundPrefixes = conceptTrie.searchPrefix(indexValues.get(key), 20).size();				
				indexCountsStart.put(key, indexCountsStart.get(key)+ 
						sizeFoundPrefixes);	
			}

		for(String key : indexCountsStart.keySet())
			if(indexCountsStart.get(key) != 0)
				atts.add(key + "StartGold=" + type + String.valueOf(indexCountsStart.get(key)));


		for(String key : indexCountsContains.keySet())
			if(indexCountsContains.get(key) != 0){
				atts.add(key + "InGold=" + type + String.valueOf(indexCountsContains.get(key)));
				//				indexCountsStart.put(key, 0); // we do not want to include the start as well
			}



		return atts;
	}

	boolean checkIsVerb(WordTagMap word) {
		return Parsing.verbs.contains(word.posTag);
	}
	
	boolean checkIsNoun(WordTagMap word) {
		return Parsing.nouns.contains(word.posTag);
	}

	boolean checkIsPossesive(WordTagMap word) {
		return Parsing.possesive.contains(word.posTag);

	}

	public boolean checkIsPossesive(String tag) {
		return Parsing.possesive.contains(tag);

	}


	String checkSpecialNoun(String content) {

		for(String ending : DiscourseProcessing.specialNounEndings)
			if(content.endsWith(ending))
				return "specialNoun";

		return null;
	}

}
