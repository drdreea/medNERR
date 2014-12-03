package preprocess.aceCorpus;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ACERelation {
	public String id;
	public String entity1;
	public String entity2;
	public String type;
	
	public ACERelation(Node neElement){
		NamedNodeMap attributes = neElement.getAttributes();
		
		id = attributes.getNamedItem("id").getTextContent();
		entity1 = attributes.getNamedItem("e1").getTextContent();
		entity2 = attributes.getNamedItem("e2").getTextContent();
		type = attributes.getNamedItem("t").getTextContent();
		
	}

}
