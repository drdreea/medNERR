package classifier.attributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import corpus.AnnotationDetail;
import corpus.MedicalRecord;
import corpus.Annotation.AnnotationType;

import classifier.ClassifierBase;
import classifier.vector.VectorCreator.InstanceType;
import classifier.vector.VectorCreator.TagType;
import preprocess.Soundex;
import preprocess.parser.SentenceContent;
import preprocess.parser.WordTagMap;
import preprocess.stemmer.EnglishStemmer;
import preprocess.textToSpeech.TextToSpeechTransformer;
import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.State;
import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.TaggingDict;
import utils.UtilMethods;

public class AttributeFactory {
	public POSAttributes posAttrs;
	public InnerContextAttributes innerContextAttrs;
	public GenericAttributes genericAttributes;
	public ConceptAttributes conceptAttributes;
	public HalgrimAttributes halgrimAttributes;
	public TextToSpeechTransformer textToSpeech;
	public TopicAttributes topicAttributes;

	ClassifierBase classifier ;
	String file;
	SentenceContent sentence;
	InstanceType concept;
	int sentenceIndex;
	AnnotationDetail annt;
	TagType tag;
	int wordIndex;
	WordTagMap word;
	private AnnotationDetail medication;
	private AnnotationDetail reason;
	private MedicalRecord medicalRecord;
	private String section;

	public static List<Character> vowels = Arrays.asList('a', 'e', 'i', 'o','u', 'y');
	public static List<Character> consonants = Arrays.asList('b', 'c', 'd', 'f','g',
			'h','j', 'k','l','m','n','p','q','r','s','t','v','w','x','z');

	public AttributeFactory(ClassifierBase givenClassifier){
		classifier = givenClassifier;

		posAttrs = new POSAttributes(classifier);
		innerContextAttrs = new InnerContextAttributes(classifier);
		genericAttributes = new GenericAttributes(classifier);
		conceptAttributes = new ConceptAttributes(classifier);
		halgrimAttributes = new HalgrimAttributes(classifier);
		topicAttributes = new TopicAttributes(classifier);
		textToSpeech = new TextToSpeechTransformer();
	}

	public void initGeneric(String file, 
			MedicalRecord record) {
		this.file = file;
		this.medicalRecord = record;
	}

	public void initSentence(SentenceContent sentence, int sentenceIndex) {
		this.sentence = sentence;
		this.sentenceIndex = sentenceIndex;
	}

	public void initAnnotation(AnnotationDetail annt, TagType tag, 
			int wordIndex, WordTagMap word) {
		this.wordIndex = wordIndex;
		this.word = word;
		this.annt = annt;
		this.tag = tag;
	}

	public void initAnnotation(AnnotationDetail medication, 
			AnnotationDetail reason) {
		this.medication = medication;
		this.reason = reason;
	}


	public String getContent(){
		
		//		return annt.content;
		if(classifier.normalizeText)
			return UtilMethods.normalizeText(word);

		return word.baseForm.toLowerCase();
	}

	public String isMed(){		
		if(tag != null && word.annotationType == AnnotationType.M)
			return "isMed";
		else 
			return "notMed";
	}

	public String isAdj(){
		if(halgrimAttributes.checkAdj(word))
			return "adj";
		else
			return "notAdj";
	}


	public String isVerb(){
		if( halgrimAttributes.checkIsVerb(word))
			return "isVerb";
		
		return "noVerb";
	}
	
	public String isThirdPersonVb(){
		if(word.posTag.equals("VBZ"))
			return "thirdPersonVB";
		
		return "noThirdPersonVB";
	}
	
	public String isNoun(){
		if (halgrimAttributes.checkIsNoun(word))
			return "isNoun";
		
		return "noNoun";
	}
	
	// time of day
	// date
	// duration
	// frequency
	public String isTime(){
		if(word.umlsSemanticType == null)
			return "noTime";
		
		if(word.umlsSemanticType.equals("tmco"))
			return "isTime";
		
		return "noTime";
	}
	
