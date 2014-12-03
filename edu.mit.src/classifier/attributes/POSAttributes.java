package classifier.attributes;

import java.util.ArrayList;
import java.util.HashMap;

import corpus.AnnotationDetail;
import corpus.MedicalRecord;

import classifier.ClassifierBase;

import discourse.DiscourseProcessing;

import patternExtraction.FindContextFromEMR;
import preprocess.parser.SentenceContent;

/**
 * Construct the POS attibutes
 * @author ab
 *
 */
public class POSAttributes extends AttributeGenerator{
	enum Direction{LEFT, RIGHT, LEFTSECOND, RIGHTSECOND, LEFTTHIRD, RIGHTTHIRD};

	enum POSCode{PUNCT(0), CC (1), CD (2), DT(3), EX(4), FW(5), IN(6), JJ(7), JJR(8), JJS(9), 
		LS(10), MD(11), NN(12), NNS(13), NNP(14), NNPS(15), PDT(16), POS(17), 
		PRP(18), PRP$(19), RB(20), RBR(21), RBS(22), RP(23), SYM(24), TO(25), 
		UH(26), VB(27), VBN(28), VBD(29), VBG(30), VBZ(31),VBP(32), WDT(33), 
		WP(34), WP$(35), WRB(36);

	private final int code;  
	POSCode(int value){this.code = value;}
	}

	enum TreeNodeCode{PUNCT(0), ADJP(1), ADVP(2), CONJP(3), INTJ(4), LST(5), NAC(6), NP(7),
		NX(8), PP(9), PRN(10), PRT(11), QP(12), RRC(13), UCP(14), VP(15), WHADJP(16), 
		WHAVP(17), WHNP(18), WHPP(19), X(20), S(21), SBAR(22), SBARQ(23), SINV(24),
		SQ(25), NN(25), NNP(26), FRAG(27) ;

	private final int code;  
	TreeNodeCode(int value){this.code = value;}
	}

	private DiscourseProcessing discourseProcessor;

	/**
	 * 
	 * @param classifier
	 */
	 public POSAttributes(ClassifierBase classifier, 
			 HashMap<String, SentenceContent> conceptSentence){
		 super(classifier);
		 discourseProcessor = new DiscourseProcessing();
	 }

	 public POSAttributes(ClassifierBase classifier ){
		 super(classifier);
		 discourseProcessor = new DiscourseProcessing();
	 }
	 
