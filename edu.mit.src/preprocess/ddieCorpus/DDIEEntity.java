package preprocess.ddieCorpus;

import java.util.ArrayList;

import edu.stanford.nlp.util.StringUtils;

public class DDIEEntity {
	public String entityID;
	public String entityContent;
	public String sentenceContent;
	public Integer offsetStart;
	public Integer offsetEnd;
	public int lineCount;

	public ArrayList<DDIEEntity> iteractsWith;

	public DDIEEntity(String sentence, 
			String text, String id, String offsets,
			int sentenceCount){
		this.entityContent = text;
		this.entityID = id;
		this.sentenceContent = sentence;
		this.lineCount = sentenceCount;

		processOffsets(offsets);	
		iteractsWith = new ArrayList<DDIEEntity>();
	}

	void processOffsets(String offsets){
		try{
			offsets = offsets.replaceAll("--", "-");
			
			int start = Integer.valueOf(offsets.split("-")[0]);
			int end = Integer.valueOf(offsets.split("-")[1]);

			int wordCount = 0;

			for(int index = 0; index < sentenceContent.length(); index ++){
				if(sentenceContent.charAt(index) == ' '){
					wordCount ++;
				}

				if(index == start)
					this.offsetStart = wordCount;

				if(index == end)
					this.offsetEnd = wordCount;

				if(offsetStart != null && offsetEnd != null)
					break;
			}

		}catch(Exception e){
			e.printStackTrace();
		}

	}

	public static String printEntity(DDIEEntity entity){
		String printing = "";
		
		printing = "\""+entity.entityContent + "\" ";
		printing += String.valueOf(entity.lineCount) + ":";
		printing += String.valueOf(entity.offsetStart) ;

		printing += " " + String.valueOf(entity.lineCount) + ":";
		printing += String.valueOf(entity.offsetEnd) ;
		
		return printing;
	}
	
	public static String toString(DDIEEntity entity){
		String printing = "m=" + DDIEEntity.printEntity(entity);

		String finalPrinting = "";

		for(DDIEEntity interact : entity.iteractsWith){
			finalPrinting += printing + "||r=" + DDIEEntity.printEntity(interact) + "\n";
		}
		
		finalPrinting = StringUtils.chomp(finalPrinting);
		
		if(entity.iteractsWith.isEmpty())
			return printing;

		return finalPrinting;
	}
}
