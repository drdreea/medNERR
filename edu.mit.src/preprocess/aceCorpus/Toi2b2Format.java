package preprocess.aceCorpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parse through the ACE corpus and output it using
 * the i2b2 corpus standards
 * 
 * Run this program with the path to the DDIE files
 * and the path for storing the i2b2 converted files 
 * 
 * @author ab
 *
 */
public class Toi2b2Format {
	String inputPath ;
	String outputPathRaw;
	String outputPathGS;



	public Toi2b2Format(String[] args) {
		this.inputPath = args[0];
		this.outputPathRaw = args[1];
		this.outputPathGS = args[2];
	}

	void execute(){

		String[] inputFiles = new File(inputPath).list();

		for(int index = 0; index < inputFiles.length; index ++){
			String fileName = inputFiles[index];
			if(fileName.startsWith(".")) continue;

			// only parse through the simple xml files
			if(fileName.endsWith("nrm.xml") || 
					fileName.endsWith("dep.xml") ||
					fileName.endsWith("ttt.xml"))
				continue;

			importSentences(fileName);

		}
	}

	private void storeSentences(ArrayList<ACESentence> sentences,
			ArrayList<ACEnamedEntity> entities, 
			String outputFileRaw, String outputFileGS) {
		ArrayList<String> rawLines = new ArrayList<String>();
		ArrayList<String> annotations = new ArrayList<String>();


		// store the raw file
		for(ACESentence sentence : sentences)
			rawLines.add(sentence.sentenceContent);			

		fileStore(outputFileRaw, rawLines);

		// store the annotations
		for (ACEnamedEntity annt : entities)
			annotations.add(ACEnamedEntity.toString(annt));

		fileStore(outputFileGS, annotations);
	}

