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
import gov.nih.nlm.nls.metamap.Result;
import gov.nih.nlm.nls.metamap.Utterance;
import io.importer.SystemConfigHandler;

import java.util.ArrayList;
import java.util.List;

import corpus.AnnotationDetail;

/**
 * We want to use MetaMap to recognize medication names given a phrase from a
 * medical record
 * 
 * @author ab
 * 
 */
public class NERRecognition {
    final String wsdPath = "/bin/wsdserverctl";
    final String skrPath = "/bin/skrmedpostctl";
    final String metaMap = "/bin/metamap11";
    public final SystemConfigHandler configs;

    MedRecognition extractMeds;
    ReasonRecognition extractReasons;

    MetaMapApi api;

    public NERRecognition(SystemConfigHandler configs) {
        // we have to start up the Metamap servers
        String command;
        this.configs = configs;
        if(configs != null) {
            this.extractMeds = new MedRecognition(this.configs.cachedData);
            this.extractReasons = new ReasonRecognition(this.configs.cachedData);
        }

        try {
            Runtime rt = Runtime.getRuntime();
            command = configs.metamapPath + this.wsdPath;
            rt.exec(command);

            command = configs.metamapPath + this.skrPath;
            rt.exec(command);
        }
        
        catch (Exception e) {
            e.printStackTrace();
        }

        api = new MetaMapApiImpl();
        ArrayList<String> theOptions = new ArrayList<String>();
        theOptions.add("-a");  // turn on Acronym/Abbreviation variants
        // theOptions.add("-y"); // turn on WordSenseDisambiguation
        if (theOptions.size() > 0) {
            api.setOptions(theOptions);
        }

    }

    /**
     * Method to parse through given input and return identified medication
     * names
     * 
     * @param input
     * @return
     */
    public NEInformation recognize(String input, String prev, 
            Boolean isList, int lineIndex) {
        ArrayList<AnnotationDetail> medication = new ArrayList<AnnotationDetail>();
        ArrayList<AnnotationDetail> reasons = new ArrayList<AnnotationDetail>();

        NEInformation tempNE = this.recognizeSub(input, prev, 
                input, isList, lineIndex);
        if (!tempNE.getMedicationInformation().isEmpty())
            medication.addAll(tempNE.getMedicationInformation());

        if (!tempNE.getReasonInformation().isEmpty())
            reasons.addAll(tempNE.getReasonInformation());

        // now check for elements that span on two lines
        if (!prev.equals("")) {
            tempNE = this.recognizeSub(prev + " " + input, 
                    prev, input, isList, lineIndex);

            reasons.addAll(tempNE.getReasonInformation());
            medication.addAll(tempNE.getMedicationInformation());

        }

        // remove reason duplicates
        tempNE.setReasonInformation(reasons);
        tempNE.setReasonInformation(
                AnnotationDetail.removeDuplicates(tempNE.getReasonInformation()));

        tempNE.setMedicationInformation(medication);
        tempNE.setMedicationInformation(
                AnnotationDetail.removeDuplicates(tempNE.getMedicationInformation()));

        return tempNE;
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
	 * Identify the potential medications within the given input Child method of
	 * the recognize method
	 * 
	 * @param input
	 * @param prev
	 * @param isList
	 * @return
	 */
	private NEInformation recognizeSub(String input, 
	        String prevLine, String currentLine, 
	        Boolean isList, int lineIndex) {
	    ArrayList<String> addedEls = new ArrayList<String>();
	
	    ArrayList<AnnotationDetail> medications = new ArrayList<AnnotationDetail>();
	    ArrayList<AnnotationDetail> reasons = new ArrayList<AnnotationDetail>();
	
	    try {
	
	        //	    if (input.toLowerCase().contains("lasix 40")) {
	        //		System.out.println("here");
	        //	    }
	        //	    else
	        //		return new NEInformation(medications, reasons);
	
	        // Note Jan 17 2012: we have to remove the ";" from the input
	        // otherwise MetaMap treats ";" as a command separator
	        List<Result> resultList = api.processCitationsFromString(input
	                .replaceAll(";", " "));
	
	        for (Result result : resultList)
	            for (Utterance utterance : result.getUtteranceList()) {
	                for (PCM pcm : utterance.getPCMList()) {
	                    int maxScore = 0;
	
	                    for (Ev ev : pcm.getCandidateList()) {
	                        if (configs.medOnly) {
	                            String foundMed = extractMeds.findNE(ev,
	                                    medications, addedEls, input, maxScore);
	
	                            if (foundMed != null && !foundMed.isEmpty()) {
	                                // make sure the phrase is not negated
	                                AnnotationDetail foundMedication = 
	                                        extractMeds.getAnnotationInformation(ev, 
	                                                input, prevLine, currentLine, lineIndex, foundMed);
	                                if(foundMedication != null)
	                                    medications.add(foundMedication);
	                            }
	                        }
	
	                        // perform only reason extraction
	                        if (configs.reasonOnly) {
	                            AnnotationDetail foundReason = extractReasons.findReasonNE(ev,
	                                    input, prevLine, currentLine, lineIndex);
	
	                            if (foundReason != null ) 
	                                reasons.add(foundReason);
	
	                        }
	
	                        if (ev.getScore() < maxScore)
	                            maxScore = ev.getScore();
	                    }
	                }
	            }
	    }
	    catch (Exception e) {
	        e.printStackTrace();
	    }
	
	    // check whether two medications are located next to each other inside
	    // the input
	    //	PhrasePair[] medicationArray = medications.toArray(new PhrasePair[] {});
	    //
	    //	for (int i = 0; i < medicationArray.length - 1; i++) {
	    //	    for (int j = i + 1; j < medicationArray.length; j++) {
	    //		String mergedVals = UtilMethods.mergeStrings(
	    //			medicationArray[i].candidates.get(0),
	    //			medicationArray[j].candidates.get(0));
	    //
	    //		if (UtilMethods.containsPunctuation(mergedVals))
	    //		    continue;
	    //		boolean toRemove = false;
	    //
	    //		if (medicationArray[i].line.toLowerCase().contains(mergedVals))
	    //		    toRemove = true;
	    //
	    //		if (!toRemove) {
	    //		    mergedVals = UtilMethods.mergeStrings(
	    //			    medicationArray[j].candidates.get(0),
	    //			    medicationArray[i].candidates.get(0));
	    //
	    //		    if (medicationArray[i].line.toLowerCase().contains(
	    //			    mergedVals))
	    //			toRemove = true;
	    //		}
	    //
	    //		if (toRemove) {
	    //		    PhrasePair pp = medicationArray[i];
	    //		    pp.candidates.remove(0);
	    //		    pp.candidates.add(mergedVals);
	    //
	    //		    medications.set(i, pp);
	    //		}
	    //	    }
	    //	}
	
	    NEInformation returnedNEs = new NEInformation();
	    returnedNEs.setMedicationInformation(medications);
	    returnedNEs.setReasonInformation(reasons);
	
	    return returnedNEs;
	}

}
