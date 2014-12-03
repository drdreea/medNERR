package classifier.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.MedicalRecord;
import corpus.Section;
import corpus.Sectionizer;
import corpus.Annotation.AnnotationType;

import patternExtraction.FindContextFromEMR;
import postprocess.pcm.QueryPCM;
import preprocess.negation.MainConText;
import preprocess.parser.SentenceContent;
import utils.UtilMethods;
import classifier.ClassifierBase;
import classifier.Utils;
import discourse.DiscourseProcessing;

public class GenericAttributes extends AttributeGenerator{
	MainConText negationIdentifier;
	DiscourseProcessing discourse;

	public QueryPCM pcmQuery;
	HashMap<String, Integer> reasonFrequencies;

	private Integer normFrequenciesMedReason;
	int normPatternOverlap;
	int normPcmQuery;
	int normReasonFrequency;

	public GenericAttributes(ClassifierBase classifier){
		super(classifier);

		pcmQuery = new QueryPCM(classifier.configs.pcmPath);

		negationIdentifier = new MainConText();
		discourse = new DiscourseProcessing();

		computeNorm();
		computeFrequencyReasons();

		normFrequenciesMedReason = Collections.max(classifier.frequencies.values());

	}

	/**
	 * get the generic feature values
	 * @param medication
	 * @param reason
	 * @param file
	 * @param sections
	 * @return
	 */
	public HashMap<String, Object> getGenericFeatures(AnnotationDetail medication, 
			AnnotationDetail reason, MedicalRecord medicalRecord){
		HashMap<String, Object> featureValues = new HashMap<String, Object>();


		//Feature 0: Same section
		featureValues.put("checkAnnotationsSameSection", 
				checkAnnotationsSameSection(medication, reason, 
						medicalRecord));

		//Feature 1: the count of pattern matches
		featureValues.put("getPatternMatches", getPatternMatches(medication, reason));

		// Feature 7: reason frequency inside the corpus
		featureValues.put("getNormalizedReasonFrequency", 
				getNormalizedReasonFrequency(reason));

		// Feature 8: the umls semantic type of the reason concept
		featureValues.put("checkReasonSemanticType", 
				checkReasonSemanticType(reason.content));

		// Feature 9: the umls semantic type of the medication concept
		featureValues.put("checkMedSemanticType", checkMedSemanticType(medication.content));

		// Feature 10: whether the med reason are contained within searches
		featureValues.put("checkSearches", checkSearches(medication.content, 
				reason.content));

		// feature 34: whether there is a punctuation between the med and the reason
		featureValues.put("checkPunctuationContext", 
				checkPunctuationContext(medication, reason, 
						medicalRecord));

		// feature 35: check the number of lines distance between the medication and the reason
		featureValues.put("checkLinesDistance", checkLinesDistance(medication, reason));       

		// feature 36: check whether the medication is an abbreviation
		featureValues.put("checkIsAbbreviation", checkIsAbbreviation(medication));

		// feature 37: check whether the reason is an abbreviation
		featureValues.put("checkIsAbbreviation", checkIsAbbreviation(reason));

		// feature 38: check if the context contains mg
		featureValues.put("checkMG", checkMG(medication, reason, 
				medicalRecord));

		// feature 39:
		featureValues.put("computeCountAbbvs", computeCountAbbvs(medication, 
				reason, medicalRecord));

		// contains x
		featureValues.put("checkContainsX", checkContainsX(medication));
		featureValues.put("checkContainsY", checkContainsY(medication));




		// Feature 11: the med-reason freq inside the dev corpus
		//        double freqReasonMed = getReqReasonMed( reason.reason.content,  medication.medication.content);
		//        featureValues.add(freqReasonMed);

		//        int countTos = computeTos(medication.medication, reason.reason, file)?1:0;
		//        featureValues.add(countTos);


		// Feature 4: the pcm query count
		//              featureCount ++;
		//              int pcmCount = pcmQueryCount(medication, reason);
		//
		//              vectorLine = writeFeature(vectorLine, featureCount, 
		//                              String.valueOf(pcmCount*1.0/normPcmQuery), vector);

		return featureValues;
	}

	boolean checkAnnotationsSameSection(AnnotationDetail annt1,
			AnnotationDetail annt2,  
			MedicalRecord medicalRecord) {
		return Sectionizer.sameSection( 
				annt1, annt2, medicalRecord);

	}

	boolean checkContainsY(AnnotationDetail annotation) {
		return annotation.content.contains("x");

	}

	boolean checkContainsX(AnnotationDetail annotation) {
		return annotation.content.contains("y");   		
	}

