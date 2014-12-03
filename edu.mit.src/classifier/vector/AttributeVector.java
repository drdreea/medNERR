package classifier.vector;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import corpus.AnnotationDetail;
import corpus.Annotation.AnnotationType;
import corpus.Relation.RelationType;

import utils.UtilMethods;

import classifier.TrainTestCreator.InfoToExtract;
import classifier.TrainTestCreator.VectorType;
import classifier.vector.VectorCreator.TagType;

/**
 * This class stores the information of the attribute vector
 * 	* the vector tag
 * 	* the vector instance name
 * 	* the vector list of attribute names and attribute values
 * 
 * @author andreea
 *
 */
public class AttributeVector {
	public TagType tag;
	public AnnotationType annotationType;
	public RelationType relationType;

	public String name;
	public AnnotationDetail annotation;
	public String file;
	public String phoneme;
	public String pos;

	HashMap<String, Object> attributes;

	public AttributeVector(AnnotationDetail annt, String filePath){
		attributes = new HashMap<String, Object>();
		annotation = annt;
		file = filePath;
	}

	public void setTag(TagType givenTag) {
		this.tag = givenTag;
	}

	public void selectAttributes(){

	}


	public String printVector(VectorType vector){
		String instance = null;

		switch(vector){
		case CODL:
			instance = printCODLVector();
			break;
		default:
			instance = printCODLVector();
			break;
		}

		return instance;
	}

	private String printCODLVector() {
		String instance = "";

		// add the instance name 
		instance = name;

		// add the instance class
		instance = instance + " " + tag.toString();

		// add the features
		for(String featureName : attributes.keySet())
			instance = instance + " " + featureName + 
			"=" + attributes.get(featureName).toString();


		return instance;

	}

	public static ArrayList<String> printVectorList(ArrayList<AttributeVector> instances,
			VectorType vector, InfoToExtract infoType,
			ArrayList<String> featureOrder,
			boolean normalizeText) {
		ArrayList<String> outputLines = new ArrayList<String>();

		//		for(AttributeVector vectorInstance : instances){
		//			String content = ""  ;
		//
		//			for(String attr : vectorInstance.attributes.keySet()){
		//				content += " " + vectorInstance.attributes.get(attr);
		//			}
		//
		//			if(vectorInstance.tag == TagType.O)
		//				content +=  " " + vectorInstance.tag.toString() ;
		//			else{
		//
		//				if(infoType == InfoToExtract.CONCEPT)
		//					content += " " +  vectorInstance.tag.toString() + 
		//							vectorInstance.annotationType.toString();				
		//				else
		//					content += " " + vectorInstance.tag.toString() + 
		//							vectorInstance.relationType.toString();
		//			}
		//			outputLines.add(content);
		//		}
		//
		//		// also mark the end of a sentence
		//		outputLines.add("\n");
		//
		//		return outputLines;

		switch(vector){
		case CODL:
		case CODL_CONTEXT:
		case CODL_MEDICATION:			
			HashMap<String, String> featureValues = new HashMap<String, String>();
			String name = "";
			String tag = "";

			for(AttributeVector vectorInstance : instances){

				name = UtilMethods.mergeStrings(name, vectorInstance.name);

				if(vectorInstance.tag == TagType.O)
					tag = UtilMethods.mergeStrings(tag, 
							vectorInstance.tag.toString());
				else 
					switch(infoType){
					case CONCEPT:
						tag = UtilMethods.mergeStrings(tag, 
								vectorInstance.tag.toString() + 
								vectorInstance.annotationType.toString());
						break;
					case RELATION:
						tag = UtilMethods.mergeStrings(tag, 
								vectorInstance.tag.toString() + 
								vectorInstance.relationType.toString());
						break;
					case BOTH:
						if(vectorInstance.annotationType != null)
							tag = UtilMethods.mergeStrings(tag, 
									vectorInstance.tag.toString() + 
									vectorInstance.annotationType.toString());
						else
							tag = UtilMethods.mergeStrings(tag, 
									vectorInstance.tag.toString() + 
									vectorInstance.relationType.toString());
						break;
					}

				for(String featureName: vectorInstance.attributes.keySet()){
					String content = "";
					if(featureValues.containsKey(featureName))
						content = featureValues.get(featureName);

					content = UtilMethods.mergeStrings(content, 
							vectorInstance.attributes.get(featureName).toString());
					featureValues.put(featureName, content);
				}
			}

			outputLines.add(name);
			outputLines.add(tag);

			for(String featureName : featureOrder)
				outputLines.add(featureValues.get(featureName));

			break;
		default:
			for(AttributeVector vectorInstance: instances)
				outputLines.add(vectorInstance.printVector(vector));
			break;
		}

		//		 also mark the end of a sentence
		outputLines.add("\n");

		return outputLines;
	}

	public void addFeatureValues(HashMap<String, Object> givenAttributes) {
		this.attributes = givenAttributes;
	}

