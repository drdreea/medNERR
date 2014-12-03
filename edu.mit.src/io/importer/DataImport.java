/**
 * Created on Jan 10, 2012
 * 
 * @author ab Contact andreeab dot mit dot edu
 * 
 */
package io.importer;

import io.log.ExperimentLog;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.MedicalRecord;
import corpus.Annotation.AnnotationType;
import corpus.Relation;
import corpus.Relation.RelationType;

import classifier.TrainTestCreator.CorpusType;
import classifier.attributes.AttributeFactory;

import preprocess.parser.SentenceContent;
import preprocess.stemmer.EnglishStemmer;
import utils.UtilMethods;

/**
 * @author ab
 * 
 */
public class DataImport {
	ExperimentLog log;
	final String annotationFileSuffix = ".i2b2.entries";
	final String xmlFileSuffix = ".xml";
	final String fileSeparator = "<->";

	public DataImport(ExperimentLog log) {
		this.log = log;
	}

	public DataImport() {

	}

	/**
	 * Import the system attributes
	 * @throws IOException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public ArrayList<String> parseAttributeNames(String featuresListFilePath) 
			throws IOException, 
			SecurityException, NoSuchMethodException {
		Class<AttributeFactory> givenClass = AttributeFactory.class;
		ArrayList<String> attributes = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new 
				FileReader(featuresListFilePath));

		String line = null;

		while((line = br.readLine()) != null){
			line = line.trim();

			if(line.startsWith("%") || line.startsWith("//") || 
					line.isEmpty())
				continue;

			if(givenClass.getMethod(line, new Class[0]) != null)
				attributes.add(line);
			else{
				System.out.println("Incorrect attribute name " + line );
			}
		}

		br.close();

		return attributes;
	}

	public ArrayList<AnnotationType> parseNERtoAnnotate(String filePath) 
			throws IOException{
		ArrayList<AnnotationType> attributes = new ArrayList<AnnotationType>();

		BufferedReader br = new BufferedReader(new FileReader(filePath));

		String line = null;

		while((line = br.readLine()) != null){
			line = line.trim();

			if(line.startsWith("%") || line.startsWith("//") || 
					line.isEmpty())
				continue;


			try{
				AnnotationType anntType = AnnotationType.valueOf(line.toUpperCase());
				attributes.add(anntType);
			}catch(Exception e){
				System.out.println(line + " not an annotation type");
			}
		}

		br.close();

		return attributes;
	}

	public ArrayList<RelationType> parseRelationToAnnotate(String filePath) 
			throws IOException{
		ArrayList<RelationType> attributes = new ArrayList<RelationType>();

		BufferedReader br = new BufferedReader(new FileReader(filePath));

		String line = null;

		while((line = br.readLine()) != null){
			line = line.trim();

			if(line.startsWith("%") || line.startsWith("//") || 
					line.isEmpty())
				continue;

			try{
				RelationType anntType = RelationType.valueOf(line);
				attributes.add(anntType);
			}catch(Exception e){
				System.out.println(line + " not an relation type");
			}
		}

		br.close();

		return attributes;
	}


	/**
	 * Import annotation data
	 * 
	 * @param annotationsPath
	 * @return a hash map with info for each annotation file
	 */
	public HashMap<String, AnnotationFile> importAnnotations(
			String annotationsPath) {
		HashMap<String, AnnotationFile> annotationList = new HashMap<String, AnnotationFile>();
		// read the system annotation
		String[] systemFiles = UtilMethods.dirListing(annotationsPath);

		List<String> gsFilesAsList = Arrays.asList(systemFiles);

		for (String file : gsFilesAsList) {
			if (file.startsWith("."))
				continue; // ignore the system files

			// read the system and gs file content
			String fileAnnotationPath = UtilMethods.joinPaths(annotationsPath,
					file);
			ArrayList<String> gsLines = UtilMethods
					.readFileLines(fileAnnotationPath);
			AnnotationFile annotationContent = new AnnotationFile();

			for (String line : gsLines) {
				Annotation result = Annotation.parseAnnotationLine(line, 
						CorpusType.I2B2);
				if (result != null)
					annotationContent.annotations.add(result);
			}

			annotationList.put(new File(fileAnnotationPath).getName(),
					annotationContent);

		}

		return annotationList;
	}