	boolean checkIsAbbreviation(AnnotationDetail annotation) {
		return this.classifier.abbreviations.contains(annotation.content);		
	}

	double getNormalizedReasonFrequency(AnnotationDetail reason) {
		int reasonFrequency = computeReasonFrequency(reason.content);
		return reasonFrequency*1.0/this.normReasonFrequency;

	}

	double getPatternMatches(AnnotationDetail medication, AnnotationDetail reason) {
		int norm = 10;
		String medContent = Utils.removePunctuation(medication.content);
		String reasonContent = Utils.removePunctuation(reason.content);
		int count = 0;

		if(classifier.patternCounts.containsKey(
				UtilMethods.mergeStrings(medContent, reasonContent)))
			count = classifier.patternCounts.get(
					UtilMethods.mergeStrings(medContent, reasonContent));

		return count*1.0/norm;

	}

	/**
	 * 
	 * @param wordAnnotation
	 * @param file
	 * @param sections
	 * @param sentence
	 * 
	 * @return the generic features
	 */
	public ArrayList<Object> getGenericFeatures(
			AnnotationDetail wordAnnotation, String file,
			ArrayList<Section> sections, 
			ArrayList<SentenceContent> sentences, int sentenceIndex,
			SentenceContent sentence, int wordIndex) {
		ArrayList<Object> featureValues = new ArrayList<Object>();

		//Feature 0: Section id
		//        String section = computeSection(sections, 
		//                wordAnnotation, classifier.medicalRecords.get(file.split("\\.")[0]));
		//        if(section != null && ! section.isEmpty())
		//            featureValues.add(section);

		String concept = wordAnnotation.content;

		// Feature 28: whether the reason is plural
		//        boolean plural = checkPlural(wordAnnotation, sentence);
		//        if(plural)
		//            featureValues.add("plural");

		// contains x
		if(wordAnnotation.content.contains("x"))
			featureValues.add("containsX");

		// contains y
		if(wordAnnotation.content.contains("y"))
			featureValues.add("containsY");

		// add the last 2, 3, 4 characters
		featureValues.addAll(addLast(wordAnnotation.content));

		// feature 37: check whether the concept is an abbreviation
		if(this.classifier.abbreviations.contains(concept))
			featureValues.add("abbreviation");

		// features: contains causal conjunction
		boolean preposition = DiscourseProcessing.containsCausalConjuction(concept);
		if(preposition)
			featureValues.add("causal");

		// contains any conjunction
		boolean conjunctionAny = discourse.containsAnyConjunction(concept);
		if(conjunctionAny)
			featureValues.add("conjunction");

		// concept length
		int length = concept.length();
		if(length > 12) length = 12;
		featureValues.add("length=" + String.valueOf(length*0.1/12));

		// feature: collins word shape
		String wordShape = computeWordShape(concept);
		featureValues.add(wordShape);

		// n-gram occurrence in training
		//        featureValues.addAll(checkNgramFrequencyInTraining(sentences, sentenceIndex, sentence, wordIndex));

		// indicate the token index on line
		featureValues.add("tokenInd=" + wordAnnotation.startOffset);

		// indicate the number of tokens away from most recent medication name
		//        String tokensAway = getTokensAwayPrevMed(sentence, wordIndex);
		//        if(tokensAway != null)
		//            featureValues.add(tokensAway);
		//
		//        tokensAway = getTokensAwayAfterMed(sentence, wordIndex);
		//        if(tokensAway != null)
		//            featureValues.add(tokensAway);


		// n-gram occurrence in dailyMed medication list
		//        featureValues.addAll(nGramInDailyMed(sentence, wordIndex));
		//
		//        // n-gram occurrence in dailyMed medication list
		//        featureValues.addAll(nGramStartDailyMed(sentence, wordIndex));

		//        int reasonFrequency = computeReasonFrequency(wordAnnotation.content);
		//        featureValues.add("freq=" + String.valueOf(
		//                reasonFrequency*1.0/this.normReasonFrequency));

		//        // reason frequency inside the corpus
		//        int reasonFrequency = computeReasonFrequency(wordAnnotation.content);
		//        featureValues.add("freq=" + String.valueOf(
		//                reasonFrequency*1.0/this.normReasonFrequency));

		return featureValues;
	}

	/**
	 * check if reasons from the training data start with the given n-gram
	 * @param ngram
	 * @return
	 */
	boolean conceptStartsWith(String ngram, HashSet<String> conceptList) {
		for(String val : conceptList )
			if(val.startsWith(ngram))
				return true;

		return false;
	}

