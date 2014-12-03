package probabModel.ccm_code.HMMLearningAndInferenceWithConstraints;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import probabModel.ccm_code.utils.CharacteristicObjects;
import probabModel.ccm_code.utils.InFile;


public class ExtractPrototypes {
	public static int prototypesPerClass=3;
	
	public static void get(String taggedFilename,int minAppearanceCount,double confThres){
		Hashtable<String,Integer> words=new Hashtable<String, Integer>();
		Hashtable<String,Boolean> tags=new Hashtable<String, Boolean>();
		Hashtable<String, Hashtable<String,Integer>> wordsFreq=new Hashtable<String, Hashtable<String,Integer>>();
		InFile in=new InFile(taggedFilename);
		String line=in.readLine();
		while(line!=null){
			Vector<String> tokens=InFile.tokenize(line, "\t");
			Vector<String> tokenTags=InFile.tokenize(in.readLine(), "\t");
			for(int i=0;i<tokens.size();i++)
			{
				String w=tokens.elementAt(i);
				String t=tokenTags.elementAt(i);
				if(!words.containsKey(w))
					words.put(w, 1);
				else{
					int c=words.get(w)+1;
					words.remove(w);
					words.put(w, c);
				}
				if(!tags.containsKey(t))
					tags.put(t, true);
				if(!wordsFreq.containsKey(t))
					wordsFreq.put(t, new Hashtable<String, Integer>());
				Hashtable<String, Integer> h=wordsFreq.get(t);
				if(!h.containsKey(w))	
					h.put(w, 1);
				else{
					int c=h.get(w)+1;
					h.remove(w);
					h.put(w, c);
				}
			}
			in.readLine();
			in.readLine();
			line=in.readLine();
		}
		for(Iterator<String> t=tags.keySet().iterator();t.hasNext();)
		{
			String tag=t.next();
			Hashtable<String, Integer>h=wordsFreq.get(tag);
			CharacteristicObjects topWord=new CharacteristicObjects(prototypesPerClass);
			for(Iterator<String> w=h.keySet().iterator();w.hasNext();){
				String word=w.next();
				double score=((double)h.get(word))/((double)words.get(word));
				if((score>confThres)&&(words.get(word)>=minAppearanceCount))
					topWord.addElement(word, words.get(word));
					//System.out.println("\t"+word+"\t("+score+")");
			}
			System.out.println(tag+":");
			for(int i=0;i<topWord.topObjects.size();i++){
				String w=(String)topWord.topObjects.elementAt(i);
				System.out.println("\t"+w+" (appearances:"+words.get(w)+" conf:"+((double)h.get(w))/((double)words.get(w))+")");
			}
		}
		
		//in java format
		String protoWords="String[] protoWords={";
		String protoTags="String[] protoTags={";
		String allTags="String[] allTags={";
		for(Iterator<String> t=tags.keySet().iterator();t.hasNext();)
		{
			String tag=t.next();
			allTags+="\""+tag+"\",";
			Hashtable<String, Integer>h=wordsFreq.get(tag);
			CharacteristicObjects topWord=new CharacteristicObjects(prototypesPerClass);
			for(Iterator<String> w=h.keySet().iterator();w.hasNext();){
				String word=w.next();
				double score=((double)h.get(word))/((double)words.get(word));
				if((score>confThres)&&(words.get(word)>=minAppearanceCount))
					topWord.addElement(word, words.get(word));
			}
			for(int i=0;i<topWord.topObjects.size();i++){
				String w=(String)topWord.topObjects.elementAt(i);
				protoWords+="\""+w+"\",";
				protoTags+="\""+tag+"\",";
			}
		}
		System.out.println(protoWords+"};");
		System.out.println(protoTags+"};");
		System.out.println(allTags+"};");
	}
	
	public static void main(String[] args){
		//get("data/ads_train.100.4.txt",2,0.7);
		//get("data/citation_train.300.4.txt",5,0.8);
		InFile.convertToLowerCaseByDefault=false;
		//get("data/POS/02-21.pos.txt",5,0.5);
		get("data/POS/00-01.txt",5,0.99);
	}
}
