/**
 * Created on Jan 10, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package corpus;

import io.importer.DataImport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import corpus.Annotation.AnnotationType;
import corpus.Relation.RelationType;
import preprocess.parser.ImportSyntacticTags;
import preprocess.parser.SentenceContent;
import preprocess.parser.WordTagMap;

import classifier.TrainTestCreator.CorpusType;
import classifier.Utils;
import classifier.vector.VectorCreator.TagType;

import utils.PhrasePair;
import utils.StringNumberPair;
import utils.StringPair;
import utils.UtilMethods;

/**
 * @author ab
 *
 */
public class MedicalRecord {
	/**
	 * some of the file paths
	 */
	public String rawFilePath;
	public String parsedDataPath;
	public String annotatedFilePath;
	public String xmlFilePath;

	/**
	 * the record named entities
	 */
	public ArrayList<Annotation> concepts;
	public ArrayList<Relation> relations;
	public ArrayList<AnnotationDetail> conceptDetails;

	public HashMap<String, TagType> conceptOffsets ;
	public HashMap<Integer, AnnotationDetail> annotationAtWord;


	public  HashMap<String,String> phonemes;
	public HashMap<AnnotationDetail, Integer> sortedConcepts;
	public HashMap<Integer, Integer> documentWordCountPerLine;
	public HashMap<Integer, String> documentOffsetAtWordCount;
	public HashMap<Integer, Integer> processed_documentWordCountPerLine;

	ImportSyntacticTags importTags;
	public Sectionizer sectionizer;

	public ArrayList<String> rawFileLines;
	public ArrayList<String> words;
	public HashMap<Integer, Integer> wordUntilLine;
	public ArrayList<String> medicationStrings;

	public ArrayList<SentenceContent> sentences;
	String sectionsPath;
	public ArrayList<Section> sections;

	public CorpusType corpusType;

	public static List<String> medsSemType = 
			Arrays.asList("phsu", "clnd", "antb", "orch", "strd", "bacs");
	public static List<String> reasonSemType = 
			Arrays.asList("dysn", "sosy", "fndg", "patf", "inpr");

	public MedicalRecord(String rawFile, String annotatedFile, 
			String relationsFile,
			ImportSyntacticTags tagsImporter, Sectionizer section,
			String parsedFilePath,
			String phonemesPath,
			String sentencesPath,
			HashMap<String, ArrayList<String>> conceptSemanticTypes,
			CorpusType givenCorpusType) {
		this.rawFilePath = rawFile;
		this.annotatedFilePath = annotatedFile;
		this.parsedDataPath = parsedFilePath;
		this.importTags = tagsImporter;
		this.sectionizer = section;
		this.corpusType = givenCorpusType;

		// read the raw record lines and the words in the record
		this.rawFileLines = DataImport.readFile(rawFile);
		this.sentences = DataImport.readSentences(sentencesPath);
		
		loadWords();

		// load the concepts
		this.concepts = DataImport.readAnnotations(annotatedFile, corpusType);
		loadConceptOffsets(); // get the document offset for each concept
		loadAttributes(); // load the individual named entities
		sortedConcepts = Utils.sortConcepts(this.conceptDetails, rawFileLines);

		// load the relations
		loadRelations(relationsFile);
		loadRelationsForConcepts();

		// load the parsed content
		importParsedContent(corpusType);

		// load the sentences and the sections
		sentences = loadSentenceConcepts(conceptSemanticTypes);
//		sections = this.sectionizer.importSections(rawFilePath, 
//				sectionsFileName);

		// load the concept offsets
		conceptOffsets = new HashMap<String, TagType>();
		this.loadConceptsOffsets();

		// load the phonemes
		phonemes = loadPhonemes(phonemesPath);

		this.documentWordCountPerLine = new HashMap<Integer, Integer>();
		this.processed_documentWordCountPerLine = new HashMap<Integer, Integer>();
		this.documentOffsetAtWordCount = new HashMap<Integer, String>();
		computeWordPerLine();
		// import the UMLS data
		loadUMLS();
	}

