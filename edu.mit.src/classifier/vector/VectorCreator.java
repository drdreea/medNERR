package classifier.vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import corpus.AnnotationDetail;
import corpus.MedicalRecord;
import corpus.Annotation.AnnotationType;
import corpus.Relation.RelationType;

import classifier.ClassifierBase;
import classifier.attributes.AttributeFactory;

import preprocess.parser.SentenceContent;
import preprocess.parser.WordTagMap;
import utils.UtilMethods;

/**
 * 
 * This class creates a vector instance based on the given parameters
 * @author andreea
 *
 */
public class VectorCreator {
	ClassifierBase classifier;

	AttributeFactory attributeFactory;

	public boolean unpairedContextTag = false;
	public int distanceContext = 0;
	public boolean beginContext = true;
	public int totalCount = 0;
	public RelationType currentRelation = RelationType.O;

	private String file;

	private MedicalRecord document;

	ArrayList<Pair> openContext;	

	public enum TagType{
		B, I,
		// other tag
		O,   ;

		TagType(){};

		public static TagType chooseTagType(AnnotationType annotationType, int count){			
			if(count == 0)
				return TagType.B;
			else 
				return TagType.I;

		}

		public boolean isBegin() {
			if(this.toString().startsWith("B"))
				return true;

			return false;
		}

		public boolean isInside(){
			if(this.toString().startsWith("I"))
				return true;

			return false;
		}


	};
	public enum InstanceType{CONCEPT, RELATION};

	/**
	 * 
	 * @param classifier
	 */
	public VectorCreator(ClassifierBase classifier){
		this.classifier = classifier;
		this.openContext = new ArrayList<Pair>();
		
		attributeFactory = new AttributeFactory(classifier);
	}

