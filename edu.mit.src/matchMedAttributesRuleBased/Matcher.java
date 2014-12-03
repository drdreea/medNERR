/**
 * 
 */
package matchMedAttributesRuleBased;

import io.importer.DataImport;
import io.importer.MatcherConfigHandler;
import io.log.ExtractMedicationLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.MedicalRecord;
import corpus.Annotation.AnnotationType;

import postprocess.bing.BingSearches;
import postprocess.bing.Scheduler;
import postprocess.svm.Mapping;
import postprocess.svm.SVMPredictions;
import preprocess.parser.SentenceContent;
import utils.UtilMethods;

/**
 * @author ab
 * 
 */
public abstract class Matcher {
	public MatcherConfigHandler configs;
	public DataImport importer;
	public BingSearches searchEngine;
	static final int requiredArgs = 1;

	public Scheduler scheduler;
	public HashMap<String, AnnotationFile> goldAnnotations;
	public HashMap<String, ArrayList<Annotation>> mergedAnnotations;
	public HashMap<String, AnnotationFile> reasonAnnotation;
	public HashMap<String, AnnotationFile> medAnnotations;
	public HashMap<String, ArrayList<Mapping>> mappings;
	public HashMap<String, Double> mappingFrequencies;
	public HashSet<String> medications ;
	public HashSet<String> dailyMeds;
	public HashSet<String> patterns;
	public HashSet<String> emrPatterns;
	public HashMap<String, String> conceptReasons;
	HashMap<String, MedicalRecord> medicalRecords;

	public ArrayList<String> medicationsList;

	public ExtractMedicationLog log;
	public HashMap<String, HashMap<String, String>> sentences;

	public Matcher(String path) {
		this.scheduler = new Scheduler();
		this.configs = new MatcherConfigHandler(path);
		this.importer = new DataImport(null);

		mergedAnnotations = new HashMap<String, ArrayList<Annotation>>();
		mappings = SVMPredictions.importMappings(configs.svmModelPath);
		searchEngine =  new BingSearches(this.configs.searchPath, 
				this.configs.patternsOutput);

		log = new ExtractMedicationLog(configs.logOutputFile);

		// import the reason and meds data
		importData();
		loadMedications();
		importMedicalRecords();

		this.dailyMeds = importer.importDailyMedNames(this.configs.dailyMedPath, false);
		this.patterns = importer.importDailyMedNames(this.configs.patternsPath, false);
		this.emrPatterns = importer.importDailyMedNames(this.configs.emrPatternPath, false);
		this.conceptReasons = importer.importConceptReasons(this.configs.conceptReasons);

		this.medicationsList = new ArrayList<String>();
		for(String val : medications)
			this.medicationsList.add(val);
	}

	public Matcher(){
		this.scheduler = new Scheduler();
		this.importer = new DataImport(null);

		mergedAnnotations = new HashMap<String, ArrayList<Annotation>>();
		this.medicationsList = new ArrayList<String>();

	}
	
	void loadMedications() {
		this.medications = new HashSet<String>();

		for(String key : this.goldAnnotations.keySet()) {
			AnnotationFile val = this.goldAnnotations.get(key);
			ArrayList<Annotation> annts = val.annotations;

			for(Annotation annt : annts){
				AnnotationDetail med = annt.annotationElements.get(AnnotationType.M);

				this.medications.add(
						UtilMethods.removePunctuation(med.content));
			}
		}
	}

	private void importMedicalRecords() {
		this.medicalRecords = new HashMap<String, MedicalRecord>();
		for(String file : this.medAnnotations.keySet()){
			MedicalRecord newRecord = importer.readRecordData(file, configs);
			this.medicalRecords.put(file, newRecord);
		}
	}

	/**
	 * @return
	 */
	public HashMap<String, Double> computeMappingFrequencies() {
		HashMap<String, Double> freq = new HashMap<String, Double>();
		HashSet<String> allValues = new HashSet<String>();

		for(String val : this.mappings.keySet()) {
			allValues.add(val);

			ArrayList<Mapping> mappingList = this.mappings.get(val);
			for(Mapping mp : mappingList) {
				allValues.add(mp.map);
			}
		}


		Iterator<String> values = allValues.iterator();

		while(values.hasNext()) {
			String currentVal = values.next();
			int countAlone = 0;
			int countPair = 0;

			for(String val : allValues) {
				if(val.equals(currentVal)) {
					countAlone ++;
				}
				else if(currentVal.contains(val) ) {
					countPair ++;
				}
			}

			if(countPair > 0)
				freq.put(currentVal, 1.0*countAlone/countPair);
			else
				freq.put(currentVal, 1.0*countAlone);

		}


		return freq;
	}


	public void importData() {
		// import the reasons data
		reasonAnnotation = importer.importAnnotations(this.configs.reasonPath);

		medAnnotations = importer.importAnnotations(this.configs.medsPath);

		goldAnnotations = importer.importAnnotations(this.configs.goldPath);

	}

	public ArrayList<Mapping> identifyMapping(String concept){
		ArrayList<Mapping> result = new ArrayList<Mapping>();

		if(this.mappings.containsKey(concept))
			return this.mappings.get(concept);



		return result;
	}

