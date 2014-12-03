package preprocess.negation;

/***************************************************************************************
 * Author: Junebae Kye,Andreae Bodnari Supervisor: Imre Solti
 * Date: 07/04/2010 
 * 
 * Modified : Feb 11th 2012
 *
 *
 *This program is to mainly print a line along with its negation scope, temporality, and experiencer. For example, 550     hiatal herniaA HIATAL HERNIA was found.     Affirmed     -1     Historical     Patient(Number TAB Phrase TAB Sentence TAB Dummystring TAB Negation Scope TAB Temporality TAB Experiencer)
 *Usage: java MainConText Annotations-1-120-random.txt yes(or no)
 *
 ****************************************************************************************/

import java.io.IOException;

public class MainConText {
	GenNegEx n;
	
	public MainConText(){
		boolean value = true;

		 n = new GenNegEx(value);
	}

	/**
	 * Test for negations within the given line
	 * Return the scope of the negation
	 * @param line
	 * @return
	 * @throws IOException
	 */
	public String test(String line) throws IOException {

//		GenExperiencer e = new GenExperiencer();
//		GenTemporality t = new GenTemporality();	  

//		String sentence = cleans(line);
		String scope = n.negScope(line);
//		String experi = e.getExperiencer(sentence);
//		String tempo = t.getTemporality(sentence);
		
		return scope;
	}

	// post: removes punctuations from a sentence
	static String cleans(String line) {
		line = line.toLowerCase();
		if (line.contains("\""))
			line = line.replaceAll("\"", "");
		if (line.contains(","))
			line = line.replaceAll(",", "");  
		if (line.contains("."))
			line = line.replaceAll("\\.", "");
		if (line.contains(";"))
			line = line.replaceAll(";", "");
		if (line.contains(":"))
			line = line.replaceAll(":", "");
		return line;
	}
}
