package probabModel.ccm_code.HMMLearningAndInferenceWithConstraints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import probabModel.ccm_code.Experiments.Parameters;

public class HMMLearnerConstraintsAsFeatures{

	public static final int LEARNING_CYLCES=100;

	public int numStates;
	public int sigmaSize;

	public double pi[];
	public double a[][];
	public double b[][];
	public double violationProb[];
	public boolean useFeatures = true;

	public HashMap<String, Double[][]> featureProbabilityMatrix;

	public HMMLearnerConstraintsAsFeatures(TaggingDict td,
			Vector<Constraint> activeConstraints, boolean setUseFeatures){
		this.useFeatures = setUseFeatures;

		numStates = td.GetTagNum();
		sigmaSize = td.GetWordNum() ;

		pi = new double[numStates];
		a = new double[numStates][numStates];
		violationProb=new double[activeConstraints.size()];
		b = new double[numStates][sigmaSize];
		featureProbabilityMatrix = new HashMap<String, Double[][]>();

		// initialize the feature probability matrix
		for(int countFeat = 0 ; countFeat < Parameters.numFeatures; countFeat ++){
			String featureName = String.valueOf(countFeat + TaggingDict.attributesCount);
			int featureLength = td.GeatFeatureLength(featureName);

			featureProbabilityMatrix.put(featureName, new Double[numStates][featureLength]);
		}
	}


	public double divide(double n, double d) {
		if (n == 0)
			return 0;
		else
			return n / d;
	}


	public void train(Data labeled_data, Data unlabeled_data,TaggingDict td,Vector<Constraint> activeConstraints) {
		System.out.println("Beam EMHMM--------------");
		// so we first train a HMM from labeled data and also learn the 
		// probabilities associated with each of the active constraints
		trainFromLabeledData(labeled_data, td,activeConstraints);

		//ExpManager.evaluate(this, mytd, false);
		if (Parameters.method == Parameters.SEMI_Avg || 
				Parameters.method == Parameters.SEMI_Avg_CON || 
				Parameters.method == Parameters.SEMI_NO_Avg){

			//if (Parameters.method == Parameters.SEMI_Avg_CON)
			//	Parameters.use_constraints_in_beamsearch = true;
			//else{
			//	Parameters.use_constraints_in_beamsearch = false;
			if (Parameters.method == Parameters.SEMI_NO_Avg)
				Parameters.sup_model_weight = 0;					
			//}

			// we use a EM beam search to learn from the unlabeled data
			HardEMByBeamSearch(unlabeled_data.sen_list, 
					unlabeled_data.isMed_list, 
					unlabeled_data.nearbyMed_list,
					unlabeled_data.pos_list,
					unlabeled_data.reasons_list,
					unlabeled_data.sections_list,
					unlabeled_data.features_list,
					5, td,activeConstraints);
			//			ArrayList<int[]> combined_list = (ArrayList<int[]>)labeled_data.sen_list.clone();
			//			combined_list.addAll(unlabeled_data.sen_list);
			//			HardEMByBeamSearch(combined_list, 5, mytd);

		}
	}


