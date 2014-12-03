package conceptExtraction;

import io.importer.ContextAnalysisConfigHandler;
import io.importer.DataImport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.MedicalRecord;
import corpus.Annotation.AnnotationType;

import postprocess.svm.Mapping;
import postprocess.svm.SVMPredictions;
import postprocess.topic.TopicIdentification;
import postprocess.topic.TopicIdentification.Topic;
import preprocess.negation.MainConText;
import preprocess.parser.ParsingDocument;
import preprocess.stemmer.EnglishStemmer;
import preprocess.wiki.WikiDisambiguate;
import utils.StringNumberPair;
import utils.SystemConstants;
import utils.UtilMethods;

public class MedicationExtractionModule {
	final static int reqArgs = 1;

	ContextAnalysisConfigHandler configs;

	private DataImport importer;
	private SVMPredictions predictions;

	private MainConText negIdentifier;

	private WikiDisambiguate wiki;
	private HashSet<String> meds;
	private HashMap<String, ArrayList<Mapping>> mappings;
	private HashSet<String> abbreviations;

	// private ClusterInfo cluster;

	HashSet<String> socialRelatedElements;
	HashMap<Annotation, HashSet<String>> relationalMapping;
	HashSet<String> dailyMed;
	List<String> noMed = Arrays.asList("enzym", "sugar");
	TopicIdentification topics;

	// private HashMap<String, Boolean> clusterPredictions;

	public MedicationExtractionModule(String configsPath) {
		this.configs = new ContextAnalysisConfigHandler(configsPath);
		this.importer = new DataImport(null);
		this.wiki = new WikiDisambiguate();
		this.setPredictions(new SVMPredictions());
		this.meds = new HashSet<String>();

		// initialize the topic identifier
		HashMap<Topic, String> tp = new HashMap<Topic, String>();
		tp.put(Topic.LAB, configs.labTopic);
		tp.put(Topic.MEDICATION, configs.medTopic);

		this.negIdentifier = new MainConText();
		// this.cluster = new ClusterInfo();
		this.socialRelatedElements = new HashSet<String>();

		// this.parser = new Parsing(this.configs.parserPath);

		mappings = SVMPredictions.importMappings(configs.mappingsPath);
		// clusterPredictions =
		// this.cluster.importPredictions(configs.predictionsPath);
		abbreviations = importer.importAbbreviations(configs.abbrevPath);
		dailyMed = importer.importDailyMedNames(configs.dailyMedPath, true);
		relationalMapping = new HashMap<Annotation, HashSet<String>>();

		this.topics = new TopicIdentification(tp, configs.stopWords,
				configs.meansPath, dailyMed, abbreviations);

	}