	public String isPlace(){
		if(word.umlsSemanticType == null)
			return "noPlace";
		
		if(word.umlsSemanticType.equals("spco") ||
				word.umlsSemanticType.equals("geoa"))
			return "isPlace";
		
		return "noPlace";
	}
	
	public String isQualitativeConcept(){
		if(word.umlsSemanticType != null &&
				word.umlsSemanticType.equals("qlco"))
			return "qualitativeConcept";
		
		return "noQConcept";
	}

	public boolean isPreposition(){
		return word.posTag.equals("IN");

	}


	public String getTag(){
		return tag.toString();
	}

	public String getTopic(){
		return this.topicAttributes.getTopic(this.annt, sentence);
	}

	public String isReason(){
		if(word.annotationType == AnnotationType.R )
			return  "isReason";
		else 
			return "notReason";
	}

	public String isConcept(){
		if(word.annotationType != AnnotationType.O)
			return "isConcept";
		else
			return "notConcept";
	}
	
	List<String> negationWords = Arrays.asList("never", "no", 
			"not", "neither", "nobody", "none", "nor", "nothing",
			"nowhere");
	
	public String isNegation(){
		// not-negator
		if(negationWords.contains(word.word.toLowerCase())||
				UtilMethods.removePunctuation(word.word).endsWith("nt"))
			return "isNegation";
		
		// affixal negation
		if(word.word.startsWith("un") ||
//				word.word.startsWith("dis")||
				word.word.startsWith("im") ||
				word.word.startsWith("in")||
				word.word.startsWith("non") ||
				word.word.startsWith("un"))
			return "isNegation";
		
		return "notNegation";
				
	}

	public String getConceptType(){
		return word.annotationType.toString();
	}

	public String getWordLengthCap(){
		int len = Math.min(12, annt.content.length());
		return "wordLen=" + String.valueOf(len);
	}

	public String getTokenIndex(){
		return "tokenInd=" + String.valueOf(annt.startOffset);
	}

	public String getDepRel(){
		return word.depRel;
	}

	public String getDepRelAndHead(){
		return word.depRel + "_" + getHeadPOS();
	}

	public String getHeadPOS(){
		//		return word.head;
		int index = Integer.parseInt(word.head);
		String head = "root";

		if(index != 0)	
			head = this.sentence.tags.get(index-1).coarseGrainedPOS;

		return head;
	}

	public String getHead(){
		//		return word.head;
		int index = Integer.parseInt(word.head);
		String head = "root";

		if(index != 0)	
			head = this.sentence.tags.get(index-1).word.toLowerCase();

		if(classifier.normalizeText)
			head = UtilMethods.normalizeText(head);

		return head;
	}

	public String getHeadAndRel(){
		return getContent() + "_" + getHead() + "_" + getDepRel();
	}

	public String getSection(){
		if(this.section == null)
			this.section = this.halgrimAttributes.computeSection(annt, medicalRecord).toLowerCase();

		return this.section;
	}

	public String getUMLSCode(){
		String wordString = word.word;

		double val = this.genericAttributes.checkMedSemanticType(wordString);

		return String.valueOf(val);
	}


	public String getPhonemes() throws IOException{
		String wordString = word.word;

		String phoneme = medicalRecord.phonemes.get(wordString);

		if (phoneme.split(" ").length > 1){
			phoneme = "compound";
		}

		return phoneme;
	}

	public String getWordEmphasis() throws IOException{
		String emphasislocation = "0";

		String phoneme = getPhonemes();
		if(phoneme.contains("'"))
			emphasislocation = String.valueOf(phoneme.indexOf("'"));

		return emphasislocation;
	}

	public String getWordSecondEmphasis() throws IOException{
		String emphasislocation = "0";

		String phoneme = getPhonemes();
		if(phoneme.contains(","))
			emphasislocation = String.valueOf(phoneme.indexOf(","));

		return emphasislocation;
	}

	public String getSoundexCode(){
		String wordString = word.word;

		String soundexCode = Soundex.soundex(wordString);

		if(soundexCode == null)
			return "NaN";

		return soundexCode;
	}