	 /**
	  * Get the POS types
	  * @param medication
	  * @param reason
	  * @param medicalRecord
	  * @return the pos feature values
	  */
	 public HashMap<String, Object> getPOSFeatureValues(AnnotationDetail medication, 
			 AnnotationDetail reason,  
			MedicalRecord medicalRecord){
		 HashMap<String, Object> featureValues = new HashMap<String, Object>();  

		 // find the med and the reason sentences
		 SentenceContent medSentence = null;
		 medSentence = medication.sentence;

		 SentenceContent reasonSentence = null;
		 reasonSentence = reason.sentence;

		 // start adding the features
		 // Feature 11: whether med and reason are in the same sentence
		 featureValues.put("checkSameSentence", checkSameSentence(medSentence, reasonSentence));

		 // Feature 12: the reason is prior to the med
		 featureValues.put("checkReasonPrior", checkReasonPrior(medSentence, reasonSentence));

		 // Feature 13: whether there is a causal conjunction inside the context
		 featureValues.put("checkCausalConjunction", checkCausalConjunction(medSentence, 
				 reasonSentence,
				 reason, medication));

		 // Feature 14: whether the conjunctions path is correct
		 featureValues.put("checkConjunctions", 
				 checkConjunctions( medication, reason, medicalRecord));

		 // Feature 15: the pos of the medication
//		 featureValues.put("computePOSTypeMed", 
//				 computePOSType(medication.medication, medSentence));
//
//		 // Feature 16: the pos of the reason
//		 featureValues.put("computePOSTypeReason", 
//				 computePOSType(reason.reason, reasonSentence));

		 // Feature 17: the pos of the 2 word left of the medication
//		 featureValues.put("getPOSWordLeft", getPOS(medication.medication,  
//				 Direction.LEFT, medSentence));
//
//		 // Feature 18: the pos of the 2 word left of the medication
//		 featureValues.put("getPOS2WordLeft", getPOS(medication.medication,  
//				 Direction.LEFTSECOND, medSentence));
//
//		 // Feature 18: the pos of the 2 word left of the medication
//		 featureValues.put("getPOS3WordLeft", getPOS(medication.medication,  
//				 Direction.LEFTTHIRD, medSentence));
//
//		 // Feature 19: the pos of the 2 word right of the medication
//		 featureValues.put("getPOSWordRight", getPOS(medication.medication,  
//				 Direction.RIGHT, medSentence));
//
//		 // Feature 20: the pos of the 2 word left of the medication
//		 featureValues.put("getPOS2WordRight", getPOS(medication.medication,  
//				 Direction.RIGHTSECOND, medSentence));
//
//		 // Feature 21: the pos of the 2 word left of the medication
//		 featureValues.put("getPOS3WordRight", getPOS(medication.medication,  
//				 Direction.RIGHTTHIRD, medSentence));
//
//		 // Feature 22: the pos of the 2 words left of the reason
//		 featureValues.put("getPOSWordLeftReason",  getPOS(reason.reason,  
//				 Direction.LEFT, reasonSentence));
//
//		 // Feature 23: the pos of the 2 word left of the medication
//		 featureValues.put("getPOS2WordLeftReason",  getPOS(reason.reason,  
//				 Direction.LEFTSECOND, reasonSentence));
//
//		 // Feature 24: the pos of the 2 word left of the medication
//		 featureValues.put("getPOS3WordLeftReason",  getPOS(reason.reason,  
//				 Direction.LEFTTHIRD, reasonSentence));
//
//		 // Feature 25: the pos of the 2 word right of the medication
//		 featureValues.put("getPOSRightReason",  getPOS(reason.reason,  
//				 Direction.RIGHT, reasonSentence));
//
//		 // Feature 26: the pos of the 2 word right of the medication
//		 featureValues.put("getPOS2RightReason",  getPOS(reason.reason,  
//				 Direction.RIGHTSECOND, reasonSentence));
//
//		 // Feature 27: the pos of the 2 word right of the medication
//		 featureValues.put("getPOS3RightReason",  getPOS(reason.reason,  
//				 Direction.RIGHT, reasonSentence));

		 // Feature 28: whether the reason is plural
//		 featureValues.put("checkReasonPlural", 
//				 checkPlural(reason.reason,  reasonSentence));
//
//		 // Feature 29: whether the medication is plural
//		 featureValues.put("checkMedicationPlural", 
//				 checkPlural(medication.medication, medSentence));

		 // Feature 30: whether there is a verb between the medication and the reason
		 featureValues.put("checkVerbBetween", checkVerbContext(medSentence, reasonSentence, 
				 reason, medication));

		 // feature 31: the node type of the common parent
		 featureValues.put("checkCommonParentType", checkCommonParentType(medSentence, reasonSentence, 
				 medication, reason, medicalRecord));

		 // feature 32: whether the medication is dependent on the reason
		 featureValues.put("checkDependencyMedReason", checkDependency(medSentence, reasonSentence, 
				 medication, reason, medicalRecord));

		 // feature 33: whether the reason is dependent on the medication
		 featureValues.put("checkDependencyReasonMed", checkDependency(medSentence, reasonSentence, 
				 reason, medication, medicalRecord));


		 return featureValues;
	 }

	 /**
	  * Check if the medication and the reason are in the same sentence
	  * @param file
	  * @param medSentence
	  * @param reasonSentence
	  * @return
	  */
	 boolean checkSameSentence(SentenceContent medSentence, 
			 SentenceContent reasonSentence){


		 if(medSentence != null && reasonSentence != null)
			 return medSentence.sentence.equals(reasonSentence.sentence);

		 return false;
	 }


	 /**
	  * 
	  * @param file
	  * @param medication
	  * @param reason
	  * @return whether the reason is a prior
	  */
	 boolean checkReasonPrior(SentenceContent medSentence, 
			 SentenceContent reasonSentence) {

		 if(medSentence != null && reasonSentence != null)
			 return (medSentence.sentenceOrder > reasonSentence.sentenceOrder);

		 return false;
	 }

	 /**
	  * Check whether there is a causal conjunction inside the context between
	  * the medication and the reason 
	  * @param medSentence
	  * @param reasonSentence
	  * @param reason
	  * @param medication
	  * @return
	  */
	 boolean checkCausalConjunction(SentenceContent medSentence, 
			 SentenceContent reasonSentence, AnnotationDetail reason, 
			 AnnotationDetail medication){

		 if(medSentence != null && reasonSentence != null){
			 // only include instances that contain causal conjunctions
			 if(medSentence.sentence.equals(reasonSentence.sentence)){
				 return medSentence.containsCausalConjunction(medication, 
						 reason);
			 }
		 }


		 return false;
	 }

	 /**
	  * 
	  * @param file
	  * @param medication
	  * @param reason
	  * @return
	  */
	 boolean checkConjunctions(AnnotationDetail medication,
			 AnnotationDetail reason, MedicalRecord emr) {

		 String context = FindContextFromEMR.getContent(emr.rawFileLines, 
				 medication, 
				 reason, false);
		 if(context != null)
			 return discourseProcessor.parseContextByConjunction(context);


		 return false;
	 }