	/**
	 * Create the vector instance for the given sentence line
	 * We basically go through each word inside the sentence and
	 * 	decide on the type of tag for the given word
	 *  we also add specific attributes and generate the final vector
	 * @param sentence
	 * @param sentenceIndex
	 *  
	 * @return a vector of attributes
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public ArrayList<AttributeVector> createSentenceVectors(SentenceContent sentence, 
			int sentenceIndex, MedicalRecord record) 
					throws SecurityException, NoSuchMethodException, 
					IllegalArgumentException, IllegalAccessException, 
					InvocationTargetException {
		ArrayList<AttributeVector> instances = new ArrayList<AttributeVector>();

		// set the attribute factory
		attributeFactory.initSentence(sentence, sentenceIndex);

		// go through each word inside the sentence lines

		for(int wordIndex = 0; wordIndex < sentence.tags.size(); wordIndex ++){

			WordTagMap word = sentence.tags.get(wordIndex);

			// make sure there are some letters in the word
			if(word.word.trim().isEmpty()) continue;

			AnnotationDetail annt = new AnnotationDetail(word, record);
			if(annt.content == null){
				System.out.println("Could not find offsets");
				continue;
			}

			// we have to chose the tag depending on the instance type
			TagType tag = selectTagType(word);
			
			AttributeVector instance = new AttributeVector(annt, file);
			instance.phoneme = document.phonemes.get(word.word);

			// init the attribute factory
			attributeFactory.initAnnotation(annt, tag, wordIndex, 
					word);

			// go through the attribute names and add the necessary information 
			// to the attribute vector
			Class<? extends AttributeFactory> classForMethods = 
					attributeFactory.getClass();

			for(String attribute : classifier.attributeNames){
				Method method = classForMethods.getMethod(attribute, new Class[0]);
				Object result = method.invoke(attributeFactory, new Object[0]);

				instance.attributes.put(attribute, result);
			}


			instance.setTag(tag);
			instance.setPOS(word.posTag);

			instance.setAnnotationType(word.annotationType);
			instance.setName(annt.content);

			// add the instance to the list of instances
			instances.add(instance);
		}


		return instances;
	}

	/**
	 * 
	 * @param sentence
	 * @param sections
	 * @param medicalRecord

	 * @return
	 * @throws NoSuchMethodException 
	 * @throws SecurityException, InvocationTargetException , IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public ArrayList<AttributeVector> createSentenceRelationVectors(
			SentenceContent sentence,
			MedicalRecord record, int sentenceIndex) 
					throws SecurityException, NoSuchMethodException, 
					IllegalArgumentException, IllegalAccessException, 
					InvocationTargetException {
		ArrayList<AttributeVector> instances = new ArrayList<AttributeVector>();

		// set the attribute factory
		attributeFactory.initSentence(sentence, sentenceIndex);
		
		// go through each word inside the sentence lines
		for(int wordIndex = 0; wordIndex < sentence.tags.size(); wordIndex ++){
			WordTagMap word = sentence.tags.get(wordIndex);
			word.word = word.word.toLowerCase();
					
			// make sure there are some letters in the word
			if(word.word.trim().isEmpty()) continue;

			AnnotationDetail annt = new AnnotationDetail(word);

			// we have to chose the tag depending on the instance type
			TagType tag = selectContextTagType(word, wordIndex);

			AttributeVector instance = new AttributeVector(annt, file);
			instance.phoneme = record.phonemes.get(annt.content);

			// init the attribute factory
			attributeFactory.initAnnotation(annt, tag, wordIndex, word);

			// if we have to include other concepts as well
			if(tag == TagType.O){
				tag = selectTagType(word);
				if(tag != TagType.O){
					instance.setAnnotationType(word.annotationType);
					instance.setTag(tag);
					attributeFactory.initAnnotation(annt, tag, wordIndex, 
							word);
				}
			}else
				System.out.print("");

			// go through the attribute names and add the necessary information 
			// to the attribute vector
			Class<? extends AttributeFactory> classForMethods = 
					attributeFactory.getClass();

			for(String attribute : classifier.attributeNames){
				Method method = classForMethods.getMethod(attribute, new Class[0]);
				Object result = method.invoke(attributeFactory, new Object[0]);
				instance.attributes.put(attribute, result);
			}


			instance.setTag(tag);					
			instance.setRelationType(currentRelation);
			instance.setPOS(word.posTag);
			
			if(classifier.normalizeText)
				instance.setName(UtilMethods.normalizeText(word));
			else
				instance.setName(word.baseForm.toLowerCase());
						
			// add the instance to the list of instances
			instances.add(instance);

		}

		return instances;
	}

	/**
	 * Choose the tag for the given word
	 * 
	 * @param word
	 * 
	 * @return
	 */
	TagType selectTagType( WordTagMap word ) {
		TagType tag = word.tagType;

		if(tag == null || word.annotationType == null) 
			tag = TagType.O; 
		else
			if(!classifier.nerToAnnotate.contains(word.annotationType))
				tag = TagType.O;

		// testing whether it helps to add the person tag
		//		if(tag == TagType.O){
		//			String wordString = word.word.toLowerCase();
		//
		//			if(wordString.equals("he") || wordString.equals("she") ||
		//					wordString.equals("we") || wordString.equals("they") ||
		//					wordString.equals("i") || wordString.equals("you") ||
		//					wordString.equals("patient") || 
		//					wordString.equals("doctor") ||
		//					wordString.equals("dr") || 
		//					wordString.equals("woman") ||
		//					wordString.equals("man") ||
		//					wordString.equals("child") ||   
		//					wordString.equals("person") || 
		//					wordString.equals("individual") ||   
		//					UtilMethods.removePunctuation(wordString).equals("mr") || 
		//					UtilMethods.removePunctuation(wordString).equals("mrs") ||
		//					word.tag.equals("NNP")){
		//				tag = TagType.PERSON;
		//			} else if(word.tag.toLowerCase().startsWith("v"))
		//				tag = TagType.ACTION;
		//			else if(word.tag.startsWith("NN"))
		//				tag = TagType.REGULARNOUN;
		//
		//		}

		return tag;
	}

