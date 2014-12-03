package probabModel.ccm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import utils.UtilMethods;

public class ConstraintsCalculator {
	List<String> patternsMedication = Arrays.asList( "given", "on", "for", "with", "as needed for");
	List<String> patternsReasons = Arrays.asList("signs of", "sign of", "risk of", "developed", 
			"concern of", "concerns of" );

	List<String> postPattern = Arrays.asList("that required", "which required");

	final static int setConstraintsSize = 2;
	ArrayList<String> medications;

	public ConstraintsCalculator(ArrayList<String> medicationStrings){
		this.medications = medicationStrings;
	}

	public double computeConstraints(HashMap<Integer, Double> roConstraints, FeatureVectorSequence input,
			FeatureSequence output){
		double score = 0.;
		ArrayList<String> instanceTokens = getAllTokenNames(input);
		ArrayList<Boolean> verbs = getAllVerbs(input);

		for(int k = 0; k < setConstraintsSize; k ++){
			int localConstraints = 0;

			ArrayList<String> instanceLabels = new ArrayList<String>();
			ArrayList<Boolean> instanceMedications = getAllMedications(input);

			// for each of the tokens inside the instance
			for(int i =0; i < input.size(); i ++){
				instanceLabels.add((String) output.get(i));

				// compute c<k_i_j> 
				// c<k_i_j> is a binary variable 1- if the label assignment to token_i
				// violates constraint_k with respect to y[1__i-1]
				localConstraints += computeConstraintViolation(k, instanceLabels, 
						instanceTokens, instanceMedications, verbs);
			}

			score = score + roConstraints.get(k) * localConstraints;
		}

		return score;
	}

	public HashMap<Integer, Double> computeConstraints(
			InstanceList instances){
		HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> constraints = 
				new 	HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();

		// for each of the constraints
		for(int k = 0; k < setConstraintsSize; k ++){
			HashMap<Integer, ArrayList<Integer>> constraintsPerInstance = 
					new HashMap<Integer, ArrayList<Integer>>();

			// for each of the instances
			for (int j=0; j< instances.size(); j++) {
				Instance inst = instances.get(j);
				ArrayList<Integer> localConstraints = new ArrayList<Integer>();

				FeatureVectorSequence input = (FeatureVectorSequence) inst.getData();
				FeatureSequence output = (FeatureSequence) inst.getTarget();

				ArrayList<String> instanceTokens = getAllTokenNames(input);
				ArrayList<String> instanceLabels = new ArrayList<String>();
				ArrayList<Boolean> instanceMedications = getAllMedications(input);
				ArrayList<Boolean> verbs = getAllVerbs(input);

				// for each of the tokens inside the instance
				for(int i =0; i < input.size(); i ++){
					instanceLabels.add((String) output.get(i));

					// compute c<k_i_j> 
					// c<k_i_j> is a binary variable 1- if the label assignment to token_i
					// violates constraint_k with respect to y[1__i-1]
					localConstraints.add(computeConstraintViolation(k, instanceLabels, 
							instanceTokens, instanceMedications, verbs));
				}

				constraintsPerInstance.put(j, localConstraints);
			}

			constraints.put(k, constraintsPerInstance);
		}

		// once we compute the constraint violation score across all the instances 
		// we compute the event probability
		HashMap<Integer, Double> eventProbability = new HashMap<Integer, Double>();

		for(int k=0; k < setConstraintsSize; k++){
			double score = 0.;
			int totalLength = 0;

			HashMap<Integer, ArrayList<Integer>> constraintsPerInstance = 
					constraints.get(k);

			for(ArrayList<Integer> vals : constraintsPerInstance.values()){
				for(Integer el : vals)
					score += el;
				totalLength += vals.size();
			}

			score = score/totalLength;
			eventProbability.put(k, score);

		}

		return eventProbability;
	}

	int computeConstraintViolation(int constraintIndex, ArrayList<String> labels, 
			ArrayList<String> tokenNames, ArrayList<Boolean> instanceMedications,
			ArrayList<Boolean> verbs){
		// choose the constraint function
		if(constraintIndex == 0)
			return isPRN(labels, tokenNames);

		if(constraintIndex == 1)
			return isNeighbour(labels, tokenNames, instanceMedications);

		//		if(constraintIndex == 2)
		//			return isHer(labels, tokenNames, instanceMedications);
		//			return isCoordinatedToReason(labels, tokenNames, verbs);

		return 0;
	}


	ArrayList<Boolean> getAllVerbs(FeatureVectorSequence input){
		ArrayList<Boolean> verbs = new ArrayList<Boolean>();

		for(int tokenIndex =0; tokenIndex < input.size(); tokenIndex ++){
			// get the token name
			FeatureVector token = (FeatureVector) input.get(tokenIndex);
			verbs.add(tokenIsVerb(token));
		}

		return verbs;
	}

	ArrayList<Boolean> getAllMedications(FeatureVectorSequence input){
		ArrayList<Boolean> tokenMeds = new ArrayList<Boolean>();

		for(int tokenIndex =0; tokenIndex < input.size(); tokenIndex ++){
			// get the token name
			FeatureVector token = (FeatureVector) input.get(tokenIndex);
			tokenMeds.add(tokenIsMedication(token));
		}

		return tokenMeds;
	}