	/**
	 * Main executor of the class
	 */
	public void execute() {
		// import the meds data
		HashMap<String, AnnotationFile> medsAnnotation = importer
				.importAnnotations(this.configs.medsPath);
		HashMap<String, ArrayList<Annotation>> annotations = new HashMap<String, ArrayList<Annotation>>();

		// get the raw files
		// we will only parse the annotation files that are found among the raw files
		List<String> rawFiles = Arrays.asList(new File(configs.rawPath).list());

		for (String file : medsAnnotation.keySet()) {
			// check if file within the raw files
			String rawFileName = file.split("\\.")[0];
			if(!rawFiles.contains(rawFileName))
				continue;

			AnnotationFile values = medsAnnotation.get(file);

			ArrayList<Annotation> filtered = filterAnnotations(file,
					values.annotations);

			annotations.put(file, filtered);
//			OutputPredictions.storeAnnotations(this.configs.outputPath + "/"
//					+ file, filtered);
		}

		// filter the annotations a second time
		//		for (String file : annotations.keySet()) {
		//			ArrayList<Annotation> values = annotations.get(file);
		//
		//			ArrayList<Annotation> filtered = refilterAnnotations(file, values);
		//			annotations.put(file, filtered);
		//			OutputPredictions.storeAnnotations(this.configs.outputPath + "/"
		//					+ file, filtered);
		//		}

		// remove duplicates from the wiki cache
		this.configs.importCache();
		this.configs.storeCacheUnique(this.configs.cachedData);
		this.configs.storeCache(configs.meansPath, this.topics.means);

		// check meds versus neighbor mappings
		if(isNeighbor)
			for (String file : annotations.keySet()) {
				ArrayList<Annotation> toFilter = new ArrayList<Annotation>();
				ArrayList<Annotation> abbvs = new ArrayList<Annotation>();

				for (Annotation annt : annotations.get(file)) {
					AnnotationDetail medication = annt.annotationElements.get(
							AnnotationType.M);

					String med = medication.content;

					if (checkAbbv(med) || checkDailyMed(med)) {
						abbvs.add(annt);
					}
					else {
						toFilter.add(annt);
					}
				}

				ArrayList<Annotation> filtered = checkMapping(toFilter);
				abbvs.addAll(filtered);
				annotations.put(file, abbvs);

			}

		for (String file : annotations.keySet()) {

			ArrayList<Annotation> toFilter = new ArrayList<Annotation>();
			ArrayList<Annotation> abbvs = new ArrayList<Annotation>();

			for (Annotation annt : annotations.get(file)) {
				AnnotationDetail medication = annt.annotationElements.get(
						AnnotationType.M);

				String med = medication.content;

				if (checkAbbv(med) || checkDailyMed(med)) {
					abbvs.add(annt);
				}
				else {
					toFilter.add(annt);
				}
			}

			ArrayList<Annotation> filteredNew = filterSocial(toFilter);
			abbvs.addAll(filteredNew);
			annotations.put(file, abbvs);
//			OutputPredictions.storeAnnotations(this.configs.outputPath + "/"
//					+ file, abbvs);
		}

		for (String file : annotations.keySet()) {
			ArrayList<Annotation> filteredNew = normalizeAnnotations(file,
					annotations.get(file));
			annotations.put(file, filteredNew);
//			OutputPredictions.storeAnnotations(this.configs.outputPath + "/"
//					+ file, filteredNew);
		}
	}

	/**
	 * @param arrayList
	 * @return
	 */
	private ArrayList<Annotation> normalizeAnnotations(String record,
			ArrayList<Annotation> annotations) {
		MedicalRecord newRecord = importer.readRecordData(record, configs);

		ArrayList<Annotation> filtered = new ArrayList<Annotation>();

		for (Annotation annt : annotations) {
			AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

			if (annt == null || medication == null)
				continue;

			String currentLine = newRecord.rawFileLines
					.get(medication.startLine - 1);
			String futureLine = newRecord.rawFileLines
					.get(medication.startLine);

			annt = normalizeMedForm(annt, currentLine, futureLine);
			if (medication.content.isEmpty())
				continue;

			// if all checks were passed, store the annt
			filtered.add(annt);
		}

		return filtered;
	}

