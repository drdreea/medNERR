package probabModel.ccm_code.Experiments;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import probabModel.ccm_code.Experiments.Parameters.Constraints;
import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.Constraint;
import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.Data;
import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.HMMLearnerConstraintsAsFeatures;
import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.TaggingDict;

/**
 * This class created a HMM model for predicting the tags of the named entities
 * of interest
 * The model also uses a set of constraints in order to more accurately predict
 * the correct tag
 * 
 * Run this class using 
 * 		1. the number of features to be used by the model (or 0 if 
 *  no feature should be used) 
 *  	2. the path to the results file where predictions output should be stored
 *  	3. the type of constraints to be used by the model (depends on the 
 *  type of tags to be predicted)
 *  
 *  The constraints are basically world knowledge that we insert into the probabilistic model
 * @author ab
 *
 */
public class HMMConstraintsMedicationSupervised {
	public static String test_file = "";
	public static int numberFeatures = 8; // the expected number of features and attributes
	public static String results_file = "";
	public Constraints constraintsType;
	public boolean useFeatures = true;
	public boolean useConstraints = true;

	public HMMConstraintsMedicationSupervised(int nrFeatures, 
			String resultsFilePath, Constraints givenConstraints,
			boolean setUseFeatures, boolean setConstraints){
		Parameters.numFeatures = nrFeatures - TaggingDict.attributesCount;

		// check whether we should be using the features
		useFeatures = setUseFeatures;
		useConstraints = setConstraints;

		results_file = resultsFilePath;
		constraintsType = givenConstraints;
	}

	public static void main(String[] args){
		//testCheating();
		//System.exit(0);

		//		int[] trainSetSize={5,10,20,300};
		//		for(int i=0;i<trainSetSize.length;i++){
		//			for(int runId=0;runId<5;runId++){
		//for(Parameters.inferenceMethod=0;Parameters.inferenceMethod<3;Parameters.inferenceMethod++){
		//					String[] params={"data/citation_train."+trainSetSize[i]+"."+runId+".txt",
		//							"data/citation_test.txt","data/citation_unlabeled.txt","0","0" ,"0.9"};
		//					System.out.println("; training size="+trainSetSize[i]+" run "+runId);
		//String[] params={"data/ads_train.100.0.txt","data/ads_test.txt","data/ads_unlabeled.txt","1","0" };
		try{
			// first set the number of features that are contained in 
			// the training and test files

			String[] params = {args[0], args[1], args[2], "0", "0", "0.9"};
			results_file = args[3];
			args=params;

			HMMConstraintsMedicationSupervised classifier = 
					new HMMConstraintsMedicationSupervised(numberFeatures, results_file,
							Constraints.MEDICATION, true, true);

			classifier.testBed(args);
		}catch(Exception e){
			System.out.println(Parameters.errorMessage);
			System.out.println("Program execution failed with following error message ");
			e.printStackTrace();
		}
		//}
		//			}
		//		}
	}

	public void testBed (String[] argvs) throws Exception{	
		String train_file = argvs[0];
		test_file = argvs[1];
		String unlabeled_file = argvs[2];
		Parameters.problem = Integer.parseInt(argvs[3]);
		Parameters.method = Integer.parseInt(argvs[4]);
		Parameters.sup_model_weight=Double.parseDouble(argvs[5]);

		// Building Lexicon (in order to have probability table latter...)
		Vector<String> all_file_list = new Vector<String>();

		all_file_list.add(train_file);
		all_file_list.add(test_file);
		all_file_list.add(unlabeled_file);
		TaggingDict td = new TaggingDict(all_file_list);

		// Trainging....
		Data train_data = new Data(train_file,td);
		Data unlabeled_data = new Data(unlabeled_file,td);		

		evaluatedifferentConstraintSettings(train_data,unlabeled_data,td);
	}

