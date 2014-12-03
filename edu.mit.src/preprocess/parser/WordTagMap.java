package preprocess.parser;

import java.util.ArrayList;

import corpus.AnnotationDetail;
import corpus.Annotation.AnnotationType;

import classifier.vector.VectorCreator.TagType;

public class WordTagMap{
    public String word;
    public String baseForm;
    public String coarseGrainedPOS;
    public String posTag;
    public String umlsSemanticType;
    
	public String namedEntity;
	public String depRel;

    public AnnotationType annotationType;
    public AnnotationDetail annt;
    
    public int sentenceNumber;
    public int sentenceOffset;
    public int documentWordOffset;
    
    public int rawFileLine;
    public int rawFileOffset;
    
    public TagType tagType;
    
    public AnnotationDetail reason;

    
    public ArrayList<String> semanticType;
	public String head;
	public boolean hasMedUMLSType;
	public boolean hasReasonUMLSType;

    public WordTagMap(String w, String t){
        word = w;
        posTag = t;
        
        tagType = TagType.O;
        annotationType = AnnotationType.O;
        
        semanticType = new ArrayList<String>();
    }


}