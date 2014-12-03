package probabModel.ccm_code.utils;

import java.util.*;



public class CharacteristicObjects {
	int maxSize;
	public Vector<Object> topObjects=new Vector<Object>();
	Vector<Double> topScores=new Vector<Double>();

	public CharacteristicObjects(int capacity)
	{
		maxSize=capacity;
	}
	public void addElement(Object o,double score){
		topObjects.addElement(o);
		topScores.addElement(score);
		if(topObjects.size()>maxSize){
			int minId=0;
			for(int i=0;i<topScores.size();i++)
				if(topScores.elementAt(minId)>topScores.elementAt(i))
					minId=i;
			topScores.removeElementAt(minId);
			topObjects.removeElementAt(minId);
		}
	}
	
	public String toString()
	{
		String res="";
		for(int i=0;i<topScores.size();i++)
			res+=(topObjects.elementAt(i).toString()+ "\t-\t"+topScores.elementAt(i)+"\n");
		return res;
	}
}