	boolean tokenIsVerb(FeatureVector token){
		int[] featureIndexes = token.getIndices();
		for (int index = 0; index < featureIndexes.length; index ++){
			String feature = (String) token.getAlphabet().lookupObject(featureIndexes[index]);
			if(feature != null){
				if(feature.equals("isVerb"))
					return true;
			}
		}

		return false;
	}

	boolean tokenIsMedication(FeatureVector token){
		int[] featureIndexes = token.getIndices();
		for (int index = 0; index < featureIndexes.length; index ++){
			String feature = (String) token.getAlphabet().lookupObject(featureIndexes[index]);
			if(feature != null){
				if(feature.equals("isDrug"))
					return true;
			}
		}

		return false;
	}

	ArrayList<String> getAllTokenNames(FeatureVectorSequence input){
		ArrayList<String> tokenNames = new ArrayList<String>();

		for(int tokenIndex =0; tokenIndex < input.size(); tokenIndex ++){
			// get the token name
			FeatureVector token = (FeatureVector) input.get(tokenIndex);
			String tokenName = getTokenName(token);
			tokenNames.add(tokenName);
		}

		return tokenNames;
	}

	String getTokenName(FeatureVector token){
		String tokenName = "";

		int[] featureIndexes = token.getIndices();
		for (int index = 0; index < featureIndexes.length; index ++){
			String feature = (String) token.getAlphabet().lookupObject(featureIndexes[index]);
			if(feature != null){
				if(feature.startsWith("w0="))
					return feature.substring(3);
			}
		}

		return tokenName;

	}

	/**
	 * Constraint 1
	 * @param labels
	 * @param tokenNames
	 * @return
	 */
	int isPRN(ArrayList<String> labels, ArrayList<String> tokenNames){

		int index = labels.size()-1;
		String word = tokenNames.get(index);
		String label = labels.get(index);

		if(UtilMethods.removePunctuation(word).equals("pain"))
			if(index - 1 >=0 && UtilMethods.removePunctuation(tokenNames.get(index-1)).equals("prn"))
				if(!label.endsWith("R"))
					return 1;

		// by default assume the constraint is not violated
		return 0;
	}


	int isHer(ArrayList<String> labels, ArrayList<String> tokenNames,
			ArrayList<Boolean> medications) {
		if(labels.size() <= 1)
			return 0;

		String currentToken = tokenNames.get(labels.size()-2);

		if(currentToken.equals("her") ||
				currentToken.equals("his"))
			if(!labels.get(labels.size()-1).endsWith("R") && !medications.contains(true))
				return 1;

		return 0;
	}

	/**
	 * Constraint 2
	 * @param labels
	 * @param tokenNames
	 * @param instanceMedications
	 * @return
	 */
	int isNeighbour(ArrayList<String> labels, ArrayList<String> tokenNames, ArrayList<Boolean> instanceMedications){

		int index = labels.size() -1;
		
		// check whether one of the tokens is a medication followed by one of
		// the known patterns, then require the following label to be a BR			
		if(instanceMedications.get(index)){
			if (!checkPattern(index+1, labels, tokenNames, patternsMedication))
				return 1;
		}

		// check if there is a reason pattern starting at current position
		//			if(!checkPattern(index, labels, tokenNames, patternsReasons))
		//				return 1;


		// by default assume the constraint is not violated
		return 0;
	}

	/**
	 * Constraint 3
	 * @param labels
	 * @param tokenNames
	 * @return
	 */
	int isCoordinatedToReason(ArrayList<String> labels, ArrayList<String> tokenNames,
			ArrayList<Boolean> verbs){
		// check if the previous token is and and token -2 is a reason
		// the require that the current label and the and label are IR labels

		if(labels.size() >= 3){
			if(tokenNames.get(labels.size()-2).equals("and") ){
				if(labels.get(labels.size()-3).endsWith("R")){
					// check if the current label is a verb
					if(!verbs.get(labels.size()-1)){
						if(!labels.get(labels.size() -2).equals("R"))
							return 1;
						if(!labels.get(labels.size() -1).equals("R"))
							return 1;
					}
				}
			}
		}

		// by default assume the constraint is not violated
		return 0;
	}

	boolean checkPattern(int index, ArrayList<String> labels, 
			ArrayList<String> tokenNames, List<String> patterns){
		if(tokenNames.size() <= index) return true;

		String tmpWord = tokenNames.get(index);

		// check if there is a pattern that starts with the current word
		for(String pattern: patterns){
			if(pattern.startsWith(tmpWord)){
				String[] splitPattern = pattern.split(" ");
				boolean metEndPattern = true;

				if(splitPattern.length > 1){
					boolean foundEntirePattern = true;

					for(int k = 1; k < splitPattern.length; k ++){
						if(index+k < labels.size()){
							if(!tokenNames.get(index+k).equals(splitPattern[k])){
								foundEntirePattern = false;
								break;
							}
						}else{
							metEndPattern = false;
							break;
						}
					}

					if(!foundEntirePattern) metEndPattern = false;
				}

				if(metEndPattern && index+ splitPattern.length  < labels.size()){
					String label = labels.get(index+splitPattern.length );

					if(!label.equals("BR")) 
						return false;
				}
			}
		}

		return true;
	}
}
