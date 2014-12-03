/**
 * Created on Jan 16, 2012
 * 
 * @author ab Contact andreeab dot mit dot edu
 * 
 */
package corpus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import classifier.TrainTestCreator.CorpusType;

import corpus.Annotation.AnnotationType;
import corpus.Relation.RelationType;

import preprocess.parser.SentenceContent;
import preprocess.parser.WordTagMap;

import utils.UtilMethods;

/**
 * @author ab
 * 
 */
public class AnnotationDetail {
	public int startLine;
	public int endLine;
	public int startOffset;
	public int endOffset;

	public int documentOffsetStart;
	public int documentOffsetEnd;

	public int offsetDoc;

	public String content;
	public Boolean isList;

	public final String defaultContent = "nm";
	final String separator = ":";
	public ArrayList<String> semanticTypes;
	public AnnotationType type;

	public HashMap<RelationType, ArrayList<AnnotationDetail>> relatedTo;


	/**
	 * the sentence from which the annotation was taken
	 */
	public SentenceContent sentence;
	public String file;

	public AnnotationDetail(){
		relatedTo = new HashMap<RelationType, ArrayList<AnnotationDetail>>();
		semanticTypes = new ArrayList<String>();
		content = "";
	}

	public AnnotationDetail(AnnotationType cType){
		relatedTo = new HashMap<RelationType, ArrayList<AnnotationDetail>>();
		semanticTypes = new ArrayList<String>();
		content = "";
		this.type = cType;
		this.content = defaultContent;
	}

	public AnnotationDetail(String el, AnnotationType cType) {
		if(el == null)
			return;

		String token = el.substring(el.indexOf(Annotation.quotes)+1, 
				el.lastIndexOf(Annotation.quotes));
		String offsets = el.substring(el.lastIndexOf(token+"\"") + 
				token.length()+1).trim();

		semanticTypes = new ArrayList<String>();
		relatedTo = new HashMap<RelationType, ArrayList<AnnotationDetail>>();

		this.type = cType;
		this.content = token;

		if (offsets != null)
			parseOffsets(offsets);
		if(this.content == null)
			this.content = this.defaultContent;

		isList = false;
	}

	public AnnotationDetail(AnnotationDetail annt) {
		relatedTo =  annt.relatedTo;
		this.semanticTypes = annt.semanticTypes;
		this.startLine = annt.startLine;
		this.endLine = annt.endLine;
		this.startOffset = annt.startOffset;
		this.endOffset = annt.endOffset;

		this.isList = annt.isList;
		this.sentence = annt.sentence;
		this.type = annt.type;
	}

	public AnnotationDetail(WordTagMap word) {
		this.content = word.word;
		this.startLine = word.sentenceNumber ;
		this.endLine = word.sentenceNumber ;
		this.startOffset = word.sentenceOffset;
		this.endOffset = word.sentenceOffset;
		this.semanticTypes = word.semanticType;
		this.documentOffsetEnd = word.documentWordOffset;
		this.documentOffsetStart = word.documentWordOffset;	
		
		if(word.annt != null)
			this.isList = word.annt.isList;
	}
	
	public AnnotationDetail(WordTagMap word, MedicalRecord record){
		String offsets = record.documentOffsetAtWordCount.get(word.documentWordOffset-1);
		
		if(offsets == null)
			return;
		
		this.content = word.word;
		this.startLine = Integer.valueOf(offsets.split("_")[0]);
		this.endLine =  Integer.valueOf(offsets.split("_")[0]);;
		this.startOffset =  Integer.valueOf(offsets.split("_")[1]);;
		this.endOffset =  Integer.valueOf(offsets.split("_")[1]);;
		this.semanticTypes = word.semanticType;
		this.documentOffsetEnd = word.documentWordOffset;
		this.documentOffsetStart = word.documentWordOffset;	
		
		if(word.annt != null)
			this.isList = word.annt.isList;
	}

	void parseOffsets(String offsets) {
		if(offsets == null || offsets.isEmpty())
			return;

		offsets = offsets.trim();
		String beginning = offsets.substring(0, offsets.indexOf(" "));
		String end = offsets.substring(offsets.indexOf(" ")).trim();

		try {
			this.startLine = Integer.parseInt(beginning.substring(0, beginning
					.indexOf(separator)));
			this.startOffset = Integer.parseInt(beginning.substring(beginning
					.indexOf(separator) + 1));

			this.endLine = Integer.parseInt(end.substring(0, end
					.indexOf(separator)));
			this.endOffset = Integer.parseInt(end.substring(end
					.indexOf(separator) + 1));
		}
		catch (Exception e) {
			this.startLine = 0;
			this.endLine = 0;
			this.startOffset = 0;
			this.endOffset = 0;
			this.content = this.defaultContent;
			System.out.println("Could not parse : " + offsets);
		}

	}

	/**
	 * Print current annotation detail
	 * @return
	 */
	public String printString() {
		String mergedInfo = type.toString() + "=\"" + this.content + "\" ";

		mergedInfo += String.valueOf(startLine) + ":"
				+ String.valueOf(startOffset);
		mergedInfo += " " + String.valueOf(endLine) + ":"
				+ String.valueOf(endOffset);

		return mergedInfo;
	}