	/**
	 * @param annt
	 * @param currentLine
	 * @param futureLine
	 * @return
	 */
	private Annotation normalizeMedForm(Annotation annt, String currentLine,
			String futureLine) {
		currentLine = currentLine.toLowerCase();
		futureLine = futureLine.toLowerCase();
		AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

		String med = medication.content;

		// first do some annotation cleaning
		String[] splitLine = currentLine.split(" ");
		if (medication.startLine == medication.endLine) {
			String[] splitMed = med.split(" ");

			String updatedMed = "";

			// make sure the index of the annotation are correct
			for (int index = medication.startOffset; index < medication.startOffset
					+ splitMed.length; index++) {
				updatedMed = UtilMethods.mergeStrings(updatedMed,
						splitLine[index]);
			}

			if (!med.isEmpty()) {
				med = updatedMed.trim();

				medication.content = med;
				medication.endOffset = medication.startOffset
						+ splitMed.length - 1;
			}
			// if the annotation contains brackets remove them and
			// re-parse the brackets to ensure they are complete
			if (medication.content.contains("(")) {
				med = medication.content.split("\\(")[0].trim();
				StringNumberPair vals = UtilMethods.checkBrackets(currentLine,
						med);
				if (vals != null) {
					medication.content = vals.getString();
					medication.endOffset = medication.startOffset
							+ medication.content.split(" ").length - 1;
					med = medication.content;
				}
			}
		}

		// perform the attribute discovery
		if (currentLine.contains(med)) {
			annt = attributeDiscovery(annt, currentLine, futureLine);

			// check if there was a set of brackets inside the given input
			// we have to perform this step here as well since we just added new
			// attributes to the annotation
			StringNumberPair medWithBrackets = UtilMethods.checkBrackets(
					currentLine, medication.content);

			if (medWithBrackets != null) {
				medication.content = medWithBrackets.getString();
				medication.endOffset = medication.endOffset
						+ medWithBrackets.getNumber();

			}

			// check if there is a percentage mentioned after the medication
			med = medication.content;
			if (currentLine.contains(med)) {
				String subStr = currentLine.substring(
						currentLine.indexOf(med) + med.length()).trim();
				String[] splitMed = subStr.split(" ");
				if (splitMed[0].endsWith("%")) {
					medication.content = UtilMethods.mergeStrings(
							medication.content, splitMed[0]);
					medication.endOffset = medication.endOffset + 1;
				}
			}

		}
		return annt;
	}

	/**
	 * @param annt
	 * @param currentLine
	 * @param futureLine
	 * @return
	 */
	private Annotation attributeDiscovery(Annotation annt, String currentLine,
			String futureLine) {
		AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

		String med = medication.content;

		String beginning = currentLine.substring(0, currentLine.indexOf(med))
				.trim();
		String[] splitString = beginning.split(" ");
		String close = splitString[splitString.length - 1];
		if (SystemConstants.preAttributes.contains(close)) {
			medication.content = UtilMethods.mergeStrings(
					splitString[splitString.length - 1],
					medication.content);
			medication.startOffset = medication.startOffset - 1;
		}

		String end = currentLine.substring(
				currentLine.indexOf(med) + med.length(), currentLine.length())
				.trim();
		boolean newLine = false;
		if (end.isEmpty()) {
			newLine = true;
			splitString = futureLine.split(" ");
			close = splitString[0];
		}
		else {
			splitString = end.split(" ");
			close = splitString[0];
		}

		if (SystemConstants.postAttributes.contains(UtilMethods
				.removePunctuation(close))) {
			medication.content = UtilMethods.mergeStrings(
					medication.content, close);
			if (newLine) {
				medication.endOffset = 1;
				medication.endLine = medication.endLine + 1;
			}
			else
				medication.endOffset = medication.endOffset + 1;
		}

		if (splitString.length > 2) {
			String merged = UtilMethods.mergeStrings(splitString[0],
					splitString[1]);
			if (SystemConstants.postAttributes.contains(UtilMethods
					.removePunctuation(merged))) {
				medication.content = UtilMethods.mergeStrings(
						medication.content, merged);
				if (newLine) {
					medication.endOffset = 2;
					medication.endLine = medication.endLine + 1;
				}
				else
					medication.endOffset = medication.endOffset + 2;
			}
		}

		if (SystemConstants.mergeAttributes.contains(close)) {
			medication.content = UtilMethods.mergeStrings(
					medication.content, close);
			medication.endOffset = medication.endOffset + 1;
			if (splitString.length > 2) {
				medication.content = UtilMethods.mergeStrings(
						medication.content, splitString[1]);
				medication.endOffset = medication.endOffset + 1;
			}
		}
		return annt;
	}

