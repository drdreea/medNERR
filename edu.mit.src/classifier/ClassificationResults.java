package classifier;

import io.importer.ClassifierConfigHandler;
import io.output.OutputPredictions;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import corpus.AnnotationDetail;
import corpus.MedicalRecord;
import corpus.Annotation.AnnotationType;
import corpus.Relation;
import corpus.Relation.RelationType;

import utils.NLPSystem;
import utils.UtilMethods;
import classifier.TrainTestCreator.CorpusType;
import classifier.TrainTestCreator.VectorType;
import classifier.vector.AttributeVector;
import classifier.vector.VectorCreator.TagType;

/**
 * handle the classification results
 * @author ab
 *
 */
public class ClassificationResults {
	ClassifierConfigHandler configs ;
	ClassifierBase classifier;
	HashMap<String, Integer> positionInPredictionList = new HashMap<String, Integer>();
	int searchBound = 15;
	enum Orient{LEFT, RIGHT};


	/**
	 * 
	 * @param configs
	 * @param classifyInstances
	 */
	public ClassificationResults(ClassifierConfigHandler configs,
			ClassifierBase classifyInstances ){
		this.configs = configs;
		this.classifier = classifyInstances;
	}

	/**
	 * Read the classification results from file
	 * @param testFile
	 * @param vectorType
	 */