	public String hasMultipleEmphasis() throws IOException{
		String emphasis = "single";

		String phoneme = getPhonemes();		

		int countEmph = 0;
		for(int index = 0; index < phoneme.length(); index ++)
			if(phoneme.charAt(index) == ',')
				countEmph ++;

		if(countEmph > 1)
			emphasis = "multiple";

		return emphasis;
	}

	public String hasWordSecondEmphasis() throws IOException{
		String emphasislocation = "False";

		String phoneme = getPhonemes();
		if(phoneme.contains(","))
			emphasislocation = "True";

		return emphasislocation;
	}


	public String getSpelledNumber() {
		return this.halgrimAttributes.getSpelledNumber(word);
	}

	public double getPercentageConsonants(){
		double consonantsPercentage = 0.;

		String wordString = word.word;

		for(int index = 0; index < wordString.length(); index ++)
			if(!vowels.contains(wordString.charAt(index)))
				consonantsPercentage ++;

		if(wordString.length() > 0)
			consonantsPercentage = consonantsPercentage/wordString.length();

		return consonantsPercentage;
	}

	public boolean checkAdverbLy(){
		String wordString = word.word;

		if(wordString.toLowerCase().endsWith("ly"))
			return true;

		return false;
	}

	public String checkContainsRareConsonant(){
		String wordString = word.word.toLowerCase();
		wordString = UtilMethods.removePunctuation(wordString);

		if(wordString.contains("x") ||
				wordString.contains("z") 
				|| 
				wordString.contains("v") 
				|| 
				(wordString.contains("y") && !wordString.endsWith("y"))
				)
			return "rareCons";

		return "noRare";
	}

	/**
	 * Get the maximum span of consonants with length at least 2
	 * 
	 * @return
	 */
	public String getMaxConsonantsSpan(){
		String maxSpan = "";
		String wordString = word.word;

		int wordLength = wordString.length();
		int index = 0;
		String foundSpan = null;

		while (index < wordLength){
			char currentChar = wordString.charAt(index);

			if(consonants.contains(currentChar)){
				if(foundSpan == null) foundSpan = String.valueOf(currentChar);
				else foundSpan = foundSpan + String.valueOf(currentChar);
			}else{
				if (foundSpan != null &&
						foundSpan.length() > 2 && // at least one consonant
						! wordString.startsWith(foundSpan) && // word should not start with the span
						foundSpan.length() > maxSpan.length()) // has to have length larger than the 
					// already max span
					maxSpan = foundSpan;

				foundSpan = null;
			}

			index ++;
		}

		if(maxSpan.isEmpty())
			maxSpan = "none";

		return maxSpan;
	}

	public String getDistancePrevMed(){
		int dist = 0;
		boolean foundMed = false;

		for(int index = word.documentWordOffset; index >= 0 && dist <= 8; index -- ){
			AnnotationDetail annt = medicalRecord.annotationAtWord.get(index);
			if(annt == null){
				dist ++;
				continue;
			}

			if(annt.type == AnnotationType.M){
				foundMed = true;
				break;
			}

			dist ++;
		}

		if(!foundMed)
			dist = 9;

		return String.valueOf(dist);
	}

	public String getDistanceNextMed(){
		int dist = 0;

		for(int index = word.documentWordOffset; dist <= 8; index ++ ){
			AnnotationDetail annt = medicalRecord.annotationAtWord.get(index);

			if(annt == null){
				dist ++;
				continue;
			}

			if(annt.type == AnnotationType.M)
				break;
			dist ++;
		}

		return String.valueOf(dist);
	}

	public String isAfterConcept(){
		if(getDistancePrevMed().equals("1") || 
				getDistancePrevReason().equals("1"))
			return "afterConcept";

		return "notAfterConcept";

	}

