package classifier.attributes;

import java.util.ArrayList;

import corpus.AnnotationDetail;

import preprocess.parser.SentenceContent;
import utils.UtilMethods;
import classifier.ClassifierBase;
import classifier.vector.VectorCreator.TagType;

public class ConceptAttributes extends AttributeGenerator{ 

    public ConceptAttributes(ClassifierBase classifier){
        super(classifier);
    }

    
    public ArrayList<Object> getConceptAttributes(
            AnnotationDetail wordAnnotation, String file,
            TagType tagType, SentenceContent sentence, int wordIndex, 
            ArrayList<SentenceContent> sentences, int sentenceIndex) {
        ArrayList<Object> attributes = new ArrayList<Object>();

        // add the semantic type attributs of the current, bigram and trigram
//        attributes.addAll(getSemanticTypes(wordAnnotation, wordIndex,
//                tagType, sentence));
//
//        // add the current token normalized. the 2 previous and next words
//        attributes.addAll(getWords(sentence, wordIndex));

        // add the current bigram and trigram, and the prev bigram and trigram
//        attributes.addAll(getNgrams(sentence, wordIndex));

        // add the isNum
//        attributes.addAll(checkNumbers(sentences, sentenceIndex, sentence, wordIndex));

        // add the isVerb
//        attributes.addAll(isVerb(sentences, sentenceIndex, 
//        		sentence, wordIndex));

        // add the isPossessive
//        attributes.addAll(isPossessive(sentences, sentenceIndex, 
//        		sentence, wordIndex));

        // add the special noun
        String specialNoun = checkSpecialNoun(wordAnnotation.content);
        if(specialNoun != null)
            attributes.add(specialNoun);

        // check if it's an adjective
//        boolean isAdjective = checkAdj(sentence, wordIndex);
//        if(isAdjective)
//            attributes.add("adjective");

        // check the word form in wordNet
//        ArrayList<String> wordNetInfo = checkWordNetFrames(sentence, wordIndex);
//        if(wordNetInfo!=null)
//            attributes.addAll(wordNetInfo);
        //        
        // check if the annotation equals prn
        if(UtilMethods.removePunctuation(wordAnnotation.content).equals("prn"))
            attributes.add("isPRN");

        // add the isQuantity
        //        boolean isQuantity = isQuantity(sentence, wordIndex);
        //        if(isQuantity)
        //            attributes.add("bigramQuantity");

        // ends with :
//        String sectionEnding = checkSectionEnding(sentence, wordIndex);
//        if(sectionEnding != null)
//            attributes.add(sectionEnding);

        return attributes;

    }

