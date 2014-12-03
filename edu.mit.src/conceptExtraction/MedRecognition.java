/**
 * Created on Feb 4, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package conceptExtraction;

import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.MatchMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author ab
 *
 */
public class MedRecognition extends Recognition{
    List<String> incorrectSemTypes = Arrays.asList("imft");

    public MedRecognition(HashMap<String, Boolean> caches) {
	super(caches);
	this.semType = Arrays.asList("phsu", "clnd", "antb", "orch", "strd", "bacs");
	//, "strd" (aka steroid) should not be part of this
    }


    /**
     * @param ev
     * @return
     */
    boolean checkValidEV(Ev ev) {
	try {
	    if (!ev.getPreferredName().equals(ev.getConceptName())) {
		List<MatchMap> val = ev.getMatchMapList();
		for(MatchMap match : val) 
		    if (match.getLexMatchVariation() == 0)
			return true;

		if( !ev.getConceptName().toLowerCase().contains("nos") && 
			! ev.getPreferredName().toLowerCase().contains("usp"))
		    if (!(ev.getMatchedWords().size() > 1))
			return false;
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}

	return true;
    }


    boolean checkValidSemanticType(List<String> types) {
	boolean found = false;

	for(String possible : types) {
	    if (this.semType.contains(possible))	
		found = true;
	    if(this.incorrectSemTypes.contains(possible))
		return false;
	}

	return found;
    }
}
