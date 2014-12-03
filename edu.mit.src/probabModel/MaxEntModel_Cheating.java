package probabModel;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import probabModel.pipe.FeatValueSequence2AugmentableFeatureVector;
import probabModel.pipe.TokenSequence2FeatValueSequence;

import edu.umass.cs.mallet.base.classify.Classifier;
import edu.umass.cs.mallet.base.classify.ClassifierTrainer;
import edu.umass.cs.mallet.base.classify.Trial;
import edu.umass.cs.mallet.base.classify.evaluate.ConfusionMatrix;
import edu.umass.cs.mallet.base.pipe.CharSequence2TokenSequence;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.Target2Label;
import edu.umass.cs.mallet.base.pipe.iterator.CsvIterator;
import edu.umass.cs.mallet.base.pipe.iterator.PipeInputIterator;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.Labeling;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.Timing;

/**
 * @author ab
 *
 */
public class MaxEntModel_Cheating {

	private static CommandOption.File modelFile = new CommandOption.File
			(MaxEntModel_Cheating.class, "model-file", "FILENAME", true, null, "Text file describing model structure.", null);

	private static CommandOption.File trainFile = new CommandOption.File
			(MaxEntModel_Cheating.class, "training", "FILENAME", true, null, "File containing training data.", null);

	private static CommandOption.File testFile = new CommandOption.File
			(MaxEntModel_Cheating.class, "testing", "FILENAME", true, null, "File containing testing data.", null);

	private static CommandOption.Integer numLabelsOption = new CommandOption.Integer
			(MaxEntModel_Cheating.class, "num-labels", "INT", true, -1,
					"If supplied, number of labels on each line of input file." +
							"  Otherwise, the token ---- must separate labels from features.", null);

	private static CommandOption.String inferencerOption = new CommandOption.String
			(MaxEntModel_Cheating.class, "inferencer", "STRING", true, "TRP",
					"Specification of inferencer.", null);

	private static CommandOption.String maxInferencerOption = new CommandOption.String
			(MaxEntModel_Cheating.class, "max-inferencer", "STRING", true, "TRP.createForMaxProduct()",
					"Specification of inferencer.", null);

	private static CommandOption.String outputFile = new CommandOption.String
			(MaxEntModel_Cheating.class, "outputFile", "STRING", true, "results.txt",
					"Evaluator to use.  Java code grokking performed.", null);

	static CommandOption.Boolean cacheUnrolledGraph = new CommandOption.Boolean
			(MaxEntModel_Cheating.class, "cache-graphs", "true|false", true, false,
					"Whether to use memory-intensive caching.", null);

	static CommandOption.Boolean useTokenText = new CommandOption.Boolean
			(MaxEntModel_Cheating.class, "use-token-text", "true|false", true, false,
					"Set this to true if first feature in every list is should be considered the text of the " +
							"current token.  This is used for NLP-specific debugging and error analysis.", null);

	static CommandOption.Integer randomSeedOption = new CommandOption.Integer
			(MaxEntModel_Cheating.class, "random-seed", "INTEGER", true, 0,
					"The random seed for randomly selecting a proportion of the instance list for training", null);


	/**
	 * @param args
	 */
	public MaxEntModel_Cheating(String[] args){
		doProcessOptions (MaxEntModel_Cheating.class, args);

	}

	/**
	 * 
	 * @throws Exception
	 */
	void execute() throws Exception{
		Timing timing = new Timing ();

		Pipe pipe = new SerialPipes (new Pipe[] {
				new Target2Label (),
				(Pipe) new CharSequence2TokenSequence(CharSequenceLexer.LEX_NONWHITESPACE_TOGETHER),
				new TokenSequence2FeatValueSequence(),
				new FeatValueSequence2AugmentableFeatureVector()
		});

		PipeInputIterator trainSource = new CsvIterator (new FileReader (trainFile.value), 
				Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
				3,2,1);

		PipeInputIterator testSource = new CsvIterator (new FileReader (testFile.value), 
				Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
				3,2,1);

		InstanceList training = new InstanceList (pipe);
		training.add (trainSource);

		InstanceList testing = new InstanceList (pipe);
		testing.add (testSource);

		ClassifierTrainer maxEnt = new MaxEntTrainer();
		Classifier classifier = maxEnt.train(training);

		// print the classification results
		//		printResults(training, classifier);

//		 store the file
//				try {
//					oos = new ObjectOutputStream
//							(new FileOutputStream (trainFile.value.getAbsolutePath() + ".model"));
//					oos.writeObject (classifier);
//								classifier.print();
//					oos.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//					throw new IllegalArgumentException ("Couldn't write classifier to filename ");
//				}

		testMaxEnt(classifier, testing);

		timing.tick ("Training");

		System.out.println ("Total time (ms) = " + timing.elapsedTime ());
	}


