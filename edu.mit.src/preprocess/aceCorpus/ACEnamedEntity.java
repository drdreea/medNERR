package preprocess.aceCorpus;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import utils.UtilMethods;

import edu.stanford.nlp.util.StringUtils;


public class ACEnamedEntity {
	public String entityID;
	public String wordStart;
	public String wordEnd;
	public String type;
	
	public int startOffset;
	public int endOffset;
	public int startLine;
	public int endLine;
	
	public String content;
	public HashMap<String, ArrayList<ACEnamedEntity>> relationships;
	

	public ACEnamedEntity(Node neElement) {
		NamedNodeMap attributes = neElement.getAttributes();
		entityID = attributes.getNamedItem("id").getTextContent();
		wordStart = attributes.getNamedItem("fr").getTextContent();
		wordEnd = attributes.getNamedItem("to").getTextContent();
		type = attributes.getNamedItem("t").getTextContent();
		type += "_" + attributes.getNamedItem("st").getTextContent().replaceAll("-", "_");
		
		NodeList children = neElement.getChildNodes();
		for(int index = 0; index < children.getLength(); index ++){
			Node child = children.item(index);
			if(child.getAttributes() == null) continue;
			
			Node localContent = child.getAttributes().getNamedItem("type");
			if(localContent != null)
				if(localContent.getTextContent().equals("extent")){
					this.content = child.getTextContent().replaceAll("\n", " ");
				}
			
						
		}
		
		this.relationships = new HashMap<String, 
				ArrayList<ACEnamedEntity>>();
	}


	public static String printEntity(ACEnamedEntity entity){
		String printing = entity.type + "=\"";
		
		printing += entity.content + "\" ";
		printing += String.valueOf(entity.startLine) + ":";
		printing += String.valueOf(entity.startOffset) ;

		printing += " " + String.valueOf(entity.endLine) + ":";
		printing += String.valueOf(entity.endOffset) ;
		
		return printing;
	}
	
	public static String toString(ACEnamedEntity entity){
		String printing =  ACEnamedEntity.printEntity(entity);

		String finalPrinting = "";

		for(String key : entity.relationships.keySet()){
			ArrayList<ACEnamedEntity> rels = entity.relationships.get(key);
			
			for(ACEnamedEntity interact : rels)
				finalPrinting += printing + "||" + ACEnamedEntity.printEntity(interact) +
					"||t=\"" + key +"\"\n";
		}
		
		finalPrinting = StringUtils.chomp(finalPrinting);
		
		if(entity.relationships.isEmpty())
			return printing;
 
		
		return finalPrinting;
	}


	public static ACEnamedEntity setOffsets(ACEnamedEntity entity, 
			ACEWord aceWordStart, ACEWord aceWordEnd, 
			ACESentence sentence) {
		int startWord = aceWordStart.wordCount;
		int endWord = aceWordEnd.wordCount;
		
		// the word offsets has to be shifted by 1
		// as in the i2b2 corpus the words are counted starting at 0
		entity.startOffset = startWord -1 ;
		entity.endOffset = endWord -1;
		entity.endLine = aceWordEnd.lineCount;
		entity.startLine = aceWordStart.lineCount;
		
		String[] words = sentence.sentenceContent.split(" ");
		String prev = entity.content;
		entity.content = "";
		
		for(int index = entity.startOffset; index <= entity.endOffset; 
				index ++){
			entity.content = UtilMethods.mergeStrings(entity.content, words[index]);
		}
		
		if(entity.content.isEmpty())
			entity.content = prev;;
		
		return entity;
	}

}