	public String getDistanceNextReason(){
		int dist = 0;

		for(int index = word.documentWordOffset; dist <= 8; index ++ ){
			AnnotationDetail annt = medicalRecord.annotationAtWord.get(index);

			if(annt == null){
				dist ++;
				continue;
			}

			if(annt.type == AnnotationType.R)
				break;
			dist ++;
		}

		return String.valueOf(dist);
	}


	public String getDistancePrevReason(){
		int dist = 0;
		boolean foundMed = false;

		for(int index = word.documentWordOffset; index >= 0 && dist <= 8; index -- ){
			AnnotationDetail annt = medicalRecord.annotationAtWord.get(index);
			if(annt == null){
				dist ++;
				continue;
			}

			if(annt.type == AnnotationType.R){
				foundMed = true;
				break;
			}

			dist ++;
		}

		if(!foundMed)
			dist = 9;

		return String.valueOf(dist);
	}

	public String getLemma(){		
		String lemma = "";

		lemma = EnglishStemmer.stem(word.word);

		return lemma;
	}

	public String getTokenBefore(){
		if(word.documentWordOffset -1 < 0)
			return "None";

		String prevWord = medicalRecord.words.get(word.documentWordOffset-1);

		if(prevWord != null){
			if(classifier.normalizeText)
				return UtilMethods.normalizeText(prevWord.toLowerCase());

			return prevWord.toLowerCase();
		}

		else
			return "None";
	}

	public String getTrigramAfter(){
		ArrayList<String> nextWords = new ArrayList<String>();

		for(int index = 1; index <= 3; index ++ ){
			String token = "None";

			if(word.documentWordOffset+index < medicalRecord.words.size())
				token = medicalRecord.words.get(word.documentWordOffset + index);

			if(token == null)
				token = "None";

			else if(classifier.normalizeText)
				token = UtilMethods.normalizeText(token);

			nextWords.add(token);
		}

		return UtilMethods.mergeStrings(nextWords, "-").toLowerCase();

	}

	public String getTrigramBefore(){
		ArrayList<String> nextWords = new ArrayList<String>();

		for(int index = -3; index < 0; index ++ ){
			String token = "None";

			if(word.documentWordOffset+index < medicalRecord.words.size() &&
					word.documentWordOffset+index >= 0)
				token = medicalRecord.words.get(word.documentWordOffset + index);

			if(token == null)
				token = "None";

			nextWords.add(token);
		}

		return UtilMethods.mergeStrings(nextWords, "-").toLowerCase();

	}

	public String getBigramAfter(){
		ArrayList<String> nextWords = new ArrayList<String>();

		for(int index = 1; index <= 2; index ++ ){
			String token = "None";

			if(word.documentWordOffset+index < medicalRecord.words.size())
				token = medicalRecord.words.get(word.documentWordOffset + index);

			if(token == null)
				token = "None";

			nextWords.add(token);
		}

		return UtilMethods.mergeStrings(nextWords, "-").toLowerCase();

	}

	public String getBigramBefore(){
		ArrayList<String> nextWords = new ArrayList<String>();

		for(int index = -2; index < 0; index ++ ){
			String token = "None";

			if(word.documentWordOffset+index < medicalRecord.words.size() && 
					word.documentWordOffset+index >= 0)
				token = medicalRecord.words.get(word.documentWordOffset + index);

			if(token == null)
				token = "None";

			nextWords.add(token);
		}

		return UtilMethods.mergeStrings(nextWords, "-").toLowerCase();

	}


	public String getPOS(){
		return word.posTag;
	}

	public String getCoarsePOS(){
		return word.coarseGrainedPOS;
	}

	public String checkTimeWord() {
		return this.halgrimAttributes.checkTimeWord(word);

	}

	public String checkPreposition() {
		return this.halgrimAttributes.checkPreposition(word);

	}

	public String checkNumber() {
		return "isNumber=" + String.valueOf(UtilMethods.isNumber(
				UtilMethods.removePunctuation(word.word)));
	}

	public String checkStartsWithQ(){
		return this.halgrimAttributes.startsWithQ(word);
	}