	public void trainFromLabeledData(Data labeled_data,TaggingDict td,Vector<Constraint> activeConstraints) {
		double[] violatons=new double[activeConstraints.size()];
		double[] noViolatons=new double[activeConstraints.size()];
		Arrays.fill(pi, 0);

		for(int i=0;i<b.length;i++)
		{
			Arrays.fill(b[i], 0);
			Arrays.fill(a[i], 0);
			Arrays.fill(violatons,0.1);
			Arrays.fill(noViolatons,0.1);
		}

		// initialize the feature probability matrix
		for(String feat : featureProbabilityMatrix.keySet()){
			Double[][] vals = featureProbabilityMatrix.get(feat);
			for(int i=0; i<b.length; i++)
				Arrays.fill(vals[i], 0.0);
			featureProbabilityMatrix.put(feat, vals);
		}

		for (int i = 0; i < labeled_data.sen_list.size(); i++) {
			if ((i+1) % 100 == 0) {
				System.out.println("supervised training "+(i+1) + "-th sequences");
			}
			int[] vec = labeled_data.sen_list.get(i);
			int[] gold_state_seq = labeled_data.tags_list.get(i);
			int[] meds = labeled_data.isMed_list.get(i);
			int[] nearbyMeds = labeled_data.nearbyMed_list.get(i);
			int[] pos = labeled_data.pos_list.get(i);
			int[] reasons = labeled_data.reasons_list.get(i);
			int[] sections = labeled_data.sections_list.get(i);

			HashMap<String, int[]> features = null;
			if(labeled_data.features_list.isEmpty())
				features = new HashMap<String, int[]>();
				else
					features = labeled_data.features_list.get(i);

			State[] states=new State[vec.length];
			states[0]=new State(0,vec,gold_state_seq[0], meds, nearbyMeds, 
					pos,reasons, sections, features);
			for(int j=1;j<gold_state_seq.length;j++){
				states[j]=new State(states[j-1],gold_state_seq[j]);
			}		
			for(int j=0;j<states.length;j++)
				for(int k=0;k<activeConstraints.size();k++)
				{
					if(activeConstraints.elementAt(k).isViolated(states[j], td))
						violatons[k]++;
					else
						noViolatons[k]++;
				}	

			// update the probabilities
			b[gold_state_seq[0]][vec[0]]++;
			pi[gold_state_seq[0]]++;
			for(String feat : features.keySet()){
				Double[][] vals= featureProbabilityMatrix.get(feat);
				vals[gold_state_seq[0]][features.get(feat)[0]]++;
				featureProbabilityMatrix.put(feat, vals);
			}

			for (int j=1;j<gold_state_seq.length;j++)
			{
				a[gold_state_seq[j-1]][gold_state_seq[j]]++;
				b[gold_state_seq[j]][vec[j]]++;

				for(String feat : features.keySet()){
					Double[][] vals= featureProbabilityMatrix.get(feat);
					vals[gold_state_seq[j]][features.get(feat)[j]]++;
					featureProbabilityMatrix.put(feat, vals);
				}
			}
		}

		//now estimate the probabilities
		estimateProbability(Parameters.smooth_prob_para);
		for(int i=0;i<violationProb.length;i++){
			violationProb[i]=violatons[i]/(violatons[i]+noViolatons[i]);
		}

	}
	public int[] makePrediction(int seq[],TaggingDict td,Vector<Constraint> activeConstraints, 
			int[] meds, int[] nearbyMeds, int[] pos, int[] reasons, int[] sections,
			HashMap<String, int[]> features){
		return predictSingle1(seq,td,activeConstraints, meds, nearbyMeds,
				pos, reasons, sections, features);
	}


	public HMMLearnerConstraintsAsFeatures makeCopy( HMMLearnerConstraintsAsFeatures h,
			TaggingDict td,Vector<Constraint> activeConstraints){
		HMMLearnerConstraintsAsFeatures ans = new HMMLearnerConstraintsAsFeatures(td,
				activeConstraints, this.useFeatures);

		int i,j;
		for(i=0;i<h.numStates;i++)
			ans.pi[i] = h.pi[i];

		for(i=0;i<h.numStates;i++)
			for(j=0;j<h.numStates;j++)
				ans.a[i][j] = h.a[i][j];

		for(i=0;i<h.numStates;i++)
			for(j=0;j<h.sigmaSize;j++)
				ans.b[i][j] = h.b[i][j];

		for(i=0;i<activeConstraints.size();i++)
			ans.violationProb[i]=h.violationProb[i];

		// also make a copy of the feature probability matrix
		ans.featureProbabilityMatrix = new HashMap<String, Double[][]>();

		for(String feat : h.featureProbabilityMatrix.keySet())
			ans.featureProbabilityMatrix.put(feat, 
					h.featureProbabilityMatrix.get(feat));

		return ans;
	}