	private ArrayList<String> addLast(String content) {
		ArrayList<String> attributes = new ArrayList<String>();

		// include the last 2 chars of the current token
		if(content.length() > 2)
			attributes .add("last2=" + content.substring(content.length()-2, content.length()));

		// include the last 3 chars of the current token
		if(content.length() > 3)
			attributes.add("last3=" + content.substring(content.length()-3, content.length()));

		// include the last 4 chars of the current token
		if(content.length() > 4)
			attributes.add("last4=" + content.substring(content.length()-4, content.length()));

		return attributes;
	}


	/**
	 * Word transformation in which each character c of a string s is substituted
	 * with X if c is uppercase else with x if is lowercase; if c is a digit it 
	 * is substituted with d and left as it is otherwise. 
	 * In addition each sequence of two or more identical characters c is 
	 * substituted with c*
	 * 
	 * @param concept
	 * @return
	 */
	String computeWordShape(String concept) {
		String shape = "";

		for(int index = 0; index < concept.length(); index ++){
			char chr = concept.charAt(index);

			if(Character.isLetter(chr) && Character.isUpperCase(chr))
				shape = shape + "X";
			else if(Character.isDigit(chr))
				shape = shape + '0';
			else if(Character.isLetter(chr) && Character.isLowerCase(chr))
				shape = shape + "x";
			else if (chr == ' ')
				shape = shape + "_";
			else if (chr == ':')
				shape = shape + "_";
			else shape = shape + chr;

		}

		shape = shape.replaceAll(" ", "_");
		shape = shape.replaceAll("\t", "_");

		return shape;
	}


	/**
	 * Compute norm for the attributes
	 */
	void computeNorm(){
		HashSet<String> uniqueWords = new HashSet<String>();

		for(String pattern : classifier.patterns){
			String[] splitPattern  = pattern.split(" ");

			for(int index = 0; index < splitPattern.length; index ++)
				uniqueWords.add(splitPattern[index]);
		}

		this.normPatternOverlap = uniqueWords.size();

		for(ArrayList<String> query : pcmQuery.previousQueries.keySet()){
			int count = pcmQuery.previousQueries.get(query);
			if(count > this.normPcmQuery)
				this.normPcmQuery = count;
		}
	}

	/**
	 * 
	 */
	void computeFrequencyReasons(){
		reasonFrequencies = new HashMap<String, Integer>();

		for(String reasonFile : classifier.trainData.medicalRecords.keySet()){
			ArrayList<Annotation> annts = classifier.trainData.medicalRecords.get(reasonFile).concepts;

			for(Annotation annt : annts )
				if(annt.annotationElements.containsKey(AnnotationType.R))
					reasonFrequencies.put(
							annt.annotationElements.get(AnnotationType.R).content, 
							0); 
		}

		HashMap<String, Integer> corpusWordFreq = classifier.importer.importWordFrequency(
				classifier.configs.wordFrequenciesEnglishCorpusPath);

		for(String reason: reasonFrequencies.keySet()){
			int sum = 0;

			if(corpusWordFreq.containsKey(reason))
				sum = corpusWordFreq.get(reason);

			reasonFrequencies.put(reason, sum);

			if(sum > this.normReasonFrequency)
				this.normReasonFrequency = sum; 
		}

	}

	/**
	 * Return the number of pcm queries that match the medication and the reason
	 * @param medication
	 * @param reason
	 * @return
	 */
	int pcmQueryCount(AnnotationDetail medication, AnnotationDetail reason){

		int result = pcmQuery.queryCount(reason.content, 
				medication.content);

		return result;

	}


	/**
	 * Compute the frequency of the reason
	 * @param concept
	 * @return
	 */
	private int computeReasonFrequency(String concept) {

		if(this.reasonFrequencies.containsKey(concept))
			return this.reasonFrequencies.get(concept);

		return 0;
	}


	/**
	 * Check whether there is a negation inside the context
	 * @param medication
	 * @param reason
	 * @param file
	 * @return
	 */
	boolean contextNegation(AnnotationDetail medication, AnnotationDetail reason,
			String file, 
			HashMap<String, MedicalRecord> medicalRecords) {
		MedicalRecord record = medicalRecords.get(file);

		String context = FindContextFromEMR.getContent(record.rawFileLines, 
				medication, reason, false);   
		if(context == null) return false;

		try{
			String result = negationIdentifier.test(context);
			if(result != null && ! result.equals("-1"))
				return true;
		}catch(Exception e){
			e.printStackTrace();
		}

		return false;
	}


	/**
	 * Check the semantic type of the reason
	 * @param reason
	 * @return the reason semantic type
	 */

