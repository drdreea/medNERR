/**
 * Created on Feb 8, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package preprocess.wiki;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import utils.StringNumberPair;
import utils.UtilMethods;

/**
 * @author ab
 *
 */
public class WikiDisambiguate {
    HashMap<Integer, Boolean> previousSearches;
    int waitTime = 10; // we will wait 10 seconds top for queries to finish

    public WikiDisambiguate(){
	previousSearches = new HashMap<Integer, Boolean>();

    }

    public boolean findReason(String medication, 
	    String reason, String textLine){

	textLine = textLine.toLowerCase();

	// first check if the textInput contains brackets
	if(medication.indexOf("(") != -1)
	    medication = medication.substring(0, medication.indexOf("("));

	// remove other punctuation signs
	medication = UtilMethods.removePunctuation(medication);
	medication = UtilMethods.removeNumber(medication);
	reason = UtilMethods.removePunctuation(reason);

	if(medication.contains(reason)) return false;

	String mergedQuery = "\"medication\"+'" + medication + 
		"\"+\"" + reason + "\"";
	mergedQuery = mergedQuery.toLowerCase();
	mergedQuery = mergedQuery.replace(" ", "+");

	boolean processValue =  process(mergedQuery, medication, reason);

	if(processValue == true) return true;

	// check with a different preset noun
	mergedQuery = "\"treatment of\"+'" + medication + 
		"\"+\"" + reason + "\"";
	mergedQuery = mergedQuery.replace(" ", "+");

	processValue =  process(mergedQuery, medication, reason);
	if(processValue == true) return true;

	// check if the reason has a bracketed explanation inside the text
	if(textLine.contains(reason)){
	    StringNumberPair medWithBrackets = 
		    UtilMethods.checkBrackets(textLine, medication);
	    if(medWithBrackets != null){
		String tmpReason = medWithBrackets.getString();
		if(tmpReason.indexOf("(") != -1 && 
			tmpReason.indexOf(")") != -1){
		    reason = tmpReason.substring(tmpReason.indexOf("(") +1, 
			    tmpReason.indexOf(")"));
		    mergedQuery = "'medication' + '" + medication + 
			    "' + '" + reason + "'";
		    return process(mergedQuery, medication, reason);
		}
	    }
	}

	return processValue;
    }

    public boolean findMedication(String content) {
	if(content.indexOf("(") != -1)
	    content = content.substring(0, content.indexOf("("));

	String mergedQuery = "'medication' + '" + content + "'" ;
	mergedQuery = mergedQuery.toLowerCase();

	return process(mergedQuery, content, "medication");
    }

    public boolean findFood(String content) {
	if(content.indexOf("(") != -1)
	    content = content.substring(0, content.indexOf("("));

	String mergedQuery = "'food menu'+'" + content + "'+'recipe'" ;
	mergedQuery = mergedQuery.toLowerCase();

	return processStrict(mergedQuery, content, "recipe");
    }

    public boolean findBloodTests(String content) {
	if(content.indexOf("(") != -1)
	    content = content.substring(0, content.indexOf("("));

	String mergedQuery = "'" + content + " test'" ;
	mergedQuery = mergedQuery.toLowerCase();

	System.out.println(mergedQuery);

	return processStrict(mergedQuery, content + " test", "");
    }

    public boolean findAlchoolTobacco(String content) {
	if(content.indexOf("(") != -1)
	    content = content.substring(0, content.indexOf("("));

	String mergedQuery = "'substance abuse'+'" + content + "'+'taxes'" ;
	mergedQuery = mergedQuery.toLowerCase();

	return processStrict(mergedQuery, content, "taxes");
    }

    public boolean findMedication(String textInput, String metamapResult){
	// first check if the textInput contains brackets
	if(textInput.indexOf("(") != -1)
	    textInput = textInput.substring(0, textInput.indexOf("("));

	String mergedQuery = "'medication' + '" + textInput + 
		"' + '" + metamapResult + "'";
	mergedQuery = mergedQuery.toLowerCase();

	return process(mergedQuery, textInput, metamapResult);
    }