	/**
	 * Select the tag for the context annotations
	 * @param word
	 * @param conceptOffsets
	 * @return
	 */
	TagType selectContextTagType(WordTagMap word,  int wordIndex) {
		TagType tag = TagType.O;

		// check if there are already open relations that we have to flag
		Iterator<Pair> it = this.openContext.iterator();
		while(it.hasNext()){
			Pair newPair = it.next();
			AnnotationDetail annt = newPair.annt;

			// if the annotation starts on the current word then we end 
			// the context and remove the annotation from the list
			if(annt.documentOffsetStart == word.documentWordOffset){
				it.remove();
				tag = TagType.O;
				currentRelation = RelationType.O;
				beginContext = true;
			}else
				if(beginContext){
					tag = TagType.B;

					currentRelation = newPair.rel;
					beginContext = false;
				}else{
					tag = TagType.I;
					currentRelation = newPair.rel;
				}
		}

		// check if there are relations starting from the current word
		AnnotationDetail annt = word.annt;
		if(annt != null && annt.relatedTo != null)
			for(RelationType rel : annt.relatedTo.keySet()){				
				if(classifier.relationsToAnnotate.contains(rel)){
					ArrayList<AnnotationDetail> relations = annt.relatedTo.get(rel);
					
					for(AnnotationDetail detail : relations){
						if(detail.documentOffsetStart > annt.documentOffsetStart && // relation follows the current annotation
								annt.documentOffsetEnd == word.documentWordOffset){ // the concept ends here
//							if(this.openContext.isEmpty())
								openContext.add(new Pair(rel, detail));
						}
					}

				}
			}





		return tag;
	}

	/**
	 * create the sequence vector for concept prediction
	 * @param reason
	 * @param arrayList
	 * @param file
	 * @param vector
	 * @param crftype
	 * @param object
	 * @param positive
	 * @param entityType
	 * 
	 * @return
	 */
	//		public ArrayList<String> createSequenceVectors(Annotation reason,
	//				HashMap<String, MedicalRecord> medicalRecords,
	//				ArrayList<SentenceContent> sentences,
	//				ArrayList<Section> sections, String file, VectorType vector,
	//				CRFType crfType, Object object, boolean positive, 
	//				Entity entityType,  AnnotationFile medications) {
	//
	//			ArrayList<String> instances = new ArrayList<String>();
	//
	//			// find the sentences for concept
	//			SentenceContent firstSentence = null;
	//			String firstComment = VectorInstance.printComments(reason.reason, file);
	//
	//			if(conceptSentence.containsKey(firstComment))
	//				firstSentence = conceptSentence.get(firstComment);
	//			else{
	//				int offset = SentenceContent.identifySentencePerConcept(reason.reason, 
	//						medicalRecords.get(file).rawFileLines, sentences);
	//				conceptSentence.put(firstComment, firstSentence);
	//				if(offset != -1)
	//					firstSentence = sentences.get(offset);         
	//			}
	//
	//
	//			if(firstSentence == null){
	//				int offset = SentenceContent.identifySentencePerConcept(reason.reason, 
	//						medicalRecords.get(file).rawFileLines, sentences );
	//				if(offset != -1)
	//					firstSentence = sentences.get(offset);         
	//			}
	//
	//			// add the words before the first concept, unless the sentence ends 
	//			instances.addAll(getInstancesBefore(reason.reason, firstSentence, 
	//					sections, medicalRecords, file, vector, crfType, null, -1, medications));
	//
	//
	//			// add the first concept instance
	//			String concept = reason.reason.content;
	//			if(concept.split(" ").length > 1){
	//				String[] splitContent = concept.split(" ");
	//				boolean beginning = true;
	//
	//				for(int index= 0; index < splitContent.length; index ++){
	//					TagType tag = null;
	//					if(beginning) {
	//						if(positive)
	//							switch(entityType){
	//							case MEDICATION:
	//								tag = TagType.BEGINMEDICATION;
	//								break;
	//							case REASON:
	//								tag = TagType.BR;
	//								break;
	//							}
	//						else 
	//							tag = TagType.O;
	//						beginning = false;
	//					}
	//					else {
	//						if(positive)
	//							switch(entityType){
	//							case MEDICATION:
	//								tag = TagType.INSIDEMEDICATION;
	//								break;
	//							case REASON:
	//								tag = TagType.IR;
	//								break;
	//							}
	//						else
	//							tag = TagType.O;
	//					}
	//
	//
	//					AnnotationDetail annt = new AnnotationDetail(null, null);
	//					annt.content = splitContent[index];
	//					annt.startLine = reason.reason.startLine;
	//					annt.endLine = reason.reason.endLine;
	//					annt.startOffset = reason.reason.startOffset + index;
	//					annt.endOffset = annt.startOffset;
	//
	//					instances.add(createVector(annt, sections,
	//							medicalRecords,
	//							file, -1, vector,  positive, tag, firstSentence, 
	//							crfType, InstanceType.RELATION, null, -1, medications));
	//				}
	//			}else{
	//				if(positive){
	//					TagType tag = null;
	//					switch(entityType){
	//					case MEDICATION:
	//						tag = TagType.BEGINMEDICATION;
	//						break;
	//					case REASON:
	//						tag = TagType.BR;
	//						break;
	//					}
	//					instances.add(createVector(reason.reason, sections, 
	//							medicalRecords, 
	//							file, -1, vector, positive, tag, firstSentence, 
	//							crfType, InstanceType.RELATION, null, -1, medications));
	//				}
	//				else
	//					instances.add(createVector(reason.reason, sections, 
	//							medicalRecords, 
	//							file, -1, vector, positive, TagType.O, firstSentence, crfType,
	//							InstanceType.RELATION, null, -1, medications));
	//			}
	//
	//			// add the words after the last concept, unless the sentence ends
	//			instances.addAll(getInstancesAfter(reason.reason, firstSentence, 
	//					sections, medicalRecords, file, vector, crfType, null, -1, medications));
	//
	//			return instances;
	//		}