	private ArrayList<Annotation> filterSocial(ArrayList<Annotation> arrayList) {

		ArrayList<Annotation> filtered = new ArrayList<Annotation>();

		ArrayList<String> stemmed = new ArrayList<String>();

		for (String social : socialRelatedElements) {
			stemmed.add(EnglishStemmer.process(UtilMethods
					.removePunctuation(social)));
		}

		for (Annotation annt : arrayList) {
			AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

			boolean found = true;
			String med = EnglishStemmer.process(UtilMethods
					.removePunctuation(medication.content));
			String[] splitMed = med.split(" ");

			for (String social : stemmed) {
				for (int index = 0; index < splitMed.length; index++) {
					if (splitMed[index].equals(social)) {
						found = false;
						break;
					}
				}

				if (found == false)
					break;
			}

			if (found)
				filtered.add(annt);
		}

		for (Annotation annt : relationalMapping.keySet()) {
			HashSet<String> values = relationalMapping.get(annt);
			values = filterOccurrence(values, stemmed);
			// if (annt.medication.content.contains("etoh"))
			// System.out.println("here");

			relationalMapping.put(annt, values);
		}

		boolean foundEmpty = true;

		ArrayList<String> removed = new ArrayList<String>();

		while (foundEmpty) {
			foundEmpty = false;
			ArrayList<String> toRemove = new ArrayList<String>();

			final Iterator<Annotation> mapIter = filtered.iterator();

			while (mapIter.hasNext()) {
				Annotation key = mapIter.next();

				if (!relationalMapping.containsKey(key))
					continue;

				if (relationalMapping.get(key).isEmpty()) {
					// if (key.medication.content.contains("etoh"))
					// System.out.println("here");

					mapIter.remove();
					AnnotationDetail medication = key.annotationElements.get(AnnotationType.M);

					if (!removed.contains(EnglishStemmer
							.process(medication.content))) {
						toRemove.add(EnglishStemmer.process(UtilMethods
								.removePunctuation(medication.content)));
						toRemove.add(EnglishStemmer
								.process(medication.content));
						removed.add(EnglishStemmer
								.process(medication.content));
					}
				}
			}

			if (!toRemove.isEmpty()) {
				foundEmpty = true;
				for (Annotation annt : relationalMapping.keySet()) {
					HashSet<String> values = relationalMapping.get(annt);
					values = filterOccurrence(values, toRemove);
					relationalMapping.put(annt, values);
				}
			}
		}

		return filtered;
	}

	/**
	 * Filter the occurrences of the given stems within the values
	 * 
	 * @param values
	 * @param stemmed
	 * @return
	 */
	private HashSet<String> filterOccurrence(HashSet<String> values,
			ArrayList<String> stemmed) {
		Iterator<String> it = values.iterator();

		while (it.hasNext()) {
			String val = it.next();
			String[] splitVal = val.split(" ");
			boolean found = false;

			for (String stem : stemmed) {

				for (int index = 0; index < splitVal.length; index++) {
					if (splitVal[index].equals(stem)) {
						found = true;
						break;
					}
				}

				if (val.equals(stem))
					found = true;
				if (found)
					break;
			}

			if (found)
				it.remove();
		}

		return values;
	}