	void fileStore(String fileName, ArrayList<String> lines){
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName));

			for(String line : lines) {
				out.write(line + "\n");
			}

			out.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<ACESentence> importSentences(String filePath) {

		// now import the sentences
		ArrayList<ACESentence> sentences = new ArrayList<ACESentence>();
		ArrayList<ACEnamedEntity> entities = new ArrayList<ACEnamedEntity>();
		ArrayList<ACERelation> relations = new ArrayList<ACERelation>();

		try {
			File file = new File(inputPath + "/" + filePath);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			System.out.println(file);

			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();

			// skip the rexml and the doc nodes
			NodeList children = doc.getChildNodes().item(0).getChildNodes();

			int sentenceCount = 1;

			for (int s = 0; s < children.getLength(); s++) {
				Node fstNode = children.item(s);

				NodeList subChildren = fstNode.getChildNodes();

				for (int s1 = 0; s1 < subChildren.getLength(); s1++) {
					Node textNode = subChildren.item(s1);
					String nodeName = textNode.getNodeName();

					if(nodeName.equals("markup")){
						RelationsAndNE annotatedData = processMarkup(textNode, sentenceCount);
						entities.addAll(annotatedData.entityList);
						relations.addAll(annotatedData.relationList);

					}else{
						ArrayList<ACESentence> aceSentences  = processText(textNode, 
								sentenceCount);
						sentenceCount += aceSentences.size();
						sentences.addAll(aceSentences);
					}
				}
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// merge the entities and the relations
		entities = mergeData(sentences, entities, relations);

		// print the raw data
		filePath = filePath.replaceAll(".xml", "");
		filePath = filePath.replaceAll("\\.", "_");

		storeSentences(sentences, entities, this.outputPathRaw + "/" + filePath, 
				this.outputPathGS + "/" + filePath + ".i2b2.entries");


		return sentences;
	}



	private ArrayList<ACEnamedEntity> mergeData(ArrayList<ACESentence> sentences,
			ArrayList<ACEnamedEntity> entities, ArrayList<ACERelation> relations) {
		// get a hashmap of all the wods String document
		HashMap<String, ACEWord> allWords = new HashMap<String, ACEWord>();
		HashMap<String, ACEnamedEntity> allEntities = new HashMap<String, ACEnamedEntity>();
		HashMap<String, ACESentence> sentenceMap = new HashMap<String, ACESentence>();
		
		for(ACESentence sent : sentences){
			allWords.putAll(sent.words);
			sentenceMap.put(sent.sentenceID, sent);
		}

		// go through the list of entities and determine the offsets
		for(int index = 0; index < entities.size(); index ++){
			ACEnamedEntity entity = entities.get(index);
			entity = ACEnamedEntity.setOffsets(entity, 
					allWords.get(entity.wordStart), 
					allWords.get(entity.wordEnd),
					sentenceMap.get(allWords.get(entity.wordEnd).sentenceId));

			entities.set(index, entity);
			allEntities.put(entity.entityID, entity);
		}

		// go through the relations and associate a relation with each named entity
		for(ACERelation relation : relations){
			ACEnamedEntity entity1 = allEntities.get(relation.entity1);
			ACEnamedEntity entity2 = allEntities.get(relation.entity2);

			ArrayList<ACEnamedEntity> contained = new ArrayList<ACEnamedEntity>();
			if(entity1.relationships.containsKey(relation.type)){
				contained = entity1.relationships.get(relation.type);
			}

			contained.add(entity2);
			entity1.relationships.put(relation.type, contained);


			// check whether the relationship is symmetrical
			if(relation.type.equals("METONYMY") ||
					relation.type.equals("PER-SOC") ||
					relation.type.equals("PHYS")){
				contained = new ArrayList<ACEnamedEntity>();
				if(entity2.relationships.containsKey(relation.type)){
					contained = entity2.relationships.get(relation.type);
				}
				contained.add(entity1);
				entity2.relationships.put(relation.type, contained);
			}

			allEntities.put(relation.entity1, entity1);
			allEntities.put(relation.entity2, entity2);
		}

		ArrayList<ACEnamedEntity> finalEntities = new ArrayList<ACEnamedEntity>();
		for(String key : allEntities.keySet())
			finalEntities.add(allEntities.get(key));

		return finalEntities;
	}

	private ArrayList<ACESentence> processText(Node textNode, int sentenceCount) {
		ArrayList<ACESentence> sentences = new ArrayList<ACESentence>();

		NodeList children = textNode.getChildNodes();


		for(int index = 0; index < children.getLength(); index ++){
			Node paragraph = children.item(index);

			// get the paragraph sentences
			NodeList paraSentences = paragraph.getChildNodes();
			for(int sIndex = 0; sIndex < paraSentences.getLength(); sIndex ++){
				Node sentence = paraSentences.item(sIndex);

				if(sentence.getAttributes() == null)
					continue;

				ACESentence newSentence = new ACESentence(sentence, sentenceCount);
				sentenceCount ++;
				sentences.add(newSentence);
			}
		}

		return sentences;

	}


	private RelationsAndNE processMarkup(Node textNode, int sentenceCount) {
		ArrayList<ACEnamedEntity> annotations = new ArrayList<ACEnamedEntity>();
		ArrayList<ACERelation> relations = new ArrayList<ACERelation>();

		NodeList children = textNode.getChildNodes();

		for(int index =0; index < children.getLength(); index ++){
			Node markupEl = children.item(index);
			String elName = markupEl.getNodeName();

			if(elName.equals("nes")){
				NodeList neNodes = markupEl.getChildNodes();

				for(int neIndex =0; neIndex <neNodes.getLength(); neIndex ++){
					Node neElement = neNodes.item(neIndex);
					if(neElement.getAttributes() == null) continue;

					annotations.add(new ACEnamedEntity(neElement));
				}

			}else if(elName.equals("rels")){
				NodeList relNodes = markupEl.getChildNodes();

				for(int neIndex =0; neIndex <relNodes.getLength(); neIndex ++){
					Node relElement = relNodes.item(neIndex);
					if(relElement.getAttributes() == null) continue;

					relations.add(new ACERelation(relElement));
				}
			}else if(elName.equals("dps")){
				System.out.println("Incorrect file");
			}
		}

		return new RelationsAndNE(annotations, relations);

	}

	public static void main(String[] args){
		if(args.length != 3){
			System.out.println("USAGE: Run this program with the path " +
					"to the ACE files" +
					" and the path for storing the i2b2 converted files");
			System.exit(-1);
		}

		Toi2b2Format processFiles = new Toi2b2Format(args);
		processFiles.execute();

	}

}

class RelationsAndNE{
	ArrayList<ACEnamedEntity> entityList;
	ArrayList<ACERelation> relationList;

	RelationsAndNE(ArrayList<ACEnamedEntity> givenEntities, 
			ArrayList<ACERelation> givenRelations ){
		this.entityList = givenEntities;
		this.relationList = givenRelations;
	}

}