	private HashMap<String, String> loadPhonemes(String phonemesPath) {
		HashMap<String, String>  phonemesPerWord = new HashMap<String, String>();

		ArrayList<String> phonemelist = DataImport.readFile(phonemesPath);

		int wordCount = 0;
		int index = 0;
		String word = null;
		String phoneme;

		while(index < phonemelist.size()){
			phoneme = phonemelist.get(index).trim();
			try{
				word = this.words.get(wordCount).toLowerCase();
			}catch(Exception e){
				//				e.printStackTrace();
				System.out.println("ERROR parsing the phonemes");
			}

			if(phoneme.trim().isEmpty() || UtilMethods.isPunctuation(word))
				phoneme = "punct";
			if(UtilMethods.containsNumber(word))
				phoneme = "number";

			phonemesPerWord.put(word, phoneme);

			wordCount ++;

			//check whether we need to escape some of the phonemes
			index = index + escapePhonemes(word, phonemelist, index);

			index ++;
		}

		return phonemesPerWord;
	}

	private int escapePhonemes(String word, ArrayList<String> phonemelist, 
			int currentIndex) {
		int escapeCount = 0;

		if(!UtilMethods.isPunctuation(word)){
			if (UtilMethods.endsWithPunctuation(word)){
				if (! word.endsWith("+"))
					if(currentIndex+1 < phonemelist.size() && 
							phonemelist.get(currentIndex+1).trim().isEmpty())
						escapeCount ++;
			}else{
				// check special cases
				if(word.contains("..."))
					escapeCount ++;		

				// check for characters inside the word
				boolean prevQuestionMark = false;
				int size = word.length();

				for(int index = 0; index < size; index ++){
					if(word.charAt(index) == '?'){
						if(!prevQuestionMark){
							escapeCount ++;
							prevQuestionMark = true;
						}
					}else{
						prevQuestionMark = false;
						// check for ";'"
						if(word.charAt(index) == ';' && index +1 < size && 
								word.charAt(index +1) == '\'')
							escapeCount ++;	
					}
				}
			}
		}								

		return escapeCount;
	}

	public void loadWords(){
		ArrayList<String> lines = DataImport.readFile(this.rawFilePath);
		this.words = new ArrayList<String>();
		this.wordUntilLine = new HashMap<Integer, Integer>();

		int wordCount = 0;

		for(int indexLine = 0; indexLine < lines.size(); indexLine ++){
			String line = lines.get(indexLine);
			this.wordUntilLine.put(indexLine +1, wordCount);

			String[] splitLine = StringUtils.split( line);

			for(int index = 0; index< splitLine.length; index ++){
				String word = splitLine[index].trim();

				if(!word.isEmpty()){
					words.add(word);
					wordCount ++;
				}
			}
		}
	}

	public MedicalRecord(String rawFile, String annotatedFile) {
		this.rawFilePath = rawFile;
		this.annotatedFilePath = annotatedFile;

		this.rawFileLines = DataImport.readFile(rawFile);

		// load the concepts
		this.concepts = DataImport.readAnnotations(annotatedFile, CorpusType.I2B2);
		loadAttributes(); // load the individual named entities
		sortedConcepts = Utils.sortConcepts(conceptDetails, rawFileLines);


	}

	private void loadAttributes(){
		this.conceptDetails = new ArrayList<AnnotationDetail>();
		
		for(Annotation annt : concepts){
			for(AnnotationType key : annt.annotationElements.keySet()){
				AnnotationDetail detail = annt.annotationElements.get(key);

				if(detail != null && !detail.content.equals("nm")){
					conceptDetails.add(detail);
				}
			} 
		}
	}

	void loadConceptOffsets(){
		for( int index = 0; index < this.concepts.size(); index ++){
			Annotation annt = this.concepts.get(index);

			for(AnnotationType type : annt.annotationElements.keySet()){
				AnnotationDetail detail = annt.annotationElements.get(type);

				if(detail != null && !detail.content.equalsIgnoreCase("nm"))
					detail = getOffsetsInDocument(detail);

				annt.annotationElements.put(type, detail);
			}

			this.concepts.set(index, annt);
		}
	}

