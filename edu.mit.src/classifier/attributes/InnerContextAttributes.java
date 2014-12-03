package classifier.attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import corpus.AnnotationDetail;
import corpus.MedicalRecord;

import classifier.ClassifierBase;
import classifier.Utils;

import patternExtraction.FindContextFromEMR;

/**
 * Attributes for the context between medication and reason
 * @author ab
 *
 */
public class InnerContextAttributes extends AttributeGenerator{
	 // the norm used for normalizing the annotation counts
	final static int normAnnotations = 100;
	
	// the norm used for normalizing the word counts
	final static int normWords = 1000; 
	
    public InnerContextAttributes(ClassifierBase classifier){
        super(classifier);
    }
 
    /**
     * Get the inner context feature values
     * @param medication
     * @param reason
     * @param file
     * @return the pos feature values
     */
    public HashMap<String, Object> getInnerContextFeatureValues(AnnotationDetail medication, 
    		AnnotationDetail reason,  MedicalRecord record){
        HashMap<String, Object> featureValues = new HashMap<String, Object>();

        String context = FindContextFromEMR.getContent(record.rawFileLines, 
                medication, reason, false);

        // Feature 3: the number of medications between the given med and reason
        featureValues.put("getMedicationCountBetween",
        		getMedicationCountBetween(medication, reason, record));

        // Feature 4: the number of reasons between the given med and reason
        featureValues.put("getReasonCountBetween", getReasonCountBetween(medication, 
        		reason, record));

        // Feature 5: the number of words between the given med and reason
        featureValues.put("getWordsCountBetween", getWordsCountBetween(medication, reason,
        		record.rawFileLines));

        // Feature 6: the number of words overlap with words from the patterns
        featureValues.put("countPatternWordsOverlap", 
        		countPatternWordsOverlap(medication, 
                reason, context));

        return featureValues;
    }

    double getWordsCountBetween(AnnotationDetail medication, AnnotationDetail reason,
    		ArrayList<String> recordLines) {
        int countWordsDistance = countWordsDistance(medication, reason, recordLines);
        
        if(countWordsDistance > normWords) 
        	countWordsDistance = normWords;
        
        return countWordsDistance*1.0/normWords;
	}

	double getReasonCountBetween(AnnotationDetail medication,
			AnnotationDetail reason, MedicalRecord record) {
        int countReasons = countReasons(medication, reason, record);
        return countReasons*1.0/normWords;
	}

	double getMedicationCountBetween(
			AnnotationDetail medication, AnnotationDetail reason, 
			MedicalRecord record) {
         int countMeds = countMeds(medication, reason, record);
         return countMeds*1.0/normWords;		
	}

	/**
     * Count the number of words between the medication and the reason that overlap
     * with words from context patterns
     * @param medication
     * @param reason
     * @param context
     * @return
     */
    double countPatternWordsOverlap(AnnotationDetail medication,
    		AnnotationDetail reason, String context) {

        if(context == null) return 0;

        String[] split = context.split(" ");
        ArrayList<String> splitContext = new ArrayList<String>();

        for(int index= 0 ; index < split.length; index ++ )
            splitContext.add(Utils.removePunctuation(split[index]));

        HashSet<String> overlapWords = new HashSet<String>();
        HashSet<String> allWords = new HashSet<String>();

        for(String pattern : classifier.patterns){
            String[] splitPattern = pattern.split(" ");

            for(int index = 0; index < splitPattern.length; index ++ ){
                allWords.add(splitPattern[index]);

                if(splitContext.contains(splitPattern[index]))
                    overlapWords.add(splitPattern[index]);
            }
        }

        return overlapWords.size()*1.0/split.length;
    }

    /**
     * Count the number of words between the medication and the reason
     * @param medication
     * @param reason
     * @param fileLines
     * @return
     */
    private int countWordsDistance(AnnotationDetail medication, 
    		AnnotationDetail reason,
           ArrayList<String> fileLines) {

        String context = FindContextFromEMR.getContent(fileLines, 
                medication, reason, false);

        if(context == null)
            return 0;

        return context.split(" ").length;
    }

    /**
     * Count the number of reasons between the medication and the reason
     * @param medication
     * @param reason
     * @param record
     * @return
     */
    private int countReasons(AnnotationDetail medication, AnnotationDetail reason,
            MedicalRecord record) {
        HashMap<AnnotationDetail, Integer> sortedConcepts = record.sortedConcepts;
        boolean foundMed = false;
        boolean foundReason = false;
        int distance = 0;

        for(AnnotationDetail annt : sortedConcepts.keySet()){
            if(foundMed && foundReason)
                break;

            if(annt != null &&  
                    medication.equalsAnnotation(annt)){
                foundMed = true;
                continue;
            }
            
            if(annt != null && 
                    reason.equalsAnnotation(annt) && 
                    (annt == null ||
                    annt.content == null || 
                    annt.content.equals("nm"))){
                foundReason = true;
                continue;
            }
           
            
            if(foundMed || foundReason)
                if(annt == null ||
                annt.content == null || 
                annt.content.equals("nm"))
                    distance ++;

        }

        if(!(foundMed && foundReason))
            return 0;

        return distance;
    }

    /**
     * Count the number of meds between the medication and the reason
     * @param medication
     * @param reason
     * @param record
     * @return
     */
    private int countMeds(AnnotationDetail medication, AnnotationDetail reason, 
    		MedicalRecord record) {

        HashMap<AnnotationDetail, Integer> sortedConcepts = record.sortedConcepts ;
        boolean foundMed = false;
        boolean foundReason = false;
        int distance = 0;

        for(AnnotationDetail annt : sortedConcepts.keySet()){
            if(foundMed && foundReason)
                break;

            if(annt != null && 
                    medication.equalsAnnotation(annt)){
                foundMed = true;
                continue;
            }
            
            if(annt != null &&
                    reason.equalsAnnotation(annt) && 
                    (annt == null ||
                    annt.content == null || 
                    annt.content.equals("nm"))){
                foundReason = true;
                continue;
            }
            
            
            if(foundMed || foundReason)
                if(!(annt == null ||
                annt.content == null || 
                annt.content.equals("nm")))
                    distance ++;

        }

        if(!(foundMed && foundReason))
            return 0;

        return distance;
    }

}
