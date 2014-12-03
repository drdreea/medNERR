/**
 * 
 */
package patternExtraction;

import io.importer.DataImport;
import io.importer.PatternFinderConfigHandler;
import io.output.OutputPredictions;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import utils.UtilMethods;

/**
 * @author ab
 *
 */
public class FindContext {
    HashSet<String> reasons;
    HashSet<String> meds;
    HashSet<String> rawDocs;
    DataImport  importer;
    PatternFinderConfigHandler configs;

    public FindContext(PatternFinderConfigHandler configs) {
	importer = new DataImport(null);
	this.configs = configs;

	reasons = importer.importDailyMedNames(configs.reasonPath, false);
	meds = importer.importDailyMedNames(configs.medsPath, false);
	rawDocs = importer.importDailyMedRawContent(configs.rawPath);	
    }

    void execute() {
	ArrayList<String> context = new ArrayList<String>();

	for(String doc : this.rawDocs) {
	    context.addAll(processDoc(doc));
	}

	Collections.sort(context);
	ArrayList<String> maxSubstring = new ArrayList<String>();
	String prev = null;
	int count = 0;

	for(String element : context) {
	    if(prev == null) {
		prev = element; 
		continue;
	    }

	    String overlap = stringOverlap(prev, element);

	    if( (overlap == null && count > 0 && prev.split(" ").length > 2) ||
		    (overlap != null && !overlap.equals(prev) && count > 0)) {
		count = 0;
		if(!maxSubstring.contains(prev))
		    maxSubstring.add(prev);
		prev = element;
		continue;
	    } else if (overlap == null || overlap.split(" ").length <= 2) {
		count = 0;
		prev = element; 
	    } else if(overlap != null){
		count ++;
		prev = overlap;
	    }
	}

	OutputPredictions.storeContents(configs.resultsOutputFile, maxSubstring);
    }

    /**
     * @param prev
     * @param element
     * @return
     */
    private String stringOverlap(String prev, String element) {
	if(prev.equals(element))
	    return prev;

	if(prev.length() < element.length()) {
	    String tmp = "";
	    tmp = prev;
	    prev = element;
	    element = tmp;
	}

	String[] prevSplit = prev.split(" ");
	String[] elSplit = element.split(" ");
	String maxMerged = null;
	int maxLength = 0;
	int len1 = elSplit.length;
	int len2 = prevSplit.length;

	for(int i = 0; i < len1; i++) {
	    for(int j = 0; j < len2; j++) {
		String merged = "";
		int length = 0;

		if(elSplit[i].equals(prevSplit[j])) {
		    merged = UtilMethods.mergeStrings(merged, elSplit[i]);
		    length ++;

		    int k = i + 1 ;
		    int p = j + 1 ;
		    while(k < len1 && p < len2) {
			if(elSplit[k].equals(prevSplit[p])) {
			    merged = UtilMethods.mergeStrings(merged, elSplit[k]);
			    length ++;
			}else
			    break;

			k++; p++;
		    }

		    if(maxLength < length) {
			maxLength = length;
			maxMerged = merged;
		    }
		}
	    }


	}

	if(maxMerged != null)
	    maxMerged = maxMerged.trim();

	return maxMerged;
    }


    /**
     * @param doc
     * @return 
     */
    private ArrayList<String> processDoc(String doc) {
	if(doc.contains("tablets usp are indicated for the treatment of hypertension"))
	    System.out.println("here");

	String[] docWords = doc.split(" ");
	ArrayList<IndexPair> medPositions = new ArrayList<IndexPair>();
	ArrayList<IndexPair> reasonPositions = new ArrayList<IndexPair>();
	IndexPair prevMed = null;
	IndexPair prevReason = null;
	ArrayList<String> foundContext = new ArrayList<String>();

	for(int i=0; i < docWords.length; i++) {
	    String word = UtilMethods.removePunctuation(docWords[i]);

	    if(this.meds.contains(word)) {
		IndexPair foundMed = new IndexPair(i, i);
		medPositions.add(foundMed);
		prevMed = foundMed;
	    }
	    else if(this.reasons.contains(word)) {
		IndexPair foundReason = new IndexPair(i, i);
		reasonPositions.add(foundReason);
		prevReason = foundReason;

		if(prevMed != null) {
		    String context = extractContext(prevMed, prevReason, docWords);
		    if(context != null)
			foundContext.add(context);
		    prevMed = null;
		}
	    }

	    ArrayList<String> merges = new ArrayList<String>();
	    merges.add(docWords[i]);

	    for(int pos = 1; pos <= 5 && i+pos < docWords.length; pos ++) {
		merges.add(docWords[i+pos]);
		String mergedWord = UtilMethods.removePunctuation(UtilMethods.mergeStrings(merges));

		if(this.meds.contains(mergedWord) ) {
		    IndexPair foundMed = new IndexPair(i, i+pos);
		    prevMed = foundMed;
		    medPositions.add(foundMed);

		}
		else if(this.reasons.contains(mergedWord) ) {
		    IndexPair foundReason = new IndexPair(i, i+pos);
		    prevReason = foundReason;
		    reasonPositions.add(foundReason);
		    if(prevMed != null) {
			String context = extractContext(prevMed, prevReason, docWords);
			if(context != null)
			    foundContext.add(context);

			prevMed = null;
		    }
		}
	    }
	}

	return foundContext;

    }

    /**
     * @param prevMed
     * @param prevReason
     * @param docWords
     * @return
     */
    private String extractContext(IndexPair prevMed, IndexPair prevReason,
	    String[] docWords) {
	String context = "";

	for(int index=prevMed.endOffset + 1; index<prevReason.startOffset; index++) {
	    context = UtilMethods.mergeStrings(context, docWords[index]);
	}
	context = context.trim();

	try {
	    context = new String(context.getBytes(), "UTF-8");
	}
	catch (UnsupportedEncodingException e) {
	    e.printStackTrace();
	    return null;
	}

	if(context.length() < 5) return null;

	context = UtilMethods.removeBrackets(context).trim();
	context = UtilMethods.removeDigits(context).trim();
	context = UtilMethods.removePunctuation(context).trim();

	if(context.split(" ").length > 15 || context.split(" ").length <= 2)
	    return null;

	return context;
    }

    public static void main(String[] args) {
	PatternFinderConfigHandler config = 
		new PatternFinderConfigHandler(args[0]);

	FindContext finder = new FindContext(config);
	finder.execute();
    }
}