	public AnnotationDetail getOffsetsInDocument(AnnotationDetail annt){
		//get the offset until the line of the annotation
		int wordCount = this.wordUntilLine.get(annt.startLine);

		// get the words on the currentLine
		for(int index = 0; index < annt.startOffset; index ++){
			wordCount ++;
		}

		// update the document offsets
		annt.documentOffsetStart = wordCount + 1;
		annt.documentOffsetEnd = wordCount + StringUtils.split(annt.content).length;

		return annt;
	}

	private void loadRelations(String relationsFile){
		this.relations = new ArrayList<Relation>();

		switch(this.corpusType){
		case I2B2:
			importI2B2Relations();
			break;
		case I2B2RELATION:
			this.relations = DataImport.readRelations(relationsFile, 
					corpusType);
		case ACE:
			break;
		}
	}

	private void loadRelationsForConcepts(){
		switch(this.corpusType){
		case I2B2RELATION:
			HashMap<String, AnnotationDetail> anntByOffsets = new HashMap<String, AnnotationDetail>();

			for(int index = 0; index < this.conceptDetails.size(); index ++ ){
				AnnotationDetail annt = this.conceptDetails.get(index);
				anntByOffsets.put(annt.mergeOffsets(), annt);
			}

			for(Relation rel : this.relations){
				AnnotationDetail to = anntByOffsets.get(rel.to.mergeOffsets());
				AnnotationDetail from = anntByOffsets.get(rel.from.mergeOffsets());

				ArrayList<AnnotationDetail> relatedTo = new ArrayList<AnnotationDetail>();
				if(to.relatedTo.get(rel.relType) != null)
					relatedTo = to.relatedTo.get(rel.relType);

				relatedTo.add(from);
				to.relatedTo.put(rel.relType, relatedTo);

				relatedTo = new ArrayList<AnnotationDetail>();
				if(from.relatedTo.get(rel.relType) != null)
					relatedTo = from.relatedTo.get(rel.relType);

				relatedTo.add(to);
				from.relatedTo.put(rel.relType, relatedTo);

				anntByOffsets.put(rel.to.mergeOffsets(), to);
				anntByOffsets.put(rel.from.mergeOffsets(), from);
			}

			this.conceptDetails.clear();

			for(AnnotationDetail annt : anntByOffsets.values())
				this.conceptDetails.add(annt);

			break;
		default:
			break;
		}
	}

	void importI2B2Relations(){
		for(int index = 0; index < this.concepts.size(); index ++){
			Annotation annt = this.concepts.get(index);

			AnnotationDetail det1 = annt.annotationElements.get(AnnotationType.R);
			AnnotationDetail det2 = annt.annotationElements.get(AnnotationType.M);

			if(det1 != null && det2 != null && !det1.content.equals("nm")){
				ArrayList<AnnotationDetail> rels = det1.relatedTo.get(RelationType.REASON);
				if(rels == null)
					rels = new ArrayList<AnnotationDetail>();

				rels.add(det2);
				det1.relatedTo.put(RelationType.REASON, rels);

				rels = det2.relatedTo.get(RelationType.REASON);
				if(rels == null)
					rels = new ArrayList<AnnotationDetail>();

				rels.add(det1);
				det2.relatedTo.put(RelationType.REASON, rels);

				this.relations.add(new Relation(det2, det1,
						RelationType.REASON));

				// update the annotation and the concepts
				annt.annotationElements.put(AnnotationType.R, det1);
				annt.annotationElements.put(AnnotationType.M, det2);
				this.concepts.set(index, annt);
			}


		}
	}

