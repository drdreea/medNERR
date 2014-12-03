/**
 * Created on Feb 4, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package conceptExtraction;

import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import corpus.AnnotationDetail;

import preprocess.wiki.WikiDisambiguate;
import utils.Distance;
import utils.UtilMethods;

/**
 * @author ab
 *
 */
public abstract class Recognition {

	List<String> semType ;
	WikiDisambiguate wikiSearches;
	HashMap<String, Boolean> caches ;

	public Recognition(HashMap<String, Boolean> caches){
		wikiSearches = new WikiDisambiguate();
		this.caches = caches;
	}

	/**
	 * Find whether there is an NE
	 * that meets the required criteria within the metamap results
	 * @param ev
	 * @param medication 
	 * @throws Exception 
	 */
	String findNE(Ev ev, ArrayList<AnnotationDetail> neList,
			ArrayList<String> addedEls, String input, int maxScore) throws Exception {

		// check whether the found semantic type is what we need
		if (! checkValidSemanticType(ev.getSemanticTypes())) return null;
		if (! checkValidEV(ev)) return null;

		String mergedMed = UtilMethods.mergeStrings(ev.getMatchedWords());

		if (mergedMed.length() <=2 && ! mergedMed.equals("o2"))  return null;

		if(mergedMed != null && !neList.contains(mergedMed)) {
			String foundVal =   checkNE(input, mergedMed, addedEls, ev);
			if (foundVal != null && !foundVal.isEmpty()) 
				return foundVal;

			// if we did not find any matches inside the returned matched words
			// we try to see if there are matches inside the preferred name
			else {
				mergedMed = ev.getPreferredName().toLowerCase();

				if(mergedMed != null && ! neList.contains(mergedMed)) {
					foundVal =  checkNE(input, mergedMed, addedEls, ev);
					if (foundVal != null && !foundVal.isEmpty()) 
						return foundVal;

				}
			}

			//	if we did not find a medication yet, we try to see whether 
			//	the medication is an abbreviation
			Position phrasePosition = ev.getPositionalInfo().get(0);
			try {
				String phrase = input.substring(phrasePosition.getX(), 
						phrasePosition.getY() + phrasePosition.getX());
				phrase = phrase.trim().toLowerCase();

				boolean cachedValue = false;
				boolean contained = false;
				if (this.caches.containsKey(phrase)){
					contained = true;
					cachedValue = this.caches.get(phrase);
				}

				if(contained == false)
					cachedValue = wikiSearches.findMedication(phrase, ev.getPreferredName());

				if (cachedValue){
					if(!this.caches.containsKey(phrase))
						this.caches.put(phrase, true);

					if(UtilMethods.checkQuantityModal(phrase))

						return UtilMethods.removeQuantity(
								UtilMethods.removeModal(phrase));
					else
						return checkNE(input, phrase.toLowerCase(), addedEls, ev);

				}else{
					if(!this.caches.containsKey(phrase))
						this.caches.put(phrase, false);
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}


		return null;
	}



	boolean checkValidSemanticType(List<String> types) {
		boolean found = false;

		for(String possibleType: types)
			if (this.semType.contains(possibleType)) {	
				found = true; continue;
			}

		return found;
	}


	/**
	 * 
	 * @param input
	 * @param mergedMed
	 * @param addedEls
	 * @param ev
	 * @return
	 * @throws Exception
	 */
	String checkNE(String input, String mergedMed, 
			ArrayList<String> addedEls, Ev ev) throws Exception {
		// if the input contains the med or if the input 
		// is a one char mispelling of the med
		input = input.toLowerCase();
		String tempMerged = mergedMed ;
		mergedMed = mergedMed.toLowerCase();

		if (input.contains(mergedMed) ) {

			if (input.length() > mergedMed.length() ) {
				// the cases where the input has a punctuation sign tied to it
				mergedMed = this.checkMedForm(mergedMed, input, addedEls);

				if (mergedMed == null) return null;

				if(	input.contains((mergedMed + " ")) || input.endsWith(mergedMed)) 
					if( ! addedEls.contains(mergedMed)) {
						return mergedMed;
					}
			}else {
				if(input.length() == mergedMed.length()) {
					if (! addedEls.contains(tempMerged)) {
						return tempMerged;
					}
				}
			}
		}
		// check the levenshtein distance
		else {
			if(Distance.LD(input.trim(), 
					mergedMed.trim()) == 1 &&
					!addedEls.contains(input.trim())) {
				return input.trim();
			}else 
				if(input.contains(
						UtilMethods.mergeDashStrings(ev.getMatchedWords()).toLowerCase())) {
					mergedMed = UtilMethods.mergeDashStrings(ev.getMatchedWords());
					if (input.length() > mergedMed.length() && 
							(input.contains((mergedMed + " ")) || input.endsWith(mergedMed)) ) {
						if( !UtilMethods.checkQuantityModal(mergedMed) && !addedEls.contains(mergedMed)) {
							return mergedMed;
						}
					}
				}
		}

		return null;

	}


	/**
	 * 
	 * @param mergedMed
	 * @param input
	 * @param addedEls 
	 * @return
	 */
	String checkMedForm(String mergedMed, String input, ArrayList<String> addedEls) {
		// if mergedMed is null or empty return null
		if (mergedMed == null || mergedMed.trim().isEmpty())
			return null;

		int indexMed = input.indexOf(mergedMed);
		if (indexMed == -1) return null;

		String tmpMed = mergedMed;

		// check whether there is an empty space after the merged med inside the input
		int indexEmptySpace = 
				input.substring(indexMed+mergedMed.length()).indexOf(" ");

		if (indexEmptySpace != -1) {
			mergedMed = input.substring(indexMed, 
					indexMed + mergedMed.length() + indexEmptySpace);
		}else {
			mergedMed = input.substring(indexMed);
		}

		// check if the updated merged med has the same morphological form
		// i.e., there is no tense or case change
		if (mergedMed.length() > tmpMed.length()) {
			char currentChar = mergedMed.charAt(tmpMed.length());
			if (currentChar >= 'a' && currentChar <= 'z')
				return null;
		}

		// check whether there is an empty space before the merged med inside the input
		indexEmptySpace = input.substring(0, indexMed ).lastIndexOf(" ");

		if(indexEmptySpace != -1) {
			mergedMed = input.substring(0, indexMed).
					substring(indexEmptySpace) + mergedMed;
		}else {
			mergedMed = input.substring(0, input.indexOf(mergedMed)) + mergedMed;
		}

		// check if the med ends with a digit, then we remove it
		if (UtilMethods.checkQuantityModal(mergedMed)) {
			mergedMed = UtilMethods.removeQuantity(mergedMed);
			mergedMed = UtilMethods.removeModal(mergedMed);
		}

		if (mergedMed.indexOf(" ") != -1) {
			String lastElements = mergedMed.substring(mergedMed.lastIndexOf(" ")).trim();

			if (lastElements.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+"))  
				mergedMed = mergedMed.substring(0, mergedMed.indexOf(lastElements)).trim();  

		}

		mergedMed = mergedMed.trim();

		// check whether there is a percentage next to the medication inside the input
		indexMed = input.toLowerCase().indexOf(mergedMed);
		if (indexMed!= -1){

			String[] splitInput = input.substring(indexMed + mergedMed.length()).trim().split(" ");
			if(splitInput.length > 1){
				if(splitInput[0].endsWith("%"))
					mergedMed = UtilMethods.mergeStrings(mergedMed, splitInput[0]);
			}
		}

		// if the medication was already added, we try to find another
		// occurrence for it within the same sentence
		if (addedEls.contains(mergedMed)) {
			return checkMedForm(tmpMed, input.substring(indexMed+mergedMed.length()), 
					addedEls);
		}

		// if the merged med ends with colon we return null
		if (mergedMed.endsWith(":")) return null;

		return mergedMed.trim();

	}



	boolean checkValidEV(Ev ev) {
		return true;
	}



	/**
	 * @param ev
	 * @param input
	 * @param prevLine
	 * @param currentLine
	 * @param lineIndex
	 * @return
	 */
	public AnnotationDetail getAnnotationInformation(Ev ev, String input,
			String prevLine, String currentLine, int lineIndex, String med) {
		AnnotationDetail annt = null;

		Position phrasePosition;
		try {
			phrasePosition = ev.getPositionalInfo().get(0);
			String phrase = input.substring(phrasePosition.getX(), 
					phrasePosition.getY() + phrasePosition.getX());
			phrase = phrase.trim().toLowerCase();	  
			phrase = phrase.trim().toLowerCase();

			if(med != null)
				phrase = med.toLowerCase();

			// check if the medication is contained in the current line
			String content = input.substring(phrasePosition.getX());
			if(currentLine.contains(content)) {
				annt = new AnnotationDetail(phrase, null);
				annt.startLine = lineIndex;
				annt.endLine = lineIndex;
				annt.startOffset = currentLine.split(" ").length - content.split(" ").length;
				annt.endOffset = annt.startOffset + phrase.split(" ").length -1;
			}else {
				// we are in the case when a medication spans on two lines
				String[] splitMed = phrase.split(" ");
				String mergedMed = "";

				for(int index=0; index < splitMed.length; index++) {
					mergedMed = UtilMethods.mergeStrings(mergedMed, splitMed[index]);
					if(prevLine.endsWith(mergedMed)) {
						break;
					}
				}

				String secondLineContent = phrase.replace(mergedMed, "").trim();
				if(!secondLineContent.isEmpty() && !mergedMed.isEmpty() 
						&& currentLine.startsWith(secondLineContent)) {
					annt = new AnnotationDetail(phrase, null);
					annt.startLine = lineIndex-1;
					annt.endLine = lineIndex;
					annt.endOffset = secondLineContent.split(" ").length -1;
					annt.startOffset = prevLine.split(" ").length - mergedMed.split(" ").length;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return annt;

	}

}
