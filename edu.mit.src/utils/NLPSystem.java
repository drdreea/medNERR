package utils;

import io.importer.DataImport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import corpus.Annotation;
import corpus.MedicalRecord;
import corpus.Relation;
import corpus.Sectionizer;
import corpus.Annotation.AnnotationType;

import classifier.TrainTestCreator.CorpusType;

import preprocess.parser.ImportSyntacticTags;
import preprocess.phonemes.Phonemizer;


/**
 * Class for storing the annotations of a given system
 * @author ab
 *
 */
public class NLPSystem {

	public HashMap<AnnotationType, HashMap<String, Integer>> otherInGoldFrequencies;
	public HashMap<AnnotationType, HashMap<String, Integer>> otherStartGoldFrequencies;

	DataImport importer;
	HashMap<String, ArrayList<String>> conceptSemanticTypes;

	public HashMap<String, MedicalRecord> medicalRecords;
	
	private String sectionsTemplate;
	ImportSyntacticTags importTags;
	private Sectionizer sectionizer;

	public NLPSystem(String conceptsFile, String relationsFile,
			String sectionsPath,
			String rawFilesPath, String parsedDataPath,
			HashMap<String, ArrayList<String>> conceptSemanticTypes,
			String sectionsTemplate, ImportSyntacticTags tagsImporter,
			String phonemesPath, CorpusType corpusType ){
		this.sectionsTemplate = sectionsTemplate;
		init(conceptSemanticTypes);
		
		importTags = tagsImporter;
		medicalRecords = new HashMap<String, MedicalRecord>();
		
		// import the content of the medical records
		String[] rawFiles = new File(rawFilesPath).list();
		
		for(int index =0; index < rawFiles.length; index ++){
			String file = rawFiles[index];
			
			if(file.startsWith(".")) continue;
			
			String sectionsFileName = sectionsPath + "/";
			String rawFile = rawFilesPath + "/" + file;
			String conceptsFilePath = conceptsFile + "/";
			String relationsPath = relationsFile + "/" ;
			
			if(corpusType == CorpusType.I2B2RELATION){
				sectionsFileName += file + Sectionizer.fileEnding; 
				conceptsFilePath += file.split("\\.")[0] + Annotation.chooseFileEnding(corpusType);
				relationsPath += file.split("\\.")[0] + Relation.fileEnding;
			}else{					
				sectionsFileName += file.split("\\.")[0] + Sectionizer.fileEnding; 
				conceptsFilePath += file + Annotation.chooseFileEnding(corpusType);
				relationsPath += file + Annotation.chooseFileEnding(corpusType);
			}
					
			String parsedFilePath = parsedDataPath + "/" + file + ImportSyntacticTags.PARSED_FILE;
 			String phonemesFilePath = phonemesPath + "/" + file + Phonemizer.fileEnding;

					
			medicalRecords.put(file, new MedicalRecord(rawFile, conceptsFilePath,
					relationsPath,
					importTags, sectionizer,
					parsedFilePath, 
					phonemesFilePath,
					sectionsFileName,
					conceptSemanticTypes, corpusType));

		}

	}

	private void init(HashMap<String, ArrayList<String>> conceptSemanticTypes ){
		this.conceptSemanticTypes = conceptSemanticTypes;
		
		importer = new DataImport(null);
		sectionizer = new Sectionizer(sectionsTemplate);
	}  

}
