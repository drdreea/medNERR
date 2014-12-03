package preprocess.ddieCorpus;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Node;

public class DDIESentence {
	public String sentenceContent;
	public String sentenceID;
	public HashMap<String, DDIEEntity> entities;
	public ArrayList<DDIEInteraction> interactions;

	public DDIESentence(String text, String id) {
		this.sentenceContent = text;
		this.sentenceID = id;

		this.entities = new HashMap<String, DDIEEntity>();
		this.interactions = new ArrayList<DDIEInteraction>();
	}

	public void add(Node list, String sentenceContent, 
			int sentenceCount) {
				
		if(list.getNodeName().equals("entity")){
			String charOffset = list.getAttributes().getNamedItem("charOffset").
					getTextContent();
			String text = list.getAttributes().getNamedItem("text").getTextContent();
			
			if(! charOffset.startsWith("-") && ! text.isEmpty())

				this.entities.put(list.getAttributes().getNamedItem("id").getTextContent(), 
						new DDIEEntity(sentenceContent,
								text,
								list.getAttributes().getNamedItem("id").getTextContent(),
								charOffset, sentenceCount));
		}else if(list.getNodeName().equals("interaction")){
			String firstEntity = list.getAttributes().getNamedItem("e1").getTextContent();
			String secondEntity = list.getAttributes().getNamedItem("e2").getTextContent();

			if(!firstEntity.equals("ERROR") && !secondEntity.equals("ERROR")){

				DDIEEntity e1 = entities.get(firstEntity);
				DDIEEntity e2 = entities.get(secondEntity);

				if(e1 == null || e2 == null)
					System.out.println("ERROR in file at annotation " + firstEntity + 
							" " + secondEntity);
				else{
					e1.iteractsWith.add(e2);
					e2.iteractsWith.add(e1);

					entities.put(firstEntity, e1);
					entities.put(secondEntity, e2);

					this.interactions.add(new DDIEInteraction(
							list.getAttributes().getNamedItem("id").getTextContent(),
							firstEntity,
							secondEntity));
				}
			}
		}


	}
}