	/**
	 * Return the string value of a feature
	 * @param previousVector
	 * @param featureCount
	 * @param featureValue
	 * @param vector
	 * @return the feature string
	 */
	String writeFeature(String previousVector, int featureCount, 
			String featureValue, VectorType vector){
		String content = previousVector;

		switch(vector){
		case HMM:
			content += " " + featureValue;

			break;
		case MAXENT:
			content += " " + featureValue + " 1";

			break;
		case CRF:
			//            content += " " + featureValue ;
			content += " " + featureValue;

			break;
		case CLASSIFIER:
		default:
			content += " " + featureValue + " 1";
			break;
		}

		return content; 
	}

	/**
	 * 
	 * @param instanceType
	 * @param vector
	 * @param wordAnnotation
	 * @param file
	 * @param tagType
	 * @param crfType 
	 * 
	 * @return
	 */
	String writeLabels(boolean instanceType, VectorType vector,
			AnnotationDetail wordAnnotation, String file, 
			TagType tagType) {
		String vectorLine = "";

		switch(vector){
		case HMM:
		case CODL:
			vectorLine =  printComments(wordAnnotation, file) ;
			break;
		case MAXENT:
			vectorLine =  printComments(wordAnnotation, file) + " " + tagType.toString();
			break;
		case CRF:
			vectorLine = tagType.toString();


			switch(tagType){
			case B:
				vectorLine  += " FIRST  ----";
				break;
			default:
				vectorLine  += " NONE ----";
				break;
			}

			vectorLine += " ----";

			String comment = printComments(wordAnnotation, file);
			vectorLine += " " + comment.replaceAll(" ", "_") ;
			vectorLine = vectorLine.replaceAll("\t", "_") ;
			break; 

		case CLASSIFIER:
		default:
			if(instanceType)
				vectorLine = "+1";
			else 
				vectorLine = "-1";
			break;
		}

		return vectorLine;
	}


	/**
	 * 
	 * @param instanceType
	 * @param vector
	 * @param med
	 * @param reason
	 * @param file
	 * @return
	 */
	String writeLabels(boolean instanceType, VectorType vector,
			AnnotationDetail med,  AnnotationDetail reason, String file) {
		String vectorLine = "";

		switch(vector){
		case CRF:
		case MAXENT:
		case HMM:

			if(instanceType)
				vectorLine = "POS";
			else 
				vectorLine = "NEG";

			String comment = printComments(med, reason, file);
			vectorLine += " " + comment.replaceAll(" ", "_") ;
			vectorLine = vectorLine.replaceAll("\t", "_") ;
			break;                      
		case CLASSIFIER:
		default:
			if(instanceType)
				vectorLine = "+1";
			else 
				vectorLine = "-1";
			break;
		}


		return vectorLine;
	}



	/**
	 * 
	 * @param annt1
	 * @param annt2
	 * @param file
	 * @return
	 */
	static public String printComments(AnnotationDetail annt1, 
			AnnotationDetail annt2, String file){
		String vectorLine = "";

		vectorLine += file ;
		vectorLine += annt1.content + "_" + annt1.mergeOffsets();
		vectorLine += annt2.content + "_" + annt2.mergeOffsets();

		return vectorLine;

	}

	/**
	 * 
	 * @param annt
	 * @param file
	 * @return
	 */
	static public String printComments(AnnotationDetail annt, String file){
		String vectorLine = "";

		if(file.contains("."))
			vectorLine += file ;
		else 
			vectorLine += file + ".i2b2.entries";

		vectorLine += annt.content + "_" + annt.mergeOffsets();

		return vectorLine;

	}

	/**
	 * 
	 * @param instanceType
	 * @param vector
	 * @param wordAnnotation
	 * @param featureValues
	 * @param file
	 * @param tagType
	 * @param crfType 
	 * @return
	 */
	public String featuresToString(boolean instanceType, VectorType vector,
			AnnotationDetail wordAnnotation, ArrayList<Object> featureValues,
			String file, TagType tagType) {
		String vectorLine = writeLabels(instanceType, vector, 
				wordAnnotation, file, tagType);

		int featureCount = 0;

		for(Object featureValue : featureValues){
			vectorLine = writeFeature(vectorLine, featureCount, 
					String.valueOf(featureValue), vector);
			featureCount ++;
		}

		switch(vector){
		case HMM:
			vectorLine = UtilMethods.mergeStrings(vectorLine, tagType.toString());
			break;
		default:
			break;
		}

		//		switch(vector){
		//		case CRF:
		//		case MAXENT:
		//			vectorLine += "\n";
		//			break;
		//		}

		//        vectorLine += printComments(medication.medication, reason.reason, file);

		return vectorLine;
	}

	public void setName(String content) {
		this.name = content;
	}

	public static ArrayList<String> printVectorAnnotations(
			ArrayList<AttributeVector> instances) {
		ArrayList<String> annotations = new ArrayList<String>();

		for(AttributeVector attr : instances)
			annotations.add(printComments(attr.annotation, attr.file));

		return annotations;
	}

	public void setAnnotationType(AnnotationType type) {
		this.annotationType = type;

	}

	public void setRelationType(RelationType type){
		this.relationType = type;
	}

	public void setPOS(String posTag) {

		this.pos = posTag;
	}

}