	public static ArrayList<Annotation> readAnnotations(String fileAnnotationPath,
			CorpusType corpusType){
		ArrayList<Annotation> annotations = new ArrayList<Annotation>();

		ArrayList<String> gsLines = UtilMethods
				.readFileLines(fileAnnotationPath);

		for (String line : gsLines) {
			Annotation result = Annotation.parseAnnotationLine(line, corpusType);
			// update the "isList" property for all annotation elements
			if (result != null){
				for(AnnotationType type: result.annotationElements.keySet()){
					AnnotationDetail annt = result.annotationElements.get(type);
					annt.isList = result.isList;
					result.annotationElements.put(type, annt);
				}
				annotations.add(result);
			}
		}

		return annotations;
	}

	public static ArrayList<Relation> readRelations(String fileRelationsPath,
			CorpusType corpusType){
		ArrayList<Relation> relations = new ArrayList<Relation>();

		ArrayList<String> gsLines = UtilMethods
				.readFileLines(fileRelationsPath);

		for (String line : gsLines) {
			Relation result = null;

			String[] splitString = line.split(Annotation.separator);
			AnnotationDetail from = new AnnotationDetail(splitString[0], null);
			AnnotationDetail to = new AnnotationDetail(splitString[2], null);
			RelationType type = RelationType.parseRelationType(splitString[1]);

			result = new Relation(from, to, type);

			if (result != null)
				relations.add(result);
		}

		return relations;
	}

	public MedicalRecord readRecordData(String key, MatcherConfigHandler configs) {
		String rawFilePath = configs.rawFilesPath + "/" + key.split("\\.")[0];
		String annotatedFile = configs.medsPath + "/" + key;

		return readRecordData(rawFilePath, annotatedFile, null);
	}

	public MedicalRecord readRecordData(String key, EMRPatternConfigHandler configs) {
		String rawFilePath = configs.rawFilesPath + "/" + key.split("\\.")[0];
		String annotatedFile = configs.mergedPath + "/" + key;

		return readRecordData(rawFilePath, annotatedFile, null);
	}

	/**
	 * read the medical record
	 * @param key
	 * @param configs
	 * @return the medical record content
	 */
	public MedicalRecord readRecordData(String key,
			ContextAnalysisConfigHandler configs) {
		String rawFilePath = configs.rawPath + "/" + key.split("\\.")[0];
		String annotatedFile = configs.medsPath + "/" + key;

		return readRecordData(rawFilePath, annotatedFile, null);
	}

	/**
	 * read the medical record at the given path
	 * @param rawFilePath
	 * @param annotatedFile
	 * @param configs
	 * @return
	 */
	public MedicalRecord readRecordData(String rawFilePath, String annotatedFile,
			SystemConfigHandler configs) {
		MedicalRecord record = null;

		record = new MedicalRecord(rawFilePath, annotatedFile);
		record.rawFileLines = readFile(rawFilePath);

		return record;
	}

	public ArrayList<MedicalRecord> importRecords(SystemConfigHandler configs) {
		ArrayList<MedicalRecord> records = new ArrayList<MedicalRecord>();

		String[] rawFiles = UtilMethods.dirListing(configs.rawFilesPath);
		String[] annotatedFiles = UtilMethods
				.dirListing(configs.annotationsPath);

		// if the numer of raw files is not equal to the number of annotated
		// files
		// we have to signal this possible error
		if (rawFiles.length != annotatedFiles.length) {
			this.log.writeError("Different number of raw and annotated files");
			this.log.writeError("Raw files :" + String.valueOf(rawFiles.length));
			this.log.writeError("Annotated files :"
					+ String.valueOf(rawFiles.length));

		}

		for (int rawFileIndex = 0; rawFileIndex < rawFiles.length; rawFileIndex++) {
			String annotationFile = configs.annotationsPath + "/"
					+ rawFiles[rawFileIndex] + this.annotationFileSuffix;
			String rawFile = configs.rawFilesPath + "/"
					+ rawFiles[rawFileIndex];

			MedicalRecord newRecord = readRecordData(rawFile, annotationFile,
					configs);
			newRecord.setXmlFilePath(configs.xmlFilePath + "/"
					+ rawFiles[rawFileIndex] + this.xmlFileSuffix);
			records.add(newRecord);
		}

		return records;

	}

	/**
	 * Read all lines in file at given path
	 * 
	 * @param path
	 * @return an array list with file lines
	 */
	public static ArrayList<String> readFile(String path) {
		ArrayList<String> content = new ArrayList<String>();

		File fl = new File(path);
		
		if(!fl.exists())
			return content;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(fl));
			String line = br.readLine();