	 /**
	  * Get the code of the pos
	  * @param pos
	  * @return
	  */
	 double computePOSCode(ArrayList<String> pos){
		 POSCode value = null;

		 for(String posWord : pos){
			 try{
				 if(!Character.isLetterOrDigit(posWord.charAt(0))){
					 value = POSCode.PUNCT;
					 continue;
				 }
				 POSCode tmp = POSCode.valueOf(posWord);

				 if(value == null || tmp.code > value.code)
					 value = tmp;
			 }catch(Exception e){
				 // here we have the punctuation signs
				 e.printStackTrace();
				 return 0;
			 }
		 }

		 if(value == null) {
			 return -1;
		 }

		 return (value.code*1.0)/POSCode.values().length;
	 }

	 double computePOSCode(String posWord){
		 POSCode value = null;

		 try{
			 if(!Character.isLetterOrDigit(posWord.charAt(0))){
				 value = POSCode.PUNCT;
			 }
			 POSCode tmp = POSCode.valueOf(posWord);

			 if(value == null || tmp.code > value.code)
				 value = tmp;
		 }catch(Exception e){
			 // here we have the punctuation signs
			 e.printStackTrace();
			 return 0;
		 }


		 if(value == null) {
			 return -1;
		 }

		 return (value.code*1.0)/POSCode.values().length;
	 }

	 /**
	  * Get the code of the tree node
	  * @param node
	  * @return
	  */
	 double computeTreeNodeCode(String node){
		 TreeNodeCode value = null;

		 try{
			 if(!Character.isLetterOrDigit(node.charAt(0)) &&
					 node.length() == 1){
				 value = TreeNodeCode.PUNCT;
				 return value.code;
			 }

			 TreeNodeCode tmp = TreeNodeCode.valueOf(node);

			 if(value == null || tmp.code > value.code)
				 value = tmp;
		 }catch(Exception e){
			 // here we have the punctuation signs
			 e.printStackTrace();
			 return 0;
		 }


		 if(value == null) {
			 return -1;
		 }

		 return (value.code*1.0)/TreeNodeCode.values().length;
	 }


	 /**
	  * 
	  * @param medSentence
	  * @param reasonSentence
	  * @param reason
	  * @param medication
	  * @return
	  */
	 boolean checkVerbContext(SentenceContent medSentence, SentenceContent reasonSentence,
			 AnnotationDetail reason, AnnotationDetail medication){

		 if(medSentence != null && reasonSentence != null){
			 // only include instances that contain causal conjunctions
			 if(medSentence.sentence.equals(reasonSentence.sentence)){
				 return medSentence.containsCausalConjunction(medication, 
						 reason);
			 }
		 }

		 return false;
	 }

	 /**
	  * Check the type of the common parent
	  * @param medication
	  * @param reason
	  * @param medSentence
	  * @param reasonSentence
	  * @param file
	  * @return
	  */
	 double checkCommonParentType(SentenceContent medSentence, SentenceContent reasonSentence,
			 AnnotationDetail medication, AnnotationDetail reason, 
			 MedicalRecord medicalRecord){

		 if(medSentence != null && reasonSentence != null){
			 try{
				 // only include instances that contain causal conjunctions
				 if(medSentence.sentence.equals(reasonSentence.sentence)){
					 Object[] fileLines = medicalRecord.rawFileLines.toArray();

					 String commonParentNode = medSentence.getCommonParentTreeNode(medication, 
							 reason, fileLines);

					 if(commonParentNode == null)
						 return -1;

					 return computeTreeNodeCode(commonParentNode);
				 }
			 }catch(Exception e){
				 e.printStackTrace();
			 }
		 }

		 return -1;
	 }

	 /**
	  * 
	  * @param concept1
	  * @param concept2
	  * @param file
	  * @param medSentence
	  * @param reasonSentence
	  * @return
	  */
	 boolean checkDependency(SentenceContent medSentence, SentenceContent reasonSentence,
			 AnnotationDetail concept1, AnnotationDetail concept2,
			 MedicalRecord medicalRecord){


		 if(medSentence != null && reasonSentence != null){
			 // only include instances that contain causal conjunctions
			 if(medSentence.sentence.equals(reasonSentence.sentence)){
				 Object[] fileLines = medicalRecord.rawFileLines.toArray();

				 return medSentence.hasDependencyRelation(concept1, concept2, fileLines);

			 }
		 }

		 return false;
	 }

	 /**
	  * some of the pos labels are incorrectly assigned (i.e., unknown nouns are
	  * treated as verbs)
	  * check inside the wordNet dictionary if the label is acceptable otherwise return noun
	  * 
	  * @param word
	  * @param label
	  * @return
	  */
	 String filterPOSLabel(String word, String label){
		 String lemma = wordNetProcessor.getLemma(word, label);


		 if(lemma == null && label.startsWith("V")){
			 return "NN"; 
		 }
		 else
			 return label;

	 }
}
