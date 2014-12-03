/**
 * Created on Jan 16, 2012
 * 
 * @author ab Contact andreeab dot mit dot edu
 * 
 */
package utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import corpus.AnnotationDetail;
import corpus.Annotation.AnnotationType;

import preprocess.parser.WordTagMap;
import preprocess.parser.ParsingDocument.Type;

/**
 * @author ab
 * 
 */
public class UtilMethods {
	public static String regexDelimiter = "\\.|\\;|\\:|\\,";
	public static final List<String> excludeHeadings = Arrays.asList(
			"admission_date", "report_status", "discharge_date",
			"identification",
			"entered",
			"entered", "dictated_by",
			"attending",
			"entered_by",
			"cc",
			"to-do_list",
			"for_pcp",
			"dictated_by",
			"attending",
			"entered_by",
			// history
			"social",
			"sh",
			"social_history",
			"family_history",
			"past_surgical_history",
			"past_medical_history",
			"history_of_present_illness",
			"history_of_the_present_illness",
			"brief_history_of_physical_illness",
			// labs data
			"laboratory_data", "admission_labs",
			"laboratory_data_on_admission", 
			"data",
			"laboratory_data_on_discharge",
			// "studies",
			"studies_at_the_time_of_admission", "labs",
			// allergies
			"allergies", "allergy",
			// diet
			"diet", "physical_examination", 
			"physical_examination_on_arrival"
			,"for_pcp",
			"follow_up", "follow-up",
			"medical_service", "potentially_serious_interaction",
			"possible_allergy"
			// diagnosis
			, "diagnoses", "admit_diagnosis", "principal_discharge_diagnosis",
			"principal_diagnosis", "principal_diagnoses",
			"secondary_diagnosis", "secondary_diagnoses",
			"discharge_diagnosis", "chief_complaint", "other_diagnosis",
			"other_diagnoses"
			// hosital course by system
			//				,"hospital_course_by_system"
			//				,"course"
			,"plan"
			// exams
			,"review_of_systems",
			"physical_examination_on_admission",
			"physical_examination",
			"PE_on_admit",
			"PE_on_discharge",
			"assesment_and_plan",
			"plan",
			"physical_examination_on_arrival",
			"consults",
			"admission_exam",
			"operations_and_procedures",
			"hpi"
			);
	public static String[] dirListing(String path) {
		File dir = new File(path);

		return dir.list();
	}

	/**
	 * @param input
	 * @return
	 */
	public static boolean containsAllergyInfo(String input) {
		if (input == null || input.isEmpty())
			return false;

		if (input.toLowerCase().contains("allerg"))
			return true;

		return false;
	}

	/**
	 * Check if a string contains a quantity or a mod of administration
	 * 
	 * @param mergedMed
	 * @return
	 */
	public static Boolean checkQuantityModal(String mergedMed) {
		mergedMed = mergedMed.toLowerCase();
		String[] splitMed = mergedMed.split(" ");

		for (String quantityEnding : SystemConstants.quantities) {
			for (int index = 0; index < splitMed.length; index++)
				if (splitMed[index].equals(quantityEnding))
					return true;
		}

		for (String modalEnding : SystemConstants.modal) {
			for (int index = 0; index < splitMed.length; index++) {
				if (splitMed[index].equals(modalEnding))
					return true;
			}
		}

		return false;
	}