	/**
	 * First phase of annotation filtering. We check 1. annotations that end
	 * with colon 2. annotations that are located in incorrect section types 3.
	 * annotations that contain dailyMed medications: we no longer filter those
	 * medications 4. annotations that represent food according to wikipedia
	 * 
	 * @param record
	 * @param annotations
	 * @return
	 */
	private ArrayList<Annotation> filterAnnotations(String record,
			ArrayList<Annotation> annotations) {
		MedicalRecord newRecord = importer.readRecordData(record, configs);
		
		ArrayList<Annotation> filtered = new ArrayList<Annotation>();

		for (Annotation annt : annotations) {
			AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

			if (annt == null || medication == null)
				continue;

			String currentLine = newRecord.rawFileLines
					.get(medication.startLine - 1);

			String futureLine = newRecord.rawFileLines
					.get(medication.startLine);

			currentLine = UtilMethods.identifySentence(currentLine, futureLine,
					medication.content, medication.startLine,
					newRecord.rawFileLines.size(),
					ParsingDocument.Type.MEDICATION);

			annt = normalizeAnnt(annt, currentLine, newRecord.rawFileLines
					.get(medication.startLine - 1));

			String med = medication.content;

			if(isStingy){

				/*
				 * FILTER RULE.			
				 * do not include annotations that are less than 4 in size
				 * unless they are an abbreviation or a medication
				 */
				if (incorrectAbbreviations(med)) {
					this.socialRelatedElements.add(med);
					continue;
				}

				/*
				 * FILTER RULE.
				 * remove annotations that end with colon
				 */
				if (medication.content.contains(":"))
					continue;

//				/*
//				 * FILTER RULE
//				 * Section type: 
//				 */
//				if (!this.sectionizer.validSection(recordSections, annt,
//						newRecord.rawFileLines.get(medication.startLine - 1),
//						futureLine))
//					continue;

				/*
				 * FILTER RULE
				 * remove negated annotations
				 */
				if (this.checkNegation(med, currentLine))
					continue;

				if (this.abbreviations.contains(UtilMethods.removePunctuation(med))) {
					filtered.add(annt);
					continue;
				}

				/*
				 * FILTER RULE:
				 * check the topic for the given annotation
				 */
				Topic topic = this.topics.computeTopic(medication,
						newRecord.rawFileLines, record, null);

				if (topic.equals(Topic.LAB))
					continue;

				/*
				 * FILTER RULE: check the probability of the current word to be a medication
				 * according to the topic model
				 */
				if (!this.topics.isMedProbable(medication, currentLine)) {
					// System.out.println(currentLine);
					continue;
				}

				// if the medication is contained in daily med, stop other checks
				if (checkDailyMed(med)) {
					filtered.add(annt);
					continue;
				}

				/*
				 * FILTER RULE:
				 * double check in wikipedia if the annotation is some sort of food
				 */
				if (!this.abbreviations.contains(medication.content)) {
					String toSearch = medication.content;
					if (toSearch.contains("(")) {
						toSearch = toSearch.split("\\(")[0];
						toSearch = toSearch.trim();
					}
					if (configs.cachedData.containsKey(toSearch)) {
						if (configs.cachedData.get(toSearch)) {
							continue;
						}
					}
					else {
						boolean found = this.wiki.findFood(toSearch);
						configs.cachedData.put(toSearch, found);
						if (found)
							continue;
					}
				}
			}

			// if all checks were passed, store the annt
			filtered.add(annt);
		}

		for (Annotation annt : filtered){
			AnnotationDetail medication = annt.annotationElements.get(
					AnnotationType.M);

			meds.add(medication.content);
		}
		
		return filtered;
	}

	/**
	 * @param med
	 * @return
	 */
	private boolean incorrectAbbreviations(String med) {
		if (med.length() <= 4
				&& !(this.abbreviations.contains(UtilMethods
						.removePunctuation(med)) || checkDailyMed(UtilMethods
								.removePunctuation(med))))
			return true;

		if (med.contains("/")) {
			String[] splitMed = med.split("\\/");
			int incorrect = 0;

			for (int index = 0; index < splitMed.length; index++) {
				if (incorrectAbbreviations(splitMed[index]))
					incorrect++;
			}

			if (incorrect == splitMed.length)
				return true;
		}
		return false;
	}

