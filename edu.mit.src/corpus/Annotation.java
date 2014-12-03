/**
 * Created on Jan 16, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package corpus;

import java.util.HashMap;

import classifier.TrainTestCreator.CorpusType;
import classifier.vector.VectorCreator.TagType;

/**
 * Class containing details for an medication annotations
 * @author ab
 *
 */
public class Annotation {

	public static final String fileEnding = ".i2b2.entries";
	public static final String fileEndingRelation = ".con";
	public String file;
	
	public HashMap<AnnotationType, AnnotationDetail> annotationElements;

	public enum AnnotationType {
		M, DO, MO, F, DU, R, LN, // tags for the i2b2 corpus
		PROBLEM, TREATMENT, TEST,  // tags for the i2b2 relations corpus
		
		// TAGS FOR THE ACE CORPUS
		PER_GROUP, PER_INDIVIDUAL, PER_INDETERMINATE, 
		ORG_SPORTS, ORG_COMMERCIAL, ORG_EDUCATIONAL, 
		ORG_ENTERTAINMENT, ORG_GOVERNMENT, ORG_MEDIA,
		ORG_MEDICAL_SCIENCE, ORG_NON_GOVERNMENTAL,
		ORG_RELIGIOUS,

		LOC_CELESTIAL, LOC_ADDRESS, LOC_BOUNDARY,
		LOC_LAND_REGION_NATURAL, LOC_REGION_GENERAL,
		LOC_REGION_INTERNATIONAL, LOC_WATER_BODY,

		GPE_NATION, GPE_STATE_OR_PROVINCE, 
		GPE_POPULATION_CENTER, GPE_CONTINENT,
		GPE_COUNTY_OR_DISTRICT, 
		GPE_SPECIAL, GPE_GPE_CLUSTER,

		WEA_BLUNT, WEA_CHEMICAL, WEA_EXPLODING, WEA_BIOLOGICAL, 
		WEA_NUCLEAR, WEA_PROJECTILE, WEA_SHARO, WEA_SHOOTING,
		WEA_UNDERSPECIFIED, WEA_SHARP,

		FAC_BUILDING_GROUNDS, FAC_AIRPORT, FAC_PATH,
		FAC_PLANT, FAC_SUBAREA_FACILITY,

		VEH_WATER, VEH_AIR, VEH_LAND, VEH_SUBAREA_VEHICLE,
		VEH_UNDERSPECIFIED, 
		O;

		public static AnnotationType chooseAnnotation(TagType currentTag) {
			AnnotationType tag = null;

			if(currentTag == TagType.O)
				return AnnotationType.O;

			tag = AnnotationType.valueOf(currentTag.toString().substring(1));

			return tag;
		}

	}; // tags for the ACE corpus

	public final static String separator = "\\|\\|";
	public final static String quotes = "\""; 

	public String annotation;
	public Boolean isList;

	public Annotation(){
		this.annotationElements = 
				new HashMap<AnnotationType, AnnotationDetail>();		
	}

	public Annotation(Annotation annt){
		this.annotationElements = 
				new HashMap<AnnotationType, AnnotationDetail>();
	}

	public Annotation(String annt) {
		this.annotation = annt;
		this.annotationElements = new HashMap<AnnotationType, AnnotationDetail>();
		
	}

	public Annotation(AnnotationDetail matchingSystemMed,
			AnnotationDetail matchingSystemReason) {
		this.annotationElements = new HashMap<AnnotationType, AnnotationDetail>();
		this.annotationElements.put(AnnotationType.M, matchingSystemMed);
		this.annotationElements.put(AnnotationType.R, matchingSystemReason);
		
	}

	public static AnnotationType parseAnnotationType(String el){
		AnnotationType type = null;
		
		el = el.substring(3, el.length()-1);
		type = AnnotationType.valueOf(el.toUpperCase());
		
		return type;
		
	}

	public void parseAnnotationElement(String el, AnnotationType type) {
		if (el != null && !el.isEmpty()) {
			// get the element type and content
			try{
				String tag = el.substring(0, el.indexOf(quotes)-1);
				
				// check if this is the relation
				if(tag.equalsIgnoreCase("t"))
					return;

				AnnotationType anntType = null;
				if(type != null)
					anntType = type;
				else
					anntType = AnnotationType.valueOf(tag.toUpperCase());

				if (anntType != null) {
					switch(anntType){
					case LN:
						if(el.contains("list"))
							this.isList = true;
						else
							this.isList = false;
						break;
					default:
						AnnotationDetail annt = null;
						annt = new AnnotationDetail(el, anntType);
						this.annotationElements.put(anntType, annt);

						break;
					}
				}else{
					System.out.println("No such annotation");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * Parse the content of an annotation line
	 * and populate the class objects with extracted values
	 * @param line
	 * @return
	 */
	public static Annotation parseAnnotationLine(String line, CorpusType corpusType) {
		Annotation current = new Annotation(line);

		if (line == null || line.isEmpty())
			return null;
		
		switch(corpusType){
		case I2B2:
			// split the annotation line into segments
			String[] splitString = line.split(Annotation.separator);

			for (int index = 0; index < splitString.length; index ++) {
				current.parseAnnotationElement(splitString[index], null);
			}
			break;
		case I2B2RELATION:
			splitString = line.split(Annotation.separator);
			AnnotationType anntType = Annotation.parseAnnotationType(
					splitString[1]);

			current.parseAnnotationElement(splitString[0], anntType);
			
			break;
		case ACE:
			
			break;
		}



		return current;
	}


	public static String printString(Annotation annt){
		String returnValue = "";

		for (AnnotationType key : annt.annotationElements.keySet()){
			if(!returnValue.isEmpty())
				returnValue += "||";
			returnValue += key.toString() + "=";

			AnnotationDetail detail = annt.annotationElements.get(key);

			returnValue += "\"" + detail.content + "\"";
			returnValue += " " + String.valueOf(detail.startLine) + 
					":" + String.valueOf(detail.startOffset);
			returnValue += " " + String.valueOf(detail.endLine) + 
					":" + String.valueOf(detail.endOffset);		
		}

		return returnValue;
	}

	public static String chooseFileEnding(CorpusType type){
		switch(type){
		case I2B2:
			return fileEnding;
		case I2B2RELATION:
			return fileEndingRelation;
		case ACE:
		default:
			return "";
		}
	}
}