	private void readNERPredictions(String testFile, VectorType vectorType) {

		if(!new File(classifier.outputFile).exists())
			return;

		try {
			// read classifier predictions
			// the prediction file is of the form tag \n tag \n tag
			readClassifierPredictions();						

			int count = 0;
			AnnotationType anntType = null;

			ArrayList<AttributeVector> mergedLines = new ArrayList<AttributeVector>();

			for(AttributeVector strLine : classifier.testAnnotations){

				String tagString = classifier.resultLabels.get(count);
				TagType currentTag = TagType.valueOf(tagString.substring(0, 1));
				AnnotationType currentAnnotation = null;

				if(currentTag == TagType.O)
					currentAnnotation = AnnotationType.O;
				else 
					try{
						currentAnnotation = AnnotationType.valueOf(tagString.substring(1));
					}catch(Exception e){
						currentAnnotation = AnnotationType.O;
						currentTag = TagType.O;
					}

				if(currentAnnotation != anntType &&
						anntType != null && !mergedLines.isEmpty()){
					classifier.resultsMap.add(mergedLines);
					mergedLines = new ArrayList<AttributeVector>();	
				}

				anntType = currentAnnotation;

				strLine.annotation.type = anntType;

				// if we found the beginning of a reason entity
				if(currentTag.isBegin()){
					mergedLines.add(strLine);
				}else{
					// if we had an entity previously and a beginning does not follow
					// we then reset the values
					if(currentTag.isInside()){
						if(mergedLines.isEmpty())
							mergedLines = new ArrayList<AttributeVector>();
						else
							mergedLines.add(strLine);
					}else{
						// if we have an other tag
						if(!mergedLines.isEmpty()){
							classifier.resultsMap.add(mergedLines);
							mergedLines = new ArrayList<AttributeVector>();
						}
					}
				}

				count ++;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Read the context predictions from file
	 * @param testFile
	 * @param vectorType
	 */
	private void readContextPredictions(String testFile, VectorType vectorType) {
		ArrayList<String> verifyOutput = new ArrayList<String>();
		ArrayList<String> gsOutput = new ArrayList<String>();

		if(!new File(classifier.outputFile).exists())
			return;

		try {
			// read classifier predictions
			// the prediction file is of the form tag \n tag \n tag
			readClassifierPredictions();						

			int count = 0;
			RelationType anntType = null;

			ArrayList<AttributeVector> mergedLines = new ArrayList<AttributeVector>();

			for(AttributeVector strLine : classifier.testAnnotations){

				String tagString = classifier.resultLabels.get(count);

				TagType currentTag = TagType.valueOf(tagString.substring(0, 1));
				RelationType currentAnnotation = null;

				verifyOutput.add(tagString + " " +strLine.name + " " + 
						strLine.annotation.startLine + " " +
						strLine.file);

				if(strLine.tag == TagType.O)
					gsOutput.add(strLine.relationType.toString() + " " +strLine.name + " " + 
							strLine.annotation.startLine + " " +
							strLine.file);
				else
					gsOutput.add(strLine.tag.toString() + strLine.relationType.toString() + " "
							+ strLine.name + " " + strLine.annotation.startLine + " " + 
							strLine.file);

				if(currentTag == TagType.O)
					currentAnnotation = RelationType.O;
				else {
					try{
						currentAnnotation = RelationType.valueOf(tagString.substring(1));
					}catch(Exception e){
						currentAnnotation = RelationType.O;
						currentTag = TagType.O;
					}
				}

				if(currentAnnotation != anntType &&
						anntType != null && !mergedLines.isEmpty()){
					if(count + 1 < classifier.resultLabels.size() &&
							!classifier.resultLabels.get(count+1).equals("O"))
						classifier.resultsMap.add(mergedLines);
					mergedLines = new ArrayList<AttributeVector>();	
				}

				anntType = currentAnnotation;
				strLine.relationType = anntType;

				// if we found the beginning of an entity
				if(currentTag.isBegin()){
					mergedLines.add(strLine);
				}else{
					// if we had an entity previously and a beginning does not follow
					// we then reset the values
					if(currentTag.isInside()){
						//						if(mergedLines.isEmpty())
						//							mergedLines = new ArrayList<AttributeVector>();
						//						else
						mergedLines.add(strLine);
					}else{
						// if we have an other tag
						if(!mergedLines.isEmpty()){
							if(count + 1 < classifier.resultLabels.size() &&
									!classifier.resultLabels.get(count+1).equals("O"))
								classifier.resultsMap.add(mergedLines);
							mergedLines = new ArrayList<AttributeVector>();
						}
					}
				}

				count ++;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		OutputPredictions.storeContents(classifier.configs.classifierOutputPath + 
				"/predictedTest.txt", 
				verifyOutput);
		OutputPredictions.storeContents(classifier.configs.classifierOutputPath + 
				"/gsTest.txt", 
				gsOutput);
	}

	HashMap<ArrayList<AttributeVector>, Direction> readContextNERPredictions(
			String testFile, VectorType vectorType) {
		if(!new File(classifier.outputFile).exists())
			return null;

		HashMap<ArrayList<AttributeVector>, Direction> results = 
				new HashMap<ArrayList<AttributeVector>, Direction>();

		try {
			// read classifier predictions
			// the prediction file is of the form tag \n tag \n tag
			readClassifierPredictions();						

			int count = 0;

			ArrayList<AttributeVector> mergedLines = new ArrayList<AttributeVector>();

			for(AttributeVector strLine : classifier.testAnnotations){

				String tagString = classifier.resultLabels.get(count);

				TagType currentTag = TagType.valueOf(tagString.substring(0, 1));
				AnnotationType currentAnnotation = null;

				if(currentTag == TagType.O)
					currentAnnotation = AnnotationType.O;
				else {
					try{
						currentAnnotation = AnnotationType.valueOf(tagString.substring(1));
					}catch(Exception e){
						currentAnnotation = AnnotationType.O;
						currentTag = TagType.O;
					}
				}

				strLine.annotation.type = currentAnnotation;

				// if we found the beginning of an entity
				boolean checkOther = false;

				if(currentAnnotation == AnnotationType.O) 
					checkOther = true;
				else{
					if(currentTag.isBegin()){
						mergedLines.add(strLine);
					}else{
						if(currentTag.isInside()){
							mergedLines.add(strLine);
						}else
							checkOther = true;
					}
				}

				// if we have an other tag
				if(checkOther)
					if(!mergedLines.isEmpty()){
						boolean foundContext = false;
						
						if(tagString.substring(0, 1).equals("O")){
							int distanceL = 1;

							while(count - distanceL >= 0){
								if(classifier.resultLabels.get(count-distanceL).endsWith("O") && 
										!foundContext) 
									break;
								else if (classifier.resultLabels.get(count-distanceL).endsWith("O") && 
										foundContext){
									classifier.resultsMap.add(mergedLines);
									results.put(mergedLines, 
											new Direction(distanceL, Orient.LEFT));
									break;
								}
								if(classifier.resultLabels.get(count-distanceL).endsWith("REASON")){
									foundContext = true;
								}

								distanceL ++;
							}

						}else{
							int distanceR = 0;
							
							while(count + distanceR < classifier.resultLabels.size()){
								if(classifier.resultLabels.get(count+distanceR).endsWith("O")) 
									break;
								if(classifier.resultLabels.get(count+distanceR).endsWith("REASON")){
									while(count + distanceR < classifier.resultLabels.size() &&
											classifier.resultLabels.get(count+distanceR).endsWith("REASON"))
										distanceR ++;
									
									classifier.resultsMap.add(mergedLines);
									results.put(mergedLines, 
											new Direction(distanceR, Orient.RIGHT));
									break;
								}

								distanceR ++;
							}
						}

						mergedLines = new ArrayList<AttributeVector>();
					}


				count ++;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		return results;
	}

	private void readClassifierPredictions() 
			throws IOException {
		FileInputStream rawFile = new FileInputStream(classifier.outputFile);

		DataInputStream in = new DataInputStream(rawFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = br.readLine();

		while(strLine != null){
			String label = strLine.trim().split(" ")[0];

			classifier.resultLabels.add(label);

			strLine = br.readLine();
		}

		rawFile.close();	
	}

	public void readConceptResultsMalletSupervisedHalgrim(VectorType vectorType) {
		String testFile = configs.classifierOutputPath + "/allTest.txt";
		readNERPredictions(testFile, vectorType);

		HashMap<String, ArrayList<AnnotationDetail>> results = 
				new HashMap<String, ArrayList<AnnotationDetail>>();

		for(ArrayList<AttributeVector> anntset : this.classifier.resultsMap){
			String file = anntset.get(0).file.split("\\.")[0] + ".i2b2.entries";
			int startLine = 0;
			int startOffset = 0;
			int endLine= 0;
			int endOffset = 0;
			String content = "";

			for(int counts = 0; counts< anntset.size(); counts ++){
				AnnotationDetail annt = anntset.get(0).annotation;

				content = annt.content;

				if(annt.startOffset > startLine && startLine != 0){
					endLine = annt.startOffset;
					endOffset = annt.endOffset;
				}
				if( annt.startOffset == startLine && startLine != 0){
					endLine = annt.startOffset;
					endOffset = annt.endOffset;
				}else{
					startLine = annt.startOffset;
					endLine = annt.startOffset;

					startOffset = annt.endOffset;
					endOffset = annt.endOffset;
				}
			}


			AnnotationDetail annotation = new AnnotationDetail();
			annotation.content = content;
			annotation.startLine = startLine;
			annotation.startOffset = startOffset;
			annotation.endLine = endLine;
			annotation.endOffset = endOffset;

			if(results.containsKey(file)){
				ArrayList<AnnotationDetail> values = results.get(file);
				values.add(annotation);
				results.put(file, values);
			}else{
				ArrayList<AnnotationDetail> values = new ArrayList<AnnotationDetail>();
				values.add(annotation);
				results.put(file, values);
			}
		}

		for(String file : results.keySet()){
			ArrayList<AnnotationDetail> updatedAnnotations = results.get(file);

			//            matchConcepts(updatedAnnotations, file);
			OutputPredictions.storeReasons(this.configs.outputPath + "/" + file, 
					updatedAnnotations);
		}

	}

	public HashMap<String, ArrayList<AnnotationDetail>> readNERresults(
			VectorType vectorType, 
			String testFile, boolean printToFile) {
		readNERPredictions(configs.classifierOutputPath + 
				"/" + testFile, vectorType);

		HashMap<String, ArrayList<AnnotationDetail>> results = 
				new HashMap<String, ArrayList<AnnotationDetail>>();

		for(ArrayList<AttributeVector> anntset : this.classifier.resultsMap){
			String file = anntset.get(0).file.split("\\.")[0] + ".i2b2.entries";

			AnnotationDetail annotation = readAnnotationFromVector(file, 
					anntset);

			// check for overlaps before we put the value in the results
			if(results.containsKey(file)){
				ArrayList<AnnotationDetail> values = results.get(file);
				if(!checkOverlap(values, annotation)){

					values.add(annotation);
					results.put(file, values);
				}
			}else{
				ArrayList<AnnotationDetail> values = new ArrayList<AnnotationDetail>();
				values.add(annotation);
				results.put(file, values);
			}
		}

		if(printToFile)
			for(String file : results.keySet()){
				ArrayList<AnnotationDetail> updatedAnnotations = results.get(file);

				//            matchConcepts(updatedAnnotations, file);
				switch(vectorType){
				case CODL_MEDICATION:
					OutputPredictions.storeMedications(this.configs.outputPath + "/" + file, 
							updatedAnnotations);
					break;
				default:
					OutputPredictions.storeReasons(this.configs.outputPath + "/" + file, 
							updatedAnnotations);
					break;

				}
			}

		return results;
	}

	private AnnotationDetail readAnnotationFromVector(String file,
			ArrayList<AttributeVector> anntset) {
		int endLine= 0;
		int endOffset = 0;
		String content = "";
		AnnotationType type = anntset.get(0).annotation.type;

		for(int counts = 0; counts< anntset.size(); counts ++){
			AnnotationDetail annt = anntset.get(counts).annotation;

			content = UtilMethods.mergeStrings(content, annt.content);
			endLine = annt.endLine;
			endOffset = annt.endOffset;

			if(annt.type != type)
				System.out.println("Different type between the annotations");
		}

		AnnotationDetail annotation = new AnnotationDetail();
		annotation.content = content;
		annotation.startLine = anntset.get(0).annotation.startLine ;
		annotation.startOffset = anntset.get(0).annotation.startOffset ;
		annotation.endLine = endLine ;
		annotation.endOffset = endOffset ;
		annotation.type = type;

		return annotation;
	}

	public void readContextResults(VectorType vectorType, 
			String testFile, NLPSystem system) {
		readContextPredictions(configs.classifierOutputPath + 
				"/" + testFile, vectorType);

		HashMap<String, ArrayList<Relation>> results = 
				new HashMap<String, ArrayList<Relation>>();
		HashMap<String, ArrayList<String>> includedResults = 
				new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> includedMeds = new HashMap<String,
				ArrayList<String>>();

		for(ArrayList<AttributeVector> anntset : this.classifier.resultsMap){
			String file;			
			switch(classifier.corpusType){
			case I2B2:
				file = anntset.get(0).file.split("\\.")[0] ;
				break;
			default:
				file = anntset.get(0).file;
				break;
			}

			if(!includedResults.containsKey(file))
				includedResults.put(file, new ArrayList<String>());
			if(!results.containsKey(file))
				results.put(file, new ArrayList<Relation>());

			MedicalRecord doc = system.medicalRecords.get(file);

			RelationType type = anntset.get(0).relationType;
			AnnotationDetail firstContextEl = anntset.get(0).annotation;
			AnnotationDetail lastContextEl = anntset.get(0).annotation;


			for(int counts = 0; counts< anntset.size(); counts ++){
				AnnotationDetail annt = anntset.get(counts).annotation;

				if(annt.documentOffsetStart< firstContextEl.documentOffsetStart)
					firstContextEl  = annt;
				if(annt.documentOffsetStart > lastContextEl.documentOffsetStart)
					lastContextEl = annt;
			}

			ArrayList<AnnotationDetail> det1List = new ArrayList<AnnotationDetail>();
			ArrayList<AnnotationDetail> det2List = new ArrayList<AnnotationDetail>();

			boolean found = false;
			int index = 0;

			while( index < searchBound){
				AnnotationDetail det = 
						doc.annotationAtWord.get(firstContextEl.documentOffsetStart-index);

				if(checkValidPairing(det, null, classifier.corpusType, type)){
					if(found && det.sentence!= null){
						if(occurTogether(det, det1List.get(det1List.size()-1), 
								det.sentence.sentence.toLowerCase()))
							det1List.add(det);
					}else{
						found = true;
						det1List.add(det);
					}
				}

				index ++;
			}

			found = false;
			index = 0;

			while(index < searchBound && !det1List.isEmpty()){
				AnnotationDetail det = 
						doc.annotationAtWord.get(lastContextEl.documentOffsetEnd+index);

				if(checkValidPairing(det, det1List.get(0), classifier.corpusType, type)){
					if(found && det.sentence!= null){
						if(occurTogether(det2List.get(det2List.size()-1), det, 
								det.sentence.sentence.toLowerCase()))
							det2List.add(det);
					}else{
						found = true;
						det2List.add(det);
					}				}

				index ++;
			}


			if(det1List.isEmpty() || det2List.isEmpty() )
				continue;

			for(AnnotationDetail det1: det1List){
				for(AnnotationDetail det2: det2List){

					// cannot have relation between the same concept
					if(det1.mergeOffsets().equals(det2.mergeOffsets()))
						continue;

					if ( (classifier.corpusType == CorpusType.I2B2 &&
							det1.type == det2.type) || 
							(det1.type == det2.type && 
							type != RelationType.PIP))
						continue;

					Relation relation = new Relation(type);

					// check that the relation was not already included
					String mergedRelationId = det1.mergeOffsets() + "_" +
							det2.mergeOffsets() ;

					if(includedResults.get(file).contains(mergedRelationId))
						continue;
					else{
						ArrayList<String> values = includedResults.get(file);
						values.add(mergedRelationId);
						includedResults.put(file, values);
					}

					relation.to = det1;
					relation.from = det2;

					// mark that we have included this medication
					ArrayList<String> included = new ArrayList<String>();
					if(includedMeds.containsKey(file))
						included = includedMeds.get(file);
					if(det1.type == AnnotationType.M && 
							!included.contains(det1.mergeOffsets()))
						included.add(det1.mergeOffsets());
					if(det2.type == AnnotationType.M && 
							!included.contains(det2.mergeOffsets()))
						included.add(det2.mergeOffsets());
					includedMeds.put(file, included);

					// check for overlaps before we put the value in the results			
					ArrayList<Relation> values = results.get(file);

					values.add(relation);
					results.put(file, values);
				}
			}
		}

		for(String file : system.medicalRecords.keySet()){
			ArrayList<Relation> updatedAnnotations = results.get(file);
			MedicalRecord doc = system.medicalRecords.get(file);
			ArrayList<String> included = includedMeds.get(file);

			if(updatedAnnotations == null){
				updatedAnnotations = new ArrayList<Relation>();
				included = new ArrayList<String>();
			}

			for(AnnotationDetail annt : doc.conceptDetails){
				if(annt.type == AnnotationType.M && 
						included != null &&
						!included.contains(annt.mergeOffsets())){
					Relation rel = new Relation(annt, 
							new AnnotationDetail(AnnotationType.R),
							RelationType.REASON);
					updatedAnnotations.add(rel);
				}
			}


			if(classifier.corpusType == CorpusType.I2B2){
				OutputPredictions.storeRelations(this.configs.outputPath + "/" + 
						file + ".i2b2.entries", 
						updatedAnnotations,
						classifier.corpusType);
			}else 
				OutputPredictions.storeRelations(this.configs.outputPath + "/" + 
						file.split("\\.")[0] + ".rel", 
						updatedAnnotations,
						classifier.corpusType);
		}
	}

	boolean occurTogether(AnnotationDetail fst, AnnotationDetail snd,
			String sentence){
		String mergedConceptsAnd = UtilMethods.mergeStrings(fst, 
				snd,
				"and").toLowerCase();
		String mergedConceptsOr = UtilMethods.mergeStrings(fst, 
				snd,
				"or").toLowerCase();
		String mergedConceptsComma = UtilMethods.mergeStrings(fst, 
				snd,
				",").toLowerCase();

		return ((sentence.contains(mergedConceptsAnd) ||
				sentence.contains(mergedConceptsOr)||
				sentence.contains(mergedConceptsComma)) &&
				fst.type == snd.type) ;
	}

	public void readContextAndConceptResults(VectorType vectorType, 
			String testFile, NLPSystem system) {
		HashMap<String, ArrayList<Relation>> results = 
				new HashMap<String, ArrayList<Relation>>();
		HashMap<String, ArrayList<String>> includedMeds = new HashMap<String,
				ArrayList<String>>();
		HashMap<String, ArrayList<String>> includedResults = 
				new HashMap<String, ArrayList<String>>();

		if(!new File(classifier.outputFile).exists())
			return ;

		HashMap<ArrayList<AttributeVector>, Direction> resultsNE = 
				readContextNERPredictions(testFile, vectorType);

		for(ArrayList<AttributeVector> anntset : resultsNE.keySet()){
			String file = anntset.get(0).file;
			Direction searchDir = resultsNE.get(anntset);
			MedicalRecord doc = system.medicalRecords.get(file);

			if(!includedResults.containsKey(file))
				includedResults.put(file, new ArrayList<String>());
			if(!results.containsKey(file))
				results.put(file, new ArrayList<Relation>());

			// get the annotation
			AnnotationDetail annotation = readAnnotationFromVector(file, 
					anntset);
			if(annotation == null)
				System.out.println("empty annotation here");

			annotation = doc.getOffsetsInDocument(annotation);
			AnnotationDetail medPairing = null;
			
			int index = 1;
			while( index <= searchDir.size + 1){
				if(searchDir.direction == Orient.LEFT)
					medPairing = 
					doc.annotationAtWord.get(annotation.documentOffsetStart-index);
				else
					medPairing = 
					doc.annotationAtWord.get(annotation.documentOffsetEnd+index);

				if(medPairing != null && medPairing.type == AnnotationType.M)
					break;

				index ++;
			}
			// stop if we did not find a medication
			if(medPairing == null || medPairing.type != AnnotationType.M ) continue;

			// otherwise add the medication-reason to the list of relations
			Relation relation = new Relation(annotation, medPairing,
					RelationType.REASON);

			// check that the relation was not already included
			String mergedRelationId = annotation.mergeOffsets() + "_" +
					medPairing.mergeOffsets() ;

			if(includedResults.get(file).contains(mergedRelationId))
				continue;
			else{
				ArrayList<String> values = includedResults.get(file);
				values.add(mergedRelationId);
				includedResults.put(file, values);
			}

			// mark that we have included this medication
			ArrayList<String> included = new ArrayList<String>();
			if(includedMeds.containsKey(file))
				included = includedMeds.get(file);
			if(!included.contains(medPairing.mergeOffsets()))
				included.add(medPairing.mergeOffsets());
			includedMeds.put(file, included);

			// check for overlaps before we put the value in the results			
			ArrayList<Relation> values = results.get(file);
			values.add(relation);
			results.put(file, values);
		}

		for(String file : system.medicalRecords.keySet()){
			if(file.contains("832352"))
				System.out.println("here");
			
			ArrayList<Relation> updatedAnnotations = results.get(file);
			MedicalRecord doc = system.medicalRecords.get(file);
			ArrayList<String> included = includedMeds.get(file);

			if(updatedAnnotations == null || included == null){
				updatedAnnotations = new ArrayList<Relation>();
				included = new ArrayList<String>();
			}

			for(AnnotationDetail annt : doc.conceptDetails){
				if(annt.type == AnnotationType.M && 
						included != null &&
						!included.contains(annt.mergeOffsets())){
					Relation rel = new Relation(annt, 
							new AnnotationDetail(AnnotationType.R),
							RelationType.REASON);
					updatedAnnotations.add(rel);
				}
			}


			if(classifier.corpusType == CorpusType.I2B2){
				OutputPredictions.storeRelations(this.configs.outputPath + "/" + 
						file + ".i2b2.entries", 
						updatedAnnotations,
						classifier.corpusType);
			}else 
				OutputPredictions.storeRelations(this.configs.outputPath + "/" + 
						file.split("\\.")[0] + ".rel", 
						updatedAnnotations,
						classifier.corpusType);
		}

	}


	boolean checkValidPairing(AnnotationDetail det,
			AnnotationDetail det1,
			CorpusType corpus, 
			RelationType type){
		if(det == null)
			return false;

		if(det.type == AnnotationType.O)
			return false;

		switch(corpus){
		case I2B2: 
			if(det.type == AnnotationType.M || det.type == AnnotationType.R)
				return true;
			break;
		default:
			switch(type){
			case TrIP:
			case TrWP:
			case TrCP:
			case TrAP:
			case TrNAP:
				if(det.type == AnnotationType.PROBLEM || 
				det.type == AnnotationType.TREATMENT)
					return true;
				break;
			case TeRP:
			case TeCP:
				if(det.type == AnnotationType.TEST || det.type == AnnotationType.PROBLEM)
					return true;
				break;
			case PIP:
				if(det.type == AnnotationType.PROBLEM)
					return true;
				break;
			default:
				break;
			}
			break;
		}

		return false;

	}

	boolean checkOverlap(ArrayList<AnnotationDetail> annts, AnnotationDetail current){
		AnnotationDetail found = null;

		for(AnnotationDetail el : annts){
			if(el.equalsAnnotation(current) || 
					el.overlapAnnotation(current) || 
					el.overlapDiffLines(current)){
				if(el.content.split(" ").length >= current.content.split(" ").length){
					found = el;
					return true;
				}else{
					found= el;
					break;
				}
			}
		}

		if(found != null)
			annts.remove( found);

		return false;
	}
	
	class Direction{
		int size;
		Orient direction;
		
		public Direction(int sz, Orient dir){
			this.size = sz;
			this.direction = dir;
		}
	}

}