			while (line != null) {
				content.add(line);
				line = br.readLine();
			}

			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return content;
	}

	public HashSet<String> importAbbreviations(String abbrevPath) {
		HashSet<String> results = new HashSet<String>();

		File abbrevFile = new File(abbrevPath);
		if (!abbrevFile.exists())
			return results;

		try {
			BufferedReader bf = new BufferedReader(new FileReader(abbrevFile));

			String line = bf.readLine();

			while (line != null) {
				if (line.trim().isEmpty())
					continue;

				String[] splitLine = line.split("\\|");
				results.add(splitLine[0]);

				line = bf.readLine();
			}

			bf.close();
		}
		catch (Exception e) {
			System.out.println("Could not read file " + abbrevPath);
		}

		return results;
	}

	public HashMap<String, String> importDailyMedIndicationsPerMed(String path) {
		HashMap<String, String> medIndications = 
				new HashMap<String, String>();

		try {
			File file = new File(path);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList children = doc.getChildNodes();

			for (int s = 0; s < children.getLength(); s++) {
				Node fstNode = children.item(s);

				NodeList subChildren = fstNode.getChildNodes();

				for (int s1 = 0; s1 < subChildren.getLength(); s1++) {
					Node sndNode = subChildren.item(s1);
					ArrayList<Node> toVisit = new ArrayList<Node>();

					if (sndNode.hasChildNodes()) {
						NodeList nodes = sndNode.getChildNodes();
						for (int index = 0; index < nodes.getLength(); index++)
							toVisit.add(nodes.item(index));
					}

					Node prev = null;

					while (!toVisit.isEmpty()) {
						Node current = toVisit.get(0);
						toVisit.remove(0);

						if ( current.getNodeName().equals("title") && 
								current.getTextContent().toLowerCase().equals("indications and usage")) {
							prev = current;
							continue;
						}

						if(prev != null) {
							if(current.getNodeName().equals("text")) {
								String context = current.getTextContent();
								if(context != null) {
									context = context.trim();
									medIndications.put("", context);
								}
								prev = null;
							}
						}

						if (current.hasChildNodes()) {
							NodeList nodes = current.getChildNodes();
							for (int index = 0; index < nodes.getLength(); index++)
								toVisit.add(nodes.item(index));
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return medIndications;
	}
	public HashSet<String> importDailyMed(String path) {
		HashSet<String> med = new HashSet<String>();

		try {
			File file = new File(path);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList children = doc.getChildNodes();

			for (int s = 0; s < children.getLength(); s++) {
				Node fstNode = children.item(s);

				NodeList subChildren = fstNode.getChildNodes();

				for (int s1 = 0; s1 < subChildren.getLength(); s1++) {
					Node sndNode = subChildren.item(s1);
					ArrayList<Node> toVisit = new ArrayList<Node>();

					if (sndNode.hasChildNodes()) {
						NodeList nodes = sndNode.getChildNodes();
						for (int index = 0; index < nodes.getLength(); index++)
							toVisit.add(nodes.item(index));
					}

					while (!toVisit.isEmpty()) {
						Node current = toVisit.get(0);
						toVisit.remove(0);

						if (current.getParentNode().getNodeName().equals(
								"manufacturedMedicine")
								&& current.getNodeName().equals("name"))
							med.add(current.getTextContent());

						if (current.getParentNode().getNodeName().equals(
								"genericMedicine")
								&& current.getNodeName().equals("name"))
							med.add(current.getTextContent());

						if (current.getParentNode().getNodeName().equals(
								"manufacturedProduct")
								&& current.getNodeName().equals("name"))
							med.add(current.getTextContent());

						if (current.getParentNode().getNodeName().equals(
								"activeIngredientSubstance")
								&& current.getNodeName().equals("name"))
							med.add(current.getTextContent());

						if (current.getParentNode().getNodeName().equals(
								"activeMoiety")
								&& current.getNodeName().equals("name"))
							med.add(current.getTextContent());

						if (current.hasChildNodes()) {
							NodeList nodes = current.getChildNodes();
							for (int index = 0; index < nodes.getLength(); index++)
								toVisit.add(nodes.item(index));
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return med;
	}

	public HashSet<String> importDailyMedNames(String dailyMedPath,
			boolean normalization) {
		HashSet<String> results = new HashSet<String>();

		try {
			File in = new File(dailyMedPath);
			BufferedReader br = new BufferedReader(new FileReader(in));
			String line = br.readLine();

			while (line != null) {

				line = line.trim().toLowerCase();
				if (normalization)
					line = EnglishStemmer.process(line);

				if (line.length() > 2)
					results.add(line);

				line = br.readLine();
			}

			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

	public String parsePCMResults(String resultPage) {
		String result = null;


		if(resultPage != null) {
			if(resultPage.contains("ncbi_resultcount")) {
				String subContent = resultPage.substring(resultPage.indexOf("ncbi_resultcount"));
				subContent = subContent.substring(0, subContent.indexOf("/>")).trim();

				String[] splitContent = subContent.split("\"");

				result = splitContent[splitContent.length-1];

			}
		}
		return result;

	}

	/**
	 * @param pcmPath
	 * @return
	 */
	public HashMap<ArrayList<String>, Integer> loadPCMCounts(String pcmPath) {
		HashMap<ArrayList<String>, Integer> counts = new HashMap<ArrayList<String>, Integer>();

		File input = new File(pcmPath);
		if(input.exists()) {
			try {

				FileInputStream rawFile = new FileInputStream(pcmPath);

				DataInputStream in = new DataInputStream(rawFile);
				BufferedReader read = new BufferedReader(new InputStreamReader(in));

				String line = read.readLine();

				while(line !=null) {
					String[] splitLine = line.split("\\|\\|");
					if(splitLine.length == 3 ) {
						ArrayList<String> values = new ArrayList<String>();
						values.add(splitLine[0]);
						values.add(splitLine[1]);

						int count = Integer.parseInt(splitLine[2]);
						counts.put(values, count);	
					}
					line  = read.readLine();
				}

				read.close();
				in.close();
				rawFile.close();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}


		return counts;
	}

	/**
	 * @return
	 */
	public HashMap<ArrayList<String>, HashMap<String, Double>> loadSearchCounts(String searchPath) {
		HashMap<ArrayList<String>, HashMap<String, Double>> counts = 
				new HashMap<ArrayList<String>, HashMap<String, Double>>();

		File input = new File(searchPath);
		if(input.exists()) {
			try {

				FileInputStream rawFile = new FileInputStream(searchPath);

				DataInputStream in = new DataInputStream(rawFile);
				BufferedReader read = new BufferedReader(new InputStreamReader(in));

				String line = read.readLine();

				while(line !=null) {
					String[] splitLine = line.split("\\|\\|");
					if(splitLine.length == 4 ) {
						ArrayList<String> values = new ArrayList<String>();
						HashMap<String, Double> countPerVals = new HashMap<String, Double>();

						values.add(splitLine[0]);
						countPerVals.put(splitLine[0], Double.parseDouble(splitLine[1]));
						values.add(splitLine[2]);
						countPerVals.put(splitLine[2], Double.parseDouble(splitLine[3]));

						counts.put(values, countPerVals);	
					}
					line  = read.readLine();
				}

				read.close();
				in.close();
				rawFile.close();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}


		return counts;
	}

	public HashSet<String> importDailyMedRawContent(String path){
		HashSet<String> results = new HashSet<String>();

		try {
			File in = new File(path);
			BufferedReader br = new BufferedReader(new FileReader(in));
			String line = br.readLine();
			String content = "";

			while (line != null) {

				line = line.trim().toLowerCase();

				if(line.contains(this.fileSeparator)) {
					if(!content.trim().isEmpty())
						results.add(content);
					content = "";
				}else
					content = UtilMethods.mergeStrings(content, line);

				line = br.readLine();
			}

			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

	/**
	 * @param patternsPath
	 * @return
	 */
	public HashMap<String, HashMap<String, Double>> loadPatternQueries(
			String patternsPath) {
		HashMap<String, HashMap<String, Double>> counts = 
				new HashMap<String, HashMap<String, Double>>();

		File input = new File(patternsPath);
		if(input.exists()) {
			try {

				FileInputStream rawFile = new FileInputStream(patternsPath);

				DataInputStream in = new DataInputStream(rawFile);
				BufferedReader read = new BufferedReader(new InputStreamReader(in));

				String line = read.readLine();

				while(line !=null) {
					String[] splitLine = line.split("\\|\\|");
					if(splitLine.length == 5 ) {
						String value = splitLine[0];
						HashMap<String, Double> countPerVals = new HashMap<String, Double>();

						countPerVals.put(splitLine[1], Double.parseDouble(splitLine[2]));
						countPerVals.put(splitLine[3], Double.parseDouble(splitLine[4]));

						counts.put(value, countPerVals);	
					}
					line  = read.readLine();
				}

				read.close();
				in.close();
				rawFile.close();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}


		return counts;
	}

	/**
	 * @param conceptReasons
	 * @return
	 */
	public HashMap<String, String> importConceptReasons(String conceptReasons) {
		HashMap<String, String> reasons = 
				new HashMap<String, String>();

		File input = new File(conceptReasons);
		if(input.exists()) {
			try {

				FileInputStream rawFile = new FileInputStream(conceptReasons);

				DataInputStream in = new DataInputStream(rawFile);
				BufferedReader read = new BufferedReader(new InputStreamReader(in));

				String line = read.readLine();

				while(line !=null) {
					String[] splitLine = line.split("\\|\\|");
					if(splitLine.length == 2 ) {
						String concept = splitLine[1];
						if(concept.contains(","))
							concept = concept.split(",")[0];

						if(!concept.equals(splitLine[0]))
							reasons.put(splitLine[0],concept);	
					}
					line  = read.readLine();
				}

				read.close();
				in.close();
				rawFile.close();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}


		return reasons;
	}

	public HashMap<String, Integer> importWordFrequency(String otherCorpusPath) {
		HashMap<String, Integer> frequencies = new HashMap<String, Integer>();

		File input = new File(otherCorpusPath);
		if(input.exists()) {
			try {

				FileInputStream rawFile = new FileInputStream(otherCorpusPath);

				DataInputStream in = new DataInputStream(rawFile);
				BufferedReader read = new BufferedReader(new InputStreamReader(in));

				String line = read.readLine();

				while(line !=null) {
					String[] splitLine = line.toLowerCase().split("\t");
					if(splitLine.length == 2){
						String word = splitLine[0].trim();
						int freq = Integer.parseInt(splitLine[1]);

						frequencies.put(word, freq);
					}

					line = read.readLine();
				}

				read.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		return frequencies;
	}

	public HashMap<String, String> importSentences(String inputFile) {
		HashMap<String, String> sentences = new HashMap<String, String>();

		File input = new File(inputFile);
		if(input.exists()) {
			try {

				FileInputStream rawFile = new FileInputStream(inputFile);

				DataInputStream in = new DataInputStream(rawFile);
				BufferedReader read = new BufferedReader(new InputStreamReader(in));

				String line = read.readLine();

				while(line !=null) {
					String[] splitLine = line.toLowerCase().split("\t");
					if(splitLine.length == 2){
						String word = splitLine[0].trim();
						String sentence = splitLine[1].trim();

						sentences.put(word, sentence);
					}

					line = read.readLine();
				}

				read.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return sentences;
	}

	/**
	 * Import the semantic types stored to file
	 * @param path
	 * @return a map concepts - semantic types
	 */
	public HashMap<String, ArrayList<String>> importSemanticType(String path){
		HashMap<String, ArrayList<String>> types = 
				new HashMap<String, ArrayList<String>>();

		File input = new File(path);
		if(input.exists()) {
			try {

				FileInputStream rawFile = new FileInputStream(path);

				DataInputStream in = new DataInputStream(rawFile);
				BufferedReader read = new BufferedReader(new InputStreamReader(in));

				String line = read.readLine();

				while(line !=null) {
					String[] splitLine = line.toLowerCase().split("\\|\\|");
					String word = splitLine[0].trim();
					ArrayList<String> wordType = new ArrayList<String>();

					for(int index=1; index< splitLine.length; index++)
						if(!splitLine[index].trim().isEmpty())
							wordType.add(splitLine[index]);

					types.put(word, wordType);

					line = read.readLine();
				}

				read.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		return types;
	}

	public static ArrayList<SentenceContent> readSentences(String sentencesPath) {
		ArrayList<SentenceContent> content = new ArrayList<SentenceContent>();

		File fl = new File(sentencesPath);
		
		if(!fl.exists())
			return content;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(fl));
			String line = br.readLine();

			while (line != null) {
				content.add(new SentenceContent(line));
				line = br.readLine();
			}

			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return content;
		}



}