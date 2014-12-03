package preprocess.aceCorpus;

import java.util.HashMap;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import utils.UtilMethods;

public class ACESentence {

	public String sentenceContent;
	public String sentenceID;
	public HashMap<String, ACEnamedEntity> entities;
	public HashMap<String, ACEWord> words;

	public ACESentence(Node sentence, int sentenceCount) {

		this.entities = new HashMap<String, ACEnamedEntity>();
		this.words = new HashMap<String, ACEWord>();
		
		// iterate through the words
		NodeList words = sentence.getChildNodes();
		this.sentenceContent = "";
		this.sentenceID = sentence.getAttributes().getNamedItem("id").
				getTextContent();
		
		int wordCount = 1;
		
		for(int index = 0; index < words.getLength(); index ++){
			Node word = words.item(index);
			if(word.getAttributes() == null) continue;
			
			String text = word.getTextContent();
			String id = word.getAttributes().getNamedItem("id").getTextContent();
			
			this.words.put(id, new ACEWord(text, id, sentenceCount, wordCount,
					sentenceID));
			sentenceContent = UtilMethods.mergeStrings(sentenceContent, text);
			wordCount ++;
		}
	}

}
