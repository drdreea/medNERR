package preprocess.textToSpeech;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * This class takes a word given as a string and returns
 * the phonemes of the word
 * 
 * It uses the eSpeak package v. 1.43.03-2 available at
 * http://packages.debian.org/squeeze/espeak
 * 
 * The software should be installed and available for usage
 * You must specify the location where speak is installed
 * @author ab
 *
 */
public class TextToSpeechTransformer {
	Runtime runtime;
	HashMap<String, String> alreadyParsed ;;
	
	public TextToSpeechTransformer(){
		runtime = Runtime.getRuntime();
		alreadyParsed = new HashMap<String, String>();
		
	}
	
	public String getPhonemesFromWord(String word) throws IOException{
		String phonemes = "";
				
		if(alreadyParsed.containsKey(word))
			return alreadyParsed.get(word);
		
		String command = "/usr/local/bin/speak -q -x \"" +word + "\"";
		Process process = runtime.exec(command);
		
		BufferedReader rd = new BufferedReader( new InputStreamReader( 
				process.getInputStream() ) );
		phonemes = rd.readLine().trim();
		phonemes = phonemes.replaceAll("_:", "");
		
		if(phonemes.split(" ").length > 0)
			phonemes = phonemes.split(" ")[0];
		
		// destroy the process once finished
		process.destroy();
		
		alreadyParsed.put(word, phonemes);
		
		return phonemes;
	}
}
