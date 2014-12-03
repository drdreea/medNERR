package preprocess.negation;

/***************************************************************************************
 * Author: Junebae Kye, Supervisor: Imre Solti 
 * Date: 07/04/2010
 * 
 * This program is to determine an experiencer for ConText: an algorithm for identifying contextual features in textual medical records
 *
 * NOTES:
 * returns an experiencer if any experiencer is found. Otherwise, return "Patient"
 * 
 ****************************************************************************************/

import java.util.LinkedList;
import java.util.List;

public class GenExperiencer {
    private List<String> list;        // list of experiencers
    private List<String> pseudo;      // list of pseudo-experiencers
    
    // post: constructs an GenExperiencer object
    //       creates two lists of experiencer phrases and pseudo-experiencer phrases
    public GenExperiencer() {
	list = new LinkedList<String>();
	pseudo = new LinkedList<String>();
	process(list, pseudo);
    }
    
    // post: saves experiencer phrases and pseudo-experiencer phrases 
    private void process(List<String> list, List<String> pseudo) {
	list.add("father");
	list.add("father's");
	list.add("dad");
	list.add("dad's");
	list.add("mother");
	list.add("mother's");
	list.add("mom");
	list.add("mom's");
	list.add("sister");
	list.add("sister's");
	list.add("brother");
	list.add("brother's");
	list.add("aunt");
	list.add("aunt's");
	list.add("uncle");
	list.add("uncle's");
	list.add("roommate");
	list.add("roommate's");
	list.add("husband");
	list.add("husband");
	list.add("wife");
	list.add("wife's");
	list.add("grandfather");
	list.add("grandfather's");
	list.add("grandmother");
	list.add("grandmother's");
	list.add("family history");
	  
	pseudo.add("by her husband");
	pseudo.add("by his wife");
	pseudo.add("for the");
	pseudo.add("for a");
    } 
    
    // post: returns an experiencer
    //       returns "Patient" if no specific experiencer is found
    public String getExperiencer(String line) {
	String[] s = line.split("\\s+");
	return helper(s, 0);
    }
    
    // post: helping method for getExperiencer to actually process a line to find an experiencer
    //       returns an experiencer
    //       returns "Patient" if no specific experiencer is found
    private String helper(String[] s, int index) {
	if (index < s.length)
	    for (int i = index; i < s.length; i++) {
		int indexII = contains(s, pseudo, i);
		if (indexII != -1)
		    if (s[i].equals("for"))
			return helper(s, indexII + 1);
		    else  
			return helper(s, indexII);
		else
		    if (contains(s, list, i) != -1) {
			String whom = s[i];
			if (whom.contains("'s"))
			    whom = whom.replace("'s", "");
			else if (whom.equals("family"))
			    whom += " Member";
			whom = Character.toUpperCase(whom.charAt(0)) + whom.substring(1);
			return whom;
		    }
	    } 
	return "Patient";
    } 
    
    // post: returns an index of an experiencer
    //       returns -1 if no specific experiencer is found
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
