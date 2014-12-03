package experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.Annotation.AnnotationType;

import utils.UtilMethods;

import io.importer.DataImport;
import io.importer.MatcherConfigHandler;

/**
 * This class computes the co-occurrence frequencies for meds and reasons
 * the co-occurrences are stored to file
 * @author ab
 *
 */
public class MedReasonFrequency {
	private static int requiredArgs = 1;
	MatcherConfigHandler configs;
	DataImport importer;
	HashMap<String, Integer> frequencies;

	public MedReasonFrequency(String configsPath){
		configs = new MatcherConfigHandler(configsPath);
		this.importer = new DataImport(null);
		frequencies = new HashMap<String, Integer>();

	}


	/**
	 * Executor method
	 */
	public void execute(){
		HashMap<String, AnnotationFile> reasonAnnotation = 
				importer.importAnnotations(this.configs.reasonPath);

		HashMap<String, AnnotationFile> medAnnotations = 
				importer.importAnnotations(this.configs.medsPath);

		// compute the frequencies for all of the files 
		// and store them in the global hashmap frequencies
		for(String file : medAnnotations.keySet()){
			if (!reasonAnnotation.containsKey(file)) {
				System.out.println("Could not find reason file: "+ file );
				continue;
			}

			computeFrequencies(medAnnotations.get(file), 
					reasonAnnotation.get(file));
		}

		// store the results to file
		try{
			File outFile = new File(configs.outputPath + "/" + "freq.txt");
			BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

			for(String val : frequencies.keySet()){
				out.write(val + " : " + String.valueOf(frequencies.get(val)));
				out.write("\n");
				out.flush();
			}

			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}

	}


	/**
	 * Compute the co-occurrence per file
	 * @param meds
	 * @param reasons
	 */
	private void computeFrequencies(AnnotationFile meds,
			AnnotationFile reasons) {

		HashMap<Integer, ArrayList<String>> orderedReasons = 
				new HashMap<Integer, ArrayList<String>>();

		// first get the reasons ordered by line number
		for(Annotation annt : reasons.annotations){
			AnnotationDetail reason = annt.annotationElements.get(AnnotationType.R);
			
			if (annt == null || reason == null){
				System.out.println("reason null");
				continue;
			}
			Integer offset = reason.startLine;
			if(orderedReasons.containsKey(offset)){
				ArrayList<String> values = orderedReasons.get(offset);
				values.add(UtilMethods.removePunctuation(reason.content));
				orderedReasons.put(offset, values);
			}else{
				ArrayList<String> values = new ArrayList<String>();
				values.add(UtilMethods.removePunctuation(reason.content));
				orderedReasons.put(offset, values);
			}
		}

		// we count the frequency of meds and reasons
		for(Annotation annt : meds.annotations){
			AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);
			
			if(annt == null || medication == null) {
				System.out.println("Null med anntotation");
				continue;
			}

			Integer offset = medication.startLine;

			for(int range = -3; range < 3; range ++){
				if(orderedReasons.containsKey(offset + range)){
					ArrayList<String> reasonStrings = orderedReasons.get(offset+range);
					for(String rsn : reasonStrings){
						String merged = UtilMethods.removePunctuation(medication.content) 
								+ "_" + rsn;
						if(this.frequencies.containsKey(merged)){
							this.frequencies.put(merged, this.frequencies.get(merged)+1);
						}else{
							this.frequencies.put(merged, 1);
						}
					}
				}
			}
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
		MedReasonFrequency frequency = 
				new MedReasonFrequency( args[0]);

		frequency.execute();
	}

}
