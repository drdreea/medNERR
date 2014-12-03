package probabModel.ccm_code.HMMLearningAndInferenceWithConstraints;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import probabModel.ccm_code.utils.CharacteristicObjects;
import probabModel.ccm_code.utils.OccurrenceCounter;
import probabModel.ccm_code.Experiments.Parameters;

public class Data {

	public ArrayList<int[]> sen_list = new ArrayList<int[]>();
	public ArrayList<int[]> tags_list = new ArrayList<int[]>();
	public ArrayList<int[]> isMed_list = new ArrayList<int[]>();
	public ArrayList<int[]> nearbyMed_list = new ArrayList<int[]>();
	public ArrayList<int[]> pos_list = new ArrayList<int[]>();
	public ArrayList<int[]> reasons_list = new ArrayList<int[]>();
	public ArrayList<int[]> sections_list = new ArrayList<int[]>();

	public ArrayList<HashMap<String, int[]>> features_list = new ArrayList<HashMap<String, int[]>>();

	public Data(ArrayList<int[]> _sen_list, ArrayList<int[]> _tags_list,
			ArrayList<int[]> _isMeds_list, ArrayList<int[]> _nearbyMeds_list,
			ArrayList<int[]> _pos_list,
			ArrayList<int[]> _reasons_list,
			ArrayList<int[]> _sections_list,
			ArrayList<HashMap<String, int[]>> _features) {
		sen_list=_sen_list;
		tags_list=_tags_list;
		isMed_list = _isMeds_list;
		nearbyMed_list = _nearbyMeds_list;
		pos_list = _pos_list;
		reasons_list = _reasons_list;
		sections_list = _sections_list;
		features_list = _features;
	}	

