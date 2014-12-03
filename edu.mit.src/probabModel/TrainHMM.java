package probabModel;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.*;

import probabModel.pipe.SimpleTaggerSentence2TokenSequence;

import cc.mallet.fst.PerClassAccuracyEvaluator;
import cc.mallet.fst.Transducer;
import cc.mallet.fst.TransducerEvaluator;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.util.*;

public class TrainHMM {
	private static CommandOption.File trainFile = new CommandOption.File
			(TrainHMM.class, "training", "FILENAME", true, null, "File containing training data.", null);

	private static CommandOption.File testFile = new CommandOption.File
			(TrainHMM.class, "testing", "FILENAME", true, null, "File containing testing data.", null);

	private static CommandOption.File unlabelledFile = new CommandOption.File
			(TrainHMM.class, "unlabelled", "FILENAME", true, null, "File containing unlabelled data.", null);

	private static CommandOption.String outputFile = new CommandOption.String
			(TrainHMM.class, "outputFile", "STRING", true, "results.txt",
					"Evaluator to use.  Java code grokking performed.", null);

	public TrainHMM(String[] args){
		doProcessOptions (TrainHMM.class, args);
	}

	public void execute() throws IOException {

		SimpleTaggerSentence2TokenSequence basePipe;
		// the AcrfData2TokenSequence param represents the number 
		// of labels in the test set
		boolean setTokenAsFeature = true;
		basePipe = new SimpleTaggerSentence2TokenSequence (setTokenAsFeature);

		Pipe pipe = new SerialPipes (new Pipe[] {
				basePipe,
				new TokenSequence2FeatureSequence (),
		});

		InstanceList trainingInstances = new InstanceList(pipe);
		InstanceList testingInstances = new InstanceList(pipe);
		InstanceList unlabelledInstances = new InstanceList(pipe);

		trainingInstances.addThruPipe(new LineGroupIterator (
				new FileReader (trainFile.value), 
				Pattern.compile ("^\\s*$"), 
				true));

		testingInstances.addThruPipe(new LineGroupIterator (
				new FileReader (testFile.value), 
				Pattern.compile ("^\\s*$"), 
				true));

		unlabelledInstances.addThruPipe(new LineGroupIterator (
				new FileReader (unlabelledFile.value), 
				Pattern.compile ("^\\s*$"), 
				true));
		
		HMM hmm = new HMM(trainingInstances.getPipe(), 
				trainingInstances.getPipe());
				hmm.addStatesForThreeQuarterLabelsConnectedAsIn(trainingInstances);
//		hmm.addStatesForLabelsConnectedAsIn(trainingInstances);
		//		hmm.addStatesForBiLabelsConnectedAsIn(trainingInstances);

		TransducerEvaluator trainingEvaluator = 
				new PerClassAccuracyEvaluator(trainingInstances, "training");
		TransducerEvaluator testingEvaluator = 
				new PerClassAccuracyEvaluator(testingInstances, "testing");

		HMMTrainerByLikelihood trainer = 
				new HMMTrainerByLikelihood(hmm);
		//		hmm.train(trainingInstances);
		//		test(hmm, testingInstances);

		//		trainer.train(trainingInstances, unlabelledInstances, 1000);
		trainer.train(trainingInstances,  1000);

		trainingEvaluator.evaluate(trainer);
		testingEvaluator.evaluate(trainer);
		test(trainer, testingInstances);
	}

	private void test(HMM trainer, InstanceList testingInstances) {
		File resultsFile = new File (outputFile.value);
		ArrayList<String> resultLabels = new ArrayList<String>();

		for (int i = 0; i < testingInstances.size(); i++) {
			Instance instance = testingInstances.get(i);

			Sequence input = (Sequence) instance.getData();
			Sequence predOutput = trainer.transduce (input);

			for (int j = 0; j < predOutput.size(); j++) {
				Object val = predOutput.get(j);

				resultLabels.add(val.toString());
			}
		}		

		try {
			PrintWriter writer = new PrintWriter (new FileWriter (resultsFile));
			for(String label : resultLabels){
				writer.write(label + "\n");
			}
			writer.close ();
		} catch (Exception e) {
			e.printStackTrace ();
		}
	}


	private void test(HMMTrainerByLikelihood trainer,
			InstanceList testingInstances) {
		File resultsFile = new File (outputFile.value);
		ArrayList<String> resultLabels = new ArrayList<String>();
		Transducer model = trainer.getTransducer();

		for (int i = 0; i < testingInstances.size(); i++) {
			Instance instance = testingInstances.get(i);

			Sequence input = (Sequence) instance.getData();
			Sequence predOutput = model.transduce (input);

			for (int j = 0; j < predOutput.size(); j++) {
				Object val = predOutput.get(j);

				resultLabels.add(val.toString());
			}
		}		

		try {
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
	 * @param childClass
	 * @param args
	 */
	public static void doProcessOptions (Class<TrainHMM> childClass, String[] args)
	{
		CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
		options.add (childClass);
		options.process (args);
		options.logOptions (Logger.getLogger (""));
	}

	public static void main (String[] args) throws Exception {
		TrainHMM trainer = new TrainHMM(args);
		try{
			trainer.execute();
		}catch(Exception e){
			e.printStackTrace();
		}

	}

}