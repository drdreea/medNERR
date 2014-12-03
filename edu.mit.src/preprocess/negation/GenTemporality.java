package preprocess.negation;

/***************************************************************************************
 * Author: Junebae Kye, Supervisor: Imre Solti 
 * Date: 07/04/2010
 * 
 * This program is to determine a temporality for ConText: an algorithm for identifying contextual features in textual medical records
 * 
 * NOTES:
 * returns a temporality if any temporality is found. Otherwise, returns "Recent"
 *
 ****************************************************************************************/

import java.util.LinkedList;
import java.util.List;

public class GenTemporality {
    private List<String> recent;          // list of recent phrases
    private List<String> hypothetical;    // list of hypothetical phrases
    private List<String> pseudo;          // list of pseudo phrases
    private List<String> historical;      // list of historical phrases

    // post: constructs a GenTemporality objects
    //       creates lists of recent phrases, hypothetical phrases, pseudo phrases, and historical phrases
    public GenTemporality() {
	recent = new LinkedList<String>();
	hypothetical = new LinkedList<String>();
	pseudo = new LinkedList<String>();
	historical = new LinkedList<String>();
	process(hypothetical, pseudo, historical);
    }
    
    // post: saves all the phrases into the database 
    private void process(List<String> hypothetical, List<String> pseudo, List<String> historical) {
	recent.add("last week");
	recent.add("yesterday");
	recent.add("week ago");
	recent.add("last night");
	recent.add("the other night");
	
	hypothetical.add("if");
	hypothetical.add("return");
	hypothetical.add("should he");
	hypothetical.add("should she");
	hypothetical.add("should there");
	hypothetical.add("should the patient");
	hypothetical.add("as needed");
	hypothetical.add("come back for");
	hypothetical.add("come back to");
	  
	pseudo.add("if negative");
	pseudo.add("history and physical");
	pseudo.add("history, physical");
	pseudo.add("history taking");
	pseudo.add("poor history");
	pseudo.add("history and examination");
	pseudo.add("history of chief complaint");
	pseudo.add("history for");
	pseudo.add("history and");
	pseudo.add("history of present illness");
	pseudo.add("social history");
	pseudo.add("family history");
	  
	historical.add("history");
	historical.add("past medical history");
	historical.add("previous");
	historical.add("since");
	historical.add("presents");
	historical.add("presented");
	historical.add("presenting");
	historical.add("complains");
	historical.add("was found");
	historical.add("states");
	historical.add("reports");
	historical.add("reported");
	historical.add("noted");
	historical.add("emergency department");
	historical.add("ed");
    }
    
    // post: returns a temporality
    //       returns "Recent" if no specific temporality is found
    public String getTemporality(String line) {
	if (recentChecks(line))
	    return "Recent";
	else {
	    String[] s = line.split("\\s+");
	    return helper(s, 0);
	}
    }
    
    // post: returns true if a recent phrase is found. Otherwise, returns false
    private boolean recentChecks(String line) {
	for (String token : recent)
	    if (line.contains(token))
		return true;
	if (line.contains("since")) {
	    String rest = line.substring(line.indexOf("since"));
	    if (rest.contains("has") || rest.contains("have"))
		return true;
	}  
	return false;
    }
    
    // post: returns a temporality if there is.  Otherwise, returns false
    private String helper(String[] s, int index) {
	if (index < s.length)
	    for (int i = index; i < s.length; i++) {
		int indexII = contains(s, pseudo, i);
		if (indexII != -1)
		    return helper(s, indexII);
		else
		    if (contains(s, historical, i) != -1)
			return "Historical";
		    else if (contains(s, hypothetical, i) != -1)
			return "Hypothetical";  
	    }
	return "Recent";
    }

    // post: returns an index of temporality if a temporality phrase is found. Otherwise, returns -1
    private int contains(String[] s, List<String> list, int index) {
	int counts = 0;
	for (String token : list) {
	    String[] element = token.split("\\s+");
	    if (element.length == 1) {
		if (s[index].equals(element[0]))
		    return index + 1;
	    } else
		if (s.length - index >= element.length) {
		    String firstWord = s[index];
		    if (firstWord.equals(element[0])) {
			counts++;
			for (int i = 1; i < element.length; i++) {
			    if (s[index + i].equals(element[i])) 
				counts++;
			    else {
				counts = 0;
				break;
			    }
			    if (counts == element.length) 
				return index + i + 1;
			}
		    }
		}
	}
	return -1;
    }
}
