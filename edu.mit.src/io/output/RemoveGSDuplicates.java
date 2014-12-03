package io.output;

import io.importer.AnnotationsConfigHandler;
import io.importer.DataImport;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import classifier.TrainTestCreator.CorpusType;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.Annotation.AnnotationType;

import utils.UtilMethods;

public class RemoveGSDuplicates {

	private static int requiredArgs = 1;
	private static DataImport importer;


	static void execute(String path){
		AnnotationsConfigHandler configs = 
				new AnnotationsConfigHandler(path);

		setImporter(new DataImport(null));

		// read the system annotation
		String[] systemFiles = 
				UtilMethods.dirListing(configs.systemAnnotationsPath);

		for(int index = 0; index < systemFiles.length; index ++){
			String filePath = UtilMethods.joinPaths(
					configs.gsAnnotationsPath, systemFiles[index]);
			String outputPath = UtilMethods.joinPaths(configs.outputPath, 
					systemFiles[index]);
			processFile(filePath, outputPath);
		}	
	}

	private static void processFile(String filePath, String outputPath){

		System.out.println(filePath);

		ArrayList<String> gsLines = 
				UtilMethods.readFileLines(filePath);
		ArrayList<Annotation> gsAnnotations = new ArrayList<Annotation>();
		ArrayList<String> addedEls = new ArrayList<String>();

		for (String line :gsLines) {
			Annotation result = Annotation.parseAnnotationLine(line, CorpusType.I2B2);
			if (result != null){
				try{
					AnnotationDetail medication = result.annotationElements.get(
							AnnotationType.M);

					String mergedId = medication.printString();
					if (!addedEls.contains(mergedId)){
						addedEls.add(mergedId);
						gsAnnotations.add(result);
					}
				}catch(Exception e){
					System.out.println("Could not parse " + line);
				}
			}
		}

		FileWriter fstream;

		try {
			fstream = new FileWriter(outputPath);
			BufferedWriter out = new BufferedWriter(fstream);

			for(Annotation annt : gsAnnotations){
				out.write(annt.annotation);
				out.write("\n");
				out.flush();
			}

			out.close();
			fstream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != requiredArgs ) {
			System.out.println(
					"Incorrect arguments! Required configuration file path.");
			System.exit(-1);
		}
		execute(args[0]);
	}

	public static DataImport getImporter() {
		return importer;
	}

	public static void setImporter(DataImport importer) {
		RemoveGSDuplicates.importer = importer;
	}

}