	/**
	 * Check if a string contains a number
	 * 
	 * @param mergedMed
	 * @return
	 */
	public static Boolean endsWithNumber(String mergedMed) {
		mergedMed = mergedMed.trim();

		String[] splitMed = mergedMed.split(" ");

		if (splitMed[splitMed.length - 1]
				.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+"))
			return true;

		return false;

	}

	public static Boolean isNumber(String med) {
		if (med == null)
			return null;

		med = med.trim();

		if (med.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+")) {
			return true;
		}
		else {
			return false;
		}
	}

	public static Boolean containsPunctuation(String med) {

		if (med.replaceAll("[\\p{Punct}]", "").equals(med))
			return false;

		return true;

	}

	public static Boolean isPunctuation(char med) {
		return isPunctuation(String.valueOf(med));
	}

	public static Boolean isPunctuation(String med) {

		if (med.replaceAll("[\\p{Punct}]", "").isEmpty())
			return true;

		return false;

	}
	/**
	 * Remove the quantity ending from a string return the string without the
	 * quantity ending
	 * 
	 * @param mergedMed
	 * @return
	 */
	public static String removeQuantity(String mergedMed) {
		String toRemove = null;
		boolean found = true;

		while (found) {
			toRemove = null;
			String[] splitWord = mergedMed.split(" ");

			for (String value : SystemConstants.quantities)
				for (int index = 0; index < splitWord.length; index++)
					if (splitWord[index].equals(value)) {
						toRemove = value;
						break;
					}

			for (int index = 0; index < splitWord.length; index++) {
				if (splitWord[index].trim().equals(toRemove)) {
					if (index > 1 && isNumber(splitWord[index - 1])) {
						toRemove = UtilMethods.mergeStrings(
								splitWord[index - 1], toRemove);
					}

					break;

				}
			}

			if (toRemove == null)
				found = false;

			if (found && mergedMed.contains(toRemove)) {
				if (toRemove.length() > 1)
					mergedMed = mergedMed.replace(toRemove, "");
				else {
					String[] splitMed = mergedMed.split(" ");
					String updatedMed = "";

					for (int index = 0; index < splitMed.length; index++) {
						if (!splitMed[index].equals(toRemove))
							updatedMed = UtilMethods.mergeStrings(updatedMed,
									splitMed[index]);
					}

					mergedMed = updatedMed;
				}
			}
		}

		return mergedMed.trim();

	}

	public static String removeModal(String mergedMed) {
		boolean found = true;

		while (found) {
			found = false;

			String[] splitWord = mergedMed.split(" ");
			String updatedMed = "";
			for (int index = 0; index < splitWord.length; index++) {
				if (!SystemConstants.modal.contains(splitWord[index].trim())) {
					updatedMed = mergeStrings(updatedMed, splitWord[index]);
				}
				else {
					found = true;
				}
			}

			mergedMed = updatedMed;
		}

		return mergedMed.trim();

	}

	/**
	 * We want to identify potential sentence boundaries and add punctuation
	 * signs a sentence is considered to end where a new capitalized word is
	 * encoutered
	 * 
	 * @param line
	 * @param opType
	 * @return
	 */
	public static String identifySentence(String line, String futureLine,
			String med, int lineIndex, int totalLines, Type opType) {
		String tmpLine = "";
		String delimiter = " . ";

		// first check if the medication is all upper case
		if (line.contains(med.toUpperCase())) {
			int offset = line.indexOf(med.toUpperCase());
			String subst = line.substring(offset + med.length());
			String[] splitSubst = subst.split(" ");

			for (int index = 0; index < splitSubst.length; index++) {

				if (splitSubst[index].toUpperCase().equals(splitSubst[index]))
					med = UtilMethods.mergeStrings(med, splitSubst[index]);
				if (UtilMethods.containsPunctuation(splitSubst[index]))
					break;
			}

			return UtilMethods.removePunctuation(med).toLowerCase();
		}

		line = line.toLowerCase();
		String[] splitLine = line.split(" ");

		for (int index = 0; index < splitLine.length; index++) {
			String word = splitLine[index];
			if (word.length() == 0)
				continue;

			char chr = word.charAt(0);

			if (Character.isUpperCase(chr) && !word.toUpperCase().equals(word)
					&& !med.toLowerCase().startsWith(word.toLowerCase())) {
				tmpLine = tmpLine + delimiter + word;
			}
			else {
				tmpLine = tmpLine + " " + word;
			}
		}

		// we will only parse the sentence that contains the medication of
		// interest
		splitLine = tmpLine.split(regexDelimiter);
		for (int index = 0; index < splitLine.length; index++) {
			String word = splitLine[index];
			if (word.toLowerCase().contains(
					UtilMethods.removePunctuation(med.toLowerCase()))) {
				tmpLine = word;
				break;
			}
		}

		// special delimiter for the lists
		splitLine = tmpLine.split("\\.\\s");
		for (int index = 0; index < splitLine.length; index++) {
			String word = splitLine[index];
			if (word.toLowerCase().contains(
					UtilMethods.removePunctuation(med.toLowerCase()))) {
				tmpLine = word;
				break;
			}
		}

		splitLine = tmpLine.split(" ");
		String updatedLine = "";

		for (int index = 0; index < splitLine.length; index++)
			updatedLine = UtilMethods.mergeStrings(updatedLine,
					splitLine[index]);

		while (updatedLine.startsWith(" "))
			updatedLine = updatedLine.trim();

		// check if the current updatedLine is the end of line
		if (line.toLowerCase().endsWith(updatedLine.toLowerCase())) {
			// we check to see if there is a sentence end
			// on the next line
			if (totalLines > lineIndex) {
				// if the future line starts with upper case, we no longer
				// proceed
				if (!Character.isUpperCase(futureLine.charAt(0))) {
					String[] futureLineSplit = futureLine.split(regexDelimiter);
					if (futureLineSplit.length > 1) {
						updatedLine = UtilMethods.mergeStrings(updatedLine,
								futureLineSplit[0].toLowerCase());
					}
				}
			}
		}

		// check if the updated line ends with a preposition or a number
		splitLine = updatedLine.split(" ");
		if (splitLine.length > 1) {
			String conjuction = splitLine[splitLine.length - 1].toLowerCase()
					.trim();
			if (SystemConstants.conjuctions.contains(conjuction)
					|| UtilMethods.isNumber(conjuction)) {
				updatedLine = updatedLine.replace(" " + conjuction, "").trim();
			}
		}

		// if opType = medication, we remove all quantities from the line
		switch (opType) {
		case MEDICATION:
			updatedLine = UtilMethods.removeQuantity(updatedLine);
			updatedLine = UtilMethods.removeModal(updatedLine);
			// updatedLine = UtilMethods.removeConjuctions(updatedLine, med);
			break;
		default:
			break;
		}

		return updatedLine.replaceAll("[#|*]", "");
	}

	/**
	 * Identify the line on which the medication is located. No distinction
	 * made for reasons vs medications
	 * @param line
	 * @param futureLine
	 * @param med
	 * @param lineIndex
	 * @param totalLines
	 * @return
	 */
	public static String identifySentenceSimple(String line, String futureLine,
			String med, int lineIndex, int totalLines) {
		String tmpLine = "";
		String delimiter = ". ";

		line = line.toLowerCase();
		String[] splitLine = line.split(" ");

		for (int index = 0; index < splitLine.length; index++) {
			String word = splitLine[index];
			if (word.length() == 0)
				continue;

			char chr = word.charAt(0);

			if (Character.isUpperCase(chr) && !word.toUpperCase().equals(word)
					&& !med.toLowerCase().startsWith(word.toLowerCase())) {
				tmpLine = tmpLine + delimiter + word;
			}
			else {
				tmpLine = tmpLine + " " + word;
			}
		}

		// we will only parse the sentence that contains the medication of
		// interest
		splitLine = tmpLine.split(regexDelimiter);
		for (int index = 0; index < splitLine.length; index++) {
			String word = splitLine[index];
			if (word.toLowerCase().contains(
					UtilMethods.removeSentencePunctuation(med.toLowerCase()))) {
				tmpLine = word;
				break;
			}
		}

		//		splitLine = tmpLine.split("\\.");
		//	        String merged = "";
		//		for(int index = 0; index < splitLine.length; index ++){
		//		    String word = splitLine[index];
		//		    merged = UtilMethods.mergeStrings(merged, word) + ".";
		//		    merged = merged.replaceAll(" +", " ").trim();
		//		    if(merged.contains(med.toLowerCase()))
		//		        break;
		//		}

		//		if(!merged.isEmpty()) {
		//		    if(!merged.endsWith(med)) 
		//		        merged = merged.substring(0, merged.length()-1);
		//		    
		//		    tmpLine = merged;
		//		}

		// special delimiter for the lists
		splitLine = tmpLine.split("\\.\\s");
		for (int index = 0; index < splitLine.length; index++) {
			String word = splitLine[index];
			if (word.toLowerCase().contains(
					UtilMethods.removeSentencePunctuation(med.toLowerCase()))) {
				tmpLine = word;
				break;
			}
		}

		splitLine = tmpLine.split(" ");
		String updatedLine = "";

		for (int index = 0; index < splitLine.length; index++)
			updatedLine = UtilMethods.mergeStrings(updatedLine,
					splitLine[index]);

		while (updatedLine.startsWith(" "))
			updatedLine = updatedLine.trim();

		updatedLine = updatedLine.replaceAll("[#|*]", "");

		return updatedLine.replaceAll(" +", " ");
	}


	/**
	 * Check whether the medication has a set of brackets following it inside
	 * the context
	 * 
	 * @param textLine
	 * @param medication
	 * @return
	 */
	public static StringNumberPair checkBrackets(String textLine,
			String medication) {
		textLine = textLine.toLowerCase();

		// if the textLine does not contain the medication, we return null
		if (textLine.indexOf(medication) == -1)
			return null;

		textLine = textLine.substring(textLine.indexOf(medication)).trim();

		// check if there was a set of brackets inside the given input
		if (textLine.indexOf("(") == (textLine.indexOf(medication)
				+ medication.length() + 1)) {
			int startBracket = textLine.indexOf("(");
			if (textLine.substring(textLine.indexOf("(")).indexOf(")") != -1) {

				int endBracket = textLine.substring(textLine.indexOf("("))
						.indexOf(")")
						+ textLine.substring(0, textLine.indexOf("(")).length();

				String textLineSubString = textLine.substring(textLine
						.indexOf(medication));

				int bracketCount = 0;
				endBracket = 0;

				for (int index = 0; index < textLineSubString.length(); index++) {
					if (textLineSubString.charAt(index) == '('
							&& bracketCount != -1)
						bracketCount++;

					if (textLineSubString.charAt(index) == ')'
							&& bracketCount != -1) {
						bracketCount--;
						endBracket = index;

						if (bracketCount == 0)
							bracketCount = -1;
					}
				}

				endBracket += textLine.length() - textLineSubString.length();

				medication = medication
						+ textLine.substring(startBracket - 1, endBracket + 1)
						.toLowerCase();

				String[] addedMed = textLine.substring(startBracket,
						endBracket + 1).split(" ");

				return new StringNumberPair(medication, addedMed.length);

			}
		}

		return null;
	}

	public static ArrayList<String> readFileLines(String filePath) {
		ArrayList<String> readLines = new ArrayList<String>();

		// read the lines of the given file
		try {
			FileInputStream rawFile = new FileInputStream(filePath);

			DataInputStream in = new DataInputStream(rawFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			while ((strLine = br.readLine()) != null) {
				readLines.add(strLine);
			}

			// Close the input stream
			in.close();
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}

		return readLines;
	}

	public static String joinPaths(String path1, String path2) {
		return (path1 + "/" + path2);
	}

	public static boolean checkStringOverlap(String first, String second) {

		String[] wordsSecond = second.toLowerCase().trim().split(" ");

		if (wordsSecond.length >= 1) {
			String toSearch = wordsSecond[0].trim();
			int index = first.lastIndexOf(toSearch);
			if (index != -1) {
				String common = first.substring(index);

				if (second.startsWith(common))
					return true;
			}
		}

		return false;
	}

	public static boolean checkStringOverlapStemmed(String first, String second) {

		second = second.replaceAll("[\\p{Punct}]", " ");
		String[] wordsSecond = second.toLowerCase().trim().split(" ");

		if (wordsSecond.length >= 1) {
			String toSearch = wordsSecond[0].trim();
			int index = first.lastIndexOf(toSearch);
			if (index != -1) {
				String common = first.substring(index);

				if (second.startsWith(common))
					return true;
			}
		}

		return false;
	}

	public static int countStringOverlap(String first, String second) {

		String[] wordsSecond = second.toLowerCase().trim().split(" ");

		if (wordsSecond.length >= 1) {
			String toSearch = wordsSecond[0].trim();
			int index = first.lastIndexOf(toSearch);
			if (index != -1) {
				String common = first.substring(index);

				if (second.startsWith(common)) {
					return common.trim().split(" ").length;
				}

			}
		}

		return 0;
	}

	public static String mergeStrings(List<String> listOfStrings, String separator) {
		String merged = "";

		for (String el : listOfStrings) {
			merged = merged + separator + el;
		}
		return merged.trim();
	}

	public static String mergeStrings(AnnotationDetail fst,
			AnnotationDetail snd,
			String separator) {

		String merged = "";

		merged = fst.content + " " + 
				separator + " " + snd.content;

		return merged.trim();
	}


	public static String mergeStrings(List<String> listOfStrings) {
		String merged = "";

		for (String el : listOfStrings) {
			merged = merged + " " + el;
		}
		return merged.trim();
	}

	public static String mergeStrings(String a, String b, String separator) {
		String merged = "";

		merged = a + separator + b;

		return merged.trim();
	}


	public static String mergeStrings(String a, String b) {
		String merged = "";

		merged = a + " " + b;

		return merged.trim();
	}

	public static String mergeStrings(int a, int b) {
		return mergeStrings(String.valueOf(a), String.valueOf(b));
	}

	public static String mergeDashStrings(String a, String b) {
		List<String> listValues = new ArrayList<String>();
		listValues.add(a);
		listValues.add(b);

		return mergeDashStrings(listValues);
	}

	public static String mergeDashStrings(List<String> listOfStrings) {
		String merged = listOfStrings.get(0);
		listOfStrings.remove(0);

		for (String el : listOfStrings) {
			merged = merged + "-" + el;
		}
		return merged;
	}

	public static String removePunctuation(String attribute) {

		return attribute.replaceAll("[\\p{Punct}]", "").replaceAll(" +", " ");
	}

	public static String removePunctuationButDot(String attribute) {

		String merged = "";

		for(int index = 0; index < attribute.length(); index ++){
			char chAt = attribute.charAt(index);

			if(Character.isLetterOrDigit(chAt))
				merged = merged + chAt;
			if(chAt == ' ')
				merged = merged + chAt;
			if(chAt == '.')
				merged = merged + chAt;
		}

		return merged;
	}


	public static String removeSentencePunctuation(String content) {
		String replaced = content.replaceAll(regexDelimiter, "");

		return replaced.replaceAll(" +", " ");
	}


	public static String removeConjuctions(String updatedLine, String med) {
		boolean found = true;

		while (found) {
			found = false;

			String[] splitWord = updatedLine.split(" ");
			String updatedMed = "";
			for (int index = 0; index < splitWord.length; index++) {
				if (SystemConstants.conjuctions.contains(splitWord[index]
						.trim())) {
					found = true;
					if (updatedMed.contains(med))
						break;
					else
						updatedMed = "";
				}
				else {
					updatedMed = mergeStrings(updatedMed, splitWord[index]);
				}
			}

			updatedLine = updatedMed;
		}

		return updatedLine.trim();
	}

	public static String removeNumber(String medication) {
		String[] splitMed = medication.split(" ");

		for (String med : splitMed)
			if (UtilMethods.isNumber(med))
				medication = medication.replace(med, "");

		return medication;
	}

	public static String removeDigits(String medication) {
		String tmpMed = medication;

		for (char chr : medication.toCharArray()) {
			if (Character.isDigit(chr))
				tmpMed = tmpMed.replaceAll(String.valueOf(chr), "");
		}

		return tmpMed;
	}

	/**
	 * @param context
	 * @return
	 */
	public static String removeBrackets(String context) {
		context = context.replaceAll("\\(.+\\)", "");

		return context.trim();
	}

	/**
	 * replace the punctuation inside the sentence with 
	 *  space + punctuation 
	 * @param sentence
	 * @return
	 */
	public static String sentenceProcessTagger(String sentence){
		String updatedString = "";

		for(int index = 0; index < sentence.length(); index ++){
			char current = sentence.charAt(index);

			if(!Character.isLetterOrDigit(current)){
				updatedString += " " + current + " ";
			}else{
				updatedString += current;
			}
		}

		return updatedString.replaceAll(" +", " ").trim();
	}

	public static String adjustConcept(String word) {
		boolean hasChar = false;
		boolean hasDigit = false;
		String merged = "";

		for(int wordIndex = 0; wordIndex < word.length(); wordIndex ++){
			if(Character.isLetter(word.charAt(wordIndex))){
				merged = merged + word.charAt(wordIndex);
				hasChar = true;
			}
			if(Character.isDigit(word.charAt(wordIndex))){
				hasDigit = true;
			}
			if(word.charAt(wordIndex) == ' ')
				merged = merged + " ";
		}

		if(hasChar && hasDigit)
			word = merged;

		if(word.endsWith(".") && word.length() == 2)
			word = word.substring(0,1);

		return merged.replaceAll(" +", " ").trim();
	}

	public static boolean checkWordsOverlap(String content, String content2) {
		List<String> splitContent = Arrays.asList(content.split(" "));
		List<String> splitContent2 = Arrays.asList(content2.split(" "));

		for(String val : splitContent)
			if(splitContent2.contains(val))
				return true;

		return false;
	}

	public static String sentenceLocationPhrase(String sentence, String phrase){
		sentence = sentence.toLowerCase();
		phrase = phrase.toLowerCase();

		int index = sentence.indexOf(phrase);
		String prevSection = "";

		while(index != -1){
			// check there is an empty space at the beginning of the phrase
			if((index != 0 && sentence.charAt(index-1) == ' ') || index == 0){
				// check there is an empty space after the phrase
				int location = index + phrase.length() ;
				if(location < sentence.length() && sentence.charAt(location)  == ' ')
					return UtilMethods.mergeStrings(prevSection, 
							sentence.substring(0, index), "").trim();
				if(location == sentence.length())
					return UtilMethods.mergeStrings(prevSection, 
							sentence.substring(0, index), "").trim();
			}


			prevSection = prevSection + sentence.substring(0,index + phrase.length());
			sentence = sentence.substring(index + phrase.length());
			index = sentence.indexOf(phrase);
		}

		return null;
	}

	public static String mergeOffsets(int startLine, int index) {
		return UtilMethods.mergeStrings(String.valueOf(startLine), 
				String.valueOf(index), "_")	;
	}

	public static boolean endsWithPunctuation(String word) {
		if(word.length() > 1)
			return UtilMethods.isPunctuation(word.charAt(word.length()-1));
		return false;
	}

	public static boolean containsNumber(String word) {
		for(int index=0; index < word.length(); index ++ )
			if(Character.isDigit(word.charAt(index)))
				return true;

		return false;
	}

	public static boolean sentenceEndAtWord(String word) {
		if(word.endsWith(".")){
			if(word.length() > 1 && word.substring(0, word.length()-1).contains("."))
				return false;

			if(word.equalsIgnoreCase("h."))
				return false;

			word = UtilMethods.removePunctuation(word);
			if(UtilMethods.isNumber(word))
				return false;

			return true;
		}

		return false;
	}

	static List<String> spelledOutNumber = Arrays.asList("one", "two", 
			"three", "four", "five", "six", "seven", "eight", "nine", 
			"ten", "ii", "iii");

	static List<String> continueWord = Arrays.asList("give", "gave", 
			"take", "continue", "begin", "start", "receive", "received",
			"restart", "treat", "remain", "maintain", 
			"initiate", "use");
	static List<String> stopWord = Arrays.asList("discontinue", 
			"hold", "held", "stop", "remove", "removed");
	
	static List<String> timeWord = Arrays.asList("week", "weeks", 
			"day", "days", "hour", "h", "month", "months", "morning", 
			"evening", "afternoon", "daily", "weekly", "monthly");

	public static String normalizeText(WordTagMap wordTag){
		String word = wordTag.baseForm.toLowerCase();
		
		return normalizeText(word);
	}

	public static String normalizeText(String word){		

		if(UtilMethods.isNumber(
				UtilMethods.removePunctuation( word)))
			return "10";

		if(UtilMethods.containsNumber(word))
			return "10";

		if(spelledOutNumber.contains(word))
			return "10";

		if(UtilMethods.isPunctuation(word))
			return ".";
		
		word = UtilMethods.removePunctuation(word);

		if(word.equals("he") || word.equals("she") || 
				word.equals("patient") || word.equals("pt") ||
				word.equals("you"))
			return "she";

		if(stopWord.contains(word))
			return "stop";

		if(continueWord.contains(word))
			return "continue";
		
		if(timeWord.contains(word))
			return "timeWord";
		

		return UtilMethods.removePunctuation(word);
	}


}
