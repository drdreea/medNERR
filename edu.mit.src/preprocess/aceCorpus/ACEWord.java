package preprocess.aceCorpus;

public class ACEWord {
	public String text;
	public String id;
	public int lineCount;
	public int wordCount;
	public String sentenceId;
	
	public ACEWord(String txt, String identifier, int line, int aWordCount,
			String sId){
		this.text = txt;
		this.id = identifier;
		this.lineCount = line;
		this.wordCount = aWordCount;
		this.sentenceId = sId;
	}
}
