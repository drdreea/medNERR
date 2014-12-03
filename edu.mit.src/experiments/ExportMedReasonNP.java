package experiments;

import io.importer.DataImport;
import io.importer.MatcherConfigHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.Annotation.AnnotationType;

import utils.UtilMethods;

/**
 * This class computes the co-occurrence frequencies for meds and reasons
 * the co-occurrences are stored to file
 * @author ab
 *
 */
public class ExportMedReasonNP {
	private static int requiredArgs = 1;
	MatcherConfigHandler configs;
	DataImport importer;

	public ExportMedReasonNP(String configsPath){
		configs = new MatcherConfigHandler(configsPath);
		this.importer = new DataImport(null);
	}


	/**
	 * Executor method
	 */
	public void execute(){
		HashMap<String, AnnotationFile> reasonAnnotation = 
				importer.importAnnotations(this.configs.reasonPath);

		HashMap<String, AnnotationFile> medAnnotations = 
				importer.importAnnotations(this.configs.medsPath);
		HashMap<String, Integer> uniqueNPs = new HashMap<String, Integer>();

		for(String file : medAnnotations.keySet()){
			AnnotationFile content = medAnnotations.get(file);
			for(Annotation annt : content.annotations){
				AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);
				
				if(medication == null || medication.content == null)
					continue;
				uniqueNPs.put(UtilMethods.removePunctuation(medication.content),
						0);
			}
		}

		for(String file : reasonAnnotation.keySet()){
			AnnotationFile content = reasonAnnotation.get(file);
			for(Annotation annt : content.annotations){
				AnnotationDetail reason = annt.annotationElements.get(AnnotationType.R);

				if(reason == null || reason.content == null)
					continue;
				uniqueNPs.put(UtilMethods.removePunctuation(reason.content),
						0);
			}
		}

		// store the results to file
		try{
			File outFile = new File(configs.outputPath + "/" + "nps.txt");
			BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

			for(String val : uniqueNPs.keySet()){
				out.write(val);
				out.write("\n");
				out.flush();
			}

			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}

	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != requiredArgs  ) {
			System.out.println(
					"Incorrect arguments! Required configuration file path.");
			System.exit(-1);
		}
		ExportMedReasonNP frequency = 
				new ExportMedReasonNP( args[0]);

		frequency.execute();
	}

}