	ArrayList<Annotation> refilterAnnotations(String record,
			ArrayList<Annotation> annotations) {
		MedicalRecord newRecord = importer.readRecordData(record, configs);

		ArrayList<Annotation> filtered = new ArrayList<Annotation>();

		for (Annotation annt : annotations) {
			AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);
			
			if (annt == null || medication == null)
				continue;

			String currentLine = newRecord.rawFileLines
					.get(medication.startLine - 1);

			String futureLine = newRecord.rawFileLines
					.get(medication.startLine);

			currentLine = UtilMethods.identifySentence(currentLine, futureLine,
					medication.content, medication.startLine,
					newRecord.rawFileLines.size(),
					ParsingDocument.Type.MEDICATION);

			annt = normalizeAnnt(annt, currentLine, newRecord.rawFileLines
					.get(medication.startLine - 1));

			// check the probability of the neighboring word
			if (!this.topics.isMedProbable(medication, currentLine)) {
				System.out.println(currentLine);
				continue;
			}

			boolean incorrect = false;
			String med = UtilMethods.removePunctuation(medication.content);

			for (String vl : this.topics.incorrectAtributes) {
				if (med.endsWith(vl) && !dailyMed.contains(med)) {
					System.out.println(currentLine);
					incorrect = true;
					break;
				}
			}

			if (incorrect)
				continue;
			// if all checks were passed, store the annt
			filtered.add(annt);
		}

