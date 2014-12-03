package preprocess.parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import corpus.AnnotationDetail;

import classifier.vector.VectorCreator.TagType;

import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.StringUtils;

import utils.UtilMethods;

/**
 * Store the content of a sentence as returned by the stanford parser
 * @author ab
 *
 */
public class SentenceContent {
	public String sentence;
	public String parse;
	public Tree tree;
	public int lineStartOffset;
	public int lineEndOffset;
	public int sentenceOrder;

	public int documentWordStart;
	public int documentWordEnd;
	public ArrayList<WordTagMap> tags;

	public static final List<String> stopWords = Arrays.asList(".", "?", "!", ";");

	public enum Order{UNIQUE, CONSECUTIVE};

	public final List<String> causalConjunctions = 
			Arrays.asList("for", "because", "because of", "if", "as", "since");

	void init(){
		tags = new ArrayList<WordTagMap>();

		lineStartOffset = -1;
		lineEndOffset = -1;
	}

	public SentenceContent(){
		init();

		sentence = "";
		sentenceOrder = -1;
		lineStartOffset = 0;
		documentWordStart = 0;
	}

	public SentenceContent(int offset){
		init();

		sentenceOrder = offset;
	}
	
	public SentenceContent(String text){
		init();

		sentence = text;
	}

	/**
	 * Read the parsed tree from file
	 * @param content
	 */
	public void parseContent(String content){
		this.parse = content;
		content = content.substring(0, content.lastIndexOf("))") +2);

		PennTreeReader reader = new PennTreeReader(new StringReader(content),
				new LabeledScoredTreeFactory());		
		try {
			this.tree = reader.readTree();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parse the given sentence and store it as a string
	 * @param content
	 */
	public void parseSentence(String content){
		String[] splitValue = content.split(", ");
		String merged = "";

		for(int index = 0 ; index < splitValue.length; index ++){
			// we have to replace right and left parantheses
			// as those are stored in the sentence with a special encoding
			String word = splitValue[index];

			if(word.contains("-RRB-"))
				word = word.replaceAll("-RRB-", ")");
			if(word.contains("-LRB"))
				word = word.replaceAll("-LRB-", "(");
			if(word.contains("\\/"))
				word = word.replace("\\/", "/");
			if(word.contains("\\*"))
				word = word.replace("\\*", "*");

			merged  = UtilMethods.mergeStrings(merged, word);
		}

		merged = merged.substring(1, merged.length()-1);

		this.sentence = merged;
	}

	/**
	 * Parse the given tags
	 * @param contentValue
	 */
	public void parseTag(String contentValue) {
		String[] splitValue = contentValue.split("\n");

		for(int index =0; index < splitValue.length; index ++){
			if(splitValue[index].trim().isEmpty()) continue;

			String[] map = splitValue[index].trim().split("\\|\\|");

			if(map.length == 2){
				String word = map[0];

				if(word.contains("-RRB-"))
					word = word.replaceAll("-RRB-", ")");
				if(word.contains("-LRB"))
					word = word.replaceAll("-LRB-", "(");
				if(word.contains("\\/"))
					word = word.replace("\\/", "/");
				if(word.contains("\\*"))
					word = word.replace("\\*", "*");

				//					tags.add(new WordTagMap(word, map[1]));
			}
		}

	}


	/**
	 * Do some content cleaning
	 * @param content
	 * @return
	 */
	static String cleanContent(String content){
		if(content.contains(" %"))
			content = content.replace(" %", "%");

		if(content.contains(" '"))
			content = content.replace(" '", "'");

		if(content.contains("\\* "))
			content = content.replace("\\* ", "\\*");

		if(content.contains(" ?"))
			content = content.replace(" ?", "?");

		if(content.contains("/"))
			content = content.replace("/", " ");

		content = UtilMethods.adjustConcept(content);

		content = UtilMethods.removePunctuation(
				content.toLowerCase());


		return content;
	}

	static boolean containsConcept(String sentence, String concept){
		if(concept.split(" ").length == 1){
			String[] splitSentence = sentence.split(" ");

			for(int index = 0; index < splitSentence.length; index ++){
				if(concept.equals(splitSentence[index]))
					return true;
			}
		}else{
			if(sentence.contains(concept))
				return true;
		}


		return false;
	}

	/**
	 * Check whether the given sentence contains a causal 
	 * conjunction between the medication and the reason
	 * @param sentence
	 * @return
	 */
	public boolean containsCausalConjunction(AnnotationDetail medication, 
			AnnotationDetail reason){

		if(!sentence.contains(medication.content) 
				|| ! sentence.contains(reason.content))
			return false;

		int indexMed = medication.offsetDoc - sentenceOrder;
		int indexReason = reason.offsetDoc - sentenceOrder;

		int start = 0;
		int end = 0 ;

		if(indexMed > indexReason){
			start = indexReason;
			end = indexMed;
		}else{
			start = indexMed;
			end = indexReason;
		}

		String[] splitContext = UtilMethods.sentenceProcessTagger(sentence).split(" ");

		boolean foundConjunction = false;

		if(start > splitContext.length){
			System.out.println("greater index");

			start = sentence.indexOf(medication.content);
			end = sentence.indexOf(reason.content);

			if(start > end){
				int tmp = start;
				start = end;
				end = tmp;
			}

			String[] context = sentence.substring(start, end).split(" ");

			for(int index=0; index< context.length; index++){
				if(this.causalConjunctions.contains(context[index])){
					foundConjunction = true;
					break;
				}
			}
		}else
			if(start < 0) start = 0;

		for(int index= start; index< end && index < splitContext.length; index ++){
			if(this.causalConjunctions.contains(splitContext[index])){
				foundConjunction =  true;
				break;
			}	
		}


		return foundConjunction;
	}

	/**
	 * 
	 * @param medication
	 * @param reason
	 * @param fileLines
	 * 
	 * @return the label of the common parent
	 */
	public String getCommonParentTreeNode(AnnotationDetail medication,
			AnnotationDetail reason, Object[] fileLines) {

		if(reason.content.equals("broke") && medication.content.equals("procainamide"))
			System.out.print("here");

		ArrayList<Tree> conceptTrees = getTreesPerConcept(medication, reason, fileLines);

		// find the relationship between the two concept trees
		if(conceptTrees.size() == 2){
			Tree parent = findParentTree(conceptTrees.get(0), conceptTrees.get(1));
			if(parent == null) 
				return null;

			String label = parent.value();
			return label;
		}

		return null;
	}

	/**
	 * 
	 * @param medication
	 * @param reason
	 * @param fileLines
	 * @return
	 */
	ArrayList<Tree> getTreesPerConcept(AnnotationDetail medication, 
			AnnotationDetail reason, Object[] fileLines){
		ArrayList<AnnotationDetail>  concepts = new ArrayList<AnnotationDetail>();
		concepts.add(reason);
		concepts.add(medication);

		ArrayList<Integer> conceptOffsets = getConceptOffsetsInSentence(concepts, 
				Order.UNIQUE, fileLines);

		Iterator<Tree> it = tree.iterator();

		int count = -1;

		// find the trees in which our concepts are stored
		ArrayList<Tree> conceptTrees = new ArrayList<Tree>();

		while(it.hasNext()){
			Tree node = it.next();

			if(node.isLeaf()){
				count ++;

				// check if the current tree node is one of our concepts
				if (conceptOffsets.contains(count)){
					conceptTrees.add(node);
				}
			}
		}

		return conceptTrees;
	}

	/**
	 * 
	 * @param firstTree
	 * @param secondTree
	 * @return the parent tree
	 */
	public Tree findParentTree(Tree firstTree, Tree secondTree) {
		// we want to first find the common parent
		if (firstTree.depth() > secondTree.depth()){
			Tree tmp = firstTree;
			firstTree = secondTree;
			secondTree = tmp;
		}

		Tree commonParent = null;

		boolean found = false;

		while(!found && firstTree != null){
			Tree deepestParent = secondTree.parent(tree);
			commonParent = firstTree.parent(tree);

			while(!found && deepestParent != null){
				if(deepestParent.equals(commonParent) && deepestParent.parent(tree).equals(commonParent.parent(tree))) 
					found = true;

				deepestParent = deepestParent.parent(tree);
			}

			firstTree = firstTree.parent(tree);
		}

		return commonParent;

	}

	/**
	 * Get the offsets of the given concepts
	 * @param concepts
	 * @param conceptOrder
	 * @param fileLines
	 * 
	 * @return offsets
	 */
	public ArrayList<Integer> getConceptOffsetsInSentence(
			ArrayList<AnnotationDetail> concepts, Order conceptOrder, Object[] fileLines) {

		String sentenceContent = this.sentence;

		ArrayList<String> conceptContents = new ArrayList<String>();
		ArrayList<Integer> conceptLengths = new ArrayList<Integer>();

		for(AnnotationDetail annt : concepts){
			String concept =  annt.content;
			conceptContents.add(concept.trim());
			if(concept.length() > 1)
				conceptContents.add(concept.substring(0, concept.length()-1));
			conceptLengths.add(concept.split(" ").length);
		}

		String[] splitSentence = sentenceContent.split(" ");
		ArrayList<Integer> offsets = new ArrayList<Integer>();

		for(int index1 = 0; index1 < splitSentence.length; index1 ++){
			for(int span : conceptLengths){
				String merged = "";
				int startOffset = index1;

				for(int index2 = index1; index2 < (startOffset + span) && index2 < splitSentence.length; index2 ++){
					merged = UtilMethods.mergeStrings(merged, splitSentence[index2]);

					if(conceptContents.contains(merged)){
						if(!offsets.contains(startOffset))
							offsets.add(startOffset);
						index1 = index2;

						switch(conceptOrder){
						case UNIQUE:
							break;
						case CONSECUTIVE:
							for(int dist = startOffset +1; dist <= index2; dist ++)
								if(!offsets.contains(span))
									offsets.add(span);
							break;
						}
					}
				}
			}
		}


		if(concepts.size() == 2 && offsets.size() != 2){
			int conceptsDistance = concepts.get(0).getDistance(concepts.get(1), fileLines);
			ArrayList<Integer> updatedOffset = new ArrayList<Integer>();

			for(int val : offsets){
				for(int val2 : offsets){
					if(val - val2 == conceptsDistance){
						updatedOffset.add(val);
						updatedOffset.add(val2);
						break;
					}
				}

				if(!updatedOffset.isEmpty()) 
					break;
			}

			//            if(updatedOffset.size() != 2)
			//                System.out.println("jere");

			return updatedOffset;
		}

		return offsets;
	}

	/**
	 * Check whether concept2 is dependent on concept1
	 * @param concept1
	 * @param concept2
	 * @param fileLines
	 * 
	 * @return true or false
	 */
	public boolean hasDependencyRelation(AnnotationDetail concept1,
			AnnotationDetail concept2, Object[] fileLines) {
		ArrayList<Tree> conceptTrees = getTreesPerConcept(concept1, concept2, fileLines);

		// find the relationship between the two concept trees
		if(conceptTrees.size() == 2){
			Tree parent = conceptTrees.get(0);
			Tree child = conceptTrees.get(1);

			if (parent.depth() > child.depth()){
				return false;
			}

			boolean found = false;

			while(child != null){
				if(parent.equals(child)) 
					found = true;
				child = child.parent(tree);
			}

			return found;
		}

		return false;
	}

	public static SentenceContent loadConcept(AnnotationDetail annotation,
			SentenceContent sentence,
			HashMap<String, ArrayList<String>> conceptSemanticTypes,
			Boolean isList){			 

		ArrayList<String> semanticTypes = new ArrayList<String>();
		if(conceptSemanticTypes.containsKey(annotation.content))
			semanticTypes = conceptSemanticTypes.get(annotation.content);


		for(int index = 0; index < sentence.tags.size(); index ++){
			// check if the anotation starts here
			if(annotation.documentOffsetStart == sentence.documentWordStart + index){
				int anntLength = StringUtils.split(annotation.content).size();
				
				for(int anntOffset = 0; anntOffset < anntLength; anntOffset ++){
					if(index + anntOffset == sentence.tags.size()){
						System.out.println("Incorrect split: " + annotation.content);
						break;
					}
					
					WordTagMap concept = sentence.tags.get(index + anntOffset);
					if(anntOffset == 0)
						concept.tagType = TagType.B;
					else
						concept.tagType = TagType.I;
					
					concept.annotationType = annotation.type;
					concept.annt = annotation;
					concept.semanticType = semanticTypes;
					sentence.tags.set(index + anntOffset, concept);
				}
				
				// finish the search
				break;
			}
		}

		return sentence;

	}

	public static int getSentenceForAnnotation(ArrayList<SentenceContent> sentences,
			AnnotationDetail annotation){
		int sentCount = sentences.size();

		for(int index = 0; index < sentCount; index ++){
			SentenceContent sent = sentences.get(index);

			if(sent.documentWordStart <= annotation.documentOffsetStart && 
					sent.documentWordEnd >= annotation.documentOffsetEnd)
				return index;

		}

		return -1;
	}

	public void setSentenceOffsets(int sentenceCount ) {
		this.sentenceOrder = sentenceCount;

	}
}

