package preprocess.parser;

public class ConceptOffsets {
    public int startOffset;
    public int endOffset;
    
    public String content;
    
    public ConceptOffsets(int start, int end, String content){
        this.startOffset = start;
        this.endOffset = end;
        this.content = content;
    }

}
