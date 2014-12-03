package discourse;

import java.util.Arrays;
import java.util.List;

import utils.UtilMethods;

public class DiscourseProcessing {
	
	public static final List<String> causalConjunctions = 
			Arrays.asList("for", "because", "because of", "since", "of", "with", "on");
	// taling out "if",
	
	public static final List<String> coordinateConjunctions = 
			Arrays.asList("and", "nor", "but", "or", "yet", "so");
	
	public static final List<String> mergerConjunctions = 
			Arrays.asList("with");
	
	public static final List<String> allConjunctions = 
			Arrays.asList("after", "although", "and", "as", "as far as",   
					"as how",  "as if",  "as long as",   
					"as soon as", "as though",  "as well as",
					"because" ,"before" ,"both", "but", 
					"either", "even if","even though", "for", "how", "however", 
					"if", "if only", "in case", "in order that", "neither", "now", 
					"now", "once", "only", "or", "provided", "rather than", "since", 
					"so", "so that", "unless", "until", "when", "whenever", "where", 
					"whereas", "wherever", "whether", "while", "yet");
	
	
	public static final List<String> specialNounEndings = 
	        Arrays.asList("itis", "ates", "ia", "as", "axis", "oma", "is", "emia", "ea", "algia");
	
	private String regexCoordinate;
	private String regexCausal;
	
	public DiscourseProcessing(){
		loadRegex();
	}

	private void loadRegex() {
		regexCoordinate = "";
		regexCausal = "";
		
		for(String coordinate : DiscourseProcessing.coordinateConjunctions)
			regexCoordinate = UtilMethods.mergeStrings(regexCoordinate, coordinate, " | ");

		for(String causal : DiscourseProcessing.causalConjunctions)
			regexCausal = UtilMethods.mergeStrings(regexCausal, causal, " | ");
		
		regexCoordinate = regexCoordinate.substring(2);
		regexCausal = regexCausal.substring(2);
		
	}
	
	/**
	 * Check if the context contains a causal conjunction
	 * @param context
	 * @return
	 */
	public static boolean containsCausalConjuction(String context){
	    if(causalConjunctions.contains(context.trim()))
                return true;
	    
	    String[] splitContext = context.split(" ");
	    
	    for(int index =0 ; index < splitContext.length; index++){
	        if(causalConjunctions.contains(splitContext[index]))
	                return true;
	    }
	    
	    return false;
	}
	
	public boolean containsAnyConjunction(String context){
	    if(causalConjunctions.contains(context.trim()))
                return true;
            
            String[] splitContext = context.split(" ");
            
            for(int index =0 ; index < splitContext.length; index++){
                if(allConjunctions.contains(splitContext[index]))
                        return true;
            }
            
            return false;
	}

	public boolean parseContextByConjunction(String context) {
		
		if(DiscourseProcessing.mergerConjunctions.contains(context.trim()))
			return true;
		
		context = replacePunctuation(context);
				
		String[] splitContext = context.split(regexCoordinate);
		boolean causal = true;
		
		int countConjunctions = 0;
		
		for(int index = 0; index < splitContext.length; index ++){
			String equalPart = splitContext[index];
			
			String causalPart = equalPart.trim().split(" ")[0].toLowerCase();
			
			if(!DiscourseProcessing.causalConjunctions.contains(causalPart) && 
					!(countConjunctions == 0))
				causal = false;
			
			// if there is more than 2 causal conjunctions we stop
			String[] splitCausal = equalPart.split(" ");
			int countCausal = 0;
			for(int offset = 0; offset < splitCausal.length; offset++){
				if(DiscourseProcessing.causalConjunctions.contains(splitCausal[offset]))
					countCausal ++;
			}
			
			if(countCausal > 1) {
				return false;
			}
			countConjunctions ++;	
		}
			
		if(countConjunctions == 1 ){
		
			splitContext = context.split(" ");
			
			for(int index= 0; index < splitContext.length; index++)
				if(DiscourseProcessing.causalConjunctions.contains(splitContext[index]))
					return true;
			
			return false;
		}
		
		return causal;
	}


	private String replacePunctuation(String context) {
		String[] splitContext = context.split(" ");
		String merged = "";
		
		for(int index = 0; index < splitContext.length; index ++){
			String word = splitContext[index];
			String nextWord = null;
			
			if(index +1 < splitContext.length)
				nextWord = splitContext[index+1];
			
			if(word.trim().isEmpty()) continue;
				
			if(word.length() -
					word.replaceAll(UtilMethods.regexDelimiter, "").length() <= 1 &&
					!Character.isLetterOrDigit(word.charAt(word.length()-1)))
				if(nextWord != null && nextWord.length() >= 1){
					if(Character.isUpperCase(nextWord.charAt(0)))
						word = word.replaceAll(UtilMethods.regexDelimiter, " and ");
					else if(!word.endsWith("."))
						word = word.replaceAll(UtilMethods.regexDelimiter, " and ");
						
				}else
					word = word.replaceAll(UtilMethods.regexDelimiter, " and ");

			
			merged = UtilMethods.mergeStrings(merged, word);
		}
		
		return merged;
	}

}
