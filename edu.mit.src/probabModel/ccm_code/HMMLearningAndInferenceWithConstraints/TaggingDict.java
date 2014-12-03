package probabModel.ccm_code.HMMLearningAndInferenceWithConstraints;

import java.io.*;
import java.util.*;
import probabModel.ccm_code.Experiments.Parameters;


public class TaggingDict implements Serializable{

	public  static final long serialVersionUID = 0L;
	public  Hashtable<String,Integer> word_map = new Hashtable<String,Integer> ();
	public  ArrayList <String> idx2word = new ArrayList <String>();

	public  Hashtable<String,Integer> tag_map = new Hashtable<String,Integer> ();
	public  ArrayList <String> idx2tag = new ArrayList <String>();

	public  Hashtable<String,Integer> isMed_map = new Hashtable<String,Integer> ();
	public  ArrayList <String> idx2IsMed = new ArrayList <String>();

	public  Hashtable<String,Integer> nearbyMed_map = new Hashtable<String,Integer> ();
	public  ArrayList <String> idx2NearbyMed = new ArrayList <String>();

	public  Hashtable<String,Integer> pos_map = new Hashtable<String,Integer> ();
	public  ArrayList <String> idx2pos = new ArrayList <String>();

	public  Hashtable<String,Integer> reason_map = new Hashtable<String,Integer> ();
	public  ArrayList <String> idx2reason = new ArrayList <String>();

	public  Hashtable<String,Integer> corsePOS_map = new Hashtable<String,Integer> ();
	public  ArrayList <String> idx2corsePOS = new ArrayList <String>();

	public  HashMap<String, Hashtable<String,Integer>> feature_map = 
			new HashMap<String, Hashtable<String,Integer>> ();
	public  ArrayList <String> idx2feature = new ArrayList <String>();

	public final static int attributesCount = 7;
	final static int numberLines = 2  + attributesCount + Parameters.numFeatures;

	private void writeObject(java.io.ObjectOutputStream out)
			throws IOException
			{
		out.writeObject(word_map);
		out.writeObject(idx2word);
		out.writeObject(tag_map);
		out.writeObject(idx2tag);
		out.writeObject(isMed_map);
		out.writeObject(idx2IsMed);
		out.writeObject(nearbyMed_map);
		out.writeObject(idx2NearbyMed);
		out.writeObject(pos_map);
		out.writeObject(idx2pos);
		out.writeObject(reason_map);
		out.writeObject(idx2corsePOS);
		out.writeObject(corsePOS_map);
		out.writeObject(idx2reason);
		out.writeObject(feature_map);
		out.writeObject(idx2feature);

			}

	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException
			{
		word_map =(Hashtable<String,Integer>)in.readObject();
		idx2word =(ArrayList <String>)in.readObject();
		tag_map=(Hashtable<String,Integer>)in.readObject();
		idx2tag=(ArrayList <String>)in.readObject();
		isMed_map=(Hashtable<String,Integer>)in.readObject();
		idx2IsMed=(ArrayList <String>)in.readObject();
		nearbyMed_map=(Hashtable<String,Integer>)in.readObject();
		idx2NearbyMed=(ArrayList <String>)in.readObject();
		pos_map=(Hashtable<String,Integer>)in.readObject();
		idx2pos=(ArrayList <String>)in.readObject();
		reason_map=(Hashtable<String,Integer>)in.readObject();
		idx2reason=(ArrayList <String>)in.readObject();
		corsePOS_map=(Hashtable<String,Integer>)in.readObject();
		idx2corsePOS=(ArrayList <String>)in.readObject();

		feature_map=(HashMap<String, Hashtable<String,Integer>>)in.readObject();
		idx2feature=(ArrayList <String>)in.readObject();
			}