	/**
	 * Store the given sentences
	 * @param sentencesPerFile
	 */
	public void storeSentences(HashMap<String,ArrayList<String>> sentencesPerFile) {
		try {


			for(String file : sentencesPerFile.keySet()) {
				BufferedWriter out = new BufferedWriter(
						new FileWriter(this.configs.sentencesPath + "/" + file));
				ArrayList<String> sentences = sentencesPerFile.get(file);
				for(String sent : sentences){
					out.write(sent + "\n");
					out.flush();
				}

				out.close();

			}

		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param previousQueries
	 */
	public void storeCache(HashMap<ArrayList<String>, 
			Integer> previousQueries) {
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(this.configs.pcmPath));

			for(ArrayList<String> keys : previousQueries.keySet()) {
				int count = previousQueries.get(keys);
				String merged = "";

				for(String val : keys)
					merged = merged + "||" + val;

				merged = merged + "||" + String.valueOf(count);

				merged = merged.replaceFirst("\\|\\|", "");

				out.write(merged + "\n");
				out.flush();

			}

			out.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void storeCacheSearches(HashMap<ArrayList<String>, 
			HashMap<String, Double>> previousQueries) {
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(this.configs.searchPath));

			for(ArrayList<String> keys : previousQueries.keySet()) {
				HashMap<String, Double> count = previousQueries.get(keys);
				String merged = "";

				for(String val : keys) {
					merged = merged + "||" + val;
					merged = merged + "||" + String.valueOf(count.get(val));
				}

				merged = merged.replaceFirst("\\|\\|", "");

				out.write(merged + "\n");
				out.flush();

			}

			out.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param patternQueries
	 */
	public void storePatterns(HashMap<String, HashMap<String, Double>> patternQueries) {
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(this.configs.patternsOutput));

			for(String keys : patternQueries.keySet()) {
				HashMap<String, Double> count = patternQueries.get(keys);
				String merged = keys + "||";

				for(String val : count.keySet()) {
					merged = merged + "||" + val;
					merged = merged + "||" + String.valueOf(count.get(val));
				}

				merged = merged.replaceFirst("\\|\\|", "");

				out.write(merged + "\n");
				out.flush();

			}

			out.close();
		}catch(Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * create a map of the meds located on each line
	 * 
	 * @param meds
	 * @return 
	 */
	public static HashMap<Integer, ArrayList<AnnotationDetail>> createListMedsPerLine(
			ArrayList<AnnotationDetail> meds){
		HashMap<Integer, ArrayList<AnnotationDetail>> medAnnotperLine = 
				new HashMap<Integer, ArrayList<AnnotationDetail>>();

		// go through each med annotation and identify the line on
		// which the medication is located
		for (AnnotationDetail annt : meds) {
			try {
				if (medAnnotperLine.containsKey(annt.startLine)) {
					ArrayList<AnnotationDetail> tmp = medAnnotperLine
							.get(annt.startLine);
					tmp.add(annt);
					medAnnotperLine.put(annt.startLine, tmp);
				}
				else {
					ArrayList<AnnotationDetail> tmp = new ArrayList<AnnotationDetail>();
					tmp.add(annt);
					medAnnotperLine.put(annt.startLine, tmp);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		return medAnnotperLine;
	}

	/**
	 * Create pairings between meds and reasons
	 * where each reason should be within +/- 3 lines away from a medication
	 * 
	 * @param medAnnotperLine
	 * @param reasons
	 * 
	 * @return 
	 */
	public static HashMap<AnnotationDetail, ArrayList<AnnotationDetail>> createMedReasonPairings(
			HashMap<Integer, ArrayList<AnnotationDetail>> medAnnotperLine,
			ArrayList<AnnotationDetail> reasons){
		HashMap<AnnotationDetail, ArrayList<AnnotationDetail>> medReasonPairing = 
				new HashMap<AnnotationDetail, ArrayList<AnnotationDetail>>();

		// go though the reason annotations and see which reason
		// is located close to the med annotations
		for (AnnotationDetail annt : reasons) {
			int anntLine = annt.startLine;

			for (int range = -2; range < 2; range++) {
				if (medAnnotperLine.containsKey(anntLine + range)) {
					ArrayList<AnnotationDetail> medAnntList = medAnnotperLine
							.get(anntLine + range);

					for (AnnotationDetail medAnnt : medAnntList) {
						if (medReasonPairing.containsKey(medAnnt)) {
							ArrayList<AnnotationDetail> pairings = medReasonPairing
									.get(medAnnt);
							pairings.add(new AnnotationDetail(annt));
							medReasonPairing.put(medAnnt, pairings);
						}
						else {
							ArrayList<AnnotationDetail> pairings = new ArrayList<AnnotationDetail>();
							pairings.add(new AnnotationDetail(annt));
							medReasonPairing.put(medAnnt, pairings);
						}
					}
				}
			}
		}

		return medReasonPairing;
	}

	public static Annotation pickCloserMedForReason(
			ArrayList<Annotation> possibleMeds, AnnotationDetail reason) {
		int minDistance = 999;
		Annotation closer = null;

		for (Annotation tmp : possibleMeds){
			AnnotationDetail medication = tmp.annotationElements.get(AnnotationType.M);
			AnnotationDetail closerMed = closer.annotationElements.get(AnnotationType.M);

			if(Math.abs(medication.startLine - reason.startLine) == minDistance){
				if(Math.abs(medication.endOffset - reason.startOffset) < 
						Math.abs(closerMed.endOffset - reason.startOffset)){
					closer = tmp;
					minDistance = Math.abs(medication.startLine - reason.startLine);
				}

			}
			else if(Math.abs(medication.startLine - reason.startLine) < minDistance ){
				closer = tmp;
				minDistance = Math.abs(medication.startLine - reason.startLine);
			}

		}
		return closer;
	}
}
