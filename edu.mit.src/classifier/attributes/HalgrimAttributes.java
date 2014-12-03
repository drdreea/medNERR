package classifier.attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.MedicalRecord;
import corpus.Section;
import corpus.Annotation.AnnotationType;

import preprocess.parser.Parsing;
import preprocess.parser.SentenceContent;
import preprocess.parser.WordTagMap;
import utils.UtilMethods;
import classifier.ClassifierBase;
import classifier.vector.VectorCreator.TagType;

public class HalgrimAttributes extends AttributeGenerator{
	public final List<String> timeWords = 
			Arrays.asList("hour", "week", "day", "year");
	List<String> preps = Arrays.asList("for","while","until","throughout");
	Pattern isnumber = Pattern.compile("^(?:one|two|three|four|five|six|seven|eight|nine)$");

	public TagType prev1 = TagType.O;
	public TagType prev2 = TagType.O;
	public TagType prev3 = TagType.O;


	public HalgrimAttributes(ClassifierBase classifier){
		super(classifier);
	}

	public ArrayList<Object> getConceptAttributesHalgrim(
			AnnotationDetail wordAnnotation, String file,
			TagType tagType, SentenceContent sentence, int wordIndex,
			ArrayList<SentenceContent> sentences, int sentenceIndex, 
			ArrayList<Section> sections, 
			HashMap<String, MedicalRecord> medicalRecords, 
			AnnotationFile medications) {

		ArrayList<Object> attributes = new ArrayList<Object>();

		// current token normalized
//		attributes.add("w0=" + getLemmatizedWord(sentences, sentenceIndex, sentence, wordIndex));

		// check whether wx contains numbers, starts with numbers, or is a number
//		attributes.addAll(checkNumbers(sentences, sentenceIndex, sentence, wordIndex));

		// add the token length
//		int len = Math.min(12, wordAnnotation.content.length());
//		attributes.add("wordLen=" + String.valueOf(len));

		// w starts with q
//		attributes.addAll(startsWithQ(sentences, sentenceIndex, sentence, wordIndex));

		// check if the current token is the spelled version of a number
//		attributes.addAll(spelledNumber(sentence, wordIndex, sentences, sentenceIndex));

		// indicate token index on line
//		attributes.add("tokenInd=" + String.valueOf(wordAnnotation.startOffset));

		// get prev token normalized
//		String w_1 = getLemmatizedWord(sentences, sentenceIndex, sentence, wordIndex -1);
//		if(w_1 != null)
//			attributes.add("wm1=" + w_1);
//		else
//			attributes.add("wm1=**bos**");

		// composition of current and nearby token
//		attributes.addAll(getNgrams(sentences, sentenceIndex, sentence, wordIndex));

		// get next word
//		w_1 = getLemmatizedWord(sentences, sentenceIndex, sentence, wordIndex +1);
//		if(w_1 != null)
//			attributes.add("w1=" + w_1);
//		else{
//			attributes.add("w1=**eos**");		
//		}

		// check if the token equals various prepositions
		// token varies from w_x to w_x+2
//		attributes.addAll(checkPrepositions(sentences, sentenceIndex, 
//				sentence, wordIndex));

		// check if w_i-1 -- w_i+2 contains "hour", "week", "day", or "year"
//		attributes.addAll(checkTimeWord(sentences, sentenceIndex,
//				sentence, wordIndex));

		// indicate distance to most recently seen "sliding scale line"
//		String slidingScale = checkDistanceSlidingScale(wordIndex, 
//				sentences, sentenceIndex);
//		if(slidingScale != null)
//			attributes.add(slidingScale);

//		// indicate if token matches the dosage pattern of ##mg
//		if(matchesDosagePattern(wordAnnotation.content))
//			attributes.add("dosePattern");

		// indicate if token matches the duration pattern x#
//		if(matchesDurationPattern(wordAnnotation.content))
//			attributes.add("durPattern");

		// indicate how many times nearby n-grams appear as each field type in
		// training data over a minimum with ceiling applied
//		attributes.addAll(checkNgramFrequencyInTraining(sentences, sentenceIndex,
//				sentence, wordIndex));

		// add in the section name
		//		String section = computeSection(sections, 
		//				wordAnnotation, medicalRecords.get(file.split("\\.")[0]));
		//		if(section != null && ! section.isEmpty() && !section.equals("other"))
		//			attributes.add(section);


		// add the class label for the three previous tokens
		//		attributes.addAll(getClassLabels(tagType));

		// indicate if current token is 0-8 tokens away from the most recent medication name
//		String distance = checkTokenDistancePrevMed(sentence, wordIndex, 
//				sentences, sentenceIndex);
//		if(distance != null)
//			attributes.add(distance);
//
//		// indicate the type of most recent medication
//		attributes.add(checkTypeMostRecentMed(sentence, wordIndex, 
//				sentences, sentenceIndex));
//
//		// indicate whether there are medications on line x
//		attributes.addAll(checkMedicationOnFiveLineWindow(sentence, wordIndex, 
//				sentences, sentenceIndex));

		// indicate if current token is 1-8 tokens away from the next medication name
//		distance = checkTokenDistanceNextMed(sentence, wordIndex, 
//				sentences, sentenceIndex);
//		if(distance != null )
//			attributes.add(distance);

		// play with a couple more attributes
		// add the isVerb
//		attributes.addAll(isVerb(sentences, sentenceIndex, sentence, wordIndex));

		// add the isadjective
		// check if it's an adjective
//		boolean isAdjective = checkAdj(sentence, wordIndex);
//		if(isAdjective)
//			attributes.add("adjective");

		// add the isPunctuation
//		boolean isPunctuation = UtilMethods.isPunctuation(wordAnnotation.content);
//		if(isPunctuation)
//			attributes.add("isPunctuation");

//		boolean isDrug = checkIsDrug(sentence, wordIndex, sentences, sentenceIndex);
//		if(isDrug)
//			attributes.add("isDrug");

		return attributes;

	}

