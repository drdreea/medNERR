package eval;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.Annotation.AnnotationType;

import classifier.Storage;


/**
 * Class for testing whether results of two system outputs are statistically significant
 * Run class with params:
 * - gold standard path
 * - system 1 output path
 * - system 2 output path
 * - output path (a directory where the shuffled outputs will be stored
 * 
 * @author ab
 *
 */
public class ApproximateRandomizationRelationOutput {
	/*
	 *required args: gs; sys1 output; sys2 output; output for randomization test 
	 */
	final static  int requiredArgs = 4;
	Random rng;
	public Storage store;

	String pathGS;
	String pathSystem1 ;
	String pathSystem2 ;
	String pathOutput ;

	int numberIterations = 100;
	int numberDiff = 0;
	double alpha = 0.016;


	public ApproximateRandomizationRelationOutput(String gsPath,
			String sys1, String sys2, String output) {
		this.pathGS = gsPath;
		this.pathSystem1 = sys1;
		this.pathSystem2 = sys2;
		this.pathOutput = output;

		rng = new Random();
		store = new Storage();
	}

	public void execute(){
		// read in the system outputs
		MatchCompare reference1 = new MatchCompare(pathGS, pathSystem1, pathOutput);
		MatchCompare reference2 = new MatchCompare(pathGS, pathSystem2, pathOutput);

		// compute the initial results different
		double scoreSystem1 = reference1.execute();
		double scoreSystem2 = reference2.execute();
		double initial_difference = Math.abs(scoreSystem2-scoreSystem1);

		for (int counter = 0; counter < numberIterations; counter ++){
			// get pseudo paths for shuffling the system outputs
			String pseudoPath1 = generatePseudoPath();
			String pseudoPath2 = generatePseudoPath();

			shuffleSystem(pseudoPath1, pseudoPath2, reference1, reference2);

			// evaluate the shuffled system outputs
			MatchCompare compareSystems1 = new MatchCompare(pathGS, pseudoPath1, pathOutput);
			MatchCompare compareSystems2 = new MatchCompare(pathGS, pseudoPath2, pathOutput);
			scoreSystem1 = compareSystems1.execute();
			scoreSystem2 = compareSystems2.execute();

			double pseudo_difference = Math.abs(scoreSystem2-scoreSystem1);
			if(pseudo_difference >= initial_difference)
				numberDiff ++;

			// clear the paths
			clearPaths(new File(pseudoPath1));
			clearPaths(new File(pseudoPath2));
		}

		// compute the probability score
		double prob = (numberDiff +1)*1.0/(1+numberIterations);

		System.out.println("Probability is: " + String.valueOf(prob));
		if(prob < alpha)
			System.out.println("Statistically significant");
		else 
			System.out.println("NOT statistically significant");


	}

	private void clearPaths(File pseudoPath1) {
		if (pseudoPath1.isDirectory()) {
			String[] children = pseudoPath1.list();
			for (int i=0; i<children.length; i++) {
				clearPaths(new File(pseudoPath1, children[i]));
			}
		}

		// The directory is now empty so delete it
		pseudoPath1.delete();
	}

	/**
	 * generate a new path where we can store the pseudo output
	 * @return new path
	 */
	String generatePseudoPath(){
		String path = null;
		String folderName = generateString("abcdefghijklmnopqrstuvwxyz",8);

		while((new File(this.pathOutput, folderName)).exists()){
			folderName = generateString("abcdefghijklmnopqrstuvwxyz",8);
		}

		// create the output path
		path = new File(this.pathOutput, folderName).getAbsolutePath();

		new File(path).mkdir();

		return path;
	}

	public String generateString(String characters, int length)
	{
		char[] text = new char[length];
		for (int i = 0; i < length; i++)
		{
			text[i] = characters.charAt(rng.nextInt(characters.length()));
		}
		return new String(text);
	}