	private void ParseSingleData(String data_file_name, ArrayList<String []> words_list, 
			ArrayList<String []> tags_list, ArrayList<String []> isMedsList, 
			ArrayList<String []> nearbyMedsList,
			ArrayList<String []> posList, ArrayList<String []> reasonList,
			ArrayList<String []> sectionsList,
			ArrayList<HashMap<String, String[]>> featuresList){
		try {
			BufferedReader br = new BufferedReader(new FileReader(data_file_name));
			String line = null;

			HashMap<String, String[]> features = new HashMap<String, String[]>();

			int counter = 0;
			while ((line = br.readLine()) != null) {
				if (counter % numberLines == 0){ // word line
					StringTokenizer st = new StringTokenizer(line);
					int i;
					String [] str_array = new String[st.countTokens()];
					for (i=0;i < str_array.length;i++){
						str_array[i] = ConvertWord(st.nextToken());
					}
					words_list.add(str_array);

				}else if (counter % numberLines == 1){ // tag line
					StringTokenizer st = new StringTokenizer(line);
					int i;
					String [] str_array = new String[st.countTokens()];
					for (i=0;i < str_array.length;i++){
						str_array[i] = st.nextToken();
					}
					tags_list.add(str_array);
				}else if(counter % numberLines == 2){ // is meds line
					StringTokenizer st = new StringTokenizer(line);
					int i;
					String [] str_array = new String[st.countTokens()];
					for (i=0;i < str_array.length;i++){
						str_array[i] = st.nextToken();
					}
					isMedsList.add(str_array);
				}else if(counter % numberLines == 3){ // nearby meds line
					StringTokenizer st = new StringTokenizer(line);
					int i;
					String [] str_array = new String[st.countTokens()];
					for (i=0;i < str_array.length;i++){
						str_array[i] = st.nextToken();
					}
					nearbyMedsList.add(str_array);
				}else if (counter % numberLines == 4){ // pos line
					StringTokenizer st = new StringTokenizer(line);
					int i;
					String [] str_array = new String[st.countTokens()];
					for (i=0;i < str_array.length;i++){
						str_array[i] = st.nextToken();
					}
					posList.add(str_array);
				}else if(counter % numberLines == 5){ // reasons line
					StringTokenizer st = new StringTokenizer(line);
					int i;
					String [] str_array = new String[st.countTokens()];
					for (i=0;i < str_array.length;i++){
						str_array[i] = st.nextToken();
					}
					reasonList.add(str_array);
				}else if(counter % numberLines == 6){ // sections line
					StringTokenizer st = new StringTokenizer(line);
					int i;
					String [] str_array = new String[st.countTokens()];
					for (i=0;i < str_array.length;i++){
						str_array[i] = st.nextToken();
					}
					sectionsList.add(str_array);
				}
				// if line is not empty, then it's a feature line
				else if(!line.trim().isEmpty()){
					StringTokenizer st = new StringTokenizer(line);
					int i;
					String [] str_array = new String[st.countTokens()];
					for (i=0;i < str_array.length;i++){
						str_array[i] = st.nextToken();
					}

					String featureName = String.valueOf(counter%numberLines);

					features.put(featureName, str_array);
				}else if(line.trim().isEmpty() && ! features.isEmpty()){
					featuresList.add(features);
					features = new HashMap<String, String[]>();
				}

				counter ++;
			}
			br.close();

		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}


	}

