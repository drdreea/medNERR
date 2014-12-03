package probabModel.ccm_code.HMMLearningAndInferenceWithConstraints;
import probabModel.ccm_code.Experiments.Parameters;


public class ConstraintsViolationCounter {	
	public static int countViolations(State s, TaggingDict td){
		int violation_count=0;//no penalty
		for(int i=0;i<Parameters.constraintsToApply.size();i++)
			if(Parameters.constraintsToApply.elementAt(i).isViolated(s, td))
				violation_count++;
		return violation_count;
	}

}