	/**
	 * create the sequence instance vectors we want to add three words before the 
	 * first concept, three words after the last concept, and the context between the concepts
	 * @param medication
	 * @param reason
	 * @param sections
	 * @param file
	 * @param vector
	 * @param positiveInstances
	 * @param positive
	 * 
	 * @return a list of the instance vectors
	 */
	//		public ArrayList<String> createSequenceVectors(Annotation medication,
	//				Annotation reason, ArrayList<Section> sections, 
	//				HashMap<String, MedicalRecord> medicalRecords,
	//				ArrayList<SentenceContent> sentences,
	//				String file,
	//				VectorType vector, CRFType crfType,
	//				HashSet<String> positiveInstances, boolean positive,
	//				AnnotationFile medications) {
	//			ArrayList<String> instances = new ArrayList<String>();
	//
	//			AnnotationDetail first = medication.medication;
	//			AnnotationDetail second = reason.reason;
	//
	//			if(first.startLine > second.startLine){
	//				AnnotationDetail tmp = first;
	//				first = second;
	//				second = tmp;
	//			}else if(first.startLine == second.startLine && 
	//					first.startOffset > second.startOffset){
	//				AnnotationDetail tmp = first;
	//				first = second;
	//				second = tmp;
	//			}
	//
	//			// first the sentences for the first and second concept
	//			SentenceContent firstSentence = null;
	//			String firstComment = VectorInstance.printComments(first, file);
	//
	//			if(conceptSentence.containsKey(firstComment))
	//				firstSentence = conceptSentence.get(firstComment);
	//			else{
	//				int offset = SentenceContent.identifySentencePerConcept(first, 
	//						medicalRecords.get(file).rawFileLines, sentences);
	//				if(offset != -1)
	//					firstSentence = sentences.get(offset);
	//				conceptSentence.put(firstComment, firstSentence);
	//			}
	//
	//			SentenceContent secondSentence = null;
	//			String secondComment = VectorInstance.printComments(second, file);
	//
	//			if(conceptSentence.containsKey(secondComment))
	//				secondSentence = conceptSentence.get(secondComment);
	//			else{
	//				int offset = SentenceContent.identifySentencePerConcept(second, 
	//						medicalRecords.get(file).rawFileLines, sentences);
	//				if(offset != -1)
	//					secondSentence = sentences.get(offset);
	//
	//				conceptSentence.put(secondComment, secondSentence);
	//			}
	//
	//			if(firstSentence == null){
	//				int offset = SentenceContent.identifySentencePerConcept(first, 
	//						medicalRecords.get(file).rawFileLines, sentences);
	//				if(offset != -1)
	//					firstSentence = sentences.get(offset);
	//			}
	//
	//			if(secondSentence == null){
	//				int offset = SentenceContent.identifySentencePerConcept(second, 
	//						medicalRecords.get(file).rawFileLines, sentences);
	//				if(offset != -1)
	//					secondSentence = sentences.get(offset);         
	//			}
	//
	//			// add three words before the first concept, unless the sentence ends 
	//			instances.addAll(getInstancesBefore(first, firstSentence, 
	//					sections, medicalRecords, file, vector, crfType, null, -1, medications));
	//
	//
	//			// add the first concept instance
	//			instances.add(createVector(first, sections, 
	//					medicalRecords,
	//					file, -1, vector, positive, TagType.ENTITY, firstSentence, 
	//					crfType, InstanceType.RELATION, null, -1, medications));
	//
	//			// add the context instances 
	//			if(positive){
	//				TagType tag = TagType.INSIDERELATION;
	//
	//				instances.addAll(createContextVectors(medication.medication, reason.reason, 
	//						false, sections, medicalRecords, file, vector, tag, 
	//						firstSentence, secondSentence, crfType, null, -1, medications));
	//			}
	//			else
	//				instances.addAll(createContextVectors(medication.medication, reason.reason, 
	//						false, sections, medicalRecords, file, vector, TagType.O, 
	//						firstSentence, secondSentence, crfType, null, -1, medications));
	//
	//			// add the second concept instance
	//
	//			instances.add(createVector(second, sections, 
	//					medicalRecords,
	//					file, -1, vector, positive, TagType.ENTITY, 
	//					secondSentence, crfType, InstanceType.RELATION, null, -1, medications));
	//
	//			// add the words after the last concept, unless the sentence ends
	//			instances.addAll(getInstancesAfter(second, secondSentence, 
	//					sections, medicalRecords, file, vector, crfType, null, -1, medications));
	//
	//			return instances;
	//		}