	private void buildWordTagMap(ArrayList<String []> words_list, 
			ArrayList<String []> tags_list, ArrayList<String []> isMeds_list,
			ArrayList<String []> nearbyMeds_list,
			ArrayList<String []> pos_list, ArrayList<String []> reason_list,
			ArrayList<String []> corsePOS_list,
			ArrayList<HashMap<String, String[]>> feature_list){
		if(words_list.size() != tags_list.size()){
			System.out.println("ERROR: in ParseData...");
			System.out.flush();
			System.exit(1);
		}

		// build word map
		// build tag map
		int i,j;

		for (i=0; i < words_list.size(); i++){
			String [] words = words_list.get(i);
			String [] tags = tags_list.get(i);
			String [] isMeds = isMeds_list.get(i);
			String [] nearbyMeds = nearbyMeds_list.get(i);
			String [] pos = pos_list.get(i);
			String [] reasons = reason_list.get(i);
			String [] corsePOSs = corsePOS_list.get(i);

			HashMap<String, String[]> features = null;
			if(feature_list.isEmpty())
				features = new HashMap<String, String[]>();
				else
					features = feature_list.get(i);

			if(words.length != tags.length){

				System.out.println("ERROR: in ParseData... when build map");
				System.out.println(i);
				System.out.println(words.length + " " + tags.length);
				System.out.flush();
				System.exit(1);
			}

			try{
				for (j=words.length -1; j >= 0 ; j--){
					String word = words[j].trim();
					String tag = tags[j].trim();
					String isMed = isMeds[j].trim();
					String nearbyMed = nearbyMeds[j].trim();
					String posEl = pos[j].trim();
					String reason = reasons[j].trim();
					String corsePOS = corsePOSs[j].trim();

					if (!word_map.containsKey(word)){
						word_map.put(word,new Integer(word_map.size()));
						idx2word.add(word);
					}

					if (!tag_map.containsKey(tag)){
						tag_map.put(tag,tag_map.size());
						idx2tag.add(tag);
					}

					if(!isMed_map.containsKey(isMed)){
						isMed_map.put(isMed, isMed_map.size());
						idx2IsMed.add(isMed);
					}

					if(!nearbyMed_map.containsKey(nearbyMed)){
						nearbyMed_map.put(nearbyMed, nearbyMed_map.size());
						idx2NearbyMed.add(nearbyMed);
					}

					if(!pos_map.containsKey(posEl)){
						pos_map.put(posEl, pos_map.size());
						idx2pos.add(posEl);
					}

					if(!reason_map.containsKey(reason)){
						reason_map.put(reason, reason_map.size());
						idx2reason.add(reason);
					}
					
					if(!corsePOS_map.containsKey(corsePOS)){
						corsePOS_map.put(corsePOS, corsePOS_map.size());
						idx2corsePOS.add(corsePOS);
					}

					for(String featureName : features.keySet()){
						String feature = features.get(featureName)[j];
						Hashtable<String, Integer> prevVals = feature_map.get(featureName); 

						if(! prevVals.containsKey(feature)){
							prevVals.put(feature, prevVals.size());

							feature_map.put(featureName, prevVals);
							idx2feature.add(feature);

						}						
					}

				}
			}catch(Exception ex){
				ex.printStackTrace();
				System.out.println();
			}
		}

		System.out.println("number of tags: " + tag_map);

	}

	public TaggingDict(Vector<String> file_name_list){
		ArrayList<String []> word_list = new ArrayList<String []>();
		ArrayList<String []> tag_list = new ArrayList<String []>();
		ArrayList<String []> isMeds_list = new ArrayList<String []>();
		ArrayList<String []> nearbyMeds_list = new ArrayList<String []>();
		ArrayList<String []> pos_list = new ArrayList<String []>();
		ArrayList<String []> reason_list = new ArrayList<String []>();
		ArrayList<String []> section_list = new ArrayList<String []>();

		ArrayList<HashMap<String, String[]>> feature_list = new ArrayList<HashMap<String, String[]>>();

		for (String file_name : file_name_list){
			this.ParseSingleData(file_name,word_list,tag_list, isMeds_list, 
					nearbyMeds_list,
					pos_list, reason_list, section_list, feature_list);
		}

		if(!feature_list.isEmpty())
			for(String featureName : feature_list.get(0).keySet())
				feature_map.put(featureName, new Hashtable<String, Integer>());

		buildWordTagMap(word_list,tag_list, isMeds_list, 
				nearbyMeds_list,
				pos_list, reason_list,section_list, feature_list);
	}

	public String ConvertWord(String in_word)
	{
		//		if(in_word.matches("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+"))
		//		return "<num>";
		//		else
		//		return in_word;

		return in_word;
	}

	public int GetWordIdx(String word)
	{
		String w = ConvertWord(word);
		Integer value = word_map.get(w);
		value.intValue();

		return value.intValue();
	}


	public String IdxToWord(int i)
	{
		return idx2word.get(i);
	}

	public String IdxToTag(int i)
	{
		return idx2tag.get(i);
	}

	public String IdxToIsMed(int i){
		return idx2IsMed.get(i);
	}

	public String IdxToNearbyMed(int i){
		return idx2NearbyMed.get(i);
	}

