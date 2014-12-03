package postprocess.topic;

import io.importer.DataImport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import corpus.Annotation;
import corpus.AnnotationDetail;

import postprocess.svm.Mapping;
import postprocess.svm.SVMPredictions;
import preprocess.stemmer.EnglishStemmer;
import utils.UtilMethods;

public class TopicIdentification {
	HashSet<String> dailyMed;
	HashSet<String> abbreviations;
	HashMap<String, String> stemmedWords;

	public enum Topic {
		MEDICATION, LAB, NONE
	};

	HashMap<Topic, HashMap<String, ArrayList<Mapping>>> predictions;
	HashSet<String> stopWords;
	DataImport importer;
	public HashSet<String> means;
	public HashSet<String> incorrectAtributes;

	public TopicIdentification(HashMap<Topic, String> topicPaths ) {
		importer = new DataImport(null);
		this.incorrectAtributes = new HashSet<String>();
		this.stemmedWords = new HashMap<String, String>();

		this.predictions = new HashMap<Topic, HashMap<String, ArrayList<Mapping>>>();
		for (Topic tp : topicPaths.keySet()) {
			HashMap<String, ArrayList<Mapping>> values = SVMPredictions
					.importMappings(topicPaths.get(tp));
			predictions.put(tp, values);
		}
	}
	
	public TopicIdentification(HashMap<Topic, String> topicPaths,
			String stopWords, String meansPath, HashSet<String> meds,
			HashSet<String> abbv) {
		importer = new DataImport(null);
		this.stopWords = importer.importDailyMedNames(stopWords, false);
		this.dailyMed = meds;
		this.abbreviations = abbv;
		this.means = importer.importDailyMedNames(meansPath, false);
		this.incorrectAtributes = new HashSet<String>();

		this.predictions = new HashMap<Topic, HashMap<String, ArrayList<Mapping>>>();
		for (Topic tp : topicPaths.keySet()) {
			HashMap<String, ArrayList<Mapping>> values = SVMPredictions
					.importMappings(topicPaths.get(tp));
			predictions.put(tp, values);
		}
	}

	public Topic computeTopic(AnnotationDetail annt, ArrayList<String> rawLines,
			String record, String parsedMed) {
		String med = annt.content;

		if (parsedMed != null)
			med = parsedMed;

		int offset = 2;
		ArrayList<String> preContext = new ArrayList<String>();
		ArrayList<String> futureContext = new ArrayList<String>();

		String prevLine = rawLines.get(annt.startLine - 2);
		String currentLine = rawLines.get(annt.startLine - 1);
		String futureLine = rawLines.get(annt.startLine);

		// check if the medication is contained within the given line
		String merged = currentLine + " " + futureLine;
		if (!merged.toLowerCase().contains(med.toLowerCase())) {
			// System.out.println(record + ":" + annt.annotation);
			return Topic.NONE;
		}

		String[] splitPrevLine = prevLine.split(" ");
		String[] splitFutureLine = futureLine.split(" ");
		String[] splitLine = currentLine.split(" ");

		for (int index = 0; index < annt.startOffset
				&& index <= offset; index++) {
			if (splitLine.length <= index)
				continue;

			String content = filterWord(splitLine[index]);
			if (content.isEmpty())
				continue;

			if (!preContext.contains(content)
					&& !futureContext.contains(content))
				preContext.add(content);
		}

		if (annt.startLine == annt.endLine)
			for (int index = annt.endOffset + 1; index < splitLine.length
					&& futureContext.size() <= offset; index++) {
				if (splitLine.length <= index)
					continue;

				String content = filterWord(splitLine[index]);
				if (content.isEmpty())
					continue;

				if (!preContext.contains(content)
						&& !futureContext.contains(content))
					futureContext.add(content);
			}
		else {
			// check if the line represents the beginning of a new section
			if (!UtilMethods.mergeStrings(Arrays.asList(splitFutureLine))
					.contains(":"))
				for (int index = annt.endOffset + 1; index < splitFutureLine.length
						&& splitFutureLine.length - index <= offset; index++) {
					String content = filterWord(splitFutureLine[index]);
					if (content.isEmpty())
						continue;

					if (!preContext.contains(content)
							&& !futureContext.contains(content))
						futureContext.add(content);
				}
		}

		String[] line = splitPrevLine;
		int range = 2;
		boolean finished = false;

		while (preContext.size() <= offset && !finished) {
			for (int index = line.length - 1; index >= 0
					&& preContext.size() <= offset; index--) {
				String content = filterWord(line[index]);
				if (content.isEmpty())
					continue;

				if (!preContext.contains(content)
						&& !futureContext.contains(content))
					preContext.add(content);
			}

			range++;
			if (annt.startLine - range < 0)
				finished = true;
			else
				line = rawLines.get(annt.startLine - range).split(
						" ");
		}

		if (annt.startLine == annt.endLine) {
			line = splitFutureLine;
			range = -1;
		}
		else {
			line = rawLines.get(annt.startLine + 1).split(" ");
			range = 1;
		}

		finished = false;
		while (futureContext.size() <= offset && !finished) {
			// check if the line represents the beginning of a new section
			if (UtilMethods.mergeStrings(Arrays.asList(line)).contains(":"))
				break;

			for (int index = 0; index < line.length
					&& futureContext.size() <= offset; index++) {
				String content = filterWord(line[index]);
				if (content.isEmpty())
					continue;

				if (!preContext.contains(content)
						&& !futureContext.contains(content))
					futureContext.add(content);
			}

			range++;
			if (annt.startLine + range < rawLines.size())
				line = rawLines.get(annt.startLine + range).split(
						" ");
			else
				finished = true;
		}

		return checkTopic(preContext, futureContext, annt, record, parsedMed);
	}

