package preprocess.parser;

import io.importer.DataImport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Properties;

import classifier.TrainTestCreator.CorpusType;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;

import org.apache.commons.lang.StringUtils;

import utils.UtilMethods;

public class ImportSyntacticTags {
	final String PHONEME_FILE = ".pho";

	public final static String PARSED_FILE = ".parsed";
	ImportData importer;
	DataImport fileImporter ;
	final String dataDelimiter = "_";

	String parseFileName;
	String rawFilePath;
	String pathModel;

	final String PARSE_MODEL = "english-left3words-distsim.tagger";

	MaxentTagger tagger;

	public ArrayList<SentenceContent> parsedFileContent;
	public ArrayList<SentenceContent> mergedParseTagFileContent;
	public CorpusType corpusType;

	public ImportSyntacticTags(String pathResources,
			CorpusType givenCorpusType){
		pathModel = pathResources + "/" + PARSE_MODEL;
		this.corpusType = givenCorpusType;

		Properties properties = new Properties();
		properties.setProperty("tokenize", "false");
		properties.setProperty("delimiter", dataDelimiter);
		properties.setProperty("model", pathModel);

		TaggerConfig configs = new TaggerConfig(properties);
		try{
			tagger = new MaxentTagger(pathModel, configs);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void init(String parseFilePath, 
			String rawFilePath,
			CorpusType corpusType){
		this.parseFileName = parseFilePath;
		this.rawFilePath = rawFilePath;

		parsedFileContent = new ArrayList<SentenceContent>();
		mergedParseTagFileContent = new ArrayList<SentenceContent>();

		importer = new ImportData();
		fileImporter = new DataImport();
	}

	public void tagFile(String filePath, String outputPath){
		try{
			String outputFileName = outputPath ;

			// check if the directory exists, otherwise create it
			new File(new File(outputFileName).getParent()).mkdir();
			outputFileName = outputPath;

			if (!(new File(outputFileName).exists())){
				BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
				ArrayList<String> rawLines = UtilMethods.readFileLines(filePath);

				for (String line : rawLines){
					String taggedSentence = tagger.tagTokenizedString(line);
					// skip the empty sentences
					if(taggedSentence.trim().equals("_IN"))
						taggedSentence = "";
					out.write(taggedSentence + "\n");
				}

				out.close();
			}

		}catch(Exception e){
			e.printStackTrace();
		}

	}

	public void parseFile(String filePath, String outputPath){
		try{
			String outputFileName = outputPath ;

			// check if the directory exists, otherwise create it
			new File(new File(outputFileName).getParent()).mkdir();
			outputFileName = outputPath;

			if (!(new File(outputFileName).exists())){
				BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
				ArrayList<String> rawLines = UtilMethods.readFileLines(filePath);

				for (String line : rawLines){
					String taggedSentence = tagger.tagTokenizedString(line);
					// skip the empty sentences
					if(taggedSentence.trim().equals("_IN"))
						taggedSentence = "";
					out.write(taggedSentence + "\n");
				}

				out.close();
			}

		}catch(Exception e){
			e.printStackTrace();
		}

	}

	/**
	 * Main method
	 */
	public void execute(){
		//1.  get the raw file lines
		ArrayList<String> rawLines = DataImport.readFile(this.rawFilePath);

		//2. check whether the parseFile was created
		if(!new File(parseFileName).exists())
			parseFile(rawFilePath, new File(parseFileName).getAbsolutePath());

		String parseFile = new File(parseFileName).toString();
		System.out.println("Importing parse file " + rawFilePath);

		this.parsedFileContent = importParseFile(parseFile, rawLines);

		if(parsedFileContent == null)
			this.parsedFileContent = new ArrayList<SentenceContent>();
	}

	private ArrayList<SentenceContent> importParseFile(String taggedFile, 
			ArrayList<String> rawLines) {
		try {
			ArrayList<String> lines = DataImport.readFile(taggedFile);
			ArrayList<WordInSentence> wordsInRawLines = readWordsInRawLines(rawLines);

			// now merge the lines into sentences 
			ArrayList<SentenceContent> sentences = new ArrayList<SentenceContent>();
			SentenceContent sentence = new SentenceContent();
			WordInSentence mappedWordToRaw = null;

			int sentenceCount = 1;
			int documentWordCount = 0;

			for(int wordCount = 0; wordCount < lines.size(); wordCount ++){
				String wordContent = lines.get(wordCount);

				if(documentWordCount < wordsInRawLines.size())
					mappedWordToRaw = wordsInRawLines.get(documentWordCount);

				if(StringUtils.chomp(wordContent).isEmpty() ){
					if(!sentence.tags.isEmpty()){
						sentence.lineEndOffset = mappedWordToRaw.wordIndexInsideSentence + 1;
						sentence.documentWordEnd = documentWordCount ;
						sentence.setSentenceOffsets(sentenceCount);
						sentences.add(sentence);

						// create  a new sentence
						sentenceCount ++;
						sentence = new SentenceContent();
						sentence.setSentenceOffsets(sentenceCount);
						sentence.lineStartOffset =  mappedWordToRaw.wordIndexInsideSentence + 1;
						sentence.documentWordStart = documentWordCount + 1;
					}
				}else{
					WordTagMap word = retrieveWordAnnotations(wordContent, 
							documentWordCount + 1, 
							mappedWordToRaw);

					if(!mappedWordToRaw.word.equals(word.word)){
						System.out.println("Mapping between parse and raw file incorrect.");
						retrieveWordAnnotations(wordContent, 
								documentWordCount + 1, 
								mappedWordToRaw);
					}
					
					sentence.tags.add(word);
					sentence.sentence = UtilMethods.mergeStrings(sentence.sentence, word.word);

					documentWordCount ++;
				}
			}


			if(!sentence.sentence.isEmpty()){
				sentence.lineEndOffset = sentence.tags.size();
				sentences.add(sentence);
			}

			return sentences;

			//            return importer.importTaggedFile(lines.getBytes(), rawLines);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private ArrayList<WordInSentence> readWordsInRawLines(ArrayList<String> rawLines) {
		ArrayList<WordInSentence> mappedWords = new ArrayList<WordInSentence>();

		for(int index = 0; index < rawLines.size(); index ++ ){
			String line = rawLines.get(index);

			String[] splitLine = StringUtils.split(line);

			for(int wordIndex = 0; wordIndex < splitLine.length; wordIndex ++){
				if(splitLine[wordIndex].trim().isEmpty())
					continue;

				WordInSentence mapping = new WordInSentence();
				mapping.word = StringUtils.deleteWhitespace(splitLine[wordIndex]);
				mapping.sentenceIndex = index;
				mapping.wordIndexInsideSentence = wordIndex;

				mappedWords.add(mapping);
			}
		}

		return mappedWords;
	}

	private WordTagMap retrieveWordAnnotations(String wordContent, 
			int docWordCount, 
			WordInSentence mappedWordToRaw) {
		String[] splitWord = wordContent.split("\t");

		if(splitWord.length != 8){
			System.err.println("Incorrect word annotation: " + wordContent);
			return null;
		}

		WordTagMap word = new WordTagMap(splitWord[1], splitWord[4]);
		//		word.sentenceOffset = Integer.parseInt(splitWord[0]) - 1;
		word.sentenceOffset = mappedWordToRaw.wordIndexInsideSentence;
		word.baseForm = splitWord[2];
		word.coarseGrainedPOS = splitWord[3];
		word.namedEntity = splitWord[5];
		word.head = splitWord[6];
		word.depRel = splitWord[7];

		word.documentWordOffset = docWordCount;
		word.sentenceNumber = mappedWordToRaw.sentenceIndex + 1;

		return word;
	}

	/**
	 * 
	 * @param sentences
	 * @param rawLines
	 * 
	 * @return 
	 */
	//	 private ArrayList<SentenceContent> findSentenceOffsets(ArrayList<SentenceContent> sentences,
	//			 ArrayList<String> rawLines) {
	//		 LineOffset lastOffset = new LineOffset(0,0);
	//
	//		 for(SentenceContent sentence : sentences){
	//			 sentence  = findOffsets(sentence, lastOffset, rawLines);
	//			 lastOffset = sentence.sentenceEndOffset;
	//		 }
	//
	//		 // go through the sentences one more time and add 1 to the sentence offset
	//		 for(SentenceContent sentence : sentences){
	//			 ArrayList<WordTagMap> updatedTags = new ArrayList<WordTagMap>();
	//
	//			 for(WordTagMap  tag : sentence.tags){
	//				 tag.sentenceLine = tag.sentenceLine + 1;
	//				 updatedTags.add(tag);
	//			 }
	//
	//			 sentence.sentenceOffsetStart.line = sentence.sentenceOffsetStart.line +1;
	//			 sentence.sentenceEndOffset.line = sentence.sentenceEndOffset.line + 1;
	//		 }
	//
	//		 return sentences; 
	//	 }

	/**
	 * Find the sentence offset inside the rawlines
	 * @param sentence
	 * @param lastOffset
	 * @param rawLines
	 * 
	 * @return the sentenceContent
	 */
	//	 private SentenceContent findOffsets(SentenceContent sentenceContent, LineOffset lastOffset, 
	//			 ArrayList<String> rawLines) {
	//
	//		 String sentence = sentenceContent.sentence;
	//
	//		 // if the sentence contains only punctuation marks, we return the same offsets
	//		 if(UtilMethods.removePunctuation(sentence).trim().isEmpty()){
	//			 sentenceContent.sentenceOffsetStart = lastOffset;
	//			 sentenceContent.sentenceEndOffset = lastOffset;
	//
	//			 return sentenceContent;
	//		 }
	//
	//		 // first clear some of chars 
	//		 sentence = clearSentence(sentence);
	//
	//		 String[] splitSentence = sentence.toLowerCase().split(" ");
	//		 LineOffset newOffset = new LineOffset(lastOffset.line, lastOffset.offset);
	//
	//		 int wordCount = 0;
	//		 // we skip one word if the lastOffset is not at the beginning of the file
	//		 boolean skipOneWord = true;
	//		 // we have to update the last offset
	//		 boolean lastOffsetUpdate = true;
	//		 int beginningOffset = lastOffset.offset;
	//
	//		 for(int line = lastOffset.line; line < rawLines.size() &&
	//				 wordCount < splitSentence.length; line ++){
	//
	//			 newOffset.line = line;
	//			 String[] splitLine = rawLines.get(line).toLowerCase().split(" ");
	//			 int lineLength = splitLine.length;
	//
	//			 if(rawLines.get(line).trim().isEmpty()) continue;
	//
	//			 for(int offset = beginningOffset; offset < lineLength && 
	//					 wordCount < splitSentence.length; offset++){
	//				 // check if we have to skip one word
	//				 if(lastOffset.line != 0 && skipOneWord) {
	//					 skipOneWord = false;
	//					 continue;
	//				 }
	//
	//				 if(!skipOneWord && lastOffsetUpdate){
	//					 lastOffset.line = line;
	//					 lastOffset.offset = offset;
	//
	//					 lastOffsetUpdate = false;
	//				 }
	//
	//				 newOffset.offset = offset; 
	//
	//				 String documentWord = cleanDocumentWord(splitLine[offset]);
	//				 String sentenceWord = splitSentence[wordCount];
	//				 int oldWordCount = wordCount;
	//
	//				 if(documentWord.isEmpty() ) continue;
	//				 if(sentenceWord.isEmpty()){
	//					 wordCount ++;
	//					 if(wordCount < splitSentence.length)
	//						 sentenceWord = splitSentence[wordCount];
	//					 else
	//						 continue;
	//				 }
	//
	//				 if(sentenceWord.equals(documentWord)){
	//					 wordCount ++;
	//				 }else {
	//					 if(documentWord.startsWith(sentenceWord)){
	//						 int newCount = processPartialEqual(sentenceWord, 
	//								 documentWord, splitSentence, wordCount);
	//						 if(newCount != wordCount){
	//							 wordCount = newCount;
	//						 }
	//					 }else{
	//						 int newCount = processInsideEqual(sentenceWord, documentWord, 
	//								 splitSentence, wordCount);
	//
	//						 if(newCount != wordCount){
	//							 wordCount = newCount;
	//						 }
	//					 }
	//				 }
	//
	//				 // now update the sentence tag
	//				 if(oldWordCount != wordCount){
	//					 for (int index = oldWordCount; index < wordCount; index++){
	//						 WordTagMap tag = sentenceContent.tags.get(index);
	//						 tag.sentenceLine = line;
	//						 tag.sentenceOffset = offset;
	//						 sentenceContent.tags.set(index, tag);
	//					 }
	//				 }
	//			 }
	//
	//			 // reset the beginning offset
	//			 beginningOffset = 0;
	//		 }
	//
	//		 // we need to add 1 to the last offset line
	//		 sentenceContent.sentenceOffsetStart = lastOffset;
	//		 sentenceContent.sentenceEndOffset =  newOffset;
	//
	//		 return sentenceContent;
	//	 }

	/**
	 * 
	 * @param sentenceWord
	 * @param documentWord
	 * @param splitSentence
	 * @param wordCount
	 * 
	 * @return
	 */
	int processInsideEqual(String sentenceWord, String documentWord,
			String[] splitSentence, int wordCount) {
		if(documentWord.endsWith(sentenceWord)){
			wordCount ++;
			return wordCount;
		}

		// check whether the document word contains a number inside the word
		if(UtilMethods.removeDigits(documentWord).equals(sentenceWord) ||
				UtilMethods.removeDigits(UtilMethods.removePunctuation(documentWord)).equals(sentenceWord)){
			wordCount ++;
			return wordCount;
		}

		if(wordCount +1 < splitSentence.length ){
			if(documentWord.endsWith(splitSentence[wordCount+1]) ||
					documentWord.startsWith(splitSentence[wordCount+1]))
				wordCount = wordCount + 2;
			else{
				int tmpWordCount = wordCount;

				while(tmpWordCount +1 < splitSentence.length){
					String word = splitSentence[tmpWordCount +1];
					if(documentWord.equals(word) ||
							UtilMethods.removeDigits(documentWord).equals(word)){
						wordCount = tmpWordCount +2;
						break;
					}

					tmpWordCount ++;
				}

				if(tmpWordCount+2 != wordCount && ! documentWord.equals(".")){

					// check if the sentence can be ended here
					if(wordCount +1 >= splitSentence.length)
						wordCount ++;
					else if(wordCount +2 >= splitSentence.length)
						wordCount += 2;
					else if(wordCount +3 >= splitSentence.length)
						wordCount += 3;
					else if(wordCount +4 >= splitSentence.length)
						wordCount += 4;
					else 
						System.out.println("error here");
				}
			}
		}else
			wordCount ++;


		return wordCount;
	}

	int processPartialEqual(String sentenceWord, String documentWord,
			String[] splitSentence, int wordCount) {

		wordCount ++;
		if(splitSentence.length > wordCount){
			if(documentWord.endsWith(splitSentence[wordCount]))
				wordCount ++;
			else{
				if(splitSentence.length > wordCount+1){
					if(documentWord.endsWith(splitSentence[wordCount+1]))
						wordCount = wordCount + 2;
					// the case when the word+1 element is a punctuation sign
					else{
						// case when sentenceWords are ?? __ ?? 
						// and documentWords are ??__??
						String merged = sentenceWord + splitSentence[wordCount];
						boolean found = false;

						while(documentWord.startsWith(merged) && wordCount < splitSentence.length){
							if(wordCount +1 < splitSentence.length)
								merged = merged + splitSentence[wordCount +1 ];
							wordCount ++;
							found = true;
						}

						// remove the possible sentence end
						if(documentWord.endsWith(".") )
							documentWord = documentWord.substring(0, documentWord.length()-1);

						if(found == false){
							if (UtilMethods.isPunctuation(splitSentence[wordCount]) && splitSentence.length > wordCount +1)
								if(documentWord.endsWith(splitSentence[wordCount+1]))
									wordCount = wordCount + 2;
						}
					}
				}
			}
		}

		return wordCount;
	}

	/**
	 * remove some of the characters inside the sentence
	 * @param sentence
	 * @return
	 */
	String clearSentence(String sentence) {

		if(sentence.endsWith("."))
			sentence = sentence.substring(0, sentence.length()-1).trim();

		// remove unwanted characters from the sentence
		if(sentence.contains("`"))
			sentence = sentence.replaceAll("`", "");

		if(sentence.contains("'"))
			sentence = sentence.replaceAll("'", "");

		// if sentence contains the report end we replace it
		if(sentence.contains("( report_end )"))
			sentence = sentence.replace("( report_end )", "[report_end]");

		// remove the double spaces
		sentence = sentence.replaceAll(" +", " ");
		return sentence;
	}

}

class WordInSentence{
	String word;
	int sentenceIndex;
	int wordIndexInsideSentence;

	public WordInSentence(){

	}
}