    public boolean process(String mergedQuery, String textInput, 
	    String metamapResult){
	int hashedQuery = mergedQuery.hashCode();
	long initialTime = System.currentTimeMillis()/1000;

	// first see if the result is inside the previous searches
	if (this.previousSearches.containsKey(hashedQuery))
	    return this.previousSearches.get(hashedQuery);

	if(mergedQuery.contains("&") || textInput.equals(metamapResult)) {
	    this.previousSearches.put(hashedQuery, false);
	    return false;
	}

	ExecutorService es = Executors.newSingleThreadExecutor();

	try {
	    String[][] searchResults = 
		    es.submit(new Wiki(mergedQuery, true)).get(this.waitTime, TimeUnit.SECONDS);

	    // check to see the progress of the query
	    long currentTime = System.currentTimeMillis()/1000;

	    if (currentTime - initialTime > this.waitTime) {
		es.shutdownNow();
		this.previousSearches.put(hashedQuery, false);
		return false;
	    }


	    int length = searchResults.length;
	    String[] splitInput = textInput.toLowerCase().split(" ");
	    String[] splitMetamap = metamapResult.toLowerCase().split(" ");

	    if (length > 0 ) {
		for(int resultCount = 0; resultCount < searchResults.length; resultCount ++){
		    String[] firstResults = searchResults[resultCount];


		    for(int index =0; index < firstResults.length; index ++ ) {
			boolean found = true;
			int foundWords = splitInput.length;
			String content = firstResults[index].toLowerCase();

			for(int i = 0; i < splitInput.length; i ++) {
			    int indexInput = content.indexOf(splitInput[i]);
			    if(indexInput == -1) {
				found = false;
				foundWords --;
			    }else {
				// check that the next char from the input is
				// a punctuation sign. otherwise the input
				// is part of another word, and we don;t want this case
				if(indexInput + splitInput[i].length() + 1 < content.length()) {
				    String subString = content.substring(indexInput+splitInput[i].length(), 
					    indexInput+ splitInput[i].length() + 1);
				    if(!UtilMethods.containsPunctuation(subString)) {
					found = false;
					break;
				    }

				}
			    }
			}

			if (found || foundWords >= (splitInput.length/2 +1)) {
			    foundWords = splitMetamap.length;
			    for(int i =0 ; i< splitMetamap.length; i++) 
				if(!content.contains(splitMetamap[i])) {
				    foundWords --;
				}

			    if(foundWords >= splitMetamap.length/2 +1)
				found = true;
			    else
				found = false;
			}

			if(found){
			    // check if the found med is a number
			    if(!UtilMethods.isNumber(textInput)) {
				this.previousSearches.put(hashedQuery, true);
				es.shutdownNow();
				return true;
			    }
			}

		    }
		}
	    }

	}catch(Exception e) {
	    e.printStackTrace();
	}

	// shutdown the executor
	es.shutdownNow();

	this.previousSearches.put(hashedQuery, false);
	return false;

    }

    public boolean processStrict(String mergedQuery, String textInput, 
	    String metamapResult){
	int hashedQuery = mergedQuery.hashCode();
	long initialTime = System.currentTimeMillis()/1000;

	// first see if the result is inside the previous searches
	if (this.previousSearches.containsKey(hashedQuery))
	    return this.previousSearches.get(hashedQuery);

	if(mergedQuery.contains("&") || textInput.equals(metamapResult)) {
	    this.previousSearches.put(hashedQuery, false);
	    return false;
	}

	ExecutorService es = Executors.newSingleThreadExecutor();

	try {
	    String[][] searchResults = 
		    es.submit(new Wiki(mergedQuery, true)).get(this.waitTime, TimeUnit.SECONDS);

	    // check to see the progress of the query
	    long currentTime = System.currentTimeMillis()/1000;

	    if (currentTime - initialTime > this.waitTime) {
		es.shutdownNow();
		this.previousSearches.put(hashedQuery, false);
		return false;
	    }


	    int length = searchResults.length;

	    if (length > 0 ) {
		for(int resultCount = 0; resultCount < searchResults.length; resultCount ++){
		    String[] firstResults = searchResults[resultCount];


		    for(int index =0; index < firstResults.length; index ++ ) {
			boolean found = true;
			String content = firstResults[index].toLowerCase();

			int indexInput = content.indexOf(textInput);
			if(indexInput == -1) {
			    found = false;
			}else {
			    // check that the next char from the input is
			    // a punctuation sign. otherwise the input
			    // is part of another word, and we don;t want this case
			    if(indexInput + textInput.length() + 1 < content.length()) {
				String subString = content.substring(indexInput+textInput.length(), 
					indexInput+ textInput.length() + 1);
				if(!UtilMethods.containsPunctuation(subString)) {
				    found = false;
				    break;
				}

			    }
			}

			int indexMetamap = -1;

			if(!content.contains(metamapResult)) {
			    found = false;
			}else {
			    indexMetamap = content.indexOf(metamapResult);
			}


			if(found) {
			    int start = indexMetamap;
			    int end = indexInput;

			    if(indexMetamap > indexInput) {
				start = indexInput;
				end = indexMetamap;
			    }

			    String contentSentence = content.substring(start, end);
			    if(contentSentence.contains("<b>...</b>") )
				found = false;
			}

			// check that the metamap results and the text input are
			// located in the same sentence


			if(found){
			    // check if the found med is a number
			    if(!UtilMethods.isNumber(textInput)) {
				this.previousSearches.put(hashedQuery, true);
				es.shutdownNow();
				return true;
			    }
			}

		    }
		}
	    }

	}catch(Exception e) {
	    e.printStackTrace();
	}

	// shutdown the executor
	es.shutdownNow();

	this.previousSearches.put(hashedQuery, false);
	return false;

    }


}
