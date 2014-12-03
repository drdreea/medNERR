package matchMedAttributesRuleBased;

import io.output.OutputPredictions;

import java.util.ArrayList;
import java.util.HashMap;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.MedicalRecord;
import corpus.Annotation.AnnotationType;

import preprocess.wiki.WikiDisambiguate;

public class MatchReasonsGreedy extends Matcher {
    WikiDisambiguate wikiSearches;

    public MatchReasonsGreedy(String configsPath) {
        super(configsPath);
        wikiSearches = new WikiDisambiguate();
    }

    void execute() {

        for (String key : medAnnotations.keySet()) {
            if (!reasonAnnotation.containsKey(key)) {
                System.out.println("Could not find file " + key);
                continue;
            }

            AnnotationFile meds = medAnnotations.get(key);
            AnnotationFile reasons = reasonAnnotation.get(key);

            MedicalRecord newRecord = importer.readRecordData(key, configs);

            ArrayList<Annotation> mergedValues = mergeAnnotations(meds,
                    reasons, newRecord);

            if (mergedValues != null && !mergedValues.isEmpty())
                mergedAnnotations.put(key, mergedValues);

            // print the merged annt to file
//            OutputPredictions.storeAnnotations(this.configs.outputPath + "/"
//                    + key, mergedValues);

        }

    }

    ArrayList<Annotation> mergeAnnotations(AnnotationFile meds,
            AnnotationFile reasons, MedicalRecord emr) {

        // read on the content of the raw file

        ArrayList<Annotation> merged = new ArrayList<Annotation>();

        HashMap<Integer, ArrayList<Annotation>> medAnnotperLine = 
                new HashMap<Integer, ArrayList<Annotation>>();
        HashMap<Annotation, ArrayList<Annotation>> medReasonPairing = 
                new HashMap<Annotation, ArrayList<Annotation>>();

        // go through each med annotation and identify the line on
        // which the medication is located
        for (Annotation annt : meds.annotations) {
            try {
            	AnnotationDetail medication = annt.annotationElements.get(
            			AnnotationType.M);
            	
                if (medAnnotperLine.containsKey(medication.startLine)) {
                    ArrayList<Annotation> tmp = medAnnotperLine
                            .get(medication.startLine);
                    tmp.add(annt);
                    medAnnotperLine.put(medication.startLine, tmp);
                }
                else {
                    ArrayList<Annotation> tmp = new ArrayList<Annotation>();
                    tmp.add(annt);
                    medAnnotperLine.put(medication.startLine, tmp);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // go though the reason annotations and see which reason
        // is located close to the med annotations
        for (Annotation annt : reasons.annotations) {
        	AnnotationDetail reason = annt.annotationElements.get(
        			AnnotationType.R);
            int anntLine = reason.endLine;
            for (int range = 0; range <= 2; range++) {
                if (medAnnotperLine.containsKey(anntLine + range)) {
                    ArrayList<Annotation> medAnntList = medAnnotperLine
                            .get(anntLine + range);

                    for (Annotation medAnnt : medAnntList) {
                        if (medReasonPairing.containsKey(medAnnt)) {
                            ArrayList<Annotation> pairings = medReasonPairing
                                    .get(medAnnt);
                            pairings.add(annt);
                            medReasonPairing.put(medAnnt, pairings);
                        }
                        else {
                            ArrayList<Annotation> pairings = new ArrayList<Annotation>();
                            pairings.add(annt);
                            medReasonPairing.put(medAnnt, pairings);
                        }
                    }
                }
            }

            anntLine = reason.startLine;
            for (int range = -2; range < 0; range++) {
                if (medAnnotperLine.containsKey(anntLine + range)) {
                    ArrayList<Annotation> medAnntList = medAnnotperLine
                            .get(anntLine + range);

                    for (Annotation medAnnt : medAnntList) {
                        if (medReasonPairing.containsKey(medAnnt)) {
                            ArrayList<Annotation> pairings = medReasonPairing
                                    .get(medAnnt);
                            pairings.add(annt);
                            medReasonPairing.put(medAnnt, pairings);
                        }
                        else {
                            ArrayList<Annotation> pairings = new ArrayList<Annotation>();
                            pairings.add(annt);
                            medReasonPairing.put(medAnnt, pairings);
                        }
                    }
                }
            }
        }

        // go through the med - reason pairings and select the closest one
        for (ArrayList<Annotation> medList : medAnnotperLine.values()) {
            for (Annotation med : medList) {
                if (medReasonPairing.containsKey(med)) {
                    ArrayList<Annotation> pairings = medReasonPairing.get(med);
                    ArrayList<AnnotationDetail> possibleReasons = new ArrayList<AnnotationDetail>();
                    // check whether any of the possible matchings is a good one
                    for (Annotation reason : pairings) 		
                        possibleReasons.add(reason.annotationElements.get(AnnotationType.R));

                    if (possibleReasons.isEmpty()) {
                        AnnotationDetail tmp = new AnnotationDetail();
                        med.annotationElements.put(AnnotationType.R, tmp) ;
                        merged.add(med);
                    }
                    else {
                        for (AnnotationDetail tmp : possibleReasons) {
                            Annotation newAnnt = new Annotation(med.annotation);
                            newAnnt.annotationElements.put(AnnotationType.R, 
                            		med.annotationElements.get(AnnotationType.M)) ;
                            newAnnt.annotationElements.put(AnnotationType.R, tmp) ;
                            merged.add(newAnnt);
                        }
                    }
                }
                else {
                    med.annotationElements.put(AnnotationType.R, 
                    		 new AnnotationDetail()) ;
                    merged.add(med);
                }

            }
        }

        return merged;
    }

    /**
     * Handle the experiment using the main function
     */
    public static void main(String[] args) {
        if (args.length != requiredArgs) {
            System.out
            .println("Incorrect arguments! Required configuration file path.");
            System.exit(-1);
        }
        MatchReasonsGreedy matcher = new MatchReasonsGreedy(args[0]);

        matcher.execute();
    }

}
