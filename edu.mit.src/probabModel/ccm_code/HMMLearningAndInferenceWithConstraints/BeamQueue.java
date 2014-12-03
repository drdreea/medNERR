package probabModel.ccm_code.HMMLearningAndInferenceWithConstraints;

import java.util.PriorityQueue;
import java.util.Comparator;;


public class BeamQueue extends PriorityQueue<State>{

	public BeamQueue(int beam){
		super(beam,
			  new Comparator<State>()
			  {
		     	public int compare(State a, State b)
		     	{
		     		if (a.score> b.score)
		     			return -1;
		     		else if (a.score < b.score)
		     			return 1;
		     		else
		     			return 0;
		     	}
			  }
		);
	}

}