	void loadConceptsOffsets(){
		annotationAtWord = new HashMap<Integer, AnnotationDetail>();

		for(AnnotationDetail concept : this.conceptDetails){ 
			for(int index = concept.documentOffsetStart; 
					index <= concept.documentOffsetEnd; index++)
				annotationAtWord.put(index, concept);

			if(concept.startLine == concept.endLine){
				int countIndexes = 0;

				for(int index = concept.startOffset; index <= 
						concept.endOffset; index ++){
					String offset = UtilMethods.mergeOffsets(concept.startLine, 
							index);

					TagType tag = TagType.chooseTagType(concept.type, countIndexes);

					conceptOffsets.put(offset, tag);
					countIndexes++;
				}
			}else{
				String rawLine = rawFileLines.get(concept.startLine -1);
				int end = rawLine.split(" ").length-1;
				int countIndexes = 0;

				for(int index = concept.startOffset; index <= 
						end; index ++){
					String offset = UtilMethods.mergeOffsets(concept.startLine, 
							index);
					TagType tag = TagType.chooseTagType(concept.type, countIndexes);

					conceptOffsets.put(offset, tag);
					countIndexes ++;
				}

				for(int index = 0; index <= 
						concept.endOffset; index ++){
					String offset = UtilMethods.mergeOffsets(concept.endLine, 
							index);
					TagType tag = TagType.chooseTagType(concept.type, countIndexes);
					conceptOffsets.put(offset, tag);
				}
			}
		}

	}

	/**
	 * Import the content of the parsed file. 
	 */
	void importParsedContent(CorpusType corpusType) {

		// reset the values
		importTags.init(parsedDataPath,  
				rawFilePath, corpusType);
		importTags.execute();
	}

	public ArrayList<SentenceContent> loadSentenceConcepts(
			HashMap<String, ArrayList<String>> conceptSemanticTypes) {
		if(sentences.isEmpty())
			return null;

		// find the system medication and reason concepts
		for(int index =0; index < conceptDetails.size(); index ++){
			AnnotationDetail annt = conceptDetails.get(index);

			// do not add nm reasons
			if(annt != null && ! annt.content.equals("nm")){
				// get the sentence
				int sentenceOffset = SentenceContent.getSentenceForAnnotation(
						sentences, annt);
				if(sentenceOffset == -1){
					System.out.println("Could not find sentence for " + annt.content);
					continue;
				}

				SentenceContent sentence = sentences.get(sentenceOffset);
				sentence = SentenceContent.loadConcept(annt, 
						sentence, conceptSemanticTypes, 
						annt.isList);
				sentences.set(sentenceOffset, sentence);  
				annt.sentence = sentence;

			}
			
			conceptDetails.set(index, annt);
		}

		return sentences;
	}



	void computeWordPerLine(){
		int count = 0;
		int words = 0;
		int processedWords = 0;
		
		for(int lineCount = 0; lineCount < this.rawFileLines.size(); lineCount ++){
			String line = this.rawFileLines.get(lineCount);
			String[] splitLine = line.split(" ");
			int lineLength = splitLine.length;
			
			if(line.trim().isEmpty()) {
				this.documentWordCountPerLine.put(count, words);
				this.processed_documentWordCountPerLine.put(count, processedWords);
				count ++;
				continue;
			}
			
			int currentLineLength = 0;
			for(int index = 0; index < lineLength; index ++){
				String currentWord = splitLine[index];
				if(currentWord.isEmpty()) continue; // skip the empty words

				this.documentOffsetAtWordCount.put(words+currentLineLength,
						UtilMethods.mergeOffsets(lineCount+1, index));
				currentLineLength ++;
			}
			
			words += currentLineLength;

			String processedLine = UtilMethods.sentenceProcessTagger(line);
			processedWords += processedLine.split(" ").length;

			this.documentWordCountPerLine.put(count, words);
			this.processed_documentWordCountPerLine.put(count, processedWords);

			count++;
		}
	}

	/**
	 * @return the xmlFilePath
	 */
	public String getXmlFilePath() {
		return xmlFilePath;
	}

