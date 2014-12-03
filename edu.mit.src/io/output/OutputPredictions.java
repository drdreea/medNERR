/**
 * Created on Jan 14, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package io.output;

import io.importer.SystemConfigHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import classifier.TrainTestCreator.CorpusType;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.Relation;

import utils.PhrasePair;

/**
 * @author ab
 *
 */
public class OutputPredictions {
	public String storagePath;

	final String outputSuffix = ".i2b2.entries";
	SystemConfigHandler configs ;

	public OutputPredictions(String rawFilePath, SystemConfigHandler configs) {
		String fileName = new File(rawFilePath).getName();
		this.configs = configs;

		if(configs != null)
			this.storagePath = configs.outputPath + 
			fileName + this.outputSuffix;
	}

	void store() {

	}

	public static void storeReasons(String path, ArrayList<AnnotationDetail> annts){
		try{
			FileWriter fstream = new FileWriter(path);
			BufferedWriter out = new BufferedWriter(fstream);

			for(AnnotationDetail annt : annts){
				out.write(annt.printString().toLowerCase() + "\n");
				out.flush();
			}

			out.close();
			fstream.close();
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());

		}
	}

	public static void storeMedications(String path, ArrayList<AnnotationDetail> annts){
		try{
			FileWriter fstream = new FileWriter(path);
			BufferedWriter out = new BufferedWriter(fstream);

			for(AnnotationDetail annt : annts){
				out.write(annt.printString().toLowerCase() + "\n");
				out.flush();
			}

			out.close();
			fstream.close();
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());

		}
	}
	
	public static void storeRelations(String path, ArrayList<Relation> annts, 
			CorpusType corpus){
		try{
			FileWriter fstream = new FileWriter(path);
			BufferedWriter out = new BufferedWriter(fstream);

			for(Relation annt : annts){
				String toWrite = "";
				if(corpus == CorpusType.I2B2)
					toWrite = Relation.printString(annt, corpus).toLowerCase() + "\n";
				else
					toWrite = Relation.printString(annt, corpus) + "\n";
				
				out.write(toWrite);
				out.flush();
			}

			out.close();
			fstream.close();
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());

		}
	}
	
	public static void storeAnnotations(String path, ArrayList<Annotation> annts, 
			CorpusType corpus){
		try{
			FileWriter fstream = new FileWriter(path);
			BufferedWriter out = new BufferedWriter(fstream);

			for(Annotation annt : annts){
				if(corpus == CorpusType.I2B2)
					out.write(Annotation.printString(annt).toLowerCase() + "\n");
				else
					out.write(Annotation.printString(annt) + "\n");

				out.flush();
			}

			out.close();
			fstream.close();
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());

		}
	}


	public static void storeAnnotations(String path, ArrayList<AnnotationDetail> annts){
		try{
			FileWriter fstream = new FileWriter(path);
			BufferedWriter out = new BufferedWriter(fstream);

			for(AnnotationDetail annt : annts){
				out.write(AnnotationDetail.printString(annt) + "\n");
				out.flush();
			}

			out.close();
			fstream.close();
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());

		}
	}

	public static void storeContents(String path, ArrayList<String> contents){
		try{
			FileWriter fstream = new FileWriter(path);
			BufferedWriter out = new BufferedWriter(fstream);

			for(String annt : contents){
				out.write(annt + "\n");
				out.flush();
			}

			out.close();
			fstream.close();
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());

		}
	}

	/**
	 * @param medicationTextIndexes
	 */
	public void writeToFile(
			ArrayList<AnnotationDetail> medicationTextIndexes, 
			ArrayList<AnnotationDetail> reasonTextIndexes) {

		try{
			FileWriter fstream = new FileWriter(this.storagePath);
			BufferedWriter out = new BufferedWriter(fstream);

			if(configs.reasonOnly && configs.medOnly){
				for (AnnotationDetail med : medicationTextIndexes) {
					out.write(med.printString() + "\n");
				}
			}else if (configs.medOnly){
				for (AnnotationDetail med : medicationTextIndexes) {
					out.write(med.printString() + "\n");
				}
			}else if (configs.reasonOnly){

				for (AnnotationDetail rn : reasonTextIndexes) {

					out.write(rn.printString() + "\n");
				}
			}
			out.close();

		}catch (Exception e){ 
			System.err.println("Error: " + e.getMessage());
		}
	}

	//    private String formatAnnnotation(String value, PhrasePair key) {
	//	return null;
	//    }

	String formatReason(String reason, PhrasePair phrasePair) {
		String returnValue = "r=";

		returnValue += "\"" + reason + "\"";
		returnValue += " " + String.valueOf(phrasePair.indexes.getStartLine()) + 
				":" + String.valueOf(phrasePair.indexes.getStartOffset());
		returnValue += " " + String.valueOf(phrasePair.indexes.getEndLine()) + 
				":" + String.valueOf(phrasePair.indexes.getEndOffset());		
		returnValue += "||";
		if (phrasePair.isList)
			returnValue += "ln=\"list\"";
		else
			returnValue += "ln=\"narrative\"";

		return returnValue;
	}

	String formatMed( String med, PhrasePair phrasePair) {

		String returnValue = "m=";

		returnValue += "\"" + med + "\"";
		returnValue += " " + String.valueOf(phrasePair.indexes.getStartLine()) + 
				":" + String.valueOf(phrasePair.indexes.getStartOffset());
		returnValue += " " + String.valueOf(phrasePair.indexes.getEndLine()) + 
				":" + String.valueOf(phrasePair.indexes.getEndOffset());		
		returnValue += "||";
		if (phrasePair.isList)
			returnValue += "ln=\"list\"";
		else
			returnValue += "ln=\"narrative\"";

		return returnValue;
	}


}