	public String checkSlidingScale(){
		return halgrimAttributes.checkDistanceSlidingScale(wordIndex, 
				medicalRecord.sentences, sentenceIndex);
	}

	public boolean checkDosagePattern(){
		return halgrimAttributes.matchesDosagePattern(word.word);
	}

	public boolean checkDurationPattern(){
		return halgrimAttributes.matchesDurationPattern(word.word);
	}


	public String checkIsPunctuation(){
		boolean isPunctuation = UtilMethods.isPunctuation(word.word);

		if(isPunctuation)
			return "isPunctuation";
		else return "noPunctuation";
	}

	public boolean checkIsDrug(){
		return halgrimAttributes.checkIsDrug(word);
	}

	public boolean hasUpcomingConcept(){

		for(int index = sentenceIndex; index < sentence.tags.size() &&
				index < sentenceIndex + 15; index ++){
			WordTagMap currentWord = sentence.tags.get(index);
			if(currentWord.annt != null && currentWord.annt.type != null)
				return true;
		}

		return false;
	}

	List<String> measureUnit = Arrays.asList("mg", "ml", "g", "sprays", 
			"units", "tablets", "mcg", "po", "prn", "qd");

	public String isQuantity(){
		if(measureUnit.contains(UtilMethods.removePunctuation(
				word.word.toLowerCase())))
			return "isQuant";
		return "notQuant";
	}

	public String isLabSection(){
		String heading = getSection();

		if(UtilMethods.excludeHeadings.contains(heading))
			return "isLabSection";

		return "noLabSection";
	}

	public String getUMLSSemanticType(){
		if(word.umlsSemanticType == null)
			return "none";

		if(word.hasMedUMLSType)
			return "medUMLS";

		return word.umlsSemanticType;
	}

	public String isPRN(){
		if(UtilMethods.removePunctuation(word.word).equals("prn"))
			return "isPRN";
			
		return "notPRN";
	}

	List<String> forWord = Arrays.asList("given", "needed");
	List<String> ofWord = Arrays.asList("signs", "sign", "risk", "concern", "concerns",
			"because");
	List<String> toWord = Arrays.asList("order");
	List<String> withWord = Arrays.asList("diagnosed");

	public String isPrepositionReason(){

		// get the previous word
		if(wordIndex-2 >=0 ){
			String prevOneWord = sentence.tags.get(wordIndex-1).word;
			String prevTwoWords = sentence.tags.get(wordIndex-2).word;

			if((prevOneWord.equals("for") && forWord.contains(prevTwoWords))
					||
					(prevOneWord.equals("of") && ofWord.contains(prevTwoWords))
					||
					(prevOneWord.equals("to") && toWord.contains(prevTwoWords))
					||
					(prevOneWord.equals("with") && withWord.contains(prevTwoWords))
					)
				return "isPrep";
				
		}
		
		return "noPrep";
	}

	public String isPronoun(){
		if(word.word.equals("his") || word.word.equals("her") ||
				word.word.equals("your"))
			return "isPronoun";
		
		return "noPronoun";
	}
	
	public String isBeing(){
		if(word.posTag.equals("PRP"))
			return "isBeing";
		
		return "noBeing";
	}
	
	public String isPossesive(){
		if(word.posTag.equals("PRP$"))
			return "isPossesive";
		
		if(word.word.equals("have") || word.word.equals("has"))
			return "isPossesive";
		
		return "noPossesive";
	}

	public HashMap<String, Object> generateAllAttributes() {
		HashMap<String, Object> attributeValues = new HashMap<String, Object>();

		//		// Add the generic attributes
		attributeValues.putAll(genericAttributes.getGenericFeatures(
				medication, reason, medicalRecord));
		//
		//		// Add the inner context attributes
		attributeValues.putAll(innerContextAttrs.getInnerContextFeatureValues(
				medication, reason, medicalRecord));
		//
		//		// Add the pos derived features
		attributeValues.putAll(posAttrs.getPOSFeatureValues(medication, 
				reason,  medicalRecord));

		return attributeValues;
	}





}