	public Data(String file_name, TaggingDict td) {
		try {
			sen_list = ReadWordsData(file_name, td);
			tags_list = ReadTagsData(file_name, td);
			isMed_list = ReadIsMedData(file_name, td);
			nearbyMed_list = ReadNearbyMedData(file_name, td);
			pos_list = ReadPosData(file_name, td);
			reasons_list = ReadReasonData(file_name, td);
			sections_list = ReadSectionsData(file_name, td);
			features_list = ReadFeatures(file_name, td);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Data() {
		// TODO Auto-generated constructor stub
	}

	public int size(){
		return sen_list.size();
	}

	// read data (only word lines); put it in array list
	public ArrayList<int[]> ReadWordsData(String file_name, TaggingDict td) {
		ArrayList<int[]> sen_list = new ArrayList<int[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file_name));
			String line = null;

			int counter = 0;
			while ((line = br.readLine()) != null) {
				if (counter % TaggingDict.numberLines == 0) { // word line
					// System.out.println(line);
										StringTokenizer st = new StringTokenizer(line);
					String[] str_array = new String[st.countTokens()];
					int i;
					for (i = 0; i < str_array.length; i++) {
						str_array[i] = td.ConvertWord(st.nextToken());
					}

					int[] fea = td.ConvertWordSeqIdx(str_array);
					sen_list.add(fea);
				}
				counter++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return sen_list;
	}

	public ArrayList<int[]> ReadTagsData(String file_name, TaggingDict td) {
		ArrayList<int[]> sen_list = new ArrayList<int[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file_name));
			String line = null;

			int counter = 0;			
			while ((line = br.readLine()) != null) {
				if (counter % TaggingDict.numberLines == 1) { // tag line
					// System.out.println(line);					
					StringTokenizer st = new StringTokenizer(line);
					String[] str_array = new String[st.countTokens()];
					int i;
					for (i = 0; i < str_array.length; i++) {
						str_array[i] = st.nextToken();
					}

					int[] fea = td.ConvertTagSeqIdx(str_array);
					sen_list.add(fea);
				}
				counter++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sen_list;
	}

	public ArrayList<int[]> ReadIsMedData(String file_name, TaggingDict td) {
		ArrayList<int[]> sen_list = new ArrayList<int[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file_name));
			String line = null;

			int counter = 0;			
			while ((line = br.readLine()) != null) {
				if (counter % TaggingDict.numberLines == 2) { // meds line
					// System.out.println(line);					
					StringTokenizer st = new StringTokenizer(line);
					String[] str_array = new String[st.countTokens()];
					int i;
					for (i = 0; i < str_array.length; i++) {
						str_array[i] = st.nextToken();
					}

					int[] fea = td.ConvertIsMedSeqIdx(str_array);
					sen_list.add(fea);
				}
				counter++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sen_list;
	}
	
	public ArrayList<int[]> ReadNearbyMedData(String file_name, TaggingDict td) {
		ArrayList<int[]> sen_list = new ArrayList<int[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file_name));
			String line = null;

			int counter = 0;		
			while ((line = br.readLine()) != null) {
				if (counter % TaggingDict.numberLines == 3) { // meds line
					// System.out.println(line);					
					StringTokenizer st = new StringTokenizer(line);
					String[] str_array = new String[st.countTokens()];
					int i;
					for (i = 0; i < str_array.length; i++) {
						str_array[i] = st.nextToken();
					}

					int[] fea = td.ConvertNearbyMedSeqIdx(str_array);
					sen_list.add(fea);
				}
				counter++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sen_list;
	}

	public ArrayList<int[]> ReadPosData(String file_name, TaggingDict td) {
		ArrayList<int[]> sen_list = new ArrayList<int[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file_name));
			String line = null;

			int counter = 0;			
			while ((line = br.readLine()) != null) {
				if (counter % TaggingDict.numberLines == 4) { // pos line
					// System.out.println(line);					
					StringTokenizer st = new StringTokenizer(line);
					String[] str_array = new String[st.countTokens()];
					int i;
					for (i = 0; i < str_array.length; i++) {
						str_array[i] = st.nextToken();
					}

					int[] fea = td.ConvertPosSeqIdx(str_array);
					sen_list.add(fea);
				}
				counter++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sen_list;
	}
	
	public ArrayList<int[]> ReadReasonData(String file_name, TaggingDict td) {
		ArrayList<int[]> sen_list = new ArrayList<int[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file_name));
			String line = null;

			int counter = 0;			
			while ((line = br.readLine()) != null) {
				if (counter % TaggingDict.numberLines == 5) { // reasons line
					// System.out.println(line);					
					StringTokenizer st = new StringTokenizer(line);
					String[] str_array = new String[st.countTokens()];
					int i;
					for (i = 0; i < str_array.length; i++) {
						str_array[i] = st.nextToken();
					}

					int[] fea = td.ConvertReasonSeqIdx(str_array);
					sen_list.add(fea);
				}
				counter++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sen_list;
	}
	
	public ArrayList<int[]> ReadSectionsData(String file_name, TaggingDict td) {
		ArrayList<int[]> sen_list = new ArrayList<int[]>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file_name));
			String line = null;

			int counter = 0;			
			while ((line = br.readLine()) != null) {
				if (counter % TaggingDict.numberLines == 6) { // sections line
					// System.out.println(line);					
					StringTokenizer st = new StringTokenizer(line);
					String[] str_array = new String[st.countTokens()];
					int i;
					for (i = 0; i < str_array.length; i++) {
						str_array[i] = st.nextToken();
					}

					int[] fea = td.ConvertSectionSeqIdx(str_array);
					sen_list.add(fea);
				}
				counter++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sen_list;
	}

	public ArrayList<HashMap<String,int[]>> ReadFeatures(String file_name, TaggingDict td) {
		ArrayList<HashMap<String, int[]>> features_list = 
				new ArrayList<HashMap<String, int[]>>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(file_name));
			String line = null;

			int counter = 0;
			HashMap<String, int[]> features = new HashMap<String, int[]>();
			
			while ((line = br.readLine()) != null) {
				// if the line is empty then we save the set of features per instance
				if(line.trim().isEmpty() ){
					if(! features.isEmpty()){
						features_list.add(features);
						features = new HashMap<String, int[]>();
					}
					
					counter ++;
					continue;
				}
				
				if (counter % TaggingDict.numberLines >= TaggingDict.attributesCount) { // feature lines
					// System.out.println(line);
					StringTokenizer st = new StringTokenizer(line);
					String[] str_array = new String[st.countTokens()];
					int i;
					for (i = 0; i < str_array.length; i++) {
						str_array[i] = st.nextToken();
					}

					String featureName = String.valueOf(counter% TaggingDict.numberLines);
					int[] fea = td.ConvertFeatureSeqIdx(str_array, featureName);
					features.put(featureName, fea);
				}
				counter++;
			}

			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return features_list;
	}