		return filtered;
	}

	/**
	 * @param parsedMed
	 * @param string
	 * @return
	 */
	boolean incorrectEnding(String parsedMed, String line) {
		line = line.toLowerCase();

		if (line.contains(parsedMed)) {
			int location = line.indexOf(parsedMed);
			if (location + parsedMed.length() < line.length()) {
				char cChar = line.charAt(location + parsedMed.length());
				if (cChar == ':')
					return true;
			}
		}
		return false;
	}

	/**
	 * Match medications to BEAGLE mappings
	 * 
	 * @param annotations
	 * @return
	 */
	private ArrayList<Annotation> checkMapping(ArrayList<Annotation> annotations) {
		ArrayList<Annotation> filtered = new ArrayList<Annotation>();
		HashMap<String, Integer> stemmedAnnts = new HashMap<String, Integer>();

		for (String med : meds) {

			String medStemmed = UtilMethods.removePunctuation(med);
			medStemmed = EnglishStemmer.process(medStemmed);
			if (medStemmed.trim().isEmpty())
				continue;

			if (stemmedAnnts.containsKey(medStemmed))
				stemmedAnnts.put(medStemmed, stemmedAnnts.get(medStemmed) + 1);
			else
				stemmedAnnts.put(medStemmed, 1);
		}

		for (String med : this.dailyMed) {

			String medStemmed = UtilMethods.removePunctuation(med);
			medStemmed = EnglishStemmer.process(medStemmed);
			if (medStemmed.trim().isEmpty())
				continue;

			if (stemmedAnnts.containsKey(medStemmed))
				stemmedAnnts.put(medStemmed, stemmedAnnts.get(medStemmed) + 1);
			else
				stemmedAnnts.put(medStemmed, 1);
		}

		for (Annotation annt : annotations) {
			AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);
			String med = medication.content;

			if (this.abbreviations.contains(med) || this.dailyMed.contains(med)) {
				filtered.add(annt);
				continue;
			}

			String medStemmed = UtilMethods.removePunctuation(med);
			medStemmed = EnglishStemmer.process(medication.content);

			if (medStemmed.trim().isEmpty() && med.length() > 2) {
				medStemmed = med;
			}

			ArrayList<Mapping> values = mappingsPartial(medStemmed);

			if (values != null) {
				boolean found = false;

				// if the word "social" is contained within the values we
				// continue
				for (Mapping mp : values)
					if (mp.map.equals("social") || mp.map.equals("sh")) {
						socialRelatedElements.add(medStemmed);
						continue;
					}

				found = findMapping(values, med, medStemmed, annt, stemmedAnnts);

				if (found == false) {
					String[] splitMed = medStemmed.split(" ");
					int count = 0;

					for (int index = 0; index < splitMed.length; index++) {
						boolean tmpFind = false;
						String tmpMed = UtilMethods
								.removePunctuation(splitMed[index]);
						tmpMed = EnglishStemmer.process(tmpMed);
						ArrayList<Mapping> tmpValues = mappingsPartial(tmpMed);
						if (tmpValues != null)
							tmpFind = findMapping(tmpValues, med, tmpMed, annt,
									stemmedAnnts);
						if (tmpFind)
							count++;
					}

					if (count == splitMed.length)
						found = true;
				}

				if (found)
					filtered.add(annt);
				else {
					socialRelatedElements.add(medStemmed);
				}
			}
			else
				filtered.add(annt);

		}

		return filtered;
	}

	/**
	 * Find medication mappings based on BEAGLE mappings
	 * 
	 * @param values
	 * @param med
	 * @param medStemmed
	 * @param annt
	 * @param stemmedAnnts
	 * @return
	 */
	private boolean findMapping(ArrayList<Mapping> values, String med,
			String medStemmed, Annotation annt,
			HashMap<String, Integer> stemmedAnnts) {
		boolean found = false;

		for (Mapping map : values) {
			for (String stem : stemmedAnnts.keySet()) {
				if (stem.equals(map.map) && !medStemmed.contains(map.map)
						&& !map.map.contains(medStemmed)) {
					// we don't want the stem and the medStemmed to be partial
					// overlaps
					if (!(UtilMethods.checkStringOverlapStemmed(stem, med) || UtilMethods
							.checkStringOverlap(med, stem))
							&&
							!this.noMed.contains(stem)
							&& (stem.length() > 3 || this.abbreviations
									.contains(stem))) {
						found = true;
						if (relationalMapping.containsKey(annt)) {
							HashSet<String> mappings = relationalMapping
									.get(annt);
							mappings.add(map.map);
							relationalMapping.put(annt, mappings);
						}
						else {
							HashSet<String> mappings = new HashSet<String>();
							mappings.add(map.map);
							relationalMapping.put(annt, mappings);
						}
					}
				}
			}
		}

		return found;
	}

	/**
	 * Check if the given stemmed medication is contained within the mappings
	 * list
	 * 
	 * @param stem
	 * @return
	 */
	private ArrayList<Mapping> mappingsPartial(String stem) {

		if (this.mappings.containsKey(stem))
			return this.mappings.get(stem);

		// if(!values.isEmpty()) {
		// if(values.size() <= 10)
		// return values;
		//
		// HashMap<String, Double> valueMap = new HashMap<String, Double>();
		// for(Mapping mp : values)
		// valueMap.put(mp.map, mp.score);
		//
		// List<Entry<String, Double>> sortedMap =
		// MapUtilities.sortByValue(valueMap);
		// values.clear();
		//
		// for(int index = 0; index < sortedMap.size(); index++ )
		// values.add(new
		// Mapping(sortedMap.get(sortedMap.size()-index-1).getKey(),
		// sortedMap.get(sortedMap.size() - index -1).getValue()));
		//
		// return values;
		// }

		// for(String val : this.mappings.keySet()) {
		// boolean found = false;
		//
		// if(val.contains(stem)) {
		// String[] splitVal = val.split(" ");
		// for(int index=0; index< splitVal.length; index++) {
		// if(splitVal[index].equals(stem)) {
		// this.socialRelatedElements.add(stem);
		// found = true;
		// break;
		// }
		//
		// String merged = UtilMethods.mergeStrings(
		// Arrays.asList(splitVal).subList(index, splitVal.length));
		// if(merged.equals(stem)) {
		// found = true;
		// break;
		// }
		//
		// merged = UtilMethods.mergeStrings(
		// Arrays.asList(splitVal).subList(0, splitVal.length-index));
		// if(merged.equals(stem)) {
		// found = true;
		// break;
		// }
		// }
		//
		// if(found) return this.mappings.get(val);
		// }
		// }

		return null;
	}

	/**
	 * Normalize the content of the givena annotation
	 * 
	 * @param annt
	 * @param currentLine
	 * @param originalLine
	 * @return
	 */
	private Annotation normalizeAnnt(Annotation annt, String currentLine,
			String originalLine) {
		AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);
		
		String med = medication.content;

		if (med.endsWith("&")) {
			med = med.replace("&", "").trim();
			medication.content = med;
			medication.endOffset = medication.endOffset - 1;
		}

		if (med.contains(".") && !med.endsWith(".")) {
			if (!med.contains("vit")) {

				String[] splitMed = med.split("\\.");
				if (medication.endOffset - splitMed.length + 1 >= 0) {
					medication.content = splitMed[0] + ".";
					medication.endOffset = medication.endOffset
							- splitMed.length + 1;
				}
			}
		}

		// check if annt is missing the vit. word
		String[] splitLine = originalLine.toLowerCase().split(" ");
		for (int index = 0; index < splitLine.length; index++) {
			if (med.startsWith(splitLine[index])) {
				if (index - 1 >= 0) {
					if (splitLine[index - 1].startsWith("vit")) {
						med = UtilMethods.mergeStrings(splitLine[index - 1],
								med);
						medication.content = med;
						medication.startOffset = medication.startOffset - 1;
					}
				}

				break;
			}
		}
		return annt;
	}

	/**
	 * Check whether the given med contains an abbreviation
	 * 
	 * @param med
	 * @return
	 */
	public boolean checkAbbv(String med) {
		String[] splitMed = med.split(" ");

		for (int index = 0; index < splitMed.length; index++) {
			String medTmp = UtilMethods.removePunctuation(splitMed[index]);
			if (this.abbreviations.contains(medTmp))
				return true;
		}

		return false;
	}

	/**
	 * Check if medication is found in dailymed
	 * 
	 * @param med
	 * @return
	 */
	public boolean checkDailyMed(String med) {
		med = med.replace("\\", " ");
		if (med.contains("("))
			med = med.split("\\(")[0].trim();

		med = UtilMethods.removePunctuation(med);
		med = EnglishStemmer.process(med);

		if (med.isEmpty())
			return false;

		if (this.dailyMed.contains(med))
			return true;

		String medTmp = UtilMethods.removePunctuation(med);
		if (this.dailyMed.contains(medTmp))
			return true;

		String[] splitMed = med.split(" ");
		for (int index = 0; index < splitMed.length; index++) {
			medTmp = UtilMethods.removePunctuation(splitMed[index]);
			if (!this.dailyMed.contains(medTmp))
				return false;
		}

		return false;
	}

	/**
	 * Verify whether there is a negation within the given input sentence. We
	 * then check whether the given medication is within the scope of the
	 * negations
	 * 
	 * @param foundMed
	 * @param input
	 * @return
	 */
	public boolean checkNegation(String foundMed, String input) {
		String negationScope;
		try {
			// foundMed = UtilMethods.removePunctuation(foundMed);

			negationScope = this.negIdentifier.test(input);
			if (negationScope != null && !negationScope.contains("-1")) {
				String[] indexes = negationScope.split("-");
				int startWord = Integer.parseInt(indexes[0].trim());
				int endWord = Integer.parseInt(indexes[1].trim());

				String[] splitInput = input.split(" ");
				String scopeString = "";

				if (endWord >= splitInput.length)
					endWord = splitInput.length - 1;

				for (int range = startWord; range <= endWord; range++) {
					scopeString = UtilMethods.mergeStrings(scopeString,
							splitInput[range]);
				}

				if (scopeString.toLowerCase().contains(foundMed.toLowerCase())) {
					return true;
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	boolean isStingy = true;
	boolean isNeighbor = true;


	public SVMPredictions getPredictions() {
		return predictions;
	}

	public void setPredictions(SVMPredictions predictions) {
		this.predictions = predictions;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != reqArgs) {
			System.out.println("Incorrect params");
			System.exit(-1);
		}

		MedicationExtractionModule analyzer = new MedicationExtractionModule(args[0]);
		analyzer.execute();
	}
}