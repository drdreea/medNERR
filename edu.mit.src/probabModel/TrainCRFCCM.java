package probabModel;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.*;

import classifier.TrainTestCreator.CorpusType;

import corpus.Annotation;

import probabModel.ccm.CRF;
import probabModel.ccm.CRFTrainerByLabelLikelihood;
import probabModel.ccm.ConstraintsCalculator;
import probabModel.ccm.PerClassAccuracyEvaluator;
import probabModel.ccm.Transducer;
import probabModel.ccm.TransducerEvaluator;
import probabModel.pipe.SimpleTaggerSentence2TokenSequence;
import utils.UtilMethods;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.util.*;

public class TrainCRFCCM {
	private static CommandOption.File trainFile = new CommandOption.File
			(TrainCRFCCM.class, "training", "FILENAME", true, null, "File containing training data.", null);

	private static CommandOption.File testFile = new CommandOption.File
			(TrainCRFCCM.class, "testing", "FILENAME", true, null, "File containing testing data.", null);

	private static CommandOption.File medicationFile = new CommandOption.File
			(TrainCRFCCM.class, "medicationFile", "FILENAME", true, null, "File containing medication data.", null);

	private static CommandOption.String outputFile = new CommandOption.String
			(TrainCRFCCM.class, "outputFile", "STRING", true, "results.txt",
					"Evaluator to use.  Java code grokking performed.", null);

	public TrainCRFCCM(String[] args){
		doProcessOptions (TrainCRFCCM.class, args);
	}

	ArrayList<Annotation> importMedication(){
		ArrayList<String> lines = UtilMethods.readFileLines(medicationFile.value.getAbsolutePath());
		ArrayList<Annotation> annotations = new ArrayList<Annotation>();

		for (String line : lines) {
			Annotation result = Annotation.parseAnnotationLine(line, CorpusType.I2B2);
			if (result != null)
				annotations.add(result);
		}

		return annotations;
	}
	
	public void execute() throws IOException {
		// import all the medication data
		ArrayList<Annotation> medications = importMedication();
//		ArrayList<String> medicationStrings = loadMedicationStrings(medications);

		SimpleTaggerSentence2TokenSequence basePipe;
		// the AcrfData2TokenSequence param represents the number 
		// of labels in the test set
		boolean setTokenAsFeature = true;
		basePipe = new SimpleTaggerSentence2TokenSequence (setTokenAsFeature);

		Pipe pipe = new SerialPipes (new Pipe[] {
				basePipe,
				new TokenSequence2FeatureVectorSequence (),
		});

		InstanceList trainingInstances = new InstanceList(pipe);
		InstanceList testingInstances = new InstanceList(pipe);

		trainingInstances.addThruPipe(new LineGroupIterator (
				new FileReader (trainFile.value), 
				Pattern.compile ("^\\s*$"), 
				true));

		testingInstances.addThruPipe(new LineGroupIterator (
				new FileReader (testFile.value), 
				Pattern.compile ("^\\s*$"), 
				true));

		CRF hmm = new CRF(trainingInstances.getPipe(), 
				trainingInstances.getPipe());
		//		HMM hmm = new HMM(pipe, null);
		hmm.addStatesForThreeQuarterLabelsConnectedAsIn(trainingInstances);
		//		hmm.addStatesForLabelsConnectedAsIn(trainingInstances);
		//		hmm.addStatesForBiLabelsConnectedAsIn(trainingInstances);

//		ConstraintsCalculator calc = new ConstraintsCalculator(medicationStrings);
//		HashMap<Integer, Double> constraintProbabilities = calc.computeConstraints(trainingInstances);
//		HashMap<Integer, Double> roConstraints = getRo(constraintProbabilities);
//		hmm.roConstraints = roConstraints;
//		hmm.constraintsCalc = calc;
		
		TransducerEvaluator trainingEvaluator = 
				new PerClassAccuracyEvaluator(trainingInstances, "training");
		TransducerEvaluator testingEvaluator = 
				new PerClassAccuracyEvaluator(testingInstances, "testing");

		CRFTrainerByLabelLikelihood trainer = 
				new CRFTrainerByLabelLikelihood(hmm);
		//		hmm.train(trainingInstances);
		//		test(hmm, testingInstances);

		trainer.train(trainingInstances, 1000);

		trainingEvaluator.evaluate(trainer);
		testingEvaluator.evaluate(trainer);
		test(trainer, testingInstances);
	}

	private HashMap<Integer, Double> getRo(
			HashMap<Integer, Double> constraintProbabilities) {
		HashMap<Integer, Double> ro = new HashMap<Integer, Double>();
		
		for(Integer pos : constraintProbabilities.keySet()){
			double newVal = (-1)*Math.log(constraintProbabilities.get(pos)*1.0/
					(1-constraintProbabilities.get(pos)));
			ro.put(pos, newVal);
		}
				
		return ro;
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

				if(val.toString().contains("R"))
					System.out.println("her");

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


	private void test(CRFTrainerByLabelLikelihood trainer,
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
	public static void doProcessOptions (Class<TrainCRFCCM> childClass, String[] args)
	{
		CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
		options.add (childClass);
		options.process (args);
		options.logOptions (Logger.getLogger (""));
	}

	public static void main (String[] args) throws Exception {
		TrainCRFCCM trainer = new TrainCRFCCM(args);
		try{
			trainer.execute();
		}catch(Exception e){
			e.printStackTrace();
		}

	}

}