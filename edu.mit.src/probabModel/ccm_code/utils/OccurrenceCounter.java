package probabModel.ccm_code.utils;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;



public class OccurrenceCounter {
	public Hashtable<String,Integer> counts=new Hashtable<String, Integer>();
	public int uniqueTokens=0;
	public int totalTokens=0;
	
	public void addToken(String s)
	{
		totalTokens++;
		if(counts.containsKey(s))
		{
			int i=counts.get(s).intValue();
			counts.remove(s);
			counts.put(s, i+1);
		}else{
			uniqueTokens++;
			counts.put(s, 1);
		}
	}
	
	public int getCount(String s)
	{
		if(counts.containsKey(s))
			return counts.get(s).intValue();
		return 0;
	}
	
	public Iterator<String> getTokensIterator(){
		return counts.keySet().iterator();
	}
}