	private Topic checkTopic(ArrayList<String> preContext,
			ArrayList<String> futureContext, AnnotationDetail annt, String record,
			String parsedMed) {
		String med = annt.content;
		if (parsedMed != null)
			med = parsedMed;

		String stem = med;
		if (stem.contains("/")) {
			String[] splitStr = stem.split("\\/");
			stem = UtilMethods.mergeStrings(Arrays.asList(splitStr));
		}

		stem = EnglishStemmer.process(stem);
		stem = stem.trim();
		HashMap<Topic, ArrayList<Mapping>> map = new HashMap<Topic, ArrayList<Mapping>>();

		for (Topic tp : this.predictions.keySet()) {
			HashMap<String, ArrayList<Mapping>> values = this.predictions
					.get(tp);
			ArrayList<Mapping> stemMappings = identifyMappings(stem, values);

			map.put(tp, stemMappings);
		}

		ArrayList<String> context = new ArrayList<String>();

		for (String vl : preContext)
			context.add(vl);
		for (String vl : futureContext)
			context.add(vl);

		String[] splitMed = stem.split(" ");
		if (splitMed.length > 1) {
			if (!context.contains(stem))
				context.add(stem);
		}

		HashMap<Topic, Double> result = new HashMap<Topic, Double>();
		HashMap<Topic, Boolean> isEmptyTopic = new HashMap<Topic, Boolean>();
		HashMap<Topic, Double> maxValues = new HashMap<Topic, Double>();

		for (Topic tp : map.keySet()) {
			ArrayList<Mapping> vals = map.get(tp);
			ArrayList<Double> sum = new ArrayList<Double>();
			double maxVal = 0.;
			int totalMap = 1;

			isEmptyTopic.put(tp, vals.isEmpty());

			for (Mapping vl : vals) {
				if (context.contains(vl.map)) {
					totalMap++;
					sum.add(vl.score);
				}

				if (vl.score > maxVal)
					maxVal = vl.score;
			}

			// for (String vl : context) {
			// totalMap++;
			// double score = computeScoreForPhrase(vl, tp);
			// sum.add(score);
			//
			// if (score > maxVal)
			// maxVal = score;
			// }

			maxValues.put(tp, maxVal);

			double finalSum = maxVal;
			for (double val : sum)
				finalSum += val / maxVal;
			finalSum = finalSum / totalMap;

			result.put(tp, finalSum);
		}

		// if the annotation contains med info
		// and the attribute modifier has a higher probability in the med model
		// we then select the med model
		if (containsMedInfo(stem)) {
			if (maxValues.get(Topic.MEDICATION) > maxValues.get(Topic.LAB)
					|| map.get(Topic.MEDICATION).size() > map.get(Topic.LAB)
					.size() || isEmptyTopic.get(Topic.LAB))
				return Topic.MEDICATION;
		}
		// check if there is one topic that is empty
		Topic emptyTopic = null;
		for (Topic tp : isEmptyTopic.keySet()) {
			if (isEmptyTopic.get(tp)) {
				if (emptyTopic == null)
					emptyTopic = tp;
				else {
					if (!stem.isEmpty()) {
						// System.out.println(annt.annotation + " "
						// + tp.toString());
						return Topic.NONE;
					}
				}
			}
		}

		if (emptyTopic != null) {
			for (Topic tp : isEmptyTopic.keySet())
				if (tp != emptyTopic) {
					return tp;
				}
		}
		double max = -1;
		Topic finalTopic = Topic.NONE;

		for (Topic tp : result.keySet()) {
			if (result.get(tp) > max) {
				max = result.get(tp);
				finalTopic = tp;
			}
		}

		if (max <= 0.5)
			return Topic.NONE;

		return finalTopic;
	}

