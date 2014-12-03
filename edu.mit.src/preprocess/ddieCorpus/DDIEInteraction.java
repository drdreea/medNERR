package preprocess.ddieCorpus;

public class DDIEInteraction {
	public String interactionId;
	public String entity1ID;
	public String entity2ID;
	
	public DDIEInteraction(String id, String e1, String e2){
		this.interactionId = id;
		this.entity1ID = e1;
		this.entity2ID = e2;
	}

}
