package corpus;

import classifier.TrainTestCreator.CorpusType;

public class Relation {
	public AnnotationDetail from;
	public AnnotationDetail to;
	public RelationType relType;
	public static final String fileEnding = ".rel";

	public enum RelationType{
		O,

		// relations in i2b2 medication corpus
		REASON,

		// relations in i2b2 relation corpus
		//Relationships between medical problems and treatments 
		TrIP, TrWP, TrCP, TrAP, TrNAP, 

		//Relationships between medical problems and tests 
		// TeRP: test reveals medical problem 
		// TeCP: test conducted to investigate medical problem 
		TeRP, TeCP, 

		//Relationships between medical problem and another medical problem 
		// PIP: medical problem indicates medical problem 
		PIP;

		public static RelationType parseRelationType(String el) {
			String rel = el.substring(3, el.length()-1);

			return RelationType.valueOf(rel);
		} 

		// relation in ace corpus
	}

	public Relation(){
	}

	public Relation(RelationType type){
		this.relType = type;
	}

	public Relation(AnnotationDetail annt1, AnnotationDetail annt2,
			RelationType type){
		this.relType = type;
		to = annt1;
		from = annt2;
	}

	public static String printString(Relation rel, CorpusType corpus){
		String returnValue = "";

//		switch(rel.relType){
//		case REASON:
			returnValue = AnnotationDetail.printString(rel.to, corpus);
			returnValue += "||";
			returnValue += AnnotationDetail.printString(rel.from, corpus);
//			break;
//		default:		
//			returnValue = AnnotationDetail.printString(rel.to, corpus);
//			returnValue += "||r=\"" + rel.relType.toString() + "\"||";
//			returnValue += AnnotationDetail.printString(rel.from, corpus);
//			break;
//		}

		return returnValue;

	}
}