	/**
	 * @param xmlFilePath the xmlFilePath to set
	 */
	public void setXmlFilePath(String xmlFilePath) {
		this.xmlFilePath = xmlFilePath;
	}

	/**
	 * Get the text indexes where we find the medications 	
	 * previously identified
	 * 
	 * @param neList
	 * @return the line indexes
	 */
	public HashMap<Integer, ArrayList<PhrasePair>> 
	getIndexesFromList(HashMap<StringPair, ArrayList<PhrasePair>> neList) {
		HashMap<Integer, ArrayList<PhrasePair>> lineIndexes = 
				new HashMap<Integer, ArrayList<PhrasePair>>();

		for(StringPair key : neList.keySet()) {
			// find the key inside the raw file lines
			int counter = 1;
			int indexFoundLine = -1;
			int phraseDistance = 10000;
			String toSearch = key.el1.trim().toLowerCase();
			String prevPhraseLine = key.el2.trim().toLowerCase();
			prevPhraseLine = prevPhraseLine.trim().toLowerCase();
			String prevLine = "";

			ArrayList<Integer> possibleIndexes = new ArrayList<Integer>();


			for (String line : this.rawFileLines) {	

				if (line.toLowerCase().contains(toSearch) 
						&& 
						(line.toLowerCase().contains(prevPhraseLine) || 
								prevLine.contains(prevPhraseLine)) 
						) {
					if (indexFoundLine == -1) {
						indexFoundLine = counter;
						phraseDistance = line.length();
					}
					else {
						if (identicalLines(line, toSearch)) {
							possibleIndexes.add(indexFoundLine);
							possibleIndexes.add(counter);
							indexFoundLine = counter;
						}else
							if (line.length() < phraseDistance) {
								indexFoundLine = counter;
								phraseDistance = line.length();
							}
					}

				}
				counter ++;
				prevLine = line.toLowerCase();
			}

			if (indexFoundLine!= -1) {
				if (!possibleIndexes.contains(indexFoundLine))
					possibleIndexes.add(indexFoundLine);

				if (!possibleIndexes.isEmpty()) {
					for (Integer index: possibleIndexes) 
						if (lineIndexes.containsKey(index)) {
							ArrayList<PhrasePair> prevPhrase = 
									lineIndexes.get(index);
							prevPhrase.addAll(neList.get(key));
							lineIndexes.put(index, prevPhrase);

						}else lineIndexes.put(index, neList.get(key));
				}

			}
		}

		return lineIndexes;

	}

	boolean identicalLines(String newLine, String line) {
		if (newLine.trim().length() == 
				line.trim().length()) return true;

		// check the case when the new line contains the 
		// list start number
		String[] splitLine = newLine.split(" ");

		if(splitLine.length > 1) {
			String word = splitLine[0];

			if(word.endsWith(".")) word = word.replace(".", "");

			return UtilMethods.isNumber(word.trim());
		}

		return false;

	}

