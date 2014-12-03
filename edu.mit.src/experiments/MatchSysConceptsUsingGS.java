package experiments;

import io.importer.DataImport;
import io.output.OutputPredictions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import classifier.TrainTestCreator.CorpusType;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.Annotation.AnnotationType;

public class MatchSysConceptsUsingGS {
	String systemPath;
	String gsPath;
	String outputPath;

	void execute(){
		// read the gs and the system files
		HashMap<String, ArrayList<Annotation>> systemAnnts = 
				readFiles(this.systemPath);
		HashMap<String, ArrayList<Annotation>> gsAnnts = 
				readFiles(this.gsPath);	

		for(String file : systemAnnts.keySet()){
			if(gsAnnts.keySet().contains(file)){
				ArrayList<Annotation> updatedAnnotations = matchAnnotations(systemAnnts.get(file), 
						gsAnnts.get(file));
				OutputPredictions.storeAnnotations(outputPath + "/" + 
						file, 
						updatedAnnotations,
						CorpusType.I2B2);
			}
		}
	}

	HashMap<String, ArrayList<Annotation>> readFiles(String path){
		HashMap<String, ArrayList<Annotation>> annts = 
				new HashMap<String, ArrayList<Annotation>>();
		String[] files = new File(path).list();

		for(int index = 0; index < files.length; index ++){
			if(files[index].startsWith(".")) continue;
			
			ArrayList<Annotation> currentAnnt = 
					DataImport.readAnnotations(path + "/" + files[index], 
							CorpusType.I2B2);
			annts.put(files[index], currentAnnt);
		}

		return annts;
	}

	ArrayList<Annotation> matchAnnotations(ArrayList<Annotation> systemAnnts,
			ArrayList<Annotation> gsAnnts){
		ArrayList<Annotation> matched = new ArrayList<Annotation>();
		ArrayList<AnnotationDetail> includedAnnotations = new ArrayList<AnnotationDetail>();

		for(Annotation gs : gsAnnts){
			// find a matching system annts
			AnnotationDetail medication = gs.annotationElements.get(AnnotationType.M);
			AnnotationDetail reason = gs.annotationElements.get(AnnotationType.R);

			if(reason != null && !reason.content.equals("nm")){
				AnnotationDetail matchingSystemMed = findMatchingAnnotation(medication, systemAnnts, 
						AnnotationType.M);

				if(matchingSystemMed != null){
					AnnotationDetail matchingSystemReason = findMatchingAnnotation(reason, 
							systemAnnts, 
							AnnotationType.R);
					if(matchingSystemReason != null){
						includedAnnotations.add(matchingSystemReason);
						includedAnnotations.add(matchingSystemMed);
						matched.add(new Annotation(matchingSystemMed, 
								matchingSystemReason));
					}
				}
			}
		}
		
		for(Annotation system: systemAnnts){
			for(AnnotationType annt : system.annotationElements.keySet()){
				if(!includedAnnotations.contains(system.annotationElements.get(annt))){
					matched.add(system);
					break;
				}
			}
		}
		
		return matched;

	}

	AnnotationDetail findMatchingAnnotation(AnnotationDetail annt, 
			ArrayList<Annotation> annts, 
			AnnotationType type){

		for(Annotation current : annts){
			AnnotationDetail currentAnnt = current.annotationElements.get(type);

			if(currentAnnt == null ) continue;
			
			if(annt.equalsAnnotation(currentAnnt) ||
					(annt.overlapAnnotation(currentAnnt) ||
							annt.overlapDiffLines(currentAnnt)))
			{
				return currentAnnt;
			}	
		}

		return null;
	}

	public static void main(String[] args) {
		if(args.length != 3) {
			System.out.println("Incorrect params");
			System.exit(-1);
		}

		MatchSysConceptsUsingGS matcher = new MatchSysConceptsUsingGS();
		matcher.systemPath = args[0];
		matcher.gsPath = args[1];
		matcher.outputPath = args[2];
		matcher.execute();

	}


}