	public void evaluatedifferentConstraintSettings(Data train_data,
			Data unlabeled_data,TaggingDict td) throws Exception{
		String[] constraints = null;

		switch(constraintsType){
		case MEDICATION:
			constraints = Parameters.medicationConstraints;
			break;
		case REASONANDRELATION:
			constraints = Parameters.reasonAndRelationConstraints;
			break;
		case RELATION:
			constraints = Parameters.relationConstraints;
			break;
		case MEDICAL:
			constraints = Parameters.medicalConstraints;
			break;
		case RELATION_GENERIC:
			constraints = Parameters.relationConstraintsGeneric;
			break;
		default:
			break;
		}

		Parameters.constraintsToApply=new Vector<Constraint>();
		//				printActiveConstraints(Parameters.constraintsToApply);
		//				evaluate(train_data, unlabeled_data, td);
		//check how much each individual constraint helps
		//		for(int i=0;i<constraints.length;i++){
		//			Parameters.constraintsToApply=new Vector<Constraint>();
		//			Parameters.constraintsToApply.addElement(Constraint.getByName(constraints[i]));
		//			printActiveConstraints(Parameters.constraintsToApply);
		//			evaluate(train_data, unlabeled_data,td);
		//		}
		//		//check how much the removal of each individual constraint hurts
		//		for(int i=0;i<constraints.length;i++){
		//			Parameters.constraintsToApply=new Vector<Constraint>();
		//			for(int j=0;j<constraints.length;j++)
		//				if(j!=i)
		//					Parameters.constraintsToApply.addElement(Constraint.getByName(constraints[j]));
		//			printActiveConstraints(Parameters.constraintsToApply);
		//			evaluate(train_data, unlabeled_data, td);
		//		}


		//check how well we perform with all the constraints
		Parameters.constraintsToApply = new Vector<Constraint>();

		// check whether we should use constraints
		if(useConstraints)
			for(int j = 0; j < constraints.length ; j ++)
				Parameters.constraintsToApply.addElement(Constraint.getByName(
						constraints[j]));

		printActiveConstraints(Parameters.constraintsToApply);

		evaluate( train_data, unlabeled_data, td);
	}

	public static void printActiveConstraints(Vector<Constraint> constraintsToApply){
		System.out.println(constraintsToApply.size()+ " active constraints:    " +
				"&&&&&&&&&&&");
		for(int i=0;i<constraintsToApply.size();i++)
			System.out.println("\t(*)"+constraintsToApply.elementAt(i).name);
	}

	public void evaluate(Data train_data,Data unlabeled_data,TaggingDict td){
		ArrayList<String> predictions = new ArrayList<String>();

		HMMLearnerConstraintsAsFeatures hmm = new HMMLearnerConstraintsAsFeatures(
				td,
				Parameters.constraintsToApply,
				useFeatures);
		// train the HMM
		hmm.train(train_data,unlabeled_data,td,Parameters.constraintsToApply);
		
		Data test_data = new Data(test_file,td);
		int num_correct = 0;
		int correct_context =0;
		int totalContext=0;

		int total = 0;		
		for(int i=0; i < test_data.size(); i++){
			int[] gold_label = test_data.tags_list.get(i);
			int[] word_seq = test_data.sen_list.get(i);
			int[] meds = test_data.isMed_list.get(i);
			int[] nearbyMeds = test_data.nearbyMed_list.get(i);
			int[] pos = test_data.pos_list.get(i);
			int[] reasons = test_data.reasons_list.get(i);
			int[] sections = test_data.sections_list.get(i);

			HashMap<String, int[]> features = null;
			if(test_data.features_list.isEmpty())
				features = new HashMap<String, int[]>();
				else 
					features = test_data.features_list.get(i);


			int[] predicted_label = hmm.makePrediction(word_seq,td,Parameters.constraintsToApply, 
					meds, nearbyMeds, pos, reasons, sections, features);
			total += word_seq.length;
			for (int j=0; j < predicted_label.length; j++){
				predictions.add(td.IdxToTag(predicted_label[j]));
				if (predicted_label[j] == gold_label[j]){
					num_correct ++;
					if(!td.IdxToTag(gold_label[j]).equals("O"))
						correct_context++;
				}
				if(!td.IdxToTag(gold_label[j]).equals("O"))
					totalContext ++;
			}
		}
		System.out.println("\tTotal: " + total + "\tCorrect: " + num_correct + "\tacc: " + 1.0 * num_correct/ total);		
		System.out.println("\tTotal: " + totalContext + "\tCorrect: " + correct_context + "\tacc: " + 1.0 * correct_context/ totalContext);		

		System.out.println("\t Constraint Violation Probs: ");
		for(int j=0;j<Parameters.constraintsToApply.size();j++)
			System.out.println(Parameters.constraintsToApply.get(j).name + " : " +
					String.valueOf(hmm.violationProb[j] ));
		System.out.println("");	

		try{
			PrintWriter writer = new PrintWriter (new FileWriter (results_file));
			for(String label : predictions){
				writer.write(label + "\n");
			}
			writer.close ();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