	/**
	 * @param map
	 * @param tp
	 * @return
	 */
	private Double computeScoreForPhrase(String map, String line, Topic tp) {
		HashMap<String, ArrayList<Mapping>> values = this.predictions.get(tp);
		double count = 0.;
		
		// stemm all the words inside the sentence
		ArrayList<String> stemmed = new ArrayList<String>();
		String[] splitLine = line.split(" ");
		int lineLength = splitLine.length;
		
		for(int index = 0; index < lineLength; index ++){
			String currentWord = splitLine[index].trim();
		
			if(this.stemmedWords.containsKey(currentWord))
				stemmed.add(this.stemmedWords.get(currentWord));
			else{
				String stem = EnglishStemmer.process(currentWord);
				stemmed.add(stem);
				this.stemmedWords.put(currentWord, stem);
			}
		}
		
		if(this.stemmedWords.containsKey(map))
			map = this.stemmedWords.get(map);
		else{
			String stem = EnglishStemmer.process(map);
			this.stemmedWords.put(map, stem);
			map = stem;
		}
		
		if (values.containsKey(map)) {
			ArrayList<Mapping> mappings = values.get(map);
			for (Mapping mp : mappings)
				if(stemmed.contains(mp.map))
					count += mp.score;
		}

		return count;
	}

	/**
	 * @param med
	 * @return
	 */
	private boolean containsMedInfo(String med) {
		String[] splitMed = med.split(" ");

		if (this.dailyMed.contains(med))
			return true;

		for (int index = 0; index < splitMed.length; index++) {
			if (this.dailyMed.contains(splitMed[index])
					|| this.abbreviations.contains(splitMed[index]))
				return true;

			String split = med.replace(splitMed[index], "");
			split = split.trim();
			if (this.dailyMed.contains(split)
					|| this.abbreviations.contains(split))
				return true;
		}
		return false;
	}

	private ArrayList<Mapping> identifyMappings(String stem,
			HashMap<String, ArrayList<Mapping>> values) {
		if (values.containsKey(stem))
			return values.get(stem);

		ArrayList<Mapping> result = new ArrayList<Mapping>();

		String[] splitStem = stem.split(" ");

		if (this.dailyMed.contains(stem))
			return result;

		for (int index = 0; index < splitStem.length; index++) {
			if (this.dailyMed.contains(splitStem[index])) {
				String split = stem.replace(splitStem[index], "");
				split = split.trim();
				if (this.dailyMed.contains(split))
					break;
				continue;

			}

			if (values.containsKey(splitStem[index]))
				result.addAll(values.get(splitStem[index]));

			String split = stem.replace(splitStem[index], "");
			split = split.trim();
			if (this.dailyMed.contains(split))
				break;

		}

		return result;
	}

