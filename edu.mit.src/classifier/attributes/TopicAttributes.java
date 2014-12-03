package classifier.attributes;

import java.util.ArrayList;
import java.util.HashMap;

import corpus.AnnotationDetail;

import postprocess.topic.TopicIdentification;
import preprocess.parser.SentenceContent;
import classifier.ClassifierBase;
import classifier.vector.VectorCreator.TagType;

public class TopicAttributes extends AttributeGenerator{ 
	TopicIdentification topicIdentifier;
	
	public TopicAttributes(ClassifierBase classifier){
		super(classifier);
		HashMap<TopicIdentification.Topic, String> paths = 
				new HashMap<TopicIdentification.Topic, String>();
//		paths.put(TopicIdentification.Topic.LAB, classifier.configs.labTopicPath);
//		paths.put(TopicIdentification.Topic.MEDICATION,classifier.configs.medTopicPath);
		
		topicIdentifier = new TopicIdentification(paths);
	}


	public ArrayList<Object> getTopicAttributes(
			AnnotationDetail wordAnnotation, String file,
			TagType tagType, SentenceContent sentence, int wordIndex, 
			ArrayList<SentenceContent> sentences, int sentenceIndex) {
		ArrayList<Object> attributes = new ArrayList<Object>();

		return attributes;
	}
	
	public String getTopic(AnnotationDetail annt, SentenceContent sentence){
		String topic = "none";
		
		TopicIdentification.Topic tp = (topicIdentifier.getTopic(annt.content, sentence.sentence));
			
		switch(tp){
		case MEDICATION:
			topic = "medTopic";
			break;
		case LAB:
			topic = "labTopic";
			break;
		default:
			topic = "none";
			break;
		}
		
		return topic;
	}

}
