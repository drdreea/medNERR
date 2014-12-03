package probabModel.ccm_code.Experiments;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.Constraint;
import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.Data;
import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.HMMLearnerConstraintsAsFeatures;
import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.TaggingDict;

public class RunReasonAndRelationConstraintsAsFeatures {


	public static String test_file = "";
	public static String results_file = "";
	public static final int numberFeatures = 9;
	public static boolean useConstraints = true;

	/*

	public static void testCheating()throws Exception{

		String[] params={"data/citation_train.300.0.txt","data/citation_test.txt","data/citation_unlabeled.txt","0","0" };
		String[] argvs=params;

		String train_file = argvs[0];
		test_file = argvs[1];
		String unlabeled_file = argvs[2];
		Parameters.problem = Integer.parseInt(argvs[3]);
		Parameters.method = Integer.parseInt(argvs[4]);


		if (Parameters.problem != Parameters.ADS && Parameters.problem != Parameters.CITATION){
			System.out.println("Wrong problem setting!!");
			System.exit(1);
		}


		if (Parameters.method != Parameters.SUP && Parameters.method != Parameters.SEMI_Avg  && 
			Parameters.method != Parameters.SEMI_Avg_CON  && Parameters.method != Parameters.SEMI_NO_Avg ){
			System.out.println("Wrong method setting!!");
			System.exit(1);
		}


		// Building Lexicon (in order to have probability table latter...)
		Vector<String> all_file_list = new Vector<String>();

		all_file_list.add(train_file);
		all_file_list.add(test_file);
		all_file_list.add(unlabeled_file);
		TaggingDict td = new TaggingDict(all_file_list);

		// Trainging....
		Data train_data = new Data(train_file,td);
		Data test_data = new Data(test_file,td);

		for(int i=0;i<test_data.sen_list.size();i++){
			train_data.sen_list.add(test_data.sen_list.get(i));
			train_data.tags_list.add(test_data.tags_list.get(i));
		}
		Data unlabeled_data = new Data(unlabeled_file,td);		

		System.out.println("Part1: HARD Constarints");		
		Parameters.hardConstraints=true;
		evaluatedifferentConstraintSettings(train_data,unlabeled_data,td);
		System.out.println("Part1: SOFT Constarints");		
		Parameters.hardConstraints=false;
		evaluatedifferentConstraintSettings(train_data,unlabeled_data,td);		

	}
	 */
	
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
			Parameters.numFeatures = numberFeatures;
			
			String[] params = {args[0], args[1], args[2], "0", "0", "0.9"};
			results_file = args[3];
			args=params;
									
			testBed(args);
		}catch(Exception e){
			System.out.println(Parameters.errorMessage);
			System.out.println("Program execution failed with following error message ");
			e.printStackTrace();
		}
		//}
		//			}
		//		}
	}

	public static void testBed (String[] argvs) throws Exception{	
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

	public static void evaluatedifferentConstraintSettings(Data train_data,Data unlabeled_data,TaggingDict td) throws Exception{
		String[] constraints=Parameters.reasonAndRelationConstraints;
		//		if(Parameters.problem == Parameters.CITATION)
		//			constraints=Parameters.citationsConstraints;

		Parameters.constraintsToApply=new Vector<Constraint>();
		//				printActiveConstraints(Parameters.constraintsToApply);
		//				evaluate(train_data, unlabeled_data, td);
		/*//check how much each individual constraint helps
		for(int i=0;i<constraints.length;i++){
			Parameters.constraintsToApply=new Vector<Constraint>();
			Parameters.constraintsToApply.addElement(Constraint.getByName(constraints[i]));
			printActiveConstraints(Parameters.constraintsToApply);
			evaluate(train_data, unlabeled_data,td);
		}
		//check how much the removal of each individual constraint hurts
		for(int i=0;i<constraints.length;i++){
			Parameters.constraintsToApply=new Vector<Constraint>();
			for(int j=0;j<constraints.length;j++)
				if(j!=i)
					Parameters.constraintsToApply.addElement(Constraint.getByName(constraints[j]));
			printActiveConstraints(Parameters.constraintsToApply);
			evaluate(train_data, unlabeled_data, td);
		}
		 */

		//check how well we perform with all the constraints
		Parameters.constraintsToApply=new Vector<Constraint>();
		for(int j=0;j<constraints.length;j++)
			Parameters.constraintsToApply.addElement(Constraint.getByName(constraints[j]));
		printActiveConstraints(Parameters.constraintsToApply);
		evaluate( train_data, unlabeled_data, td);
	}

	public static void printActiveConstraints(Vector<Constraint> constraintsToApply){
		System.out.println(constraintsToApply.size()+ " active constraints:    &&&&&&&&&&&");
		for(int i=0;i<constraintsToApply.size();i++)
			System.out.println("\t(*)"+constraintsToApply.elementAt(i).name);
	}

	public static void evaluate(Data train_data,Data unlabeled_data,TaggingDict td){
		ArrayList<String> predictions = new ArrayList<String>();

		HMMLearnerConstraintsAsFeatures hmm = new HMMLearnerConstraintsAsFeatures(td,
				Parameters.constraintsToApply, useConstraints);
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
