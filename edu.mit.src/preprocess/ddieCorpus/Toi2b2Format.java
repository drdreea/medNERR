package preprocess.ddieCorpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parse through the DDIE corpus and output it using
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
			
			ArrayList<DDIESentence> sentences = importSentences(inputPath +"/" + fileName);
			fileName = fileName.split("\\.")[0];
			
			storeSentences(sentences, this.outputPathRaw + "/" + fileName, 
					this.outputPathGS + "/" + fileName + ".i2b2.entries");
		}
	}

	private void storeSentences(ArrayList<DDIESentence> sentences, 
			String outputFileRaw, String outputFileGS) {
		ArrayList<String> rawLines = new ArrayList<String>();
		ArrayList<String> annotations = new ArrayList<String>();
		
		
		for(DDIESentence sentence : sentences){
			rawLines.add(sentence.sentenceContent);
			
			for(String entityID : sentence.entities.keySet()){
				DDIEEntity entity = sentence.entities.get(entityID);
				
				annotations.add(DDIEEntity.toString(entity));
			}			
		}
		
		fileStore(outputFileRaw, rawLines);
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

	public ArrayList<DDIESentence> importSentences(String filePath) {

		// now import the sentences
		ArrayList<DDIESentence> sentences = new ArrayList<DDIESentence>();

		try {
			File file = new File(filePath);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			System.out.println(file);
			
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList children = doc.getChildNodes();
			int sentenceCount = 1;

			for (int s = 0; s < children.getLength(); s++) {
				Node fstNode = children.item(s);

				NodeList subChildren = fstNode.getChildNodes();

				for (int s1 = 0; s1 < subChildren.getLength(); s1++) {
					Node sndNode = subChildren.item(s1);
					NamedNodeMap attributes = sndNode.getAttributes();
					
					if(attributes == null ) continue;
					
					DDIESentence sentence = new DDIESentence(
							attributes.getNamedItem("text").getTextContent(),
							attributes.getNamedItem("origId").getTextContent());

					if (sndNode.hasChildNodes()) {
						NodeList listChild = sndNode.getChildNodes();

						for (int s2 = 0; s2 < listChild.getLength(); s2++) {
							Node list = listChild.item(s2);
							
							sentence.add(list, sentence.sentenceContent, sentenceCount);
						}
					}
					
					sentences.add(sentence);
					sentenceCount ++;
				}

			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return sentences;
	}


	public static void main(String[] args){
		if(args.length != 3){
			System.out.println("USAGE: Run this program with the path " +
					"to the DDIE files" +
					" and the path for storing the i2b2 converted files");
			System.exit(-1);
		}

		Toi2b2Format processFiles = new Toi2b2Format(args);
		processFiles.execute();

	}

}