	void printResults(InstanceList trainingFileIlist,
			Classifier classifier) {

		System.out.println("\n-------------------- Trial " );

		Trial trainTrial = new Trial (classifier, trainingFileIlist);

		System.out.println(new ConfusionMatrix (trainTrial).toString());
	}

	public enum TagType{
		IF, BF,
		ID, BD,
		IM, BM, 
		IU, BU,
		IR, O, BR,
		INSIDEMEDICATION, BEGINMEDICATION,
		BEGINRELATION, INSIDERELATION,
		ENTITY};
		/**
		 * 
		 * @param childClass
		 * @param args
		 */
		public static void doProcessOptions (Class childClass, String[] args)
		{
			CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
			options.add (childClass);
			options.process (args);
			options.logOptions (Logger.getLogger (""));
		}

		private void testMaxEnt(Classifier maxEnt, InstanceList testing) {
			
			Iterator instances = testing.iterator();
			ArrayList<String> resultLabels = new ArrayList<String>();

			HashMap<Integer, String> keysClass3 = new HashMap<Integer, String>();
			HashMap<Integer, String> keysClass2 = new HashMap<Integer, String>();
			HashMap<Integer, String> keysClass1 = new HashMap<Integer, String>();

			// retrieve the alphabet
			FeatureVector data = (FeatureVector) ((Instance)testing.get(0)).getData();
			Alphabet alp = data.getAlphabet();

			TagType[] allTags = TagType.values();

			for(int index =0; index < allTags.length; index ++){
				TagType tag = allTags[index];

				int classIndex = alp.lookupIndex("tag-1=" + tag.toString());
				keysClass1.put(classIndex, "tag-1=" + tag.toString());

				classIndex = alp.lookupIndex("tag-2=" + tag.toString());
				keysClass2.put(classIndex, "tag-2=" + tag.toString());

				classIndex = alp.lookupIndex("tag-3=" + tag.toString());
				keysClass3.put(classIndex, "tag-3=" + tag.toString());          
			}

			HashMap<Integer, Integer> prevLabels = new HashMap<Integer, Integer>();
			String initialName = "";

			while (instances.hasNext()) {
				Instance inst = (Instance) instances.next();
				String name = (String)inst.getName();

				// reset the values if a new record has been started
				if(!initialName.equals(name.split("i2b2.entries")[0])){
					initialName = name.split("i2b2.entries")[0];
					prevLabels.put(1, alp.lookupIndex("tag-1=O"));
					prevLabels.put(2, alp.lookupIndex("tag-2=O"));
					prevLabels.put(3, alp.lookupIndex("tag-3=O"));
				}


				// get the data
				data = (FeatureVector) inst.getData();

				// get the indices, the values, and the alphabet
				int[] indices = data.getIndices();

				for(int index=0; index < indices.length; index ++){
					if(keysClass1.containsKey(indices[index])){
						indices[index] = prevLabels.get(1);
					}
					if(keysClass2.containsKey(indices[index])){
						indices[index] = prevLabels.get(2);
					}
					if(keysClass3.containsKey(indices[index])){
						indices[index] = prevLabels.get(3);
					}
				}

				// update the data labels 
				FeatureVector newFV = new FeatureVector(alp, indices, 
						data.getValues());

				// set the instance data
				inst.unLock(); inst.setData(newFV); inst.setLock();

				// classify the updated instance
				Labeling labeling = maxEnt.classify(inst).getLabeling();

				// Update the prev labels
				String label2 = (String) alp.lookupObject(prevLabels.get(1));
				int label2_int = alp.lookupIndex("tag-2=" + label2.split("=")[1]);
				
				String label3 = (String) alp.lookupObject(prevLabels.get(2));
				int label3_int = alp.lookupIndex("tag-3=" + label3.split("=")[1]);
				
				prevLabels.put(3, label3_int);
				prevLabels.put(2, label2_int);
				prevLabels.put(1, alp.lookupIndex("tag-1=" + labeling.getBestLabel().toString()));

				resultLabels.add(labeling.getBestLabel().toString());

			}


			try {
				File resultsFile = new File (outputFile.value);
				PrintWriter writer = new PrintWriter (new FileWriter (resultsFile));
				for(String label : resultLabels){
					writer.write(label + "\n");
				}
				writer.close ();
			} catch (Exception e) {
				e.printStackTrace ();
			}
		}

		/**
		 * 
		 * @param args
		 */
		public static void main(String[] args){
			MaxEntModel_Cheating maxEnt = new MaxEntModel_Cheating(args);
			try{
				maxEnt.execute();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
}
