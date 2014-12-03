/**
 * Created on Jan 10, 2012
 * 
 * @author ab Contact andreeab dot mit dot edu
 * 
 */

package preprocess.parser;

import io.log.ExtractMedicationLog;

import java.io.StringReader;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import corpus.MedicalRecord;

import utils.PhrasePair;
import utils.StringNumberPair;
import utils.StringPair;
import utils.SystemConstants;
import utils.UtilMethods;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * @author ab
 * 
 */
public class ParsingDocument implements Callable<MedicalRecord> {
    LexicalizedParser parser;
    MedicalRecord emr;
    TokenizerFactory<CoreLabel> tokenizerFactory;
    ExtractMedicationLog log;

    enum Operation {
	ADD, REMOVE
    };

    public enum Type {
	REASON, MEDICATION
    };

    public ParsingDocument(LexicalizedParser lp, MedicalRecord record,
	    ExtractMedicationLog log) {
	this.parser = lp;
	this.emr = record;
	this.log = log;

	tokenizerFactory = PTBTokenizer
		.factory(new CoreLabelTokenFactory(), "");

    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public MedicalRecord call() throws Exception {

//	this.parseDocument(emr.medicationTextIndexes, Type.MEDICATION);

	// make sure we did not introduce overlapping entries
//	this.emr.medicationTextIndexes = emr
//		.removeOverlappingNE(this.emr.medicationTextIndexes);

	String message = "FINISHED parsing file " + emr.rawFilePath;
	log.writeError(message);
	System.out.println(message);

	return emr;
    }

    /**
     * @param medicationTextIndexes
     * 
     */
    public void parseDocument(HashMap<PhrasePair, String> textIndexes,
	    Type opType) {
	// go through each line that has a medication associated with it
	// and parse it

	Set<Entry<PhrasePair, String>> currentSet = textIndexes.entrySet();

	for (Entry<PhrasePair, String> index : currentSet) {

	    String line = this.emr.rawFileLines.get(index.getKey().indexes
		    .getStartLine() - 1);
	    // this move should be safe since on the last line
	    // of the emrs is the </report> marker
	    String futureLine = this.emr.rawFileLines
		    .get(index.getKey().indexes.getStartLine());
	    String attribute = index.getValue();
	    StringPair medPair = null;

	    // we have to remove the special characters out of the line
	    // before we parse the line
	    String tmpLine = UtilMethods.identifySentence(line, futureLine,
		    attribute, index.getKey().indexes.getStartLine(),
		    this.emr.rawFileLines.size(), opType).trim();
	    // if the line is null or empty we continue to next phrase
	    if (tmpLine == null || tmpLine.isEmpty())
		continue;

	    List<CoreLabel> rawWords2 = tokenizerFactory.getTokenizer(
		    new StringReader(tmpLine)).tokenize();
	    Tree parse = parser.apply(rawWords2);

	    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

	    for (TypedDependency td : tdl) {
		if (td.gov().label().beginPosition() == -1)
		    continue;

		String gov = tmpLine.substring(
			td.gov().label().beginPosition(),
			td.gov().label().endPosition()).toLowerCase();

		String dep = tmpLine.substring(
			td.dep().label().beginPosition(),
			td.dep().label().endPosition()).toLowerCase();
		String relation = td.reln().getShortName();

		switch (opType) {
		case MEDICATION:
		    StringPair tmpResult = operateMedication(dep, gov,
			    attribute, relation, medPair, line, index,
			    futureLine);
		    if (tmpResult != null) {
			if (tmpResult.el1 != null)
			    attribute = tmpResult.el1;
			medPair = tmpResult;
		    }
		    break;
		case REASON:
		    tmpResult = operateReason(dep, gov, attribute, relation,
			    medPair, line, index);
		    if (tmpResult != null) {
			if (tmpResult.el1 != null)
			    attribute = tmpResult.el1;
			medPair = tmpResult;
		    }
		}

	    }

	}

    }

    private StringPair operateReason(String dep, String gov, String attribute,
	    String relation, StringPair medPair, String line,
	    Entry<PhrasePair, String> index) {

	PhrasePair pair = index.getKey();

	boolean found = false;
	String fst = null;
	String snd = null;

	String tmpAtrribute = attribute;
	if (UtilMethods.containsPunctuation(attribute)) {
	    tmpAtrribute = UtilMethods.removePunctuation(attribute);
	}

	if (tmpAtrribute.endsWith(gov) || tmpAtrribute.startsWith(gov)) {
	    fst = gov;
	    snd = dep;
	    found = true;

	}
	else
	    if (tmpAtrribute.endsWith(dep) || tmpAtrribute.startsWith(dep)) {
		fst = dep;
		snd = gov;
		found = true;
	    }

	if (found) {

	    if (!SystemConstants.reasonDependencies.contains(relation))
		return null;

	    if ((tmpAtrribute.startsWith(fst) && !tmpAtrribute.contains(snd))) {
		return expandReason(attribute, snd, line.trim(), pair, index,
			medPair, Operation.ADD);
	    }
	    else
		if (tmpAtrribute.endsWith(fst) && !tmpAtrribute.contains(snd))
		    return expandReason(snd, attribute, line.trim(), pair,
			    index, medPair, Operation.REMOVE);

	}

	return null;
    }

    StringPair operateMedication(String dep, String gov, String attribute,
	    String relation, StringPair medPair, String line,
	    Entry<PhrasePair, String> index, String futureLine) {
	if (UtilMethods.checkQuantityModal(gov)
		|| UtilMethods.endsWithNumber(gov)
		|| UtilMethods.checkQuantityModal(dep)
		|| UtilMethods.endsWithNumber(dep))
	    return null;

	PhrasePair pair = index.getKey();

	boolean found = false;
	String fst = null;
	String snd = null;

	if (attribute.endsWith(gov) || attribute.startsWith(gov)) {
	    fst = gov;
	    snd = dep;
	    found = true;

	}
	else
	    if (attribute.endsWith(dep) || attribute.startsWith(dep)) {
		fst = dep;
		snd = gov;
		found = true;
	    }

	// special case when the medication ends with a percentage
	boolean percentage = false;
	if (attribute.endsWith("%")) {
	    String[] splitMed = attribute.split(" ");
	    if (splitMed.length > 2) {
		if (splitMed[splitMed.length - 2].endsWith(gov)) {
		    fst = gov;
		    snd = dep;
		    found = true;
		    percentage = true;
		}
		else
		    if (splitMed[splitMed.length - 2].endsWith(dep)) {
			fst = dep;
			snd = gov;
			found = true;
			percentage = true;
		    }
	    }
	}

	if (found) {

	    if (!SystemConstants.medDependencies.contains(relation))
		return null;

	    if ((attribute.startsWith(fst) && !attribute.contains(snd))
		    || percentage) {
		return expandMedication(attribute, snd, line.trim(), pair,
			index, medPair, Operation.ADD, futureLine);

	    }
	    else
		if (attribute.endsWith(fst) && !attribute.contains(snd))
		    return expandMedication(snd, attribute, line.trim(), pair,
			    index, medPair, Operation.REMOVE, futureLine);

	}

	return null;
    }

    /**
     * Check whether the medication has a punctuation sign attached to it
     * 
     * @param med
     * @param line
     * @return
     */
    String checkCharSurroundingMed(String mergedMed, String input) {
	// check whether there is an empty space after the merged med inside the
// input
	int indexMed = input.indexOf(mergedMed);
	try {
	    int indexEmptySpace = input
		    .substring(indexMed + mergedMed.length()).indexOf(" ");

	    if (indexMed == -1)
		return mergedMed;

	    if (indexEmptySpace != -1) {
		mergedMed = input.substring(indexMed, indexMed
			+ mergedMed.length() + indexEmptySpace);
	    }
	    else {
		mergedMed = input.substring(indexMed);
	    }

	    // check whether there is an empty space before the merged med
// inside the input
	    indexEmptySpace = input.substring(0, indexMed).lastIndexOf(" ");

	    if (indexEmptySpace != -1) {
		mergedMed = input.substring(0, indexMed).substring(
			indexEmptySpace)
			+ mergedMed;
	    }
	    else {
		mergedMed = input.substring(0, input.indexOf(mergedMed))
			+ mergedMed;
	    }
	}
	catch (Exception e) {

	}

	return mergedMed;
    }

    /**
     * 
     * @param fst
     * @param snd
     * @param line
     * @param pair
     * @param index
     * @param updatedMed
     * @param action
     * @return
     */
    StringPair expandReason(String fst, String snd, String line,
	    PhrasePair pair, Entry<PhrasePair, String> index,
	    StringPair updatedMed, Operation action) {
	String reason = UtilMethods.mergeStrings(fst, snd).toLowerCase();
	if (line.contains(UtilMethods.mergeStrings(snd, fst).toLowerCase())
		|| UtilMethods.checkStringOverlap(line, UtilMethods
			.mergeStrings(snd, fst).toLowerCase()))
	    reason = UtilMethods.mergeStrings(snd, fst).toLowerCase();

	if (line.toLowerCase().contains(reason)
		|| UtilMethods.checkStringOverlap(line.toLowerCase(), reason)) {
	    // check whether there is a punctuation sign attached to the med
	    reason = checkCharSurroundingMed(reason, line.toLowerCase());

	    switch (action) {
	    case ADD:
		pair.indexes.setEndOffset(pair.indexes.getEndOffset() + 1);
		break;
	    case REMOVE:
		pair.indexes.setStartOffset(pair.indexes.getStartOffset() - 1);
		break;
	    }

	    reason = reason.trim();

	    return new StringPair(reason, null);
	}
	else {
	    // check whether the reason is included in a coordinate structure
	    Entry<PhrasePair, String> coordinateCheck = this.checkCoordinate(
		    line, fst, snd, reason, pair);

	    if (coordinateCheck != null) {
		pair.indexes.setStartOffset(coordinateCheck.getKey().indexes
			.getStartOffset());
		pair.indexes.setEndOffset(coordinateCheck.getKey().indexes
			.getEndOffset());


		return new StringPair(coordinateCheck.getValue(), null);
	    }
	    else {
		// check whether the reason is located at a couple words span
		// from the current subordinate
		StringPair gapCheck = this.containsGap(line, fst, snd, pair,
			reason, updatedMed);

		if (gapCheck != null) {

		    gapCheck.el1 = reason;
		    updatedMed = gapCheck;
		    boolean found = true;

		    for (String key : gapCheck.wordsMap.keySet())
			if (gapCheck.wordsMap.get(key) == false)
			    found = false;

		    if (found) {
			reason = "";
			String[] splitLine = line.split(" ");
			for (int range = updatedMed.startIndex; range <= updatedMed.endIndex; range++) {
			    reason = UtilMethods.mergeStrings(reason,
				    splitLine[range]);
			}
			reason = reason.toLowerCase().trim();

			pair.indexes.setStartOffset(updatedMed.startIndex);
			pair.indexes.setEndOffset(updatedMed.endIndex);

			// we have stored the medication so we clear the updated
// med
			updatedMed.el1 = null;
			updatedMed.wordsMap.clear();
		    }

		    return updatedMed;
		}
	    }
	}

	return null;
    }

    /**
     * Update the medication entry inside the emr list of medications Also check
     * whether the given medication should contain a set of brackets
     * 
     * @param med
     * @param line
     * @param pair
     * @param index
     * @param tdl
     * @param action
     *            : param telling whether to increase or decrease the medication
     *            index
     */
    StringPair expandMedication(String fst, String snd, String line,
	    PhrasePair pair, Entry<PhrasePair, String> index,
	    StringPair updatedMed, Operation action, String futureLine) {
	String med = UtilMethods.mergeStrings(fst, snd).toLowerCase();

	// first check whether the medication ends with "PO"
	if (med.toLowerCase().trim().endsWith(" po"))
	    return null;

	// check whether there is a set of brackets after the given medication
	try {
	    StringNumberPair medBracketed = UtilMethods
		    .checkBrackets(line, med);
	    if (medBracketed != null) {
		med = medBracketed.getString();
		pair.indexes.setEndOffset(pair.indexes.getEndOffset()
			+ medBracketed.getNumber());
	    }
	}
	catch (Exception e) {
	    System.out.println("ERROR");
	}

	if (line.toLowerCase().contains(med)) {
	    // check whether there is a punctuation sign attached to the med
	    med = checkCharSurroundingMed(med, line.toLowerCase());

	    switch (action) {
	    case ADD:
		pair.indexes.setEndOffset(pair.indexes.getEndOffset() + 1);
		break;
	    case REMOVE:
		pair.indexes.setStartOffset(pair.indexes.getStartOffset() - 1);
		break;
	    }

	    med = med.trim();

//	    emr.medicationTextIndexes.remove(index);
//	    emr.medicationTextIndexes.put(pair, med);

	    // System.out.println(line + " >>> " + med);

	    return new StringPair(med, null);
	}
	else {
	    // check whether the medication is included in a coordinate
// structure
	    Entry<PhrasePair, String> coordinateCheck = this.checkCoordinate(
		    line, fst, snd, med, pair);

	    if (coordinateCheck != null) {
		pair.indexes.setStartOffset(coordinateCheck.getKey().indexes
			.getStartOffset());
		pair.indexes.setEndOffset(coordinateCheck.getKey().indexes
			.getEndOffset());
//		emr.medicationTextIndexes.remove(index);
//		emr.medicationTextIndexes.put(pair, coordinateCheck.getValue());

		// System.out.println(line + " >>> " +
		// coordinateCheck.getValue());

		return new StringPair(coordinateCheck.getValue(), null);
	    }
	    else {
		// check whether the medication is located at a couple words
// span
		// from the current subordinate
		StringPair gapCheck = this.containsGap(line, fst, snd.trim(),
			pair, med, updatedMed);

		if (gapCheck != null) {

		    gapCheck.el1 = med;
		    updatedMed = gapCheck;
		    boolean found = true;

		    for (String key : gapCheck.wordsMap.keySet())
			if (gapCheck.wordsMap.get(key) == false)
			    found = false;

		    if (found) {
			med = "";
			String[] splitLine = line.split(" ");
			for (int range = updatedMed.startIndex; range <= updatedMed.endIndex; range++) {
			    med = UtilMethods.mergeStrings(med,
				    splitLine[range]);
			}
			med = med.toLowerCase().trim();

			pair.indexes.setStartOffset(updatedMed.startIndex);
			pair.indexes.setEndOffset(updatedMed.endIndex);
//			emr.medicationTextIndexes.remove(index);
//			emr.medicationTextIndexes.put(pair, med);

			// we have stored the medication so we clear the updated
// med
			updatedMed.el1 = null;
			updatedMed.wordsMap.clear();
		    }

		    return updatedMed;
		}
		else {
		    // check whether the medication was spanning on two lines
		    if (UtilMethods.checkStringOverlap(line, med)
			    && UtilMethods.checkStringOverlap(med, futureLine)) {
			int overlap = UtilMethods.countStringOverlap(med,
				futureLine);
			pair.indexes
			.setEndLine(pair.indexes.getStartLine() + 1);
			pair.indexes.setEndOffset(overlap);
//			emr.medicationTextIndexes.remove(index);
//			emr.medicationTextIndexes.put(pair, med);

			return new StringPair(med, null);
		    }
		    else {
			// check to see whether we had the incorrect merging
			// for the medication
			if (line.toLowerCase().contains(
				UtilMethods.mergeStrings(snd, fst)
				.toLowerCase())) {
			    return expandMedication(snd, fst, line, pair,
				    index, updatedMed, Operation.REMOVE,
				    futureLine);
			}
		    }
		}
	    }
	}

	return null;
    }

    StringPair containsGap(String line, String fst, String snd,
	    PhrasePair pair, String med, StringPair updatedMed) {

	try {
	    String[] splitLine = line.trim().split(" ");

	    for (int i = 0; i < splitLine.length - 1; i++) {
		if (splitLine[i].toLowerCase().equals(fst)) {
		    boolean found = false;
		    int j = -1;
		    String[] splitSnd = snd.split(" ");

		    for (j = i + 1; j < splitLine.length; j++)
			for (int sndIndex = 0; sndIndex < splitSnd.length; sndIndex++) {
			    if (splitSnd[sndIndex].equals(splitLine[j]
				    .toLowerCase())) {
				found = true;
				break;
			    }

			    if (found)
				break;
			}

		    if (found) {
			if (updatedMed == null || updatedMed.wordsMap == null) {
			    updatedMed = new StringPair(null, null);
			    updatedMed.wordsMap = new HashMap<String, Boolean>();
			    updatedMed.setIndexes(i, j);
			}
			else {
			    if (updatedMed.endIndex < j)
				updatedMed.endIndex = j;
			    if (updatedMed.startIndex > i)
				updatedMed.startIndex = i;

			}

			updatedMed.wordsMap.put(splitLine[i], true);
			for (int range = i; range <= j; range++)
			    if (!updatedMed.wordsMap
				    .containsKey(splitLine[range]))
				updatedMed.wordsMap
				.put(splitLine[range], false);

			updatedMed.wordsMap.put(splitLine[j], true);

			return updatedMed;

		    }
		}
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	}

	return null;
    }

    Entry<PhrasePair, String> checkCoordinate(String line, String fst,
	    String snd, String med, PhrasePair pair) {
	if (line.toLowerCase().indexOf(fst) == -1)
	    return null;

	String inputStr = line.toLowerCase().substring(
		line.toLowerCase().indexOf(fst));

	String[] splitStr = inputStr.split(" ");

	for (int i = 0; i < splitStr.length - 1; i++) {
	    if (splitStr[i].equals(fst)) {
		int j = i + 1;
		boolean found = false;

		for (j = i + 1; j < splitStr.length; j++) {
		    if (splitStr[j].equals(snd)) {
			found = true;
			break;
		    }
		}
		if (!found)
		    return null;
		else
		    found = false;

		for (int k = i + 1; k < j - 1; k += 1)
		    if (splitStr[k].equals("and")) {
			found = true;
			break;
		    }

		if (found) {
		    med = "";
		    for (int k = i; k <= j; k++)
			med = UtilMethods.mergeStrings(med, splitStr[k]);

		    pair.indexes.setEndOffset(pair.indexes.getEndOffset() + j
			    - i);
		    med = med.trim();

		    Entry<PhrasePair, String> newEntry = new AbstractMap.SimpleEntry<PhrasePair, String>(
			    pair, med);
		    return newEntry;
		}
	    }
	}

	return null;
    }

}