	/**
	 * check whether two annotation details are equal
	 * @param abb
	 * @return
	 */
	public boolean equalsAnnotation(AnnotationDetail abb) {
		if(abb == null || this.content == null || abb.content == null)
			return false;

		if ((this.content.equals(abb.content) || 
				UtilMethods.removePunctuation(content).equals(
						UtilMethods.removePunctuation(abb.content))) && 
						this.startLine == abb.startLine
						&& this.endLine == abb.endLine
						&& this.startOffset == abb.startOffset
						&& this.endOffset == abb.endOffset)
			return true;

		return false;

	}

	/**
	 * Check whether two annotation details equal partially. This excludes 
	 * total equality
	 * @param abb
	 * @return
	 */
	public boolean overlapAnnotation(AnnotationDetail abb) {

		try{
		if (this.startLine == abb.startLine
				&& this.endLine == abb.endLine
				&& ((this.startOffset <= abb.startOffset && abb.startOffset <= this.endOffset)
						|| (this.startOffset <= abb.endOffset && abb.endOffset <= this.endOffset)
						|| (abb.startOffset <= this.startOffset && this.startOffset <= abb.endOffset) || (abb.startOffset <= this.endOffset && this.endOffset <= abb.endOffset)))
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;

	}

	/**
	 * Check whether two annotation details located on two lines are overlapping
	 * @param abb
	 * @return
	 */
	public boolean overlapDiffLines(AnnotationDetail abb) {
		if (this.startLine == abb.startLine 
				&& this.endLine != abb.endLine
				&& ((this.startOffset == abb.endOffset && this.endLine == abb.endLine + 1) || 
						(abb.startOffset == this.endOffset && abb.endLine == this.endLine + 1)||
						(abb.startOffset == this.startOffset )))
			return true;

		if(this.startLine == abb.startLine && 
				this.endLine == abb.endLine &&
				this.endLine != this.startLine)
			return true;

		if (this.endLine == abb.endLine 
				&& this.startLine != abb.startLine
				&& this.endOffset == abb.endOffset )
			return true;

		return false;
	}

	/**
	 * Remove duplicates from a given list of annotation details
	 * @param medicationInformation
	 * @return 
	 */
	public static ArrayList<AnnotationDetail> removeDuplicates(
			ArrayList<AnnotationDetail> medicationInformation) {
		Iterator<AnnotationDetail> it = medicationInformation.iterator();

		while(it.hasNext()) {
			AnnotationDetail annt = it.next();

			for(AnnotationDetail newAnnt : medicationInformation) {
				if(annt.equals(newAnnt)) continue;

				if(annt.equalsAnnotation(newAnnt) ) {
					it.remove();
					break;
				}

				if(annt.overlapAnnotation(newAnnt)) {
					if((annt.endOffset-annt.startOffset) < 
							(newAnnt.endOffset -newAnnt.startOffset)){
						it.remove();
						break;
					}
				}

				if(annt.overlapDiffLines(newAnnt)) 
					if(annt.content.split(" ").length < 
							newAnnt.content.split(" ").length) {
						it.remove();
						break;
					}

			}
		}

		return medicationInformation;
	}

	public String mergeOffsets(){
		String mergedOffsets = UtilMethods.mergeStrings(String.valueOf(this.startLine),
				String.valueOf(this.startOffset), "_");

		mergedOffsets = UtilMethods.mergeStrings(mergedOffsets, 
				UtilMethods.mergeStrings(String.valueOf(this.endLine),
						String.valueOf(this.endOffset), "_"), ":");

		return mergedOffsets;
	}

	/**
	 * Get the words distance between two concepts
	 * @param annt
	 * @param fileLines
	 * @return the distance in word counts
	 */
	public int getDistance(AnnotationDetail annt, Object[] fileLines){

		if(this.endLine == annt.startLine){
			return Math.abs(this.endOffset - annt.startOffset);
		}

		int distance = 0;

		if(this.endLine < annt.startLine){
			String currentLine = ((String) fileLines[this.endLine-1]).trim();

			for(int index = this.endLine; index < annt.startLine -1; index ++ )
				distance += ((String)fileLines[index]).trim().split(" ").length;

			distance += currentLine.split(" ").length - this.endOffset ;
			distance += annt.startOffset;
		}else{
			String currentLine = ((String) fileLines[annt.endLine-1]).trim();

			for(int index = annt.endLine; index < this.startLine -1; index ++ )
				distance += ((String)fileLines[index]).trim().split(" ").length;

			distance += currentLine.split(" ").length - annt.endOffset ;
			distance += this.startOffset;
		}

		return distance;
	}

	public static String printString(AnnotationDetail detail, CorpusType corpus) {
		String returnValue = "";

		if(corpus == CorpusType.I2B2RELATION)
			returnValue += "c=";
		else
			returnValue += detail.type.toString().toString() + "=";

		returnValue += "\"" + detail.content + "\"";

		if(!detail.content.equals(detail.defaultContent)){	
			returnValue += " " + String.valueOf(detail.startLine) + 
					":" + String.valueOf(detail.startOffset);
			returnValue += " " + String.valueOf(detail.endLine) + 
					":" + String.valueOf(detail.endOffset);			
		}

		return returnValue;
	}

	public static String printString(AnnotationDetail detail) {
		String returnValue = "";

		returnValue += detail.type.toString().toString() + "=";

		returnValue += "\"" + detail.content + "\"";
		returnValue += " " + String.valueOf(detail.startLine) + 
				":" + String.valueOf(detail.startOffset);
		returnValue += " " + String.valueOf(detail.endLine) + 
				":" + String.valueOf(detail.endOffset);			

		return returnValue;
	}

}