	public String filterWord(String word) {
		String content = UtilMethods.removePunctuation(word).toLowerCase();

		// if (this.stopWords.contains(content))
		// return "";

		if (UtilMethods.isNumber(content))
			return "";

		content = EnglishStemmer.stem(content);
		if (content.length() <= 2)
			return "";

		return content.toLowerCase().trim();
	}
	
	public Topic getTopic(String word, String currentLine){
		double probMed = computeScoreForPhrase(word, currentLine, Topic.MEDICATION);
		double probLab = computeScoreForPhrase(word, currentLine, Topic.LAB);

		if(probMed == 0 && probLab == 0)
			return Topic.NONE;

		if (probMed < probLab)
			return Topic.LAB;
		else 
			return Topic.MEDICATION;
		
	}

	/**
	 * @param annt
	 * @param currentLine
	 * @return
	 */
	public boolean isMedProbable(AnnotationDetail annt, String currentLine) {
		String med = annt.content;
		currentLine = currentLine.toLowerCase();
		if (currentLine.indexOf(med) != -1) {
			String endOfLine = currentLine.substring(currentLine.indexOf(med)
					+ med.length());
			endOfLine = endOfLine.trim();
			String[] split = endOfLine.split(" ");
			String word = EnglishStemmer.process(split[0]);

//			if (this.stopWords.contains(split[0])
//					|| this.stopWords.contains(word))
//				return true;

			// check if the word is a means of admin
			// basically check whether the word occurs with the med inside the
			// medication topic
			if (occurMedTopicOnly(word, med) ) {
				return true;
			}

			if (occurLabTopicOnly(word, med)) {
//				this.incorrectAtributes.add(word);
				// System.out.println(currentLine);
				return false;
			}

			double probMed = computeScoreForPhrase(word, currentLine, Topic.MEDICATION);
			double probLab = computeScoreForPhrase(word, currentLine, Topic.LAB);

			
			if (probMed < probLab)
				return false;
		}
		return true;
	}

	/**
	 * @param word
	 * @param med
	 * @return
	 */
	private boolean occurMedTopicOnly(String word, String med) {
		med = EnglishStemmer.process(med);
		if (this.predictions.get(Topic.MEDICATION).containsKey(word)) {
			ArrayList<Mapping> predMed = this.predictions.get(Topic.MEDICATION)
					.get(word);
			boolean found = false;

			for (Mapping map : predMed) {
				if (map.map.equals(med)) {
					found = true;
					break;
				}
			}

			if (found) {
				if (this.predictions.get(Topic.LAB).containsKey(word)) {
					ArrayList<Mapping> predWord = this.predictions.get(
							Topic.LAB).get(word);
					for (Mapping map : predWord) {
						if (map.map.equals(med)) {
							found = false;
							break;
						}
					}
				}
			}

			if (found)
				return true;
		}
		return false;
	}

	private boolean occurLabTopicOnly(String word, String med) {
		med = EnglishStemmer.process(med);
		if (this.predictions.get(Topic.LAB).containsKey(word)) {
			ArrayList<Mapping> predMed = this.predictions.get(Topic.LAB).get(
					word);
			boolean found = false;

			for (Mapping map : predMed) {
				if (map.map.equals(med)) {
					found = true;
					break;
				}
			}

			if (found) {
				if (this.predictions.get(Topic.MEDICATION).containsKey(word)) {
					ArrayList<Mapping> predWord = this.predictions.get(
							Topic.MEDICATION).get(word);
					for (Mapping map : predWord) {
						if (map.map.equals(med)) {
							found = false;
							break;
						}
					}
				}
			}

			if (found)
				return true;
		}
		return false;
	}
}
