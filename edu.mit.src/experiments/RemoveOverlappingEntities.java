package experiments;

import io.importer.DataImport;
import io.output.OutputPredictions;

import java.util.ArrayList;
import java.util.HashMap;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.Annotation.AnnotationType;



public class RemoveOverlappingEntities {

	public static void execute(String input, String output) {
		DataImport importer = new DataImport(null);
		
		HashMap<String, AnnotationFile> annotations = 
				importer.importAnnotations(input);
		
		for(String file : annotations.keySet()) {
//			AnnotationFile value = annotations.get(file);
//			ArrayList<Annotation> updated = filter(value.annotations);
			OutputPredictions.storeAnnotations(output + "/" + file, 
					null);
		}
	}
	
	static ArrayList<Annotation> filter(ArrayList<Annotation> annotations) {
		ArrayList<Annotation> updated = new ArrayList<Annotation>();
		
		for(int index=0; index<annotations.size(); index++) {
			boolean found = false;
			
			for(int offset= index+1; offset< annotations.size()-1; offset++) {
				AnnotationDetail a1Med = annotations.get(index).annotationElements.get(
						AnnotationType.M);
				AnnotationDetail a2Med = annotations.get(offset).annotationElements.get(
						AnnotationType.M);
				
				if(a1Med.equalsAnnotation(a2Med) ||
						a1Med.overlapAnnotation(a2Med)) {
					System.out.println(a1Med);
					System.out.println(a2Med);
					System.out.println("======================");

					if(Math.abs(a1Med.startOffset-a1Med.endOffset) >
					Math.abs(a2Med.startOffset-a2Med.endOffset))
					updated.add(annotations.get(index));
					else updated.add(annotations.get(index));
					found = true;
					break;
				}
			}
			
			if(!found) updated.add(annotations.get(index));
		}
		
		
		return updated;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 2) {
			System.out.println("Incorrect params");
			System.exit(-1);
		}
		
		execute(args[0], args[1]);

	}

}