	public void smoothHMM(double lambda, double[] pi1, double[][] a1, double[][] b1,
			HashMap<String, Double[][]> featureProbabilityMatrix1) {	

		for (int i = 0; i < numStates; i++) {
			for (int k = 0; k < sigmaSize; k++) {
				b1[i][k] = (1 - lambda) * b1[i][k] + lambda * 1.0 /sigmaSize;
			}
		}

		double lambda1 = lambda;
		for (int i = 0; i < numStates; i++) {
			pi[i] = (1 - lambda1) * pi[i] + lambda1 * 1.0 / numStates;
		}

		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				a[i][j] = (1 - lambda1) * a[i][j] + lambda1 * 1.0 /numStates;
			}
		}

		for(String feat : featureProbabilityMatrix.keySet()){
			Double[][] vals = featureProbabilityMatrix.get(feat);
			int featLength = vals[0].length;

			for(int i =0; i<numStates; i++)
				for(int j=0; j< featLength; j++)
					vals[i][j] = (1-lambda)*vals[i][j] + lambda*1.0/featLength;

			featureProbabilityMatrix.put(feat, vals);
		}

	}


	public void HardEMByBeamSearch(ArrayList<int[]> list, ArrayList<int[]> meds,
			ArrayList<int[]> nearbyMeds,
			ArrayList<int[]> pos, ArrayList<int []> reasons,
			ArrayList<int []> sections,
			ArrayList<HashMap<String, int[]>> features,
			int steps,TaggingDict td,Vector<Constraint> activeConstraints) {
		System.out.println("Start training multiple observations by Hard EM Algorithm By BeamSearch " + 1 + "  Best...");
		System.out.println("#data: " + (new Integer(list.size())).toString());
		double p_avg = 1.0 - Parameters.sup_model_weight;

		HMMLearnerConstraintsAsFeatures init_m = makeCopy(this,td,activeConstraints);

		for (int s = 0; s < steps; s++) {
			System.out.println("step " +s);
			System.out.flush();

			ArrayList<int[]> new_tag_list=new ArrayList<int[]>();
			for (int m = 0; m < list.size(); m++){
				HashMap<String, int[]> currentFeatures = null;
				if(features.isEmpty())
					currentFeatures = new HashMap<String, int[]>();
					else
						currentFeatures = features.get(m);

				new_tag_list.add(m,makePrediction(list.get(m), td,activeConstraints, 
						meds.get(m), nearbyMeds.get(m), pos.get(m), reasons.get(m), 
						sections.get(m), currentFeatures));
			}

			HMMLearnerConstraintsAsFeatures newHmm=new HMMLearnerConstraintsAsFeatures(td,
					activeConstraints, this.useFeatures);

			Data newData=new Data(list, new_tag_list, meds, nearbyMeds, pos, 
					reasons, sections, features);
			newHmm.trainFromLabeledData(newData,td,activeConstraints);

			for (int j = 0; j < numStates; j++)
				pi[j] =((1-p_avg)*init_m.pi[j]+p_avg*newHmm.pi[j]);

			for (int k = 0; k < numStates; k++)
				for (int j = 0; j < numStates; j++)
					a[j][k] =((1-p_avg)*init_m.a[j][k]+ p_avg*newHmm.a[j][k]);

			for (int j = 0; j < numStates; j++)
				for (int k = 0; k < sigmaSize; k++)
					b[j][k] = ((1-p_avg)*init_m.b[j][k]+p_avg*newHmm.b[j][k]);

			for(String feat : featureProbabilityMatrix.keySet()){
				Double[][] vals = featureProbabilityMatrix.get(feat);
				int featLength = vals[0].length;
				System.out.println("length of features " + featLength);

				for(int j=0; j<numStates; j++)
					for(int k =0; k< featLength; k++)
						vals[j][k] = ((1-p_avg))*init_m.featureProbabilityMatrix.get(feat)[j][k] + 
						p_avg*newHmm.featureProbabilityMatrix.get(feat)[j][k];
				featureProbabilityMatrix.put(feat, vals);
			}

			for(int k=0;k<activeConstraints.size();k++)
				violationProb[k]=((1-p_avg)*init_m.violationProb[k]+p_avg*newHmm.violationProb[k]);
		}//iterations of EM
	}


	public int[] predictSingle1(int seq[],TaggingDict td,Vector<Constraint> activeConstraints, 
			int[] meds,int[] nearbyMeds, int[] pos, int[] reasons, int[] sections,
			HashMap<String, int[]> features){
		//System.out.println("in PredictSingle(...)");
		int beamsize = Parameters.BEAMSIZE;
		BeamQueue bq = new BeamQueue(beamsize);
		for(int i=0;i<numStates;i++){
			State st=new State(0,seq,i, meds, nearbyMeds, pos, reasons, sections,
					features);
			updateScore(st,td,activeConstraints);
			bq.add(st);
		}

		int step = 0;
		State final_state = null;

		while (true){
			BeamQueue expand = new BeamQueue(beamsize * numStates);

			while(!bq.isEmpty()){
				State s = bq.remove();
				//s.print(td);

				if (s.isComplete()){
					final_state = s;
					break;
				}
				for(int i=0;i<numStates;i++)
				{
					State newSt=new State(s,i);
					updateScore(newSt,td,activeConstraints);
					expand.add(newSt);
				}
			}
			if (final_state != null){
				break;
			}
			//select best BEAMSIZE elements into next queue
			bq = new BeamQueue(beamsize);
			while(!expand.isEmpty() && bq.size() < beamsize){
				State good_state = expand.remove();
				bq.add(good_state);
			}
			step ++;
		}

		//final_state.print();
		return final_state.toArr();
	}

	private void estimateProbability(double gamma){
		//Lapalce Smoothing

		double pi_sum = 0;
		int i, j;

		for (i = 0; i < numStates; i++) {
			pi_sum += pi[i];
		}

		for (i = 0; i < numStates; i++) {
			pi[i] = divide(pi[i], pi_sum);
		}

		double a_sum[] = new double[numStates];

		for (i = 0; i < numStates; i++) {
			for (j = 0; j < numStates; j++) {
				a_sum[i] += a[i][j];
			}
		}

		for (i = 0; i < numStates; i++) {
			for (j = 0; j < numStates; j++) {
				a[i][j] = divide(a[i][j], a_sum[i]);
				//a[i][j] = divide(a[i][j] + 1, a_sum[i] + numStates);
			}
		}

		double b_sum[] = new double[numStates];

		for (i = 0; i < numStates; i++) {
			for (j = 0; j < sigmaSize; j++) {
				b_sum[i] += b[i][j];
			}
		}

		for (i = 0; i < numStates; i++) {
			for (j = 0; j < sigmaSize; j++) {
				b[i][j] = divide(b[i][j], b_sum[i]);
				//b[i][j] = divide(b[i][j] + gamma, b_sum[i] + gamma*sigmaSize);
			}
		}	

		for(String feat : featureProbabilityMatrix.keySet()){
			Double[][] vals = featureProbabilityMatrix.get(feat);
			double[] feat_sum = new double[numStates];
			int featLength = vals[0].length;

			for(i=0; i<numStates; i++){
				for(j=0; j< featLength ; j++)
					feat_sum[i] += vals[i][j];
			}

			for(i=0; i< numStates; i++)
				for(j=0; j< featLength; j++)
					vals[i][j] = divide(vals[i][j], feat_sum[i]);

			featureProbabilityMatrix.put(feat, vals);
		}


		smoothHMM(gamma,pi,a,b, featureProbabilityMatrix);
	}

	public void updateScore(State st,TaggingDict td,Vector<Constraint> activeConstraints){

		// get the current emission probability from all the features
		double featProbs = 0.;

		// check whether we should also include the features into the prob model
		if(useFeatures)
			for(String feat : featureProbabilityMatrix.keySet()){
				featProbs += Math.log(featureProbabilityMatrix.get(feat)[st.state][st.features.get(feat)[st.idx]]);
			}

		if (st.idx == 0)
			st.score = Math.log(pi[st.state]) + Math.log(b[st.state][st.wordseq[st.idx]]) + featProbs;
		else
			st.score += Math.log(a[st.prev.state][st.state]) + 
			Math.log(b[st.state][st.wordseq[st.idx]]) + featProbs;


		for(int k=0;k<activeConstraints.size();k++)
		{
			if(activeConstraints.elementAt(k).isViolated(st, td))
				st.score += Math.log(violationProb[k]);
			else
				st.score += Math.log(1-violationProb[k]);
		}
	}
}