	/**
	 * Get the indexes of the medications 
	 * @return 
	 */
	public HashMap<PhrasePair, String> getOffsetIndexes(
			HashMap<Integer, ArrayList<PhrasePair>> lineIndexes) {
		HashMap<String, String> foundIndexes = new HashMap<String, String>();
		HashMap<String, Indexes> storedIndexes = new HashMap<String, Indexes>();
		HashMap<PhrasePair, String> offsetIndexes = 
				new HashMap<PhrasePair, String>();

		for (Entry<Integer, ArrayList<PhrasePair>>  entry :  
			lineIndexes.entrySet()) {

			String textLine = this.rawFileLines.get(entry.getKey() -1);

			for (PhrasePair phrase : entry.getValue()) {
				int tokenCount = 0;
				int start = -1;
				int end = -1;

				String medication = phrase.candidates.get(0);
				//			MedicationList.chooseMedFromList(phrase.candidates, 
				//				textLine);

				String tempIndex = null;

				// check if medication is empty 
				if (medication.isEmpty()) {
					lineIndexes.remove(entry);
					continue;
				}

				StringTokenizer tokenizer = new StringTokenizer(textLine, " ");

				// we are looking for the first occurrence of a medication 
				// inside the given text line
				while(tokenizer.hasMoreTokens()) {
					String token = ((String) tokenizer.nextElement()).trim();

					if (!token.isEmpty()) {
						if (medication.toLowerCase().startsWith(
								token.toLowerCase()) && start == -1) 
							if	(medication.length() == token.length() || 
							medication.contains(" "))
								start = tokenCount;

						if (medication.toLowerCase().endsWith(
								token.toLowerCase())
								&& 
								(medication.length() == token.length() || 
								medication.contains(" ")) && end == -1)
							end = tokenCount;


						tokenCount += 1;
					}
				}

				int startLine = entry.getKey();
				int endLine = entry.getKey();

				// take care of the case when the medication starts on the previous line
				if(start == -1) {
					String prevLine = this.rawFileLines.get(entry.getKey() -2);
					start = prevLine.split(" ").length - 
							medication.split(" ").length + end + 1;
					startLine = startLine -1;
				}

				// if either the start or the end offsets are -1 we 
				// move to process the next medication
				if(start == -1 || end == -1) continue;

				int offset = 0;

				// check if there was a set of brackets inside the given input
				StringNumberPair medWithBrackets = 
						UtilMethods.checkBrackets(textLine, medication);

				if (medWithBrackets != null) {
					medication = medWithBrackets.getString();
					end = end + medWithBrackets.getNumber() ;
					offset = medWithBrackets.getNumber()  ;
					tempIndex = this.mergeIndexes(entry.getKey(), end - 1);
				}

				// add the medication and its indexes

				// check if the indexes have been already used
				String mergedStartIndex = this.mergeIndexes(startLine, start);
				String mergedEndIndex = this.mergeIndexes(endLine, end);
				String storedVal = null;
				Indexes location = null;

				if (foundIndexes.containsKey(mergedStartIndex) ) {
					storedVal = mergedStartIndex;
					location = storedIndexes.get(mergedStartIndex);
				}

				if(foundIndexes.containsKey(mergedEndIndex)) {
					storedVal = mergedEndIndex;
					location = storedIndexes.get(mergedEndIndex);
				}

				if (tempIndex != null ) 
				{
					for (int decrease = 1; decrease <= offset; decrease ++) {
						String mergedEnd = this.mergeIndexes(endLine, end - decrease);
						if (foundIndexes.containsKey(mergedEnd)) {
							storedVal = mergedEnd;
							location = storedIndexes.get(mergedEnd);
						}
					}
				}

				if (storedVal != null) {
					// we want to store the maximum span for given medication
					if (foundIndexes.get(storedVal).length() < medication.length()) {
						foundIndexes.put(mergedEndIndex, medication);
						foundIndexes.put(mergedStartIndex, medication);

						offsetIndexes.remove(location);
						PhrasePair newPhrase = 
								this.addPhrase(startLine, endLine, start, end, phrase);
						offsetIndexes.put(newPhrase, medication);
						Indexes indexes = newPhrase.indexes;

						// check if the same medication is encountered twice in the given line
						if (textLine.lastIndexOf(medication) != textLine.indexOf(medication)) {
							String subs = textLine.substring(textLine.indexOf(medication), 
									textLine.lastIndexOf(medication));
							int wordCounts = 1 + subs.replaceAll("[^\\s]", "").length();

							offsetIndexes.remove(location);
							newPhrase = this.addPhrase(startLine, endLine, 
									start + wordCounts, 
									end + wordCounts, phrase);
							offsetIndexes.put(newPhrase, medication);
							indexes = newPhrase.indexes;
						}

						// now update the found indexes and the location
						foundIndexes.put(storedVal, medication);
						storedIndexes.put(storedVal, indexes);

					}
				}else {

					PhrasePair newPhrase = 
							this.addPhrase(startLine, endLine, start, end, phrase);
					offsetIndexes.put(newPhrase, medication);
					Indexes indexes = newPhrase.indexes;

					// check if the same medication is encountered twice in the given line
					if (textLine.toLowerCase().lastIndexOf(medication + " ") != 
							textLine.toLowerCase().indexOf(medication + " ")) {
						String subs = 
								textLine.toLowerCase().substring(
										textLine.toLowerCase().indexOf(medication), 
										textLine.toLowerCase().lastIndexOf(medication));
						int wordCounts = subs.replaceAll("[^\\s]", "").length();

						newPhrase = this.addPhrase(startLine, endLine, 
								start +wordCounts, end + wordCounts, phrase);
						offsetIndexes.put(newPhrase, medication);
						indexes = newPhrase.indexes;

					}

					// now update the found indexes and the location
					foundIndexes.put(mergeIndexes(entry.getKey(), start), medication);
					foundIndexes.put(mergeIndexes(entry.getKey(), end), medication);
					storedIndexes.put(mergeIndexes(entry.getKey(), start), indexes);
					storedIndexes.put(mergeIndexes(entry.getKey(), end), indexes);

					if (tempIndex != null) {
						for (int decrease = 1; decrease <= offset; decrease ++) {
							String mergedMed = this.mergeIndexes(entry.getKey(), end -decrease);
							foundIndexes.put(mergedMed, medication);
							storedIndexes.put(mergedMed, indexes);	
						}

					}
				}
			}

		}

		return offsetIndexes;
	}

