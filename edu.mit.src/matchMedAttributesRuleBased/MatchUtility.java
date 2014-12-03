/**
 * 
 */
package matchMedAttributesRuleBased;

import java.util.ArrayList;
import java.util.HashMap;

import corpus.AnnotationDetail;

import preprocess.stemmer.EnglishStemmer;

/**
 * @author ab
 *
 */
public class MatchUtility {
    public HashMap<String, String> stemmedConcepts;
    public HashMap<Integer, ArrayList<AnnotationDetail>> medPerLine;
    public HashMap<AnnotationDetail, ArrayList<AnnotationDetail>> medReasonPairing;

    public MatchUtility() {
        stemmedConcepts = new HashMap<String, String>();
        medPerLine = new HashMap<Integer, ArrayList<AnnotationDetail>>();
        medReasonPairing = new HashMap<AnnotationDetail, ArrayList<AnnotationDetail>>();
    }

    public void getMatches(ArrayList<AnnotationDetail> meds, 
    		ArrayList<AnnotationDetail> reasons){
        //go through each med annotation and identify the line on
        // which the medication is located
        for(AnnotationDetail  annt : meds){
            if (medPerLine.containsKey(annt.startLine)){
                ArrayList<AnnotationDetail> tmp = 
                        medPerLine.get(annt.startLine);
                tmp.add(annt);
                medPerLine.put(annt.startLine, tmp);

            }else{
                ArrayList<AnnotationDetail> tmp = 
                        new ArrayList<AnnotationDetail>();
                tmp.add(annt);
                medPerLine.put(annt.startLine, tmp);
            }
            stemmedConcepts.put(annt.content, 
                    EnglishStemmer.process(annt.content));
        }

        // go though the reason annotations and see which reason 
        // is located close to the med annotations
        for(AnnotationDetail annt : reasons){
            stemmedConcepts.put(annt.content, 
                    EnglishStemmer.process(annt.content));

            int anntLine = annt.endLine;
            for(int range = -2; range <= 2; range ++)
                if(medPerLine.containsKey(anntLine + range)){
                    ArrayList<AnnotationDetail> medAnntList = 
                            medPerLine.get(anntLine + range);

                    for(AnnotationDetail medAnnt : medAnntList) {

                        if(medReasonPairing.containsKey(medAnnt)){
                            ArrayList<AnnotationDetail> pairings = 
                                    medReasonPairing.get(medAnnt);
                            pairings.add(annt);
                            medReasonPairing.put(medAnnt, pairings );
                        }else{
                            ArrayList<AnnotationDetail> pairings = 
                                    new ArrayList<AnnotationDetail>();
                            pairings.add(annt);
                            medReasonPairing.put(medAnnt, pairings );
                        }
                    }
                }
        }
    }
}