	/**
	 * NOTE: THIS METHOD IS PROBABLY INCORRECT. DESIGNING A NEW ONE ABOVE
	 * create the sequence instance vectors we want to add three words before the 
	 * first concept, three words after the last concept, and the context between the concepts
	 * @param medication
	 * @param reason
	 * @param sections
	 * @param file
	 * @param vector
	 * @param positiveInstances
	 * @param positive
	 * 
	 * @return a list of the instance vectors
	 */
	//    public ArrayList<String> createSequenceVectors(Annotation medication,
	//            Annotation reason, ArrayList<Section> sections, String file,
	//            VectorType vector, HashSet<String> positiveInstances, boolean positive) {
	//        ArrayList<String> instances = new ArrayList<String>();
	//
	//        AnnotationDetail first = medication.medication;
	//        AnnotationDetail second = reason.reason;
	//
	//        if(first.startLine > second.startLine){
	//            AnnotationDetail tmp = first;
	//            first = second;
	//            second = tmp;
	//        }else if(first.startLine == second.startLine && 
	//                first.startOffset > second.startOffset){
	//            AnnotationDetail tmp = first;
	//            first = second;
	//            second = tmp;
	//        }
	//
	//        // add three words before the first concept, unless the sentence ends 
	//        SentenceContent firstSentence = SentenceContent.identifySentencePerConcept(first, 
	//                classifier.medicalRecords.get(file).rawFileLines, 
	//                classifier.discourse.parseTagContent.get(file.split("\\.")[0]));
	//        if(firstSentence != null){
	//            instances.addAll(getInstancesBefore(first, firstSentence, 
	//                    sections, file, vector));
	//        }
	//
	//
	//        // add the medication reason instance
	//        instances.add(createVector(medication, 
	//                reason, positive, classifier.allSections.get(file), 
	//                file, vector));
	//
	//        // add the context instances 
	//        instances.addAll(createContextVectors(medication.medication, reason.reason, 
	//                false, sections, file, vector));
	//
	//        // add three words after the last concept, unless the sentence ends
	//        SentenceContent secondSentence = SentenceContent.identifySentencePerConcept(second, 
	//                classifier.medicalRecords.get(file).rawFileLines, 
	//                classifier.discourse.parseTagContent.get(file.split("\\.")[0]));
	//        if(secondSentence != null){
	//            instances.addAll(getInstancesAfter(second, secondSentence, 
	//                    sections, file, vector));
	//        }
	//
	//        return instances;
	//    }

