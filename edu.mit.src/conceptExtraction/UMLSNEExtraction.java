/**
 * Created on Jan 13, 2012
 * 
 * @author ab Contact andreeab dot mit dot edu
 * 
 */
package conceptExtraction;

import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.MetaMapApi;
import gov.nih.nlm.nls.metamap.MetaMapApiImpl;
import gov.nih.nlm.nls.metamap.PCM;
import gov.nih.nlm.nls.metamap.Position;
import gov.nih.nlm.nls.metamap.Result;
import gov.nih.nlm.nls.metamap.Utterance;
import io.importer.DataImport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import classifier.Storage;

import utils.UtilMethods;


/**
 * Extract all named entities inside a given document
 * NEs as identified by UMLS
 * 
 * @author ab
 * 
 */
public class UMLSNEExtraction {
	final String wsdPath = "/bin/wsdserverctl";
	final String skrPath = "/bin/skrmedpostctl";
	final String metaMap = "/bin/metamap11";

	String systemInput;
	String systemOutput;

	MetaMapApi api;

	public UMLSNEExtraction() {

		api = new MetaMapApiImpl();
		ArrayList<String> theOptions = new ArrayList<String>();
		theOptions.add("-a");  // turn on Acronym/Abbreviation variants
		// theOptions.add("-y"); // turn on WordSenseDisambiguation
		if (theOptions.size() > 0) {
			api.setOptions(theOptions);
		}

	}

	public void execute(){
		String[] files = new File(this.systemInput).list();

		for(int index = 0; index < files.length; index ++){
			String fileName = files[index];
			if(fileName.startsWith(".")) continue;

			System.out.println("Processing " + fileName);

			ArrayList<String> outputLines = new ArrayList<String>();
			ArrayList<String> fileSentences = DataImport.readFile(systemInput + fileName);

			int lineCount = 1;
			for(String sent : fileSentences){
				ArrayList<Ev> nes = findNEperSentence(sent);
				outputLines.add(prinNE(nes, lineCount));
				outputLines.add("\n");
				lineCount++;
			}
			
			// store the file
			Storage.storeFile(systemOutput+"/" + fileName, outputLines);

		}
	}
	


	private String prinNE(ArrayList<Ev> nes, int lineCount) {
		String content = "";

		for(Ev ne : nes){
			content += String.valueOf(lineCount);

			try {
//				content += " [" + ne.getConceptName() + "]";
				content += " " + ne.getScore();
				content += " " + UtilMethods.mergeDashStrings(
						ne.getSemanticTypes());

				Position phrasePosition = ne.getPositionalInfo().get(0);
				content += " " + phrasePosition.getX(); 
				content += " " + (phrasePosition.getY() + phrasePosition.getX());

			} catch (Exception e) {
				e.printStackTrace();
			}

			content += "\n";
		}

		return content;
	}

	/**
	 * Get the medical concept associated with the given input
	 * @param input
	 * @return the medical concept
	 */
	public String getConcept(String input) {
		String concept = null;

		try {
			// Note Jan 17 2012: we have to remove the ";" from the input
			// otherwise MetaMap treats ";" as a command separator
			List<Result> resultList = api.processCitationsFromString(input
					.replaceAll(";", " "));
			int maxScore = 0;

			for (Result result : resultList)
				for (Utterance utterance : result.getUtteranceList()) {
					for (PCM pcm : utterance.getPCMList()) {

						for (Ev ev : pcm.getCandidateList()) {

							if (ev.getScore() < maxScore) {
								maxScore = ev.getScore();
								concept = ev.getConceptName();
							}
						}
					}
				}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return concept;
	}

	/**
	 * Get the semantic types associated with the input
	 * @param input
	 * @return the semantic types
	 */
	public List<String> getConceptCUI(String input) {
		List<String> concept = null;

		try {
			// Note Jan 17 2012: we have to remove the ";" from the input
			// otherwise MetaMap treats ";" as a command separator
			List<Result> resultList = api.processCitationsFromString(input
					.replaceAll(";", " "));
			int maxScore = 0;

			for (Result result : resultList)
				for (Utterance utterance : result.getUtteranceList()) {
					for (PCM pcm : utterance.getPCMList()) {

						for (Ev ev : pcm.getCandidateList()) {

							if (ev.getScore() < maxScore) {
								maxScore = ev.getScore();
								concept = ev.getSemanticTypes();
							}
						}
					}
				}

			for (Result result : resultList)
				for (Utterance utterance : result.getUtteranceList()) {
					for (PCM pcm : utterance.getPCMList()) {

						for (Ev ev : pcm.getCandidateList()) {

							if (ev.getScore() == maxScore) {
								for(String el : ev.getSemanticTypes())
									if(!concept.contains(el))
										concept.add(el);
							}
						}
					}
				}
		}
		catch (Exception e) {
			e.printStackTrace();
		}


		return concept;
	}

	/**
	 * Identify the potential NEs within the given input Child method of
	 * the recognize method
	 * 
	 * @param input
	 * @param prev
	 * @param isList
	 * @return
	 */
	private ArrayList<Ev> findNEperSentence(String input) {

		ArrayList<Ev> entities = new ArrayList<Ev>();

		try {

			// Note Jan 17 2012: we have to remove the ";" from the input
			// otherwise MetaMap treats ";" as a command separator
			List<Result> resultList = api.processCitationsFromString(input
					.replaceAll(";", " "));

			for (Result result : resultList)
				for (Utterance utterance : result.getUtteranceList()) {
					for (PCM pcm : utterance.getPCMList()) {
						int score = 0;
						Ev finalEv = null;
						
						for (Ev ev : pcm.getCandidateList()) {
							if(ev.getScore() < score){
								score = ev.getScore();
								finalEv = ev;
							}
							
						}
						
						if(finalEv != null) entities.add(finalEv);
					}
				}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return entities;
	}

	public static void main(String args[]){
		if(args.length != 2){
			System.out.println("Require input and output folders");
			System.exit(-1);
		}

		UMLSNEExtraction neExtractor = new UMLSNEExtraction();
		neExtractor.systemInput = args[0];
		neExtractor.systemOutput = args[1];

		neExtractor.execute();

	}

}
