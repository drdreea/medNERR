package probabModel.ccm_code.Experiments;

import java.util.Vector;

import probabModel.ccm_code.HMMLearningAndInferenceWithConstraints.Constraint;

/**
 * Specify the general constraints for each of the classifier types
 * @author andreea
 *
 */
public class Parameters {
	public enum Constraints{MEDICATION, REASONANDRELATION, 
		RELATION, MEDICAL, 
		RELATION_GENERIC};

	public final static String errorMessage = "To run this program you need to pass" +
			" as argument the following file paths \n" +
			" 1. file path for the training file \n" +
			" 2. file path for the test file \n" +
			" 3. file path for the file containing unlabelled examples \n" +
			" 4. file path for the results file \n\n";
	
	public enum CLASSIFIER{MEDICATION};
	public static int problem = -1;
	public static boolean hardConstraints=false;
	
	public static final int SUP = 0;
	public static final int SEMI_NO_Avg = 1;
	public static final int SEMI_Avg = 2;
	public static final int SEMI_Avg_CON = 3;
	
	public static int method  = -1;
	//public static boolean use_constraints_in_beamsearch = false;
	
	public static int BEAMSIZE = 50;
	public static int numFeatures = 11; // features for relation extraction
//	public final static int numFeatures = 9; // features for med extraction
//	public final static int numFeatures = 10; // features for the medical concept extraction
//	
	public static double smooth_prob_para =  0.1; 
	public static double sup_model_weight =  0.9;//0.9;	
	
	//public static final String[] adsConstraints={/*"MustAppearAds",*/"RandomConstraint","MinFieldLengthAds"/*,"AddressProto","AvailableProto","ContactProto","FeaturesProto","NeighborhoodProto","PhotosProto","RentProto","RestrictionsProto","RoomatesProto","SizeProto","UtilitiesProto"*/,"TransitionOnPunctuationsAds"};
	//public static final String[] citationsConstraints={"RandomConstraint",/*"MutuallyExclusive",*/"MustAppearCitation","TopologicalOrder",/*"MustStartAuthEditor",*/"StateAppearsOnce","TransitionsOnPunctuationsCitations","BookJournalProto","DateProto","EditorsProto","JournalProto","NoteProto","PagesProto","TechRepProto","TitleProto","LocationProto"};
	
	public static final String[] adsConstraints={"MinFieldLengthAds","AddressProto","AvailableProto",
		"ContactProto","FeaturesProto","NeighborhoodProto","PhotosProto","RentProto","RestrictionsProto",
		"RoomatesProto","SizeProto","TransitionOnPunctuationsAds","UtilitiesProto"};
	public static final String[] citationsConstraints={"MustStartAuthEditor","StateAppearsOnce",
		"TransitionsOnPunctuationsCitations","BookJournalProto","DateProto","EditorsProto","JournalProto",
		"NoteProto","PagesProto","TechRepProto","TitleProto","LocationProto"};
	
	
	public static final String[] medicationConstraints = {
		 "CannotStartWithInsideGeneric"
		,"NoAnnotationStartOnPunctuation"
		,"MedMinLength"
		
		,"NoAdjMedications"
		,"NoLabMedications"
		,"OnlyOneBeginning"
		
		,"HomeAnnotation"
	};
	
	public static final String[] medicalConstraints={
		"CannotStartWithInsideGeneric"
		,"NoAnnotationStartOnPunctuation"
		,"MinLength"

		,"PRNPlusReasonPrediction" 
		,"ReasonPreposition" 		
		,"Pronoun"
		,"FilterByPOS"
		,"NearbyMedication"
		,"MedFor"
//		,"LabSection"
	};
	
	public static final String[] relationConstraints = {
		// constraints included in thesis writeup
		"CannotStartWithInside"
		,"MedReasonAsO"
//		,"PrepBeforeReason"

//		,"NoNumberRelation"
		,"NoNumberQuantRelation"
		
		,"PRNRelation"
		,"RelationStartOnNumber"
		,"ForRelation"
		
		,"NoChangeInLabelThroughoutSequence"
		,"NoComma"
		,"NoAnd"
		,"NoReasonOutsidePhrase"
		
//		,"CannotEndOnLastItem"
		,"NoContextWithoutMedAndReason"
		,"StartAtConcept"
		,"EndAtConcept"
	};
	
	public static final String[] relationConstraintsGeneric = {
		"NoChangeInLabelThroughoutSequence"
		, "CannotStartWithInsideGeneric"
		,"StartAtConcept"
		,"EndAtConcept"
	};

	public static final String[] reasonAndRelationConstraints = {
		// the relation constraints
		"CannotStartWithInsideGeneric"
		,"PrepBeforeReason"

		,"NoNumberRelation"
		,"NoNumberQuantRelation"
		
		// relation constraints
		,"MedReasonAsO"

		,"NoNumberQuantRelation"
		
		,"PRNRelation"
		,"RelationStartOnNumber"
		,"ForRelation"
		
		,"NoChangeInLabelThroughoutSequence"
		,"NoAnd"
		,"NoDetReason"
		,"NoPossesiveReason"
		,"PenalizeStopWords"
		,"NoReasonOutsidePhrase"
 
		,"StartAtConceptBoth"
		,"EndAtConceptBoth"
//		
		// New constraints
		,"NoRelationWithoutReason"
		,"ReasonAnnotationWithoutContext"
		,"NoDoubleReasons"
	};
	
	public static Vector<Constraint> constraintsToApply=new Vector<Constraint>();
	
}