	boolean checkIsDrug(WordTagMap currentWord) {
		if(currentWord.annotationType == AnnotationType.M )
			return true;

		return false;
	}

	public boolean checkAdj(WordTagMap currentWord) {
		String tag = currentWord.posTag;
		return Parsing.adj.contains(tag);

	}
	/**
	 * check the number of lines to the closest slinding scale
	 * 
	 * @param sentence
	 * @param wordIndex
	 * @param sentences
	 * @param sentenceIndex
	 * 
	 * @return
	 */
	String checkDistanceSlidingScale(int wordIndex, 
			ArrayList<SentenceContent> sentences, int sentenceIndex) {
		int distance = -1;

		for(int index = -8; index <= sentenceIndex +8; index ++){
			if(index == 0) continue;
			if(index < 0 || index >= sentences.size()) continue;

			SentenceContent sentence = sentences.get(index);
			if(sentence.sentence.contains("slinding scale"))
				if(distance > Math.abs(index))
					distance = index;
		}

		if(distance > 0)
			return "slideDist=" + String.valueOf(distance);

		return null;
	}

	public boolean matchesDosagePattern(String content){

		if(content.endsWith("mg") ){
			content = content.substring(0, content.length()-2);

			if( !content.isEmpty() && UtilMethods.isNumber(content))
				return true;
		}

		return false;
	}

	public boolean matchesDurationPattern(String content){
		if(content.equals("x"))
			return true;

		if(content.startsWith("x")){
			content = content.substring(1);

			if(UtilMethods.isNumber(content))
				return true;
		}

		return false;
	}

	public String getSpelledNumber(WordTagMap wordMap){
		String number = "notNumber";

		if(wordMap != null){
			String word = wordMap.word;
			if(isnumber.matcher(word).matches())
				return "isNumber";
		}

		return number;
	}

	public String checkPreposition(WordTagMap wordMap){
		String prep = "noPrep";

		if(wordMap != null){
			String word = wordMap.word;

			if(preps.contains(word))
				prep = "prep";
		}

		return prep;
	}

	public String checkTimeWord(WordTagMap wordMap){
		String time = "noTimeWord";

		if(wordMap != null){
			for(String el : timeWords){
				if(wordMap.word.contains(el))
					return "timeWord";
			}
		}

		return time;
	}

	public String startsWithQ(WordTagMap wordMap){
		String starts = "noQ";

		if(wordMap != null){
			String word = wordMap.word;

			if(word.toLowerCase().startsWith("q")){
				return "startsWithq";
			}
		}

		return starts;
	}


	ArrayList<String> getClassLabels(TagType label){
		ArrayList<String> labels = new ArrayList<String>();

		labels.add("tag-1=" + prev1.toString());
		labels.add("tag-2=" + prev2.toString());
		labels.add("tag-3=" + prev3.toString());

		prev3 = prev2;
		prev2 = prev1;
		prev1 = label;

		return labels;

	}
}