	/**
	 * 
	 * @param pseudoPath2 
	 * @param pseudoPath1 
	 * @param pathSystem12
	 * @param reference2
	 */
	private void shuffleSystem(String pseudoPath1, String pseudoPath2, 
			MatchCompare reference1, 
			MatchCompare reference2) {

		HashMap<String, AnnotationFile> system1Annotations = reference1.reasonAnnotation;
		HashMap<String, AnnotationFile> system2Annotations = reference2.reasonAnnotation;

		// go through each file and shuffle them
		for(String file : system1Annotations.keySet()){
			// check if the second system has the same file
			if(!system2Annotations.containsKey(file)){
				System.out.println("ERROR: file " + file + " not contained in system2");
				continue;
			}

			ArrayList<Annotation> annotations1 = system1Annotations.get(file).annotations;
			ArrayList<Annotation> annotations2 = system2Annotations.get(file).annotations;

			// align the annotations based on the medication names
			ArrayList<AnnotationPair> alignment = alignAnnotations(annotations1, annotations2);

			// shuffle the aligned annotations with a 0.5 probability
			ArrayList<String> pseudoLines1 = new ArrayList<String>();
			ArrayList<String> pseudoLines2 = new ArrayList<String>();

			for(AnnotationPair pair : alignment){
				//generate rand int between 0 (False) and 1 (True) to determine if
				// we will swap the given chain in the system and reference files	
				boolean swap = new java.util.Random().nextBoolean();
				if(swap){
					if(pair.systemAnnt1 != null)
						pseudoLines2.add(Annotation.printString(pair.systemAnnt1));
					if(pair.systemAnnt2 != null)
						pseudoLines1.add(Annotation.printString(pair.systemAnnt2));
				}else{
					if(pair.systemAnnt1 != null)
						pseudoLines1.add(Annotation.printString(pair.systemAnnt1));
					if(pair.systemAnnt2 != null)
						pseudoLines2.add(Annotation.printString(pair.systemAnnt2));
				}
			}

			// store the pseudo outputs
			String newFilePath = new File(pseudoPath1, file).getAbsolutePath();
			store.storeFile(newFilePath, pseudoLines1);

			newFilePath = new File(pseudoPath2, file).getAbsolutePath();
			store.storeFile(newFilePath, pseudoLines2);
		}	
	}

	private static ArrayList<AnnotationPair> alignAnnotations(ArrayList<Annotation> annotations1,
			ArrayList<Annotation> annotations2) {
		ArrayList<AnnotationPair> alignments = new ArrayList<AnnotationPair>();
		ArrayList<Annotation> alignedAnnotations = new ArrayList<Annotation>();

		// search for matching medications
		for(Annotation annt1 : annotations1){
			AnnotationDetail med1 = annt1.annotationElements.get(AnnotationType.M);

			for(Annotation annt2 : annotations2){
				// check that the annotation was not aligned already
				if(alignedAnnotations.contains(annt2))
					continue;
				AnnotationDetail med2 = annt2.annotationElements.get(AnnotationType.M);

				if(med1.equalsAnnotation(med2) || 
						med1.overlapAnnotation(med2) ||
						med1.overlapDiffLines(med2)){
					alignments.add(new AnnotationPair(annt1, annt2));
					alignedAnnotations.add(annt2);
					alignedAnnotations.add(annt1);
					break;
				}		
			}
		}

		// add the annotations that did not get aligned
		for(Annotation annt : annotations1){
			if(alignedAnnotations.contains(annt)) continue;

			alignments.add(new AnnotationPair(annt, null));
		}

		for(Annotation annt : annotations2){
			if(alignedAnnotations.contains(annt)) continue;

			alignments.add(new AnnotationPair(annt, null));
		}

		return alignments;

	}

	public static void main(String[] args) {
		if (args.length != requiredArgs) {
			System.out
			.println("Incorrect arguments! Required configuration file path.");
			System.exit(-1);
		}

		ApproximateRandomizationRelationOutput approxClass = new
				ApproximateRandomizationRelationOutput(args[0], args[1], args[2], args[3]);


		approxClass.execute();

	}
}

class AnnotationPair{
	public Annotation systemAnnt1;
	public Annotation systemAnnt2;

	AnnotationPair(Annotation annt1, Annotation annt2){
		systemAnnt1 = annt1;
		systemAnnt2 = annt2;
	}
}
