/**
 * Created on Feb 4, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package conceptExtraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import corpus.AnnotationDetail;

import preprocess.stemmer.EnglishStemmer;
import utils.Distance;
import utils.UtilMethods;
import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.Position;

/**
 * @author ab
 *
 */
public class ReasonRecognition extends Recognition {


    public ReasonRecognition(HashMap<String, Boolean> caches) {
	super(caches);
	this.semType =  Arrays.asList("sosy", "dsyn", 
		"mobd", "patf", "diap","clna", "phpr",
		"fndg", "ortf","lbtr","lbpr", "bact",
		"inpo" // injury or poisoning
		);
    }

    /**
     * Find whether there is a reason that meets the required 
     * criteria within the metamap results
     * 
     * @param ev
     * @param reasons
     * @param addedReasons
     * @param input
     * @return
     */
    AnnotationDetail findReasonNE(Ev ev, String input, 
	    String prevLine, String currentLine, int lineIndex) throws Exception {
	AnnotationDetail annt = null;

	// check whether the found semantic type is what we need
	for (String foundST : ev.getSemanticTypes()) {
	    if ( ! this.semType.contains(foundST)) continue;
	    
	    annt = 
		    getAnnotationInformation(ev, input, prevLine, currentLine, lineIndex, null);
	    
	    if(annt != null && annt.content.length() <=2)
		return null;
	}

	return annt;
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
	// check the normalized form
	else if(normFormEqual(ev, mergedMed, input)!=null) {
	    return normFormEqual(ev, mergedMed, input);
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
     * @param ev
     * @return
     */
    private String normFormEqual(Ev ev, String med, String lineInput) {
	// get the med form according to the ev
	try {
	    Position phrasePosition = ev.getPositionalInfo().get(0);

	    String phrase = lineInput.substring(phrasePosition.getX(), 
		    phrasePosition.getY() + phrasePosition.getX());
	    phrase = phrase.trim().toLowerCase();

	    if(EnglishStemmer.process(phrase).equals(
		    EnglishStemmer.process(med)))
		return phrase;
	}
	catch (Exception e) {
	    e.printStackTrace();
	}

	return null;
    }
}