	/**
	 * 
	 * @param first
	 * @param sentence
	 * @param sections
	 * @param file
	 * @param vector
	 * @param crfType 
	 * 
	 * @return
	 */
	//		private ArrayList<String> getInstancesBefore(AnnotationDetail first,
	//				SentenceContent sentence, ArrayList<Section> sections,
	//				HashMap<String, MedicalRecord> medicalRecords,
	//				String file, VectorType vector, CRFType crfType, 
	//				ArrayList<SentenceContent> sentences, int sentenceIndex,
	//				AnnotationFile medications) {
	//			ArrayList<String> instances = new ArrayList<String>();
	//
	//			if(sentence == null) return instances;
	//
	//			// go through the words in the sentence and identify the location
	//			// of the annotation
	//			String[] splitSentence = sentence.sentence.toLowerCase().split(" ");
	//			ArrayList<String> preWords = new ArrayList<String>();
	//
	//			for(int index = 0; index < splitSentence.length; index ++){
	//				String  word = splitSentence[index];
	//
	//				if(first.content.toLowerCase().startsWith(word)){
	//					if(first.content.split(" ").length > 1){
	//						if(index +1 < splitSentence.length && 
	//								first.content.contains(splitSentence[index+1]))
	//							break;
	//					}else break;
	//				}
	//
	//				preWords.add(word);
	//			}
	//
	//			for(int index = 0; index < preWords.size(); index ++){
	//				if(index < 0) continue;
	//
	//				AnnotationDetail wordAnnotation = new AnnotationDetail(null, null);
	//				wordAnnotation.content = preWords.get(index);
	//
	//				wordAnnotation.startLine = first.startLine;
	//				wordAnnotation.startOffset = first.startOffset + index ;
	//
	//				wordAnnotation.endLine = wordAnnotation.startLine;
	//				wordAnnotation.endOffset = wordAnnotation.startOffset ;
	//
	//				if(wordAnnotation.content.trim().isEmpty() ||
	//						UtilMethods.removePunctuation(wordAnnotation.content).trim().isEmpty()) 
	//					continue;
	//
	//				instances.add(createVector(wordAnnotation, 
	//						sections, medicalRecords, 
	//						file, -1, vector, false, TagType.O, sentence, 
	//						crfType, InstanceType.RELATION, sentences, sentenceIndex,
	//						medications));
	//			}
	//
	//			return instances;
	//		}


