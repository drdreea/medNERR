package preprocess.wordnet;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

import utils.UtilMethods;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IVerbFrame;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

public class WordNetProcessing {
	IDictionary dict;
	WordnetStemmer stemmer;

	public WordNetProcessing(String wordNetPath){
		// create the dictionary
		String path = wordNetPath + File.separator + "dict";
		URL url;
		try {
			url = new URL("file", null , path );
			// construct the dictionary object and open it
			dict = new Dictionary ( url);
			dict.open ();
		}catch(Exception e){
			e.printStackTrace();
		}

		// create the stemmer
		stemmer = new WordnetStemmer(dict);

	}


	public HashSet<String> getSentenceFrames(String contextWord, String label){
		HashSet<String> sentenceFrames = new HashSet<String>();

		POS posLabel = null;

		if(label.startsWith("VB"))
			posLabel = POS.VERB;
		else if(label.startsWith("N"))
			posLabel = POS.NOUN;
		else if(label.startsWith("JJ"))
			posLabel = POS.ADJECTIVE;
		else 
			return null;

		List<String> stems = stemmer.findStems(contextWord, posLabel);

		for(String stem : stems){
			IIndexWord idxWord = dict.getIndexWord (stem, posLabel );

			if(idxWord == null)
				continue;

			IWordID wordID = idxWord.getWordIDs ().get (0) ;
			IWord word = dict.getWord ( wordID );

			List<IVerbFrame> frames = word.getVerbFrames();
			for(IVerbFrame frame: frames){
				sentenceFrames.add(frame.getTemplate());
			}

		}


		return sentenceFrames;
	}


	/**
	 * get the lemma for the given word
	 * @param word
	 * @param label
	 * @return
	 */
	public String getLemma(String word, String label) {
		if(UtilMethods.isPunctuation(word))
			return word;
		
		POS posLabel = null;

		if(label.startsWith("VB"))
			posLabel = POS.VERB;
		else if(label.startsWith("N"))
			posLabel = POS.NOUN;
		else if(label.startsWith("JJ"))
			posLabel = POS.ADJECTIVE;
		else if(label.startsWith("CD"))
			posLabel = POS.ADVERB;
		else
			return word;

		try{
			List<String> stems = stemmer.findStems(word, posLabel);

			if(stems == null || stems.isEmpty())
				return word;

			return stems.get(0);

		}catch(Exception e){
			System.out.println("Could not process stem: " + word);
			return null;
		}

	}

}
