package probabModel.ccm;

import java.util.Comparator;

import probabModel.ccm.AStarInference.ViterbiNode;

class VectorNodeComparator implements Comparator<ViterbiNode>
{
	@Override
	public int compare(ViterbiNode x, ViterbiNode y)
	{
		// Assume neither string is null. Real code should
		// probably be more robust
		// we want to have the elements with the lowest score as the first elements 
		// in the queue
		if(x.f_score < y.f_score)
			return 1;
		if(x.f_score > y.f_score)
			return -1;
		
		
//		if (x.length() < y.length())
//		{
//			return -1;
//		}
//		if (x.length() > y.length())
//		{
//			return 1;
//		}
		return 0;
	}
}