	public static Hashtable<Integer,Integer> getMajorityTagger(Data data,int numberOfMostFrequentWords){
		OccurrenceCounter wordCounts=new OccurrenceCounter();
		Hashtable<Integer,OccurrenceCounter> tagCountsPerWord= new Hashtable<Integer, OccurrenceCounter>(); 
		for(int i=0;i<data.sen_list.size();i++){
			for(int j=0;j<data.sen_list.get(i).length;j++){
				int fid=data.sen_list.get(i)[j];
				int tag=data.tags_list.get(i)[j];
				wordCounts.addToken(String.valueOf(fid));
				if(!tagCountsPerWord.containsKey(fid)){
					OccurrenceCounter oc=new OccurrenceCounter();
					oc.addToken(String.valueOf(tag));
					tagCountsPerWord.put(fid,oc);
				}
				else{
					tagCountsPerWord.get(fid).addToken(String.valueOf(tag));
				}
			}				
		}
		CharacteristicObjects top=new CharacteristicObjects(numberOfMostFrequentWords);
		for(Iterator<String> iter=wordCounts.getTokensIterator();iter.hasNext();){
			String s=iter.next();
			top.addElement(Integer.parseInt(s), wordCounts.getCount(s));
		}
		Hashtable<Integer, Integer> res=new Hashtable<Integer, Integer>();
		for(int i=0;i<top.topObjects.size();i++){
			Integer fid=(Integer)top.topObjects.elementAt(i);
			OccurrenceCounter distrib=tagCountsPerWord.get(fid);
			int maxTag=-1;
			int maxScore=-1;
			for(Iterator<String> iter=distrib.getTokensIterator();iter.hasNext();){
				int tagId=Integer.parseInt(iter.next());
				int score=distrib.getCount(String.valueOf(tagId));
				if(score>maxScore){
					maxScore=score;
					maxTag=tagId;
				}
			}
			res.put(fid,maxTag);
		}
		return res;
	}

	public static void printMajorityTagger(Hashtable<Integer, Integer> majTagg,TaggingDict td){
		for(Iterator<Integer> iter=majTagg.keySet().iterator();iter.hasNext();){
			int fid=iter.next();
			int tag=majTagg.get(fid);
			System.out.println(td.IdxToWord(fid)+" : "+td.IdxToTag(tag));
		}
	}

	public static void printCommonWords(Hashtable<Integer, Integer> majTagg,TaggingDict td){
		for(Iterator<Integer> iter=majTagg.keySet().iterator();iter.hasNext();){
			int fid=iter.next();
			int tag=majTagg.get(fid);
			System.out.print("\""+td.IdxToWord(fid)+"\",");
		}
		System.out.println();
	}

	public static void main(String[] args){
		Vector<String> files=new Vector<String>();
		files.addElement("data/POS/00-01.txt");
		TaggingDict td=new TaggingDict(files);
		Data data=new Data("data/POS/00-01.txt",td);
		Hashtable<Integer,Integer> tagger=Data.getMajorityTagger(data,1000);
		printCommonWords(tagger, td);
		Data.printMajorityTagger(tagger, td);
	}
}