    /**
     * Check in wordNet what are the usual frames for the given 
     * sentence word
     * 
     * @param sentence
     * @param wordIndex
     * 
     * @return a list of the frames
     */
//    private ArrayList<String> checkWordNetFrames(SentenceContent sentence,
//            int wordIndex) {
//        WordTagMap word = sentence.tags.get(wordIndex);
//
//        HashSet<String> frames = wordNetProcessor.getSentenceFrames(word.word, word.tag );
//        if(frames != null){
//            ArrayList<String> framesList = new ArrayList<String>();
//            for(String el : frames){
//                el = el.replaceAll(" ", "");
//                framesList.add(el);
//            }
//
//            return framesList;
//        }
////        else
////            System.out.println("Empty frame for " + word.word);
//
//        return null;
//    }
//
//    private boolean checkAdj(SentenceContent sentence, int wordIndex) {
//        String tag = sentence.tags.get(wordIndex).tag;
//        return Parsing.adj.contains(tag);
//
//    }
//
//    /**
//     * Check if the next word from wordIndex is a semicolon
//     * @param sentence
//     * @param wordIndex
//     * @return
//     */
//    private String checkSectionEnding(SentenceContent sentence, int wordIndex) {
//        if(sentence.tags.size() > wordIndex +1){
//            String word = sentence.tags.get(wordIndex +1).word;
//            if(word.equals(":"))
//                return "sectionEnd";
//        }
//        return null;
//    }
//
//    /**
//     * check if the previous bigram is a quantity
//     * @param sentence
//     * @param wordIndex
//     * @return
//     */
//    boolean isQuantity(SentenceContent sentence, int wordIndex){
//        if(wordIndex -2 > 0){
//            // check if word -2 is a number
//            String word = sentence.tags.get(wordIndex -2).word;
//
//            if(UtilMethods.isNumber(word)){
//
//                // check if word -1 is mg
//                word = sentence.tags.get(wordIndex -2).word;
//                if(word.toLowerCase().equals("mg"))
//                    return true;
//            }else{
//                word = sentence.tags.get(wordIndex -2).word;
//                if(word.toLowerCase().endsWith("mg"))
//                    return true;
//            }
//        }
//
//        return false;
//    }
//
//    /**
//     * include the lemmatized form of the words, if available
//     * 
//     * @param sentence
//     * @param wordIndex
//     * @return
//     */
//    private ArrayList<String> getWords(SentenceContent sentence,
//            int wordIndex) {
//        ArrayList<String> attributes = new ArrayList<String>();
//        String word = null;
//        
//        WordTagMap wordMap = getWord(sentence, wordIndex);
//        if(wordMap != null)
//        	word = wordMap.word;
//        
//        String lemma = wordNetProcessor.getLemma(word, sentence.tags.get(wordIndex).tag);
//        if( lemma != null)
//            word = lemma;
//
//        if(word != null)
//            attributes.add("W=" + word);
//
//        // add the previous words
//        String prev = null;
//        wordMap = getWord(sentence, wordIndex-1);
//        if(wordMap !=null) prev = wordMap.word;
//        
//        if(prev != null){
//            lemma = wordNetProcessor.getLemma(prev, 
//                    sentence.tags.get(wordIndex-1).tag);
//            if( lemma != null)
//                prev = lemma;
//            attributes.add("W=" + prev + "@-1");
//        }
//
//        wordMap = getWord(sentence, wordIndex - 2);
//        if(wordMap != null) prev = wordMap.word;
//        
//        if(prev != null){
//            lemma = wordNetProcessor.getLemma(prev, 
//                    sentence.tags.get(wordIndex-2).tag);
//            if( lemma != null)
//                prev = lemma;
//            attributes.add("W=" + prev + "@-2");
//        }
//
//        // add the next words
//        String next = null;
//        wordMap = getWord(sentence, wordIndex +1);
//        if(wordMap != null) next = wordMap.word;
//        
//        if(next != null){
//            lemma = wordNetProcessor.getLemma(next, 
//                    sentence.tags.get(wordIndex +1).tag);
//            if( lemma != null)
//                next = lemma;
//            attributes.add("W=" + next + "@1");
//        }
//
//        wordMap = getWord(sentence, wordIndex +2);
//        if(wordMap != null) next = wordMap.word;
//        
//        if(next != null){
//            lemma = wordNetProcessor.getLemma(next, 
//                    sentence.tags.get(wordIndex+2).tag);
//            if( lemma != null)
//                next = lemma;
//            attributes.add("W=" + next + "@2");
//        }
//
//        return attributes;
//    }
//
//    ArrayList<String> getSemanticTypes(AnnotationDetail wordAnnotation,
//            int wordIndex,
//            TagType tagType, SentenceContent sentence){
//        ArrayList<String> attributes = new ArrayList<String>();
//
//        if(wordAnnotation.semanticTypes != null && 
//                !wordAnnotation.semanticTypes.isEmpty()){
//            for(String type : wordAnnotation.semanticTypes){
//                attributes.add("sem." + type );  
//            }
//        }else if(classifier.conceptSemanticTypes.containsKey(wordAnnotation.content)){
//            for(String type :classifier.conceptSemanticTypes.get(wordAnnotation.content)){
//                attributes.add("sem." + type );  
//            }
//        }
//
//        // check the previous word
//        if(wordIndex >= 1 ){
//            ArrayList<String> types = sentence.tags.get(wordIndex-1).semanticType;
//
//            if(types != null && !types.isEmpty()){
//                for(String type : wordAnnotation.semanticTypes){
//                    attributes.add("sem." + type + "@-1");  
//                }
//            }
//        }
//
//        if(wordIndex >= 2 ){
//            ArrayList<String> types = sentence.tags.get(wordIndex-2).semanticType;
//
//            if(types != null && !types.isEmpty()){
//                for(String type : wordAnnotation.semanticTypes){
//                    attributes.add("sem." + type + "@-2");  
//                }
//            }
//        }
//
//        // check the next word
//        if(wordIndex +1 < sentence.tags.size()){
//            ArrayList<String> types = sentence.tags.get(wordIndex+1).semanticType;
//
//            if(types != null && !types.isEmpty()){
//                for(String type : wordAnnotation.semanticTypes){
//                    attributes.add("sem." + type + "@1");  
//                }
//            }
//        }
//
//        return attributes;
//
//    }

}