	public String IdxToPos(int i){
		return idx2pos.get(i);
	}

	public String IdxToReason(int i){
		return idx2reason.get(i);
	}
	
	public String IdxToCorsePOS(int i){
		return idx2corsePOS.get(i);
	}


	public int GetTagIdx(String word)
	{
		Integer value = tag_map.get(word);
		return value.intValue();
	}
	
	public boolean ContainsTagIdx(String word)
	{
		Integer value = tag_map.get(word);
		if(value == null)
			return false;
		
		return true;
	}

	public int GetIsMedIdx(String word)
	{
		Integer value = isMed_map.get(word);

		value.intValue();

		return value.intValue();
	}


	public int GetNearbyMedIdx(String word)
	{
		Integer value = nearbyMed_map.get(word);
		return value.intValue();
	}

	public int GetPosIdx(String word)
	{
		Integer value = pos_map.get(word);
		return value.intValue();
	}

	public int GetReasonIdx(String word)
	{
		Integer value = reason_map.get(word);
		return value.intValue();
	}
	
	public int GetSectionIdx(String word)
	{
		Integer value = corsePOS_map.get(word);
		return value.intValue();
	}

	public int GeatFeatureLength(String featureName){
		return feature_map.get(featureName).size();
	}

	public int GetFeatureIdx(String word, String featureName)
	{
		Integer value = feature_map.get(featureName).get(word);
		return value.intValue();
	}

	public int GetWordNum()
	{
		return word_map.size();
	}

	public int GetTagNum()
	{
		return tag_map.size();
	}

	public int[] ConvertWordSeqIdx(String [] str_list)
	{
		int [] fea = new int[str_list.length];
		int i;
		for (i=0; i<str_list.length; i++){
			fea[i] = GetWordIdx(str_list[i]);
		}
		return fea;
	}

	public int[] ConvertTagSeqIdx(String [] str_list)
	{
		int [] fea = new int[str_list.length];
		int i;
		for (i=0; i<str_list.length; i++){
			fea[i] = GetTagIdx(str_list[i]);
		}
		return fea;
	}

	public int[] ConvertIsMedSeqIdx(String [] str_list)
	{
		int [] fea = new int[str_list.length];
		int i;
		for (i=0; i<str_list.length; i++){
			fea[i] = GetIsMedIdx(str_list[i]);
		}
		return fea;
	}

	public int[] ConvertNearbyMedSeqIdx(String [] str_list)
	{
		int [] fea = new int[str_list.length];
		int i;
		for (i=0; i<str_list.length; i++){
			fea[i] = GetNearbyMedIdx(str_list[i]);
		}
		return fea;
	}

	public int[] ConvertFeatureSeqIdx(String[] str_array, String featureName) {
		int [] fea = new int[str_array.length];
		int i;
		for (i=0; i<str_array.length; i++){
			fea[i] = GetFeatureIdx(str_array[i], featureName);
		}
		return fea;
	}

	public int[] ConvertPosSeqIdx(String [] str_list)
	{
		int [] fea = new int[str_list.length];
		int i;
		for (i=0; i<str_list.length; i++){
			fea[i] = GetPosIdx(str_list[i]);
		}
		return fea;
	}

	public int[] ConvertReasonSeqIdx(String [] str_list)
	{
		int [] fea = new int[str_list.length];
		int i;
		for (i=0; i<str_list.length; i++){
			fea[i] = GetReasonIdx(str_list[i]);
		}
		return fea;
	}
	
	public int[] ConvertSectionSeqIdx(String [] str_list)
	{
		int [] fea = new int[str_list.length];
		int i;
		for (i=0; i<str_list.length; i++){
			fea[i] = GetSectionIdx(str_list[i]);
		}
		return fea;
	}

	public String seqToString(int[] seq)
	{
		String res="";
		for(int i=0;i<seq.length;i++)
			res+=(IdxToTag(seq[i])+" ");
		return res;
	}

	public String obsToString(int[] obs)
	{
		String res="";
		for(int i=0;i<obs.length;i++)
			res+=(IdxToWord(obs[i])+" ");
		return res;
	}


}

