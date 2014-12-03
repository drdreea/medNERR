package preprocess.aceCorpus;

import io.importer.DataImport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.text.WordUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import classifier.TrainTestCreator.CorpusType;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.Annotation.AnnotationType;


/**
 * This class converts the i2b2 format into the apf format used by ACE
 * You have to pass as arguments the path to the i2b2 files 
 * and the path for storing the ACE formatted files
 * @author ab
 *
 */
public class ToAPFformat {
	String documentPath;
	String outputPath;
	DocumentBuilderFactory docFactory;
	DocumentBuilder docBuilder;

	public ToAPFformat(String pathIn, String pathOut){
		this.documentPath = pathIn;
		this.outputPath = pathOut;

		docFactory = DocumentBuilderFactory.newInstance();
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	void execute(){
		String[] inFiles = new File(documentPath).list();

		for(int index = 0; index < inFiles.length; index ++){
			String fileName = inFiles[index];

			if(fileName.startsWith(".")) continue;

			String outputPath = this.outputPath + "/" + fileName;

			String annotatedFile = this.documentPath + "/" + fileName;
			ArrayList<Annotation> annotations = DataImport.readAnnotations(annotatedFile, 
					CorpusType.I2B2);
			annotationsToXML(outputPath, annotations, fileName);
		}
	}

	public void annotationsToXML(String outputFile, 
			ArrayList<Annotation> annotations, String fileName) {

		try {

			// root elements
			Document doc = docBuilder.newDocument();
			Element sourceElement = doc.createElement("source_file");
			sourceElement.setAttribute("TYPE", "text");
			sourceElement.setAttribute("ENCODING", "UTF-8");
			sourceElement.setAttribute("URI", fileName);
			sourceElement.setAttribute("SOURCE", "newsire");

			doc.appendChild(sourceElement);

			Element rootElement = doc.createElement("document");
			rootElement.setAttribute("DOCID", fileName);
			sourceElement.appendChild(rootElement);
			int entityCount = 0;
			int mentionCount = 0;
			
			HashMap<String, Element> entityNodes = new HashMap<String, Element>();

			// now add all the entity types
			for(Annotation annt : annotations){

				for(AnnotationType type : annt.annotationElements.keySet()){
					AnnotationDetail det = annt.annotationElements.get(type);
					String typeString = null;
					String subTypeString = null;

					try{
						typeString = det.type.toString().substring(0,
								det.type.toString().indexOf("_"));
						subTypeString = det.type.toString().substring(
								det.type.toString().indexOf("_")+1);

						String[] splitType = subTypeString.toLowerCase().split("_");
						subTypeString = WordUtils.capitalize(splitType[0]);

						for(int index = 1; index < splitType.length; index ++)
							subTypeString += "-" + WordUtils.capitalize(splitType[index]);

					}catch(Exception e){
						e.printStackTrace();
					}

					// create the entity element
					Element entity = null;
					if(entityNodes.containsKey(subTypeString)){
						entity = entityNodes.get(subTypeString);
					}else{
						entityCount ++;
						entity = doc.createElement("entity");
						entity.setAttribute("id", fileName + "-E" + 
								String.valueOf(entityCount));
						entity.setAttribute("CLASS", "GEN");
						entity.setAttribute("TYPE", typeString);
						entity.setAttribute("SUBTYPE", subTypeString);
					}

					// create the entity mention element
					mentionCount ++;
					Element entityMention = doc.createElement("entity_mention");
					entityMention.setAttribute("id", entity.getAttribute("id") + "-" + 
							String.valueOf(mentionCount));
					entityMention.setAttribute("TYPE", "NAM");
					entityMention.setAttribute("LDCTYPE", "NOMINAL");

					// create the extent 
					Element extent = doc.createElement("extent");

					// create the charseq
					Element charseq = doc.createElement("charseq");
					charseq.setAttribute("START", String.valueOf(det.startLine));
					charseq.setAttribute("END", String.valueOf(det.endLine));
					charseq.appendChild(doc.createTextNode(det.content));

					extent.appendChild(charseq);
					entityMention.appendChild(extent);

					// create the head
					Element head = doc.createElement("head");

					// create the charseq
					Element charseqHead = doc.createElement("charseq");
					charseqHead.setAttribute("START", String.valueOf(det.startLine));
					charseqHead.setAttribute("END", String.valueOf(det.endLine));
					charseqHead.appendChild(doc.createTextNode(det.content.split(" ")[0]));

					head.appendChild(charseqHead);
					entityMention.appendChild(head);

					entity.appendChild(entityMention);

					entityNodes.put(subTypeString, entity);
				}
			}
			
			for(String key : entityNodes.keySet()){
				Element entity = entityNodes.get(key);
				rootElement.appendChild(entity); 
			}

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(outputFile));

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

			System.out.println("File saved!");

		} catch (Exception pce) {
			pce.printStackTrace();
		}
	}

	public static void main(String[] args){
		if(args.length != 2){
			System.out.println("Incorrect number of arguments");
			System.exit(-1);
		}

		ToAPFformat process = new ToAPFformat(args[0], args[1]);
		process.execute();


	}
}
