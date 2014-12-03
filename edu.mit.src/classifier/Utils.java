package classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import corpus.AnnotationDetail;


public class Utils {

	public static String removePunctuation(String word) {
		// remove punctuation signs
		if(word.contains(";") || word.contains(".")) 
			word = word.replaceAll("\\.|;", "");

		return word;
	}

	static LinkedHashMap<AnnotationDetail, Integer> sortHashMap(
			HashMap<AnnotationDetail, Integer> input){
		LinkedHashMap<AnnotationDetail, Integer> tempMap = 
				new LinkedHashMap<AnnotationDetail, Integer>();

		for (AnnotationDetail wsState : input.keySet()){
			tempMap.put(wsState,input.get(wsState));
		}

		List<AnnotationDetail> mapKeys = new ArrayList<AnnotationDetail>(tempMap.keySet());
		List<Integer> mapValues = new ArrayList<Integer>(tempMap.values());
		LinkedHashMap<AnnotationDetail, Integer> sortedMap = 
				new LinkedHashMap<AnnotationDetail, Integer>();
		TreeSet<Integer> sortedSet = new TreeSet<Integer>(mapValues);
		Object[] sortedArray = sortedSet.toArray();
		int size = sortedArray.length;

		for (int i=0; i<size; i++){
			sortedMap.put(mapKeys.get(mapValues.indexOf(sortedArray[i])), 
					(Integer)sortedArray[i]);
		}
		return sortedMap;
	}

	public static HashMap<AnnotationDetail, Integer> sortConcepts(
			ArrayList<AnnotationDetail> conceptDetails, ArrayList<String> recordLines){
		HashMap<AnnotationDetail, Integer> annotationIndexes = 
				new HashMap<AnnotationDetail, Integer>();
		HashMap<Integer, Integer> reportWordCounts = new HashMap<Integer, Integer>();

		int prevCount = 0;

		for(int index = 0; index < recordLines.size(); index++){
			String line = (String) recordLines.get(index);
			String[] splitLine = line.split(" ");

			reportWordCounts.put(index, splitLine.length + prevCount);
			prevCount += splitLine.length;
		}

		for(AnnotationDetail annt : conceptDetails){
			int startLine = annt.startLine;

			if(startLine < 2)
				annotationIndexes.put(annt, 
						reportWordCounts.get(0) + annt.startOffset);
			else
				annotationIndexes.put(annt, 
						reportWordCounts.get(startLine-2) + annt.startOffset);
		}

		return Utils.sortHashMap(annotationIndexes);
	}

}