	String checkReasonSemanticType(String reason){
		if(classifier.conceptSemanticTypes.containsKey(reason)){
			ArrayList<String> types = classifier.conceptSemanticTypes.get(reason);

			return types.get(0);
		}

		return null;

	}
	/**
	 * Get the UMLS type of the concept
	 * @param medication
	 * @return the umls code
	 */
	double checkMedSemanticType(String medication){
		int type = -1;

		if(classifier.conceptSemanticTypes.containsKey(medication)){
			ArrayList<String> types = classifier.conceptSemanticTypes.get(medication);

			return types.get(0).hashCode()% 40 ;

		}

		return type;

	}

	/**
	 * check whether the med-reason combination was captured in web searches
	 * @param med
	 * @param reason
	 * @return 
	 */
	boolean checkSearches(String med, String reason){
		String merged = Utils.removePunctuation(
				UtilMethods.mergeStrings(med, reason));

		if(classifier.patternCounts.containsKey(merged))
			if(classifier.patternCounts.get(merged) > 0)
				return true;

		return false;
	}

	double getReqReasonMed(String reason, String med){
		String merged = UtilMethods.mergeStrings(med, reason, "_");

		if(classifier.frequencies.containsKey(merged)){
			int value = classifier.frequencies.get(merged);

			return value/normFrequenciesMedReason;
		}

		return -1;

	}


	/**
	 * 
	 * @param medication
	 * @param reason
	 * @param file
	 * @return
	 */
	boolean checkPunctuationContext(AnnotationDetail medication, AnnotationDetail reason, 
			MedicalRecord medicalRecord){
		String context = FindContextFromEMR.getContent
				(medicalRecord.rawFileLines, 
						medication, reason, false);

		if(context == null) return false;

		return UtilMethods.containsPunctuation(context);

	}

	/**
	 * 
	 * @param medication
	 * @param reason
	 * @return
	 */
	double checkLinesDistance(AnnotationDetail medication, AnnotationDetail reason){
		double distance = 0.0;

		distance = Math.abs(medication.startLine - reason.startLine)/3.0;

		return distance;
	}

	/**
	 * 
	 * @param medication
	 * @param reason
	 * @param file
	 * @return
	 */
	boolean checkMG(AnnotationDetail medication, AnnotationDetail reason, 
			MedicalRecord medicalRecord ){
		String context = FindContextFromEMR.getContent(
				medicalRecord.rawFileLines, 
				medication, reason, false);

		if(context == null) return false;

		String[] splitContext = context.split(" ");

		for(String word : splitContext)
			if(UtilMethods.removePunctuation(word).equals("mg"))
				return true;


		return false;

	}

	/**
	 * Check if the context contains digits or punctuation marks
	 * @param context
	 * @return
	 */
	boolean containsDigitPunct(String context){
		context = context.replaceAll(" ", "");

		if(UtilMethods.removePunctuation(context).length() != context.length())
			return true;

		if(UtilMethods.removeDigits(context).length() != context.length())
			return true;

		return false;
	}

	/**
	 * Check the count of abbreviations inside the context
	 * we consider abbreviation something that is less than 2 in length
	 * or has a punctuation sign inside/ends with punctuation
	 * @param medication
	 * @param reason
	 * @param file
	 * @return
	 */
	int computeCountAbbvs(AnnotationDetail medication, AnnotationDetail reason, 
			MedicalRecord medicalRecord ){
		String context = FindContextFromEMR.getContent(
				medicalRecord.rawFileLines, 
				medication, reason, false);

		if(context == null) return 0;

		String[] splitContext = context.split(" ");
		int count = 0;

		for(String word : splitContext){
			word  = word.trim();
			if(word.isEmpty()) continue;

			if(word.length() <= 2 && !UtilMethods.removePunctuation(word).isEmpty()){
				count ++;
				continue;
			}

			boolean foundPunctuation = false;

			for(int index = 0; index < word.length(); index ++){
				if(word.charAt(index) == '.' && index != word.length()-1) foundPunctuation = true;
			}

			if(foundPunctuation)
				count ++;
		}

		return count/splitContext.length;

	}

	/**
	 * 
	 * @param medication
	 * @param reason
	 * @param file
	 * @return
	 */
	boolean computeTos(AnnotationDetail medication, AnnotationDetail reason, 
			HashMap<String, MedicalRecord> medicalRecords,
			String file){
		String context = FindContextFromEMR.getContent(
				medicalRecords.get(file).rawFileLines, 
				medication, reason, false);

		if(context == null) return false;

		String[] splitContext = context.split(" ");

		for(String word : splitContext)
			if(UtilMethods.removePunctuation(word).equals("to")){
				FindContextFromEMR.getContent(
						medicalRecords.get(file).rawFileLines, 
						medication, reason, false);
				return true;
			}


		return false;

	}





}