	/**
	 * 
	 * @param first
	 * @param sentence
	 * @param sections
	 * @param file
	 * @param vector
	 * @param crfType 
	 * 
	 * @return
	 */
	//		private ArrayList<String> getInstancesAfter(AnnotationDetail first,
	//				SentenceContent sentence, ArrayList<Section> sections,
	//				HashMap<String, MedicalRecord> medicalRecords,
	//				String file, VectorType vector, CRFType crfType, 
	//				ArrayList<SentenceContent> sentences, int sentenceIndex,
	//				AnnotationFile medications) {
	//			ArrayList<String> instances = new ArrayList<String>();
	//
	//			if(sentence == null) return instances;
	//
	//			// go through the words in the sentence and identify the location
	//			// of the annotation
	//			String[] splitSentence = sentence.sentence.toLowerCase().split(" ");
	//			ArrayList<String> preWords = new ArrayList<String>();
	//			boolean found = false;
	//
	//			for(int index = 0; index < splitSentence.length; index ++){
	//				String  word = splitSentence[index];
	//
	//				if(first.content.toLowerCase().startsWith(word)){
	//					if(first.content.split(" ").length > 1){
	//						if(index +1 < splitSentence.length && 
	//								first.content.contains(splitSentence[index+1])){
	//							found = true;
	//							index += first.content.split(" ").length - 1;
	//							continue;
	//						}
	//					}else {
	//						found = true;
	//						continue;
	//					}
	//
	//				}
	//
	//				if(found)
	//					preWords.add(word);
	//			}
	//
	//			for(int index = 0; index < preWords.size() ; index ++){
	//
	//				AnnotationDetail wordAnnotation = new AnnotationDetail(null, null);
	//				wordAnnotation.content = preWords.get(index);
	//
	//				wordAnnotation.startLine = first.startLine;
	//				wordAnnotation.startOffset = first.startOffset + index ;
	//
	//				wordAnnotation.endLine = wordAnnotation.startLine;
	//				wordAnnotation.endOffset = wordAnnotation.startOffset ;
	//
	//				if(wordAnnotation.content.trim().isEmpty() ||
	//						UtilMethods.removePunctuation(wordAnnotation.content).trim().isEmpty()) 
	//					continue;
	//
	//				instances.add(createVector(wordAnnotation, 
	//						sections, medicalRecords, 
	//						file, -1, vector, false, TagType.O, 
	//						sentence, crfType, InstanceType.RELATION, sentences, sentenceIndex,
	//						medications));
	//
	//			}
	//
	//			return instances;
	//		}
	/**
	 * 
	 * @param medication
	 * @param reason
	 * @param instanceType
	 * @param sections
	 * @param file
	 * @param vector
	 * @param tagType
	 * @param secondSentence 
	 * @param firstSentence 
	 * @param crfType 

	 * @return the list of vectors
	 */
	//		ArrayList<String> createContextVectors(AnnotationDetail medication,
	//				AnnotationDetail reason, boolean instanceType, 
	//				ArrayList<Section> sections, 
	//				HashMap<String, MedicalRecord> medicalRecords,
	//				String file, VectorType vector, TagType tagType, 
	//				SentenceContent firstSentence, SentenceContent secondSentence, 
	//				CRFType crfType, ArrayList<SentenceContent> sentences, int sentenceIndex,
	//				AnnotationFile medications){
	//			Object[] fileLines = medicalRecords.get(file).rawFileLines.toArray();
	//			ArrayList<String> vectors = new ArrayList<String>();
	//
	//
	//			SentenceContent sentence = SentenceContent.mergeSentences(firstSentence, secondSentence);
	//
	//			// first find the context between the medication and the reason
	//			ArrayList<AnnotationDetail> context = FindContextFromEMR.getContentObjects(fileLines, 
	//					medication, reason);
	//
	//			boolean firstTag = true;
	//
	//			if(context != null && !context.isEmpty()){
	//				for(AnnotationDetail wordAnnotation : context){
	//					if(wordAnnotation.content.trim().isEmpty() ||
	//							UtilMethods.removePunctuation(wordAnnotation.content.trim()).isEmpty())
	//						continue;
	//
	//					if(tagType == TagType.INSIDERELATION && firstTag == true){
	//						firstTag = false;
	//
	//						String vectorInstance = createVector(wordAnnotation,  
	//								sections, medicalRecords, 
	//								file,-1,  vector, instanceType, TagType.BEGINRELATION, 
	//								sentence, crfType, InstanceType.RELATION, sentences, sentenceIndex,
	//								medications);
	//
	//						if(vectorInstance != null) vectors.add(vectorInstance);
	//					}else{
	//
	//						String vectorInstance = createVector(wordAnnotation,  
	//								sections, medicalRecords, 
	//								file, -1, vector, instanceType, tagType, 
	//								sentence, crfType, InstanceType.RELATION, sentences, sentenceIndex,
	//								medications);
	//
	//						if(vectorInstance != null) vectors.add(vectorInstance);
	//					}
	//				}
	//			}
	//
	//			return vectors;
	//		}

	public void init(String file, 
			MedicalRecord record) {
		this.file = file;
		this.document = record;

		this.attributeFactory.initGeneric(file, record);
	}

	class Pair{
		public RelationType rel;
		public AnnotationDetail annt;

		Pair(RelationType rel, AnnotationDetail annt){
			this.rel = rel;
			this.annt = annt;
		}
	}

}