	/**
	 * Add a new medication entry to the list of medications
	 * construct the medication from the given params
	 * @param key
	 * @param start
	 * @param end
	 * @param phrase
	 */
	private PhrasePair addPhrase(int startLine, int endLine, int start, int end,
			PhrasePair phrase) {

		PhrasePair newPhrase = 
				new PhrasePair(phrase.line, phrase.prevLine, phrase.isList);
		newPhrase.indexes.setEndLine(endLine);
		newPhrase.indexes.setStartLine(startLine);
		newPhrase.indexes.setEndOffset(end);
		newPhrase.indexes.setStartOffset(start);

		return newPhrase;
	}

	/**
	 * Merge the given integer values into an underscore-delimited string
	 * @param val1
	 * @param val2
	 * @return
	 */
	String mergeIndexes (int val1, int val2) {
		return String.valueOf(val1) + "_" + String.valueOf(val2);
	}

	/**
	 * Go through the elements of the neList and identify whether certain
	 * elements are overlapping in text offsets
	 * We only keep the largest spanning element in case of found overlapping
	 * @param neList
	 * @return
	 */
	public HashMap<PhrasePair, String> removeOverlappingNE(
			HashMap<PhrasePair, String> neList) {
		HashMap<PhrasePair, String> updatedList = 
				new HashMap<PhrasePair, String>();
		HashMap<String, ArrayList<Entry<PhrasePair, String>>> visitedIndexes = 
				new HashMap<String, ArrayList<Entry<PhrasePair, String>>>();

		// go through the list and find the overlaps
		for( Entry<PhrasePair, String> entry : neList.entrySet()) {
			PhrasePair value = entry.getKey();
			boolean listChecked = false;

			for (int range = value.indexes.getStartOffset();
					range <= value.indexes.getEndOffset(); range ++) {
				listChecked = true;

				String mergedRange = mergeIndexes(value.indexes.getStartLine(), 
						range);

				if(visitedIndexes.containsKey(mergedRange)) {
					ArrayList<Entry<PhrasePair, String>> lst = 
							visitedIndexes.get(mergedRange);
					lst.add(entry);
					visitedIndexes.put(mergedRange, lst);
				}else {
					ArrayList<Entry<PhrasePair, String>> lst = new
							ArrayList<Entry<PhrasePair, String>>();
					lst.add(entry);
					visitedIndexes.put(mergedRange, lst);
				}
			}

			if (!listChecked)
				updatedList.put(entry.getKey(), entry.getValue());
		}

		// select the largest spanning element in case of overlap
		for(Entry<String, ArrayList<Entry<PhrasePair, String>>> entry : 
			visitedIndexes.entrySet()) {

			ArrayList<Entry<PhrasePair, String>> values = entry.getValue();
			Entry<PhrasePair, String> maxEntry = null;

			for (Entry<PhrasePair, String> entity: values) {
				if(maxEntry == null) maxEntry = entity;
				else if (maxEntry.getValue().length() < 
						entity.getValue().length()) {
					System.out.println("Removing : " + maxEntry.getKey().line);
					maxEntry = entity;
				}
			}

			updatedList.put(maxEntry.getKey(), maxEntry.getValue());
		}

		return updatedList;
	}

	AnnotationDetail getAnnotationOffset(AnnotationDetail annotation) {
		if(this.processed_documentWordCountPerLine == null)
			return annotation;

		if(annotation.startLine <= 2)
			return annotation;

		int wordCounts = this.processed_documentWordCountPerLine.get(
				annotation.startLine-2);
		String[] sentence = this.rawFileLines.get(annotation.startLine-1).split(" ");

		for(int index = 0; index < annotation.startOffset ; index ++){
			String[] words = UtilMethods.sentenceProcessTagger(sentence[index]).split(" ");
			wordCounts += words.length;			
		}

		annotation.offsetDoc = wordCounts;

		return annotation;
	}
	
	void loadUMLS(){
		String parentFolder = new File( new File(this.rawFilePath).getParent()).getParent();
		String fileName = new File(this.rawFilePath).getName();
		String umlsPath = parentFolder + "/umls/" + fileName;
		
		ArrayList<String> entities = DataImport.readFile(umlsPath);
		
		//process the nes
		for(String entity : entities){
			if(StringUtils.chomp(entity).trim().isEmpty()) continue;
			
			String[] splitEntity = entity.split(" ");
			int sentenceIndex = Integer.parseInt(splitEntity[0]);
			SentenceContent sentence = this.sentences.get(sentenceIndex-1);
			int startOffset = Integer.parseInt(splitEntity[3]);
			int endOffset = Integer.parseInt(splitEntity[4]);
			String semanticType = splitEntity[2];
			
			int wordCount = 0;
			int entitySpan = 0;
			for(int index = 0; index < sentence.sentence.length(); index ++){
				if(Character.isWhitespace(sentence.sentence.charAt(index)))
					wordCount ++;
				
				if(index >= startOffset && 
						Character.isWhitespace(sentence.sentence.charAt(index)))
					entitySpan ++ ;
				
				if(index == endOffset + 1) break;
			}
					
			if(entitySpan == 0) entitySpan = 1;
			
			if(wordCount > 0 && wordCount != sentence.tags.size() -1 )
				wordCount = wordCount -1;
			
			for(int index = wordCount; index > wordCount - entitySpan && 
					index >= 0; index --){
				WordTagMap word = sentence.tags.get(index);
				word.umlsSemanticType = semanticType;
				word.hasMedUMLSType = getMedUMLS(semanticType);
				word.hasReasonUMLSType = getReasonUMLS(semanticType);
				
				sentence.tags.set(index, word);
			}
				
			this.sentences.set(sentenceIndex-1, sentence);

		}
	}
	
	boolean getMedUMLS(String semanticType){
		for(String el : MedicalRecord.medsSemType){
			if(semanticType.contains(el))
				return true;
		}
		
		return false;
	}
	
	boolean getReasonUMLS(String semanticType){
		for(String el : MedicalRecord.reasonSemType){
			if(semanticType.contains(el))
				return true;
		}
		
		return false;
	}

	public void updateAnnotations(ArrayList<AnnotationDetail> annotations) {
		for(AnnotationDetail concept : annotations){ 
			concept = this.getOffsetsInDocument(concept);
			for(int index = concept.documentOffsetStart; 
					index <= concept.documentOffsetEnd; index++)
				annotationAtWord.put(index, concept);
		}
	}

}
