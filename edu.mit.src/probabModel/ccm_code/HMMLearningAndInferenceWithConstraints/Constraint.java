package probabModel.ccm_code.HMMLearningAndInferenceWithConstraints;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import utils.UtilMethods;

public  class Constraint {
	public static double randomConstraintProb=0;


	public String name;
	boolean isLocal=true;
	public static boolean constraintsInstalled=false;
	public static Vector<Constraint> constraints=new Vector<Constraint>();
	public boolean isViolated(State s, TaggingDict td){
		return false;
	}

	public static Constraint getByName(String constraintName) throws Exception{
		makeConstraintsAvailable();
		for(int i=0;i<constraints.size();i++)
			if(constraints.elementAt(i).name.equals(constraintName))
				return constraints.elementAt(i);
		System.out.println("Fatal Error: Cannot get the constraint: "+constraintName);
		System.exit(0);
		return null;
	}

	public static void makeConstraintsAvailable() throws Exception{
		if(!constraintsInstalled)
		{
			Class[] c=(Class.forName("probabModel.ccm_code.HMMLearningAndInferenceWithConstraints." +
					"Constraint")).getClasses();
			for(int i=0;i<c.length;i++)
				c[i].newInstance();//  getConstructor()).newInstance();
		}
		constraintsInstalled=true;		
	}

	public static void listAvailableConstraints(){
		System.out.println("Listing the installed constraints:");
		for(int i=0;i<constraints.size();i++)
			System.out.println(/*"Constraint name:"+*/ constraints.elementAt(i).name);
		System.out.println("Done.");
	}


	public static void main(String[] args) throws Exception{
		makeConstraintsAvailable();
		listAvailableConstraints();
	}


	public static class RandomConstraint extends Constraint{
		public RandomConstraint(){
			isLocal=false;
			name="RandomConstraint";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			if(s.randomConstraint>-1)
				if(s.state!=s.randomConstraint)
					return true;
			return false;
		}	
	}


	public static class MustAppearAds extends Constraint{		
		String[] fields={"contact","features","size"};
		public MustAppearAds(){
			isLocal=false;
			name="MustAppearAds";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			if(!s.isComplete())
				return false;
			if(s.isComplete())
			{
				int[] tags=s.toArr();
				for(int j=0;j<fields.length;j++)
				{
					boolean appears=false;
					for(int i=0;i<tags.length;i++)
						if(tags[i]==td.GetTagIdx(fields[j]))
							appears=true;
					if(!appears)
						return true;
				}
			}
			return false;
		}	
	}

	public static class TransitionOnPunctuationsAds extends Constraint{
		public TransitionOnPunctuationsAds(){
			isLocal=false;
			name="TransitionOnPunctuationsAds";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if((T>0)&&(s.assignment(T)!=s.assignment(T-1))){
				String prevWord=td.IdxToWord(s.wordseq[T-1]);
				if(!(prevWord.equals(".")||prevWord.equals(",")||prevWord.equals(";")||
						prevWord.equals("!")||prevWord.equals("_newline_")))
					return true;
			}
			return false;
		}	
	}

	public static class FeaturesProto extends Constraint{
		public FeaturesProto(){
			name="FeaturesProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("features")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("kitchen"))||(word.equalsIgnoreCase("laundry"))||(word.equalsIgnoreCase("parking")))
					return true;
			}
			return false;
		}	
	}

	public static class SizeProto extends Constraint{
		public SizeProto(){
			name="SizeProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("size")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("sq"))||(word.equalsIgnoreCase("ft"))||(word.indexOf("bdrm")>-1))
					return true;
			}			return false;
		}	
	}

	public static class RentProto extends Constraint{
		public RentProto(){
			name="RentProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("rent")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("_money_")))
					return true;
			}			return false;
		}	
	}
	public static class NeighborhoodProto extends Constraint{
		public NeighborhoodProto(){
			name="NeighborhoodProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("neighborhood")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("close"))||(word.equalsIgnoreCase("near"))||(word.equalsIgnoreCase("shopping")))
					return true;
			}			return false;
		}	
	}

	public static class ContactProto extends Constraint{
		public ContactProto(){
			name="ContactProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("contact")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("_phone_"))||(word.equalsIgnoreCase("_email_")))
					return true;
			}
			return false;
		}	
	}

	public static class AvailableProto extends Constraint{
		public AvailableProto(){
			name="AvailableProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("available")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("immediately"))||(word.equalsIgnoreCase("begin"))||(word.equalsIgnoreCase("cheaper")))
					return true;
			}
			return false;
		}	
	}

	public static class RoomatesProto extends Constraint{
		public RoomatesProto(){
			name="RoomatesProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("roomates")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("roomates"))||word.equalsIgnoreCase("roomates")||(word.equalsIgnoreCase("respectful"))||(word.equalsIgnoreCase("drama")))
					return true;
			}
			return false;
		}	
	}

	public static class RestrictionsProto extends Constraint{
		public RestrictionsProto(){
			name="RestrictionsProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("restrictions")){
				String word=td.IdxToWord(s.wordseq[T]);
				if(word.equalsIgnoreCase("pets")||word.equalsIgnoreCase("smoking")||word.equalsIgnoreCase("dog")||word.equalsIgnoreCase("dogs")||word.equalsIgnoreCase("cat")||word.equalsIgnoreCase("cats"))
					return true;
			}
			return false;
		}	
	}

	public static class PhotosProto extends Constraint{
		public PhotosProto(){
			name="PhotosProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("photos")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("_http_") || word.equalsIgnoreCase("image") || word.equalsIgnoreCase("link")))
					return true;
			}			return false;
		}	
	}

	public static class AddressProto extends Constraint{
		public AddressProto(){
			name="AddressProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("address")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("address"))||(word.equalsIgnoreCase("carlmont"))||(word.equalsIgnoreCase("st"))||(word.equalsIgnoreCase("cross")))
					return true;
			}			return false;
		}	
	}

	public static class UtilitiesProto extends Constraint{
		public UtilitiesProto(){
			name="UtilitiesProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if(s.assignment(T)!=td.GetTagIdx("utilities")){
				String word=td.IdxToWord(s.wordseq[T]);
				if((word.equalsIgnoreCase("utilities"))||(word.equalsIgnoreCase("pays"))||(word.equalsIgnoreCase("electricity")))
					return true;
			}			return false;
		}	
	}

	public static class MinFieldLengthAds extends Constraint{
		public MinFieldLengthAds(){
			isLocal=false;
			name="MinFieldLengthAds";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			if((T >=3 )&&(s.assignment(T)!=s.assignment(T-1))){
				int i = T-2;
				int pre_field_len = 1;
				while (i >= 0 && s.assignment(i) == s.assignment(i+1)){
					i --;
					pre_field_len ++;
				}
				if (pre_field_len < 3)
					return true;
			}			return false;
		}	
	}

	/*
	 * 
	 * 
	 * 			CITATIONS CONSTRAINTS
	 * 
	 * 
	 */

	public static class StateAppearsOnce extends Constraint{
		public StateAppearsOnce(){
			isLocal=false;
			name="StateAppearsOnce";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			// we should only use the states which we didn't not use before.
			if (T > 0 )
			{
				if (s.assignment(T) != (s.assignment(T-1))){
					boolean appear = false;
					for (int i=0; i< T-1;i++)
						if (s.assignment(i) == s.assignment(T))
							appear = true;
					if (appear)
						return true;
				}
			}
			return false;
		}	
	}

	public static class MustStartAuthEditor extends Constraint{
		public MustStartAuthEditor(){
			name="MustStartAuthEditor";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			// must start with "author" or editor
			if(T==0)
			{
				if ((s.assignment(T) !=td.GetTagIdx("author") &&
						(s.assignment(T) !=td.GetTagIdx("editor"))))
					return true;
			}
			return false;
		}	
	}	

	public static class MustAppearCitation extends Constraint{		
		String[] fields={"date","title","author"};
		String[] conference={"journal","booktitle","tech"};
		public MustAppearCitation(){
			isLocal=false;
			name="MustAppearCitation";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			if(!s.isComplete())
				return false;
			if(s.isComplete())
			{
				int[] tags=s.toArr();
				for(int j=0;j<fields.length;j++)
				{
					boolean appears=false;
					for(int i=0;i<tags.length;i++)
						if(tags[i]==td.GetTagIdx(fields[j]))
							appears=true;
					if(!appears)
						return true;
				}
				boolean confAppears=false;
				for(int j=0;j<(conference.length)&&(!confAppears);j++)
					for(int i=0;i<tags.length;i++)
						if(tags[i]==td.GetTagIdx(conference[j]))
							confAppears=true;
				if(!confAppears)
					return true;
			}
			return false;
		}	
	}


	public static class TransitionsOnPunctuationsCitations extends Constraint{		

		public TransitionsOnPunctuationsCitations(){
			isLocal=false;
			name="TransitionsOnPunctuationsCitations";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			//allow transitions only on dots, commas or semicolumns
			if((T>0) && (s.assignment(T)!=s.assignment(T-1))){
				String prevWord=td.IdxToWord(s.wordseq[T-1]);
				if(!(prevWord.equals(".")||prevWord.equals(",")||prevWord.equals(";")))
					return true;
			}
			return false;
		}	
		public boolean limitSpace(int[] seq,boolean[][] constraintsSpace, TaggingDict td) {
			for(int i=1;i<seq.length;i++)
			{
				String word = td.IdxToWord(seq[i-1]);
				if(!(word.equals(".")||word.equals(",")||word.equals(";")))
					for(int j=0;j<td.GetTagNum();j++)
						if(!constraintsSpace[i-1][j])
							constraintsSpace[i][j]=false;
			}	
			for(int i=seq.length-2;i>=0;i--)
			{
				String word = td.IdxToWord(seq[i]);
				if(!(word.equals(".")||word.equals(",")||word.equals(";")))
					for(int j=0;j<td.GetTagNum();j++)
						if(!constraintsSpace[i+1][j])
							constraintsSpace[i][j]=false;
			}	
			return true;
		}
	}

	public static class TitleProto extends Constraint{
		public TitleProto(){
			name="TitleProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			// a few prototype words for the title
			String word = td.IdxToWord(s.wordseq[T]);
			if ((s.assignment(T) !=td.GetTagIdx("title")) && (word.equalsIgnoreCase("\"")))
				return true;
			return false;
		}	
		public boolean limitSpace(int[] seq,boolean[][] constraintsSpace, TaggingDict td) {			
			for(int i=0;i<seq.length;i++)
			{
				String word = td.IdxToWord(seq[i]);
				if(word.equalsIgnoreCase("\""))
				{
					for(int j=0;j<td.GetTagNum();j++)
						constraintsSpace[i][j]=false;
					constraintsSpace[i][td.GetTagIdx("title")]=true;
				}
			}	
			return false;
		}		
	}	

	public static class NoteProto extends Constraint{
		public NoteProto(){
			name="NoteProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			//a few prototypes for the note
			String word = td.IdxToWord(s.wordseq[T]);
			if ((s.assignment(T) !=td.GetTagIdx("note"))&&(word.equalsIgnoreCase("submitted")||word.equalsIgnoreCase("appear")))
				return true;
			return false;
		}	
	}


	public static class LocationProto extends Constraint{
		public LocationProto(){
			name="LocationProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			//a few prototypes for the location
			String word = td.IdxToWord(s.wordseq[T]);			
			if ((s.assignment(T) !=td.GetTagIdx("location"))&&(word.equals("IL")||word.equals("CA")||word.equalsIgnoreCase("Australia")||word.equals("NY")))
				return true;
			return false;
		}	
	}


	public static class TechRepProto extends Constraint{
		public TechRepProto(){
			name="TechRepProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			//a few prototypes for the tech
			String word = td.IdxToWord(s.wordseq[T]);
			if ((s.assignment(T) !=td.GetTagIdx("tech")) && T < s.wordseq.length - 1){
				if(((word.equalsIgnoreCase("tech"))||
						(word.equalsIgnoreCase("Technical"))) &&
						(td.IdxToWord(s.wordseq[T+1]).equalsIgnoreCase("report")))
					return true;
			}
			return false;
		}	
	}

	public static class BookJournalProto extends Constraint{
		public BookJournalProto(){
			name="BookJournalProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			// if the prefix is "Proc" => Proceeding
			String word = td.IdxToWord(s.wordseq[T]);
			if(word.equalsIgnoreCase("proc")||word.equalsIgnoreCase("proceedings")||word.equalsIgnoreCase("acm")){
				if (s.assignment(T) != td.GetTagIdx("journal")&&(s.assignment(T) != td.GetTagIdx("booktitle")))
					return true;
			}
			return false;
		}	
	}




	public static class JournalProto extends Constraint{
		public JournalProto(){
			name="JournalProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			// if the prefix is "Proc" => Proceeding
			String word = td.IdxToWord(s.wordseq[T]);
			if(word.equalsIgnoreCase("journal")){
				if (s.assignment(T) != td.GetTagIdx("journal"))
					return true;
			}
			return false;
		}	
	}



	public static class PagesProto extends Constraint{
		public PagesProto(){
			name="PagesProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			// if the word is "pp" => pages
			String word = td.IdxToWord(s.wordseq[T]);
			if(word.equalsIgnoreCase("pp"))
				if (s.assignment(T) != td.GetTagIdx("pages"))
					return true;
			return false;
		}	
	}

	public static class EditorsProto extends Constraint{
		public EditorsProto(){
			name="EditorsProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			// if the word is "ed" => editors
			String word = td.IdxToWord(s.wordseq[T]);
			if(word.equalsIgnoreCase("ed")||word.equalsIgnoreCase("editors")||word.equalsIgnoreCase("editor"))
				if (s.assignment(T) != td.GetTagIdx("editor"))
					return true;
			return false;
		}	
	}


	public static class DateProto extends Constraint{
		public DateProto(){
			name="DateProto";
			constraints.addElement(this);
		}
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			// If word is 19xx or 20xx, then it is date
			String word = td.IdxToWord(s.wordseq[T]);
			if(word.length() == 4){
				String prefix = word.substring(0,2);
				if ( (prefix.equals("19") || prefix.equals("20")))
					if (s.assignment(T) != td.GetTagIdx("date")){
						return true;
					}
			}
			return false;
		}	
	}

	/***
	 * Medication constraints
	 * 
	 */

	public static class PRN extends Constraint{
		public PRN(){
			name="PRN";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String word = td.IdxToWord(s.wordseq[T]);
			// get the previous word
			if(T>=1){
				String prevWord = td.IdxToWord(s.wordseq[T-1]);

				if(prevWord.equals("prn") || prevWord.equals("p.r.n."))
					if(s.assignment(T) != td.GetTagIdx("BR"))
						return true;
			}


			return false;
		}
	}

	/**
	 * check for the case medication (..) prn disease
	 * @author ab
	 *
	 */
	public static class PRNRelation extends Constraint{
		public PRNRelation(){
			name="PRNRelation";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			// get the previous word
			if(T+1 < s.wordseq.length){
				String currentWord = td.IdxToWord(s.wordseq[T]);
				boolean nextWordIsReason = td.IdxToReason(s.reasonSeq[T+1]).
						equals("isReason");

				if(UtilMethods.removePunctuation(currentWord).equals("prn"))
					if(nextWordIsReason) {
						if(s.assignment(T) == td.GetTagIdx("O"))
							return true;
					}else if(s.assignment(T) != td.GetTagIdx("O"))
						return true;
			}


			return false;
		}
	}

	public static class RelationStartOnNumber extends Constraint{
		public RelationStartOnNumber(){
			name="RelationStartOnNumber";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String currentWord = UtilMethods.removePunctuation(
					td.IdxToWord(s.wordseq[T]));

			if(s.assignment(T) == td.GetTagIdx("BREASON") && 
					UtilMethods.isNumber(currentWord)){
				for(int index = 1; index < 7 && index +T < s.wordseq.length; index ++){
					String word = UtilMethods.removePunctuation(
							td.IdxToWord(s.wordseq[index + T]));
					if(word.equals("prn") || word.equals("for"))
						return false;
				}
				return true;
			}

			return false;
		}
	}

	List<String> patternsMedication = Arrays.asList( "given for", "on", "for", "with", "needed for");
	List<String> patternsReasons = Arrays.asList("signs of", "sign of", "risk of", "developed", 
			"concern of", "concerns of" );


	public static class ReasonPreposition extends Constraint{
		List<String> forWord = Arrays.asList("given", "needed");
		List<String> ofWord = Arrays.asList("signs", "sign", "risk", "concern", "concerns",
				"because");
		List<String> toWord = Arrays.asList("order");
		List<String> withWord = Arrays.asList("diagnosed");

		public ReasonPreposition(){
			name = "ReasonPreposition";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			// get the previous word
			if(T-2 >=0 ){
				String prevOneWord = td.IdxToWord(s.wordseq[T-1]);
				String prevTwoWords = td.IdxToWord(s.wordseq[T-2]);

				if((prevOneWord.equals("for") && forWord.contains(prevTwoWords))
						||
						(prevOneWord.equals("of") && ofWord.contains(prevTwoWords))
						||
						(prevOneWord.equals("to") && toWord.contains(prevTwoWords))
						||
						(prevOneWord.equals("with") && withWord.contains(prevTwoWords))
						)
					if(s.assignment(T) != td.GetTagIdx("BR"))
						return true;
			}


			return false;
		}
	}

	public static class Pronoun extends Constraint{

		public Pronoun(){
			name = "Pronoun";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T-1 >= 0 && 
					s.assignment(T) == td.GetTagIdx("O") &&
					s.assignment(T-1) == td.GetTagIdx("BR")){
				String word = td.IdxToWord(s.wordseq[T -1]);

				if(word.equals("his") || word.equals("her") ||
						word.equals("your"))
					return true;
			}
			return false;
		}
	}

	public static class Treatment extends Constraint{

		public Treatment(){
			name = "Treatment";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String currentWord = td.IdxToWord(s.wordseq[T]);

			if(currentWord.equals("treatment") && T-1 >= 0) 
				if(s.assignment(T-1) != td.GetTagIdx("IR") && 
				s.assignment(T-1) != td.GetTagIdx("BR"))
					return true;



			return false;
		}
	}

	public static class TreatmentFromConcept extends Constraint{

		public TreatmentFromConcept(){
			name = "TreatmentFromConcept";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String currentWord = td.IdxToWord(s.wordseq[T]);
			//				String prevTwoWords = td.IdxToWord(s.wordseq[T-2]);

			if(currentWord.equals("treatment") && T-1 >= 0) 
				if(s.assignment(T-1) != td.GetTagIdx("IR") && 
				s.assignment(T-1) != td.GetTagIdx("BR"))
					return true;



			return false;
		}
	}

	public static class NearbyMedication extends Constraint{

		public NearbyMedication(){
			name = "NearbyMedication";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			boolean medNearby = false;
			for(int index =0; index < s.isMed.length; index ++){
				if(td.IdxToIsMed(s.isMed[index]).equals("isMed")){
					medNearby = true;
					break;
				}
			}

			if(!medNearby) 
				if(td.ContainsTagIdx("IR")){
					if(s.assignment(T) == td.GetTagIdx("IR") || 
							s.assignment(T) == td.GetTagIdx("BR"))
						return true;
				}else
					return true;


			return false;
		}
	}

	public static class MedFor extends Constraint{

		public MedFor(){
			name = "MedFor";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T-2 >= 0 ){
				if(td.IdxToIsMed(s.isMed[T-2]).equals("isMed")){
					String prevWord = td.IdxToWord(s.wordseq[T-1]);

					if(prevWord.equals("for"))
						if(td.IdxToTag(s.assignment(T)).equals("O"))
							return true;
				}
			}



			return false;
		}
	}

	public static class NounContained extends Constraint{
		List<String> tags = Arrays.asList("DT", "CD", "JJ", "RB", "PRP$", "PRP");

		public NounContained(){
			name = "NounContained";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1){
				// check if the previous annotation is a BR and not a noun
				// then the current one cannot be O

				String prevPOS = td.IdxToPos(s.posSeq[T-1]);

				if(tags.contains(prevPOS))
					if((s.assignment(T-1) == td.GetTagIdx("BR") || 
					s.assignment(T-1) == td.GetTagIdx("IR")) &&
					s.assignment(T) == td.GetTagIdx("O"))
						return true;

			}

			return false;
		}
	}


	public static class Length extends Constraint{
		public Length(){
			name="Length";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){

			int T = s.idx;

			if(s.assignment(T) == td.GetTagIdx("O") && T > 0){
				if(s.assignment(T-1) == td.GetTagIdx("BR") && 
						td.IdxToWord(s.wordseq[T-1]).length() <=2)
					return true;
			}
			return false;
		}
	}

	public static class ConceptEndingOf extends Constraint{
		public ConceptEndingOf(){
			name="ConceptEndingOf";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){

			int T = s.idx;

			if(T > 1){
				// check if the previous annotation is a BR and not a noun
				// then the current one cannot be O

				String currentWord = td.IdxToWord(s.wordseq[T]);

				if(currentWord.equals("of"))
					if((s.assignment(T-1) == td.GetTagIdx("BR") || 
					s.assignment(T-1) == td.GetTagIdx("IR")) &&
					s.assignment(T) == td.GetTagIdx("O"))
						return true;

			}


			return false;
		}
	}

	public static class ToPlusNoun extends Constraint{
		public ToPlusNoun(){
			name="ToPlusNoun";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){

			int T = s.idx;

			if(T > 1){
				// check if the previous word is to and the 
				// current word is a noun
				// cases like due to, secondary to, attributed to
				String prevWord = td.IdxToWord(s.wordseq[T-1]);

				if(prevWord.equals("to"))
					if(td.IdxToPos(s.posSeq[T]).equals("NN"))
						if(s.assignment(T) != td.GetTagIdx("BR"))
							return true;

			}


			return false;
		}
	}

	public static class Merged extends Constraint{
		public Merged(){
			name = "Merged";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){

			int T = s.idx;

			if(T > 2){
				// check if the previous word is to and the 
				// current word is a noun
				// cases like due to, secondary to, attributed to
				String prevWord = td.IdxToWord(s.wordseq[T-1]);

				if(prevWord.equals("and") || prevWord.equals("or"))
					if(s.assignment(T-2) == td.GetTagIdx("BR") || 
					s.assignment(T-2) == td.GetTagIdx("IR"))
						if(s.assignment(T) != td.GetTagIdx("BR"))
							return true;

			}

			return false;
		}
	}

	public static class TransitionOnPunctConjVerbs extends Constraint{
		List<String> allowedTransitionOn = Arrays.asList("VBZ", "VBN", "VBP", "VBD", "VB",
				"IN", 
				"CC", "RB", "TO" , "JJR", 
				"PRP", "NNP",
				",", ".", ";", ":");

		public TransitionOnPunctConjVerbs(){
			name = "TransitionOnPunctConjVerbs";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1){
				int prevAssignment = s.assignment(T-1);

				// if the transition was made on the current state
				// make sure that the current element is a verb, conjunction, preposition
				if(prevAssignment == td.GetTagIdx("BR") || 
						prevAssignment == td.GetTagIdx("IR"))
					if(s.assignment(T) == td.GetTagIdx("O")){
						String currentTag = td.IdxToPos(s.posSeq[T]);

						return !allowedTransitionOn.contains(currentTag);
					}
			}

			return false;
		}
	}

	public static class NoTransitionOnNNFW extends Constraint{
		public NoTransitionOnNNFW(){
			name = "NoTransitionOnNNFW";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1){
				int prevAssignment = s.assignment(T-1);
				String currentTag = td.IdxToPos(s.posSeq[T]);

				if(currentTag.equals("NN") || currentTag.equals("FW")){
					if(prevAssignment == td.GetTagIdx("BR") || 
							prevAssignment == td.GetTagIdx("IR"))
						if(td.IdxToPos(s.posSeq[T-1]).equals("NN") ||
								td.IdxToPos(s.posSeq[T-1]).equals("FW"))	
							return (s.assignment(T) == td.GetTagIdx("O"));
				}
			}


			return false;
		}
	}

	public static class NoStartVerb extends Constraint{
		List<String> tagsList = Arrays.asList("VBD", "VBZ", "VB", "VBN");

		public NoStartVerb(){
			name = "NoStartVerb";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String currentTag = td.IdxToPos(s.posSeq[T]);
			if(tagsList.contains(currentTag)){
				if(s.assignment(T) == td.GetTagIdx("BR"))
					return true;
			}

			return false;
		}
	}

	public static class FilterByPOS extends Constraint{
		public FilterByPOS(){
			name = "FilterByPOS";
			constraints.addElement(this);
		}

		List<String> filterByPOS = Arrays.asList("PRP$", "JJ", "CC", "DT",
				"IN", "JJR");

		public boolean isViolated(State s, TaggingDict td){

			int T = s.idx;

			if(T> 0){
				if(s.assignment(T-1) == td.GetTagIdx("BR") || 
						s.assignment(T-1) == td.GetTagIdx("IR"))
					if(s.assignment(T) == td.GetTagIdx("O")){
						String prevPOS = td.IdxToPos(s.posSeq[T-1]);
						if(filterByPOS.contains(prevPOS))
							return true;

					}
			}

			return false;
		}
	}

	public static class NoNNWithoutItsJJ extends Constraint{
		public NoNNWithoutItsJJ(){
			name = "NoNNWithoutItsJJ";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T >0){
				if(s.assignment(T) == td.GetTagIdx("BR")){
					if(s.assignment(T-1) == td.GetTagIdx("O") && 
							td.IdxToPos(s.posSeq[T-1]).equals("JJ"))
						return true;
				}
			}


			return false;
		}

	}

	public static class NoMedAsReason extends Constraint{
		public NoMedAsReason(){
			name = "NoMedAsReason";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(s.assignment(T) == td.GetTagIdx("BR")
					|| s.assignment(T) == td.GetTagIdx("IR")){
				if(td.IdxToIsMed(s.isMed[T]).equals("isMed"))
					return true;
			}



			return false;
		}

	}

	public static class PatientAttribute extends Constraint{
		public PatientAttribute(){
			name = "PatientAttribute";
			constraints.addElement(this);
		}

		List<String> verbForms = Arrays.asList("has", "had", "been",
				"is", "was", "showed");

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T >0){
				String prevTag = td.IdxToPos(s.posSeq[T-1]);

				int loc = T -1;
				boolean foundVerbSubjSeq = false;
				boolean foundVerb = false;
				String seq = "";


				while(loc>0){
					String word = td.IdxToWord(s.wordseq[loc-1]);
					prevTag = td.IdxToPos(s.posSeq[loc-1]);

					if(foundVerb && prevTag.equals("PRP")){
						foundVerbSubjSeq = true;
						seq = word + " " + seq;
						break;
					}

					if(foundVerb && word.equals("patient")){
						foundVerbSubjSeq = true;
						seq = word + " " + seq;
						break;
					}
					if(!prevTag.contains("VB"))
						break;
					else{
						if(!verbForms.contains(word))
							break;

						seq = word + " " + seq;
						foundVerb = true;
					}

					loc --;
				}

				if(foundVerbSubjSeq && s.assignment(T) != td.GetTagIdx("BR")){
					String 	tag = td.IdxToPos(s.posSeq[T]);
					if(tag.startsWith("NN") || tag.startsWith("JJ") || 
							tag.equals("RB")){
						seq = seq + " " + td.IdxToWord(s.wordseq[T]);

						return true;
					}
				}
			}



			return false;
		}

	}


	public static class StartWithHyp extends Constraint{
		public StartWithHyp(){
			name = "StartWithHyp";
			constraints.addElement(this);
		}

		//		List<String> words = Arrays.asList("hy","cy", "my","ly", 
		//				"th", "hyper", "hypo",
		//				"peri", "pero",
		//				 "pha"
		//				);	

		List<String> words = Arrays.asList("zym", "xer", "xen", "xantho", "viscer", "vesic", 
				"ventr", "ven", "vas", "varic", "vagin", "uter", "urin", "ungui", "ula", "ule", 
				"trophy", "tripsy", "trich", "trans", "trache", "tox", "tort", "top", 
				"tony", "tono", "tomy", "tome", "toco", "tic", "thorac", "therm", 
				"tachy", "sy", "syl", "sym", "syn", "sys",
				"super", "supra", "stomy", "sub", "stheno", "steth", "sten", "stasis", 
				"somat", "sial", "scoli", "scler", "schiz", "sarco", "sang", 
				"rrh", "retro", "pyro", "pyo", "ptysis", "ptosis", "psor", "pterygo",
				"psych", "pseud", "pro", "prim", "pneum", "poiesis", "plexy", "pleio",
				"plegia", "plasia", "phobia", "penia", "path", "para", "oma", "odyn", 
				"necro", "narc", "lysis", "leps", "itis", "ismus", "ism", 
				"hyp", "hyper", "emia", "emesis", "ectas", "dys", "dynia", 
				"brady", "brachy", "blast", "asthenia", "atel", "aniso", "ankyl",
				"alg");

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String word = td.IdxToWord(s.wordseq[T]);
			boolean found = false;

			for(String el : words)
				if (word.contains(el)){
					found = true;
					break;
				}

			if(found){
				if(s.assignment(T) != td.GetTagIdx("BR") && 
						s.assignment(T) != td.GetTagIdx("IR"))
					return true;
			}

			return false;
		}

	}

	public static class DailyPRN extends Constraint{
		public DailyPRN(){
			name="DailyPRN";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String word = UtilMethods.removePunctuation(td.IdxToWord(s.wordseq[T]));
			String tag = td.IdxToTag(s.assignment(T));

			// in case the annotation starts on a number
			if(UtilMethods.isNumber(word) && tag.startsWith("B")){
				boolean found = false;
				// check if there is a prn within the next 6 tokens
				for(int index = T + 1; index < T + 8 && index < s.wordseq.length; 
						index ++){
					String wordAtIndex = UtilMethods.removePunctuation(
							td.IdxToWord(s.wordseq[index]));
					if(wordAtIndex.equals("prn")){
						found = true;
						break;
					}
				}

				return !found;
			}


			return false;
		}
	}

	public List<String> verbs = Arrays.asList("VBZ", "VBN", "VBP", "VBD", "VB");

	public static class PersonPossesion extends Constraint{
		public PersonPossesion(){
			name = "PersonPossesion";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T >0){
				boolean foundVerbAndPronoun = false;
				String prevSentence = "";

				int loc = T-1;

				while(loc >=0){
					String prevPos = td.IdxToPos(s.posSeq[loc]);
					if(!verbs.contains(prevPos))
						break;

					prevSentence =  td.IdxToWord(s.wordseq[loc]) + " " +prevSentence ;
					loc --;
				}

				if(loc >=0 & loc != T -1){
					String prevPos = td.IdxToPos(s.posSeq[loc]);
					if(prevPos.equals("PRP") )
						foundVerbAndPronoun = true;
					prevSentence = td.IdxToWord(s.wordseq[loc])+ " " + prevSentence;
				}

				if(foundVerbAndPronoun){
					if(s.assignment(T) != td.GetTagIdx("BR") ){
						String prevPOS = td.IdxToPos(s.posSeq[T]);
						if(prevPOS.equals("JJ") || prevPOS.contains("NN")){
							prevSentence = prevSentence + " " + td.IdxToWord(s.wordseq[T]);
							System.out.println(prevSentence);

							return true;
						}
					}
				}

			}


			return false;
		}

	}

	public static class CannotStartWithInside extends Constraint{
		public CannotStartWithInside(){
			name = "CannotStartWithInside";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T= s.idx;
			int currentAssignment = s.assignment(T);

			if(T>= 1){
				int prevAssignment = s.assignment(T-1);

				if(currentAssignment == td.GetTagIdx("IREASON") && 
						prevAssignment == td.GetTagIdx("O"))
					return true;
			}else{
				if(currentAssignment == td.GetTagIdx("IREASON"))
					return true;
			}

			return false;
		}
	}

	public static class BeginOnMedsOrReasons extends Constraint{
		public BeginOnMedsOrReasons(){
			name = "BeginOnMedsOrReasons";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1){
				int currentAssignment = s.assignment(T);

				// basically have to check whether the current assignment is a BR 
				// then ask for the previous word to be a med or reason with an O tag
				if(currentAssignment == td.GetTagIdx("BREASON")){
					// check if the previous tag is an O
					int prevAssignment = s.assignment(T-1);

					if(prevAssignment == td.GetTagIdx("O")){
						String isMed = td.IdxToIsMed(s.isMed[T-1]);
						String isReason =  td.IdxToReason(s.reasonSeq[T-1]);

						if(!isMed.equals("isMed") && !isReason.equals("isReason"))
							return true;
					}else
						return true;
				}
			}

			return false;
		}
	}

	public static class BeginOnMedsOrReasonsPrediction extends Constraint{
		public BeginOnMedsOrReasonsPrediction(){
			name = "BeginOnMedsOrReasonsPrediction";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1){
				int currentAssignment = s.assignment(T);

				// basically have to check whether the current assignment is a BREASON 
				// then ask for the previous word to be a med or reason with an O tag
				if(currentAssignment == td.GetTagIdx("BREASON")){
					// check if the previous tag is an O
					int prevAssignment = s.assignment(T-1);

					if(prevAssignment == td.GetTagIdx("O")){
						String isMed = td.IdxToIsMed(s.isMed[T-1]);
						int isReason = s.assignment(T-1);

						if(!isMed.equals("isMed") && 
								!(isReason == td.GetTagIdx("BR") ||isReason == td.GetTagIdx("IR")) )
							return true;
					}
				}
			}

			return false;
		}
	}

	public static class MedReasonAsO extends Constraint{
		public MedReasonAsO(){
			name = "MedReasonAsO";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String isMed = td.IdxToIsMed(s.isMed[T]);
			String isReason =  td.IdxToReason(s.reasonSeq[T]);

			if(isMed.equals("isMed") || isReason.equals("isReason")){
				int currentAssignment = s.assignment(T);
				if(currentAssignment != td.GetTagIdx("O"))
					return true;
			}

			return false;
		}
	}

	public static class WithAndMedNearby extends Constraint{
		public WithAndMedNearby(){
			name = "WithAndMedNearby";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T >1){
				String currentWord = td.IdxToWord(s.wordseq[T]);

				if(currentWord.equals("with") && 
						td.IdxToNearbyMed(s.nearbyMed[T]).equals("medNearby"))
					if(s.assignment(T) != td.GetTagIdx("BREASON"))
						return true;
			}


			return false;
		}
	}

	public static class CannotEndOnLastItem extends Constraint{
		public CannotEndOnLastItem(){
			name = "CannotEndOnLastItem";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T +1 >= s.wordseq.length){
				if(s.assignment(T) == td.GetTagIdx("IREASON") ||
						s.assignment(T) == td.GetTagIdx("IREASON"))
					return true;
			}


			return false;
		}
	}

	public static class CheckReasonWithinSentence extends Constraint{
		public CheckReasonWithinSentence(){
			name = "CheckReasonWithinSentence";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(s.assignment(T) == td.GetTagIdx("IREASON") ||
					s.assignment(T) == td.GetTagIdx("BREASON")){
				boolean found = false;

				for(int index = 0; index < s.reasonSeq.length; index++){
					if(td.IdxToReason(s.reasonSeq[index]).equals("isReason")){
						found = true;
						break;
					}
				}

				if(!found)
					return true;
			}


			return false;
		}
	}

	public static class CheckReasonWithinSentencePrediction extends Constraint{
		public CheckReasonWithinSentencePrediction(){
			name = "CheckReasonWithinSentencePrediction";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(s.assignment(T) == td.GetTagIdx("IREASON") ||
					s.assignment(T) == td.GetTagIdx("IREASON")){
				boolean found = false;

				for(int index = 0; index < T; index++){
					if(s.assignment(index) == td.GetTagIdx("BR") ||
							s.assignment(index) == td.GetTagIdx("IR")){
						found = true;
						break;
					}
				}

				if(!found)
					return true;
			}


			return false;
		}
	}
	public static class PRNPlusReasonPrediction extends Constraint{
		public PRNPlusReasonPrediction(){
			name = "PRNPlusReasonPrediction";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1 ){
				String prevWord = td.IdxToWord(s.wordseq[T-1]);

				if(UtilMethods.removePunctuation(prevWord).equals("prn")){
					if(s.assignment(T) != td.GetTagIdx("BR"))
						return true;
				}
			}

			return false;
		}
	}

	/**
	 * basically if the model identified a reason and there is no
	 * context before the reason, then we must start the context here
	 * @author ab
	 *
	 */
	public static class ReasonInduceContext extends Constraint{
		public ReasonInduceContext(){
			name = "ReasonInduceContext";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1 ){

				// check the previous assignment 
				if(s.assignment(T-1) == td.GetTagIdx("BR") || 
						s.assignment(T-1) == td.GetTagIdx("IR")){
					// check the current assignment
					if(s.assignment(T) != td.GetTagIdx("BR") && 
							s.assignment(T) != td.GetTagIdx("IR")){
						// check to see if there is context to the left
						boolean foundContext = false;

						for(int index =0; index < T-2; index ++){
							if(s.assignment(index) == td.GetTagIdx("IREASON") || 
									s.assignment(index) == td.GetTagIdx("IREASON")){
								foundContext = true;
								break;
							}
						}

						if(!foundContext && 
								!(s.assignment(T) == td.GetTagIdx("IREASON") || 
								s.assignment(T) == td.GetTagIdx("IREASON")))
							return true;
					}
				}
			}

			return false;
		}
	}

	boolean isReasonOnLine(State s, TaggingDict td, int offset){
		boolean found = false;

		for(int index = offset; index < s.reasonSeq.length; index++){
			if(td.IdxToReason(s.reasonSeq[index]).equals("isReason")){
				found = true;
				break;
			}
		}

		return found;
	}

	public static class NoContextWithoutConcepts extends Constraint{
		public NoContextWithoutConcepts(){
			name = "NoContextWithoutConcepts";
			constraints.addElement(this);
		}

		// check whether there is a context and no previous concept
		// either medication or medical concepts
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(s.assignment(T) != td.GetTagIdx("O")){
				if(T == 0)
					return true;
				// check whether there was no previous medication or reason
				if(!isMedToTheLeft(s, td, T) && !isReasonToTheLeft(s, td, T))
					return true;
			}

			return false;
		}
	}


	public static class NoRelationWithoutReason extends Constraint{
		public NoRelationWithoutReason(){
			name = "NoRelationWithoutReason";
			constraints.addElement(this);
		}

		// check whether there is a context and no previous concept
		// either medication or medical concepts
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			// if the current assignment is an other
			if(T >= 1 && s.assignment(T) == td.GetTagIdx("O")){
				// if the previous assignment was a reason
				if(td.IdxToTag(s.assignment(T-1)).endsWith("REASON") )
					// if the current state is not a med and was not labeled as reason either
					if(!td.IdxToIsMed(s.isMed[T]).equals("isMed"))
						return true;
			}

			// if the current assignment is a BREASON
			if(T >= 1 && s.assignment(T) == td.GetTagIdx("BREASON")){
				if(!td.IdxToTag(s.assignment(T-1)).endsWith("R") &&
						!td.IdxToIsMed(s.isMed[T-1]).equals("isMed") )
					return true;
			}

			//			 if the current assignment is a IREASON and prev is a med concept
			if(T>=1 && td.IdxToTag(s.assignment(T)).endsWith("REASON") &&
					td.IdxToTag(s.assignment(T-1)).equals("O"))
				if(!td.IdxToIsMed(s.isMed[T-1]).equals("isMed"))
					return true;

			return false;
		}
	}

	public static class NoContextWithoutMedAndReason extends Constraint{
		public NoContextWithoutMedAndReason(){
			name = "NoContextWithoutMedAndReason";
			constraints.addElement(this);
		}

		// check whether there is a context and no previous 
		// med followed by reason or reason followed by med
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			// we are dealing with a context annotation
			if(s.assignment(T) != td.GetTagIdx("O")){
				int i = -1;
				boolean found = false;

				for(i = T-1; i >=0; i --)
					if(s.assignment(i) == td.GetTagIdx("O")){
						found = true;
						break;
					}

				if(found){
					if(!(td.IdxToIsMed(s.isMed[i]).equals("isMed") ||
							td.IdxToReason(s.reasonSeq[i]).equals("isReason")))
						return true;

					found = false;
					int indexSecond = 0;

					for(indexSecond = T+1; indexSecond < s.wordseq.length && 
							indexSecond < T+15; indexSecond ++)
						if(td.IdxToIsMed(s.isMed[indexSecond]).equals("isMed") ||
								td.IdxToReason(s.reasonSeq[indexSecond]).equals("isReason")){
							found = true;
							break;
						}


					if(found){
						if((td.IdxToIsMed(s.isMed[i]).equals("isMed") &&
								td.IdxToReason(s.reasonSeq[indexSecond]).equals("isReason"))
								||
								(td.IdxToIsMed(s.isMed[indexSecond]).equals("isMed") &&
										td.IdxToReason(s.reasonSeq[i]).equals("isReason")))
							return false;

					}

					// otherwise constraint is violated
					return true;
				}
				// if we did not find a previous O tag
				else
					return true;

			}

			return false;

		}

	}

	public static class NoContextWithoutConceptPrediction extends Constraint{
		public NoContextWithoutConceptPrediction(){
			name = "NoContextWithoutConceptPrediction";
			constraints.addElement(this);
		}

		// check whether there is a context and no previous concept
		// either medication or medical concepts
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(s.assignment(T) == td.GetTagIdx("IREASON") || 
					s.assignment(T) == td.GetTagIdx("IREASON")){
				if(T == 0)
					return true;
				// check whether there was no previous medication or reason
				if(!isMedToTheLeft(s, td, T) && 
						!isReasonPredictionToTheLeft(s, td, T))
					return true;
			}

			return false;
		}
	}

	public static class RelationPatterns extends Constraint{
		public RelationPatterns(){
			name = "RelationPatterns";
			constraints.addElement(this);
		}

		List<String> patterns = Arrays.asList(
				"for treatment of"
				,"for relief of"
				,"for treatment of"
				,"for the treatment of"
				,"for the control of"
				,"for relief of"
				,"indicated for"
				,"as treatment of"
				,"for prevention of"
				,"because of"
				,"decrease of"
				,"increase of"

				,"treatment with"
				,"treated with"
				,"managed on"
				,"risk of"
				,"as needed for"
				,"started on"
				,"continued on"
				,"controlled with"
				);

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 0){
				String currentWord = td.IdxToWord(s.wordseq[T]);
				if(currentWord.equals("for") || currentWord.equals("because") ||
						currentWord.equals("as")){
					if(td.IdxToIsMed(s.isMed[T-1]).equals("isMed") && 
							isReasonOnLine(s, td, T))
						if(s.assignment(T) != td.GetTagIdx("BREASON")){

							return true;
						}
				}

				if(T+1 < s.wordseq.length ){
					String nextWord = td.IdxToWord(s.wordseq[T+1]);

					if(patterns.contains(UtilMethods.mergeStrings(currentWord, nextWord)))
						if(s.assignment(T) != td.GetTagIdx("BREASON") ||
						s.assignment(T) != td.GetTagIdx("IREASON"))
							return true;

					if(T+2 < s.wordseq.length){
						String secondWord = td.IdxToWord(s.wordseq[T+1]);
						if(patterns.contains(UtilMethods.mergeStrings(currentWord, 
								UtilMethods.mergeStrings(nextWord, secondWord))))
							if(s.assignment(T) != td.GetTagIdx("BREASON") ||
							s.assignment(T) != td.GetTagIdx("IREASON"))
								return true;
					}
				}
			}


			return false;
		}
	}

	public static class RelationPatternsPrediction extends Constraint{
		public RelationPatternsPrediction(){
			name = "RelationPatternsPrediction";
			constraints.addElement(this);
		}

		List<String> patterns = Arrays.asList("for treatment of"
				,"for relief of"
				,"for treatment of"
				,"for the treatment of"
				,"for the control of"
				,"for relief of"
				,"indicated for"
				,"as treatment of"
				,"for prevention of"
				,"because of"
				);

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 0){
				String currentWord = td.IdxToWord(s.wordseq[T]);
				if(currentWord.equals("for") || currentWord.equals("because") ||
						currentWord.equals("as")){
					if(td.IdxToIsMed(s.isMed[T-1]).equals("isMed") && 
							isReasonOnLine(s, td, T))
						if(s.assignment(T) != td.GetTagIdx("BREASON") && 
						s.assignment(T) != td.GetTagIdx("IREASON")){
							return true;
						}
				}
			}


			return false;
		}
	}

	public static class ForRelation extends Constraint{
		public ForRelation(){
			name = "ForRelation";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			String currentWord = td.IdxToWord(s.wordseq[T]);

			if(s.assignment(T) == td.GetTagIdx("BREASON") && 
					T>= 1 && currentWord.equals("for")){
				if(!td.IdxToIsMed(s.isMed[T-1]).equals("isMed"))
					return true;

			}

			return false;
		}

	}


	public static class PrepBeforeReason extends Constraint{
		public PrepBeforeReason(){
			name = "PrepBeforeReason";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T+1 < s.wordseq.length){
				String currentWord = td.IdxToWord(s.wordseq[T]);

				if(currentWord.equals("for")){

					if(td.IdxToReason(s.reasonSeq[T+1]).equals("isReason") && 
							isMedToTheLeft(s, td, T)){
						if(s.assignment(T) == td.GetTagIdx("O"))
							return true;
					}else{
						if(T+2 < s.wordseq.length){
							if(td.IdxToReason(s.reasonSeq[T+2]).equals("isReason") && 
									isMedToTheLeft(s, td, T)){
								if(s.assignment(T) == td.GetTagIdx("O"))
									return true;
							}
						}
					}
				}
			}

			return false;

		}
	}


	boolean isMedToTheLeft(State s, TaggingDict td, int offset){
		boolean found = false;

		for(int index =0; index < offset; index ++){
			if(td.IdxToIsMed(s.isMed[index]).equals("isMed")){
				found=true;
				break;
			}
		}

		return found;
	}

	boolean isReasonToTheLeft(State s, TaggingDict td, int offset){
		boolean found = false;

		for(int index =0; index < offset; index ++){
			if(td.IdxToReason(s.reasonSeq[index]).equals("isReason")){
				found=true;
				break;
			}
		}

		return found;
	}

	boolean isReasonPredictionToTheLeft(State s, TaggingDict td, int offset){
		boolean found = false;

		for(int index =0; index < offset; index ++){
			if((s.assignment(index) == td.GetTagIdx("BR")) || 
					(s.assignment(index) == td.GetTagIdx("IR"))){
				found=true;
				break;
			}
		}

		return found;
	}

	public static class FirstReasonOcc extends Constraint{
		public FirstReasonOcc(){
			name = "FirstReasonOcc";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 0){
				if(td.IdxToReason(s.reasonSeq[T-1]).equals("isReason")){
					// check whether there is a med occ to the left
					if(!isMedToTheLeft(s, td, T-1) &&
							isMedToTheLeft(s, td, s.wordseq.length)){
						if(s.assignment(T) != td.GetTagIdx("BREASON") &&
								!td.IdxToReason(s.reasonSeq[T]).equals("isReason") &&
								!td.IdxToIsMed(s.isMed[T]).equals("isMed")){
							//							String sentence = "";
							//
							//														for(int index = 0; index < s.wordseq.length; index ++)
							//															sentence = sentence + " " + td.IdxToWord(s.wordseq[index]);
							//														System.out.println(sentence);

							return true;

						}
					}
				}
			}

			return false;
		}
	}

	public static class FirstReasonOccPrediction extends Constraint{
		public FirstReasonOccPrediction(){
			name = "FirstReasonOccPrediction";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 0){
				if( s.assignment(T-1) == td.GetTagIdx("BR") || 
						s.assignment(T-1) == td.GetTagIdx("IR")){
					// check whether there is a med occ to the left
					if(!isMedToTheLeft(s, td, T-1) &&
							isMedToTheLeft(s, td, s.wordseq.length)){
						if(s.assignment(T) != td.GetTagIdx("BREASON") &&
								!(s.assignment(T) == td.GetTagIdx("BR") || 
								s.assignment(T) == td.GetTagIdx("IR")) &&
								!td.IdxToIsMed(s.isMed[T]).equals("isMed")){

							return true;

						}
					}
				}
			}

			return false;
		}
	}

	public static class PunctuationAfterReason extends Constraint{
		public PunctuationAfterReason(){
			name = "PunctuationAfterReason";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T>1 ){
				String isReason = td.IdxToReason(s.reasonSeq[T-1]);

				if(isReason.equals("isReason")){
					if(UtilMethods.isPunctuation(td.IdxToWord(s.wordseq[T]))){

						if(s.assignment(T) != td.GetTagIdx("BREASON")){
							//						String sentence = "";
							//
							//													for(int index = 0; index < s.wordseq.length; index ++)
							//														sentence = sentence + " " + td.IdxToWord(s.wordseq[index]);
							//													System.out.println(sentence);
							return true;
						}
					}else if(td.IdxToWord(s.wordseq[T-1]).endsWith(".")){
						if(s.assignment(T) != td.GetTagIdx("BREASON")){
							//						String sentence = "";
							//
							//													for(int index = 0; index < s.wordseq.length; index ++)
							//														sentence = sentence + " " + td.IdxToWord(s.wordseq[index]);
							//													System.out.println(sentence);
							return true;
						}
					}
				}

			}

			return false;


		}
	}

	public static class PunctuationAfterReasonPrediction extends Constraint{
		public PunctuationAfterReasonPrediction(){
			name = "PunctuationAfterReasonPrediction";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T>1 ){
				int isReason = s.assignment(T-1);

				if(isReason == td.GetTagIdx("BR") || isReason == td.GetTagIdx("IR")){
					if(UtilMethods.isPunctuation(td.IdxToWord(s.wordseq[T]))){

						if(s.assignment(T) != td.GetTagIdx("BREASON")){
							return true;
						}
					}else if(td.IdxToWord(s.wordseq[T-1]).endsWith(".")){
						if(s.assignment(T) != td.GetTagIdx("BREASON")){
							return true;
						}
					}
				}

			}

			return false;


		}
	}


	/*
	 * MEDICATION CONSTRAINTS
	 */

	public static class NoAnnotationStartOnPunctuation extends Constraint{
		public NoAnnotationStartOnPunctuation(){
			name = "NoAnnotationStartOnPunctuation";
			constraints.addElement(this);
		}

		/**
		 * Check if the current assignment is a medication start
		 * and the word is a punctuation sign
		 */
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String word = td.IdxToWord(s.wordseq[T]);

			if(UtilMethods.isPunctuation(word)){

				if(td.IdxToTag(s.assignment(T)).startsWith("B") )
					return true;


				if(((T>=1 && s.assignment(T-1) == td.GetTagIdx("O")) || T==0) 
						&&
						td.IdxToTag(s.assignment(T)).startsWith("I"))
					return true;

			}


			return false;
		}


	}

	public static class NoPunctuationMedications extends Constraint{

		public NoPunctuationMedications(){
			name = "NoPunctuationMedications";
			constraints.addElement(this);
		}

		/**
		 * Check if the current assignment is a medication start
		 * and the word is a punctuation sign
		 */
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T>1){
				if(UtilMethods.isPunctuation(td.IdxToWord(s.wordseq[T]))){
					if(s.assignment(T-1) == td.GetTagIdx("O") && 
							(s.assignment(T) == td.GetTagIdx("BM") ||
							s.assignment(T) == td.GetTagIdx("IM")))
						return true;
				}
			}

			return false;
		}
	}

	public static class NoAdjMedications extends Constraint{
		List<String> posNotAsMedication = Arrays.asList("JJ", "RB");

		public NoAdjMedications(){
			name = "NoAdjMedications";
			constraints.addElement(this);
		}

		/**
		 * Check if the current assignment is a medication start
		 * and the word is a punctuation sign
		 */
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T>1){
				if(posNotAsMedication.contains(td.IdxToPos(s.posSeq[T-1]))){
					if(s.assignment(T) == td.GetTagIdx("O") && 
							s.assignment(T-1) == td.GetTagIdx("BM"))
						return true;
				}
			}

			return false;
		}
	}

	public static class MedMinLength extends Constraint{

		public MedMinLength(){
			name = "MedMinLength";
			constraints.addElement(this);
		}

		/**
		 * Check if the current assignment is a medication start
		 * and the word is a punctuation sign
		 */
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T>1){
				if(s.assignment(T) == td.GetTagIdx("O") && 
						s.assignment(T-1) == td.GetTagIdx("BM") &&
						td.IdxToWord(s.wordseq[T-1]).length() <= 2)
					return true;
			}

			return false;
		}
	}

	public static class HomeAnnotation extends Constraint{

		public HomeAnnotation(){
			name = "HomeAnnotation";
			constraints.addElement(this);
		}

		/**
		 * Check if the current assignment is a medication start
		 * and the word is a punctuation sign
		 */
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T>1){
				if(s.assignment(T) == td.GetTagIdx("O") && 
						s.assignment(T-1) == td.GetTagIdx("BM") &&
						td.IdxToWord(s.wordseq[T-1]).equals("home"))
					return true;
			}

			return false;
		}
	}

	public static class MinLength extends Constraint{

		public MinLength(){
			name = "MinLength";
			constraints.addElement(this);
		}

		/**
		 * Check if the current assignment is a medication start
		 * and the word is a punctuation sign
		 */
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T>1)
				if(s.assignment(T) == td.GetTagIdx("O") && 
				s.assignment(T-1) == td.GetTagIdx("BR") &&
				td.IdxToWord(s.wordseq[T-1]).length() <= 2)
					return true;


			return false;
		}
	}

	public static class NoLabMedications extends Constraint{

		List<String> measureUnit = Arrays.asList("mg", "ml", "g", "sprays", 
				"units", "tablets", "mcg", "po", "prn", "qd");

		public NoLabMedications(){
			name = "NoLabMedications";
			constraints.addElement(this);
		}

		/**
		 *  check if there is a med annotation followed by a number 
		 *  without a measure unit
		 *  we are basically trying to identify the cases when the 
		 *  lab results are flagged as medications as the lab results
		 *  are followed by a number but not by measure units
		 */
		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T + 1 < s.wordseq.length){
				String nextWord = td.IdxToWord(s.wordseq[T+1]);

				if(s.assignment(T) == td.GetTagIdx("BM") || 
						s.assignment(T) == td.GetTagIdx("IM")){
					// case "potassium 1.5"
					if(UtilMethods.isNumber(UtilMethods.removePunctuation(nextWord))){
						if ( T + 2 < s.wordseq.length){
							String potentialUnitMeasure = td.IdxToWord(s.wordseq[T+2]).toLowerCase();
							potentialUnitMeasure = 
									UtilMethods.removePunctuation(potentialUnitMeasure);

							if(!measureUnit.contains(potentialUnitMeasure))
								return true;	
						}else 
							return true;

						// case potassium of 1.5
					}else if(nextWord.equals("of"))
						return true;
					else if(nextWord.equals("is")){
						// case potassium is 1.5
						if ( T + 2 < s.wordseq.length){
							String potentialUnitMeasure = 
									td.IdxToWord(s.wordseq[T+2]).toLowerCase();

							if(UtilMethods.isNumber(
									UtilMethods.removePunctuation(potentialUnitMeasure)))
								return true;
						}
					}
				}
			}

			return false;
		}
	}


	/************************
	 *  Constraints for the i2b2 relations corpus
	 ************************
	 */

	public static class NoChangeInLabelThroughoutSequence extends Constraint{
		public NoChangeInLabelThroughoutSequence(){
			name = "NoChangeInLabelThroughoutSequence";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 2){
				String currentAssignment = td.IdxToTag(s.assignment(T));
				String prevAssignment = td.IdxToTag(s.assignment(T-1));
				String prev2Assignment = td.IdxToTag(s.assignment(T-2));

				// if current and previous assignments are not O
				// then we check that they belong to the same label class
				if(!currentAssignment.equals("O") && !prevAssignment.equals("O") &&
						!prev2Assignment.equals("O")){
					currentAssignment = currentAssignment.substring(1);
					prevAssignment = prevAssignment.substring(1);

					if(!currentAssignment.equals(prevAssignment))
						return true;
				}
			}

			return false;
		}
	}


	public static class OnlyOneBeginning extends Constraint{
		public OnlyOneBeginning(){
			name = "OnlyOneBeginning";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 2){
				String firstAssign = td.IdxToTag(s.assignment(T-2));
				String currentAssignment = td.IdxToTag(s.assignment(T));
				String prevAssignment = td.IdxToTag(s.assignment(T-1));

				// if current and previous assignments are not O
				// then we check that they belong to the same label class
				if(!currentAssignment.equals("O") && !prevAssignment.equals("O")){
					currentAssignment = currentAssignment.substring(0,1);
					prevAssignment = prevAssignment.substring(0,1);

					if(firstAssign.equals("O") && 
							!(currentAssignment.equals("I") && prevAssignment.endsWith("B")))
						return true;

					if(!currentAssignment.equals("I") || ! prevAssignment.equals("I"))
						return true;

				}
			}

			return false;
		}
	}

	public static class CannotStartWithInsideGeneric extends Constraint{
		public CannotStartWithInsideGeneric(){
			name = "CannotStartWithInsideGeneric";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T= s.idx;
			String currentAssignment = td.IdxToTag(s.assignment(T));

			if(T> 1){
				String prevAssignment = td.IdxToTag(s.assignment(T-1));

				if(currentAssignment.startsWith("I") && 
						prevAssignment.equals("O"))
					return true;
			}else{
				if(currentAssignment.startsWith("I"))
					return true;
			}

			return false;
		}
	}

	public static class StartAtConcept extends Constraint{
		public StartAtConcept(){
			name = "StartAtConcept";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String currentAssignment = td.IdxToTag(s.assignment(T));

			// basically have to check whether the current assignment is a begin label 
			// then ask for the previous word to be a concept with an O tag
			if(currentAssignment.startsWith("B")){
				// when the relation starts at the beginning of a sentence
				if(T == 0)
					return true;

				// check if the previous tag is an O
				int prevAssignment = s.assignment(T-1);

				if(prevAssignment == td.GetTagIdx("O")){
					String isConcept = td.IdxToIsMed(s.isMed[T-1]);
					String isReason = td.IdxToReason(s.reasonSeq[T-1]);

					if(isConcept.equals("isMed") || isReason.equals("isReason"))
						return false;
					else
						return true;

				}else
					return true;
			}


			return false;
		}
	}

	public static class StartAtConceptBoth extends Constraint{
		public StartAtConceptBoth(){
			name = "StartAtConceptBoth";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String currentAssignment = td.IdxToTag(s.assignment(T));

			// basically have to check whether the current assignment is a begin label 
			// then ask for the previous word to be a concept with an O tag
			if(currentAssignment.startsWith("BREASON")){
				// when the relation starts at the beginning of a sentence
				if(T == 0)
					return true;

				// check if the previous tag is an O or reason NE
				String prevAssignment = td.IdxToTag(s.assignment(T-1));

				if(prevAssignment.equals("O")){
					String isConcept = td.IdxToIsMed(s.isMed[T-1]);

					if(isConcept.equals("isMed") )
						return false;
					else
						return true;

				}else if(prevAssignment.endsWith("R"))
					return false;
				else 
					return true;
			}


			return false;
		}
	}

	public static class EndAtConcept extends Constraint{
		public EndAtConcept(){
			name = "EndAtConcept";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T >= 1){
				String currentAssignment = td.IdxToTag(s.assignment(T));

				// basically have to check whether the current assignment is an O label 
				// if the prev label is a B or I
				// then ask for the current word to be a concept
				if(currentAssignment.equals("O")){
					// check if the previous tag is an O
					String prevAssignment = td.IdxToTag(s.assignment(T-1));

					if(prevAssignment.startsWith("B") || 
							prevAssignment.startsWith("I")){
						String isConcept = td.IdxToIsMed(s.isMed[T]);
						String isReason = td.IdxToReason(s.reasonSeq[T]);

						if(isConcept.equals("isMed") || isReason.equals("isReason"))
							return false;
						else
							return true;

					} 
				}
			}

			return false;
		}
	}

	public static class EndAtConceptBoth extends Constraint{
		public EndAtConceptBoth(){
			name = "EndAtConceptBoth";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T >= 1){
				String currentAssignment = td.IdxToTag(s.assignment(T));

				// basically have to check whether the current assignment is an O label 
				// if the prev label is a B or I
				// then ask for the current word to be a concept
				if(currentAssignment.equals("O") || currentAssignment.endsWith("R")){
					// check if the previous tag is an O
					String prevAssignment = td.IdxToTag(s.assignment(T-1));

					if(prevAssignment.endsWith("REASON")){
						String isConcept = td.IdxToIsMed(s.isMed[T]);
						String isReason = td.IdxToTag(s.assignment(T));

						if(isConcept.equals("isMed") || isReason.endsWith("R"))
							return false;
						else
							return true;

					} 
				}
			}

			return false;
		}
	}

	public static class BeginOnConcept extends Constraint{
		public BeginOnConcept(){
			name = "BeginOnConcept";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1){
				int currentAssignment = s.assignment(T);

				// basically have to check whether the current assignment is a BR 
				// then ask for the previous word to be a med or reason with an appropriate tag
				if(currentAssignment == td.GetTagIdx("BREASON")){
					// check if the previous tag is an O
					int prevAssignment = s.assignment(T-1);

					if(prevAssignment == td.GetTagIdx("O"))
						return false;
				}
			}

			return false;
		}
	}

	public static class EndOnConcept extends Constraint{
		public EndOnConcept(){
			name = "EndOnConcept";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1){
				int currentAssignment = s.assignment(T);
				int prevAssignment = s.assignment(T-1);

				// basically have to check whether the current assignment is a O
				// and the previous assignment is a BREASON or IREASON
				// then ask for the previous word to be a med or reason with an O tag
				if(currentAssignment == td.GetTagIdx("O") &&
						(prevAssignment == td.GetTagIdx("IREASON") ||
						prevAssignment == td.GetTagIdx("BREASON"))){
					return false;
				}
			}

			return false;
		}
	}

	public static class NoNumberRelation extends Constraint{
		public NoNumberRelation(){
			name = "NoNumberRelation";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T >= 1){
				int currentAssignment = s.assignment(T);
				int prevAssignment = s.assignment(T-1);

				// basically check if the prev tag is a breason and the 
				// prev word is a number
				if(currentAssignment == td.GetTagIdx("O") &&
						prevAssignment == td.GetTagIdx("BREASON")){
					String prevWord = td.IdxToWord(s.wordseq[T-1]);

					if( UtilMethods.isNumber(prevWord))
						return true;

				}
			}

			return false;
		}
	}

	public static class NoNumberQuantRelation extends Constraint{
		public NoNumberQuantRelation(){
			name = "NoNumberQuantRelation";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;
			int currentAssignment = s.assignment(T);

			if(T >= 2 && currentAssignment == td.GetTagIdx("O") ){
				int prevAssignment = s.assignment(T-1);
				int prev2Assignment = s.assignment(T-2);

				// basically check if the prev tag is a breason and the 
				// prev word is a number
				if(prevAssignment == td.GetTagIdx("IREASON") &&
						prev2Assignment == td.GetTagIdx("BREASON")){
					String prevWord = UtilMethods.removePunctuation(
							td.IdxToWord(s.wordseq[T-2]));

					if( UtilMethods.isNumber(prevWord))
						return true;

				}

				if(T >= 3){
					int prev3Assignment = s.assignment(T-3);

					if(prevAssignment == td.GetTagIdx("IREASON") &&
							prev2Assignment == td.GetTagIdx("IREASON") && 
							prev3Assignment == td.GetTagIdx("BREASON")){
						String prevWord = UtilMethods.removePunctuation(
								td.IdxToWord(s.wordseq[T-3]));

						if( UtilMethods.isNumber(prevWord))
							return true;
					}

					if(T >= 4){
						int prev4Assignment = s.assignment(T-4);

						if(prevAssignment == td.GetTagIdx("IREASON") &&
								prev2Assignment == td.GetTagIdx("IREASON") && 
								prev3Assignment == td.GetTagIdx("IREASON") &&
								prev4Assignment == td.GetTagIdx("IREASON")){
							String prevWord = UtilMethods.removePunctuation(
									td.IdxToWord(s.wordseq[T-4]));

							if( UtilMethods.isNumber(prevWord))
								return true;
						}
					}
				}


			}


			return false;
		}
	}

	public static class IncludeEntirePOS extends Constraint{
		public IncludeEntirePOS(){
			name = "IncludeEntirePOS";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T > 1){
				int currentAssignment = s.assignment(T);
				int prevAssignment = s.assignment(T-1);


				if(currentAssignment == td.GetTagIdx("O") &&
						(prevAssignment == td.GetTagIdx("IREASON") ||
						prevAssignment == td.GetTagIdx("BREASON"))){
					String pos = td.IdxToPos(s.posSeq[T]);
					if(pos.startsWith("I"))
						return true;

				}
			}

			return false;
		}
	}

	public static class NoComma extends Constraint{
		public NoComma(){
			name="NoComma";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String word = td.IdxToWord(s.wordseq[T]);

			if(word.equals(".")  &&
					s.assignment(T) == td.GetTagIdx("BREASON"))
				return true;


			return false;
		}
	}

	public static class NoAnd extends Constraint{
		public NoAnd(){
			name="NoAnd";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String word = td.IdxToWord(s.wordseq[T]);

			if(word.equals("and")  &&
					s.assignment(T) == td.GetTagIdx("BREASON"))
				return true;


			return false;
		}
	}

	public static class NoDetReason extends Constraint{
		public NoDetReason(){
			name="NoDetReason";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T-1 >= 0){
				String pos = td.IdxToPos(s.posSeq[T-1]);

				// current other assignment and prev r assignment with pos DET
				if(!td.IdxToTag(s.assignment(T)).endsWith("R") && 
						td.IdxToTag(s.assignment(T-1)).endsWith("R") && 
						(pos.equals("DT") || pos.equals("JJR")))
					return true;
			}

			return false;
		}
	}

	public static class NoPossesiveReason extends Constraint{
		public NoPossesiveReason(){
			name="NoPossesiveReason";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T-1 >= 0){
				String pos = td.IdxToPos(s.posSeq[T-1]);

				if(!td.IdxToTag(s.assignment(T)).endsWith("R") && 
						td.IdxToTag(s.assignment(T-1)).endsWith("R") && 
						(pos.equals("PRP$") || pos.equals("JJ") || pos.equals("RB")) )
					return true;
			}

			return false;
		}
	}

	public static class PenalizeStopWords extends Constraint{
		public PenalizeStopWords(){
			name="PenalizeStopWords";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String word = td.IdxToWord(s.wordseq[T]);
			String label = td.IdxToTag(s.assignment(T));

			if(label.endsWith("REASON") && 
					(word.contains("stop") || word.contains("discontin")) )
				return true;

			return false;
		}
	}

	public static class NoEndOnPunctuation extends Constraint{
		public NoEndOnPunctuation(){
			name="NoEndOnPunctuation";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T-1 >= 0){
				String prevword = td.IdxToWord(s.wordseq[T]);

				if(prevword.equals(".")  &&
						s.assignment(T-1) != td.GetTagIdx("O") )
					if(s.assignment(T) == td.GetTagIdx("O"))
						return true;
			}

			return false;
		}
	}

	public static class ReasonAnnotationWithoutContext extends Constraint{
		public ReasonAnnotationWithoutContext(){
			name="ReasonAnnotationWithoutContext";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			if(T-1 >= 0){
				String currentAnnotation = td.IdxToTag(s.assignment(T));
				String prevAnnotation = td.IdxToTag(s.assignment(T-1));

				// if the current annotation is O and we had a reason
				// then we check for a context
				if( currentAnnotation.equals("O") && 
						prevAnnotation.endsWith("R")){
					for(int index = T-1; index >= 0; index --){
						String assignment = td.IdxToTag(s.assignment(index));
						//						System.out.print(assignment + " ");
						// we found an O label before the context
						if(assignment.equals("O"))
							return true;

						if(assignment.endsWith("REASON"))
							return false;

					}

					// we did not find a context
					return true;
				}
			}

			return false;
		}
	}

	public static class NoDoubleReasons extends Constraint{
		public NoDoubleReasons(){
			name="NoDoubleReasons";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String currentAnnotation = td.IdxToTag(s.assignment(T));
			// if current tag is BR
			// or current tag is IR and prev is a REASON
			if(currentAnnotation.equals("BR") ||
					(currentAnnotation.equals("IR") && T-1 >=0 &&
					td.IdxToTag(s.assignment(T-1)).endsWith("REASON") )){

				for(int index = T-1; index >= 0; index --){
					String assignment = td.IdxToTag(s.assignment(index));
					if(assignment.endsWith("REASON"))
						continue;
					// we should not find two reasons
					if(assignment.endsWith("R"))
						return true;
					if(assignment.equals("O"))
						return false;
				}
			}

			return false;
		}
	}

	public static class NoReasonOutsidePhrase extends Constraint{
		public NoReasonOutsidePhrase(){
			name="NoReasonOutsidePhrase";
			constraints.addElement(this);
		}

		public boolean isViolated(State s, TaggingDict td){
			int T = s.idx;

			String currentAnnotation = td.IdxToTag(s.assignment(T));
			String currentCorseTag = td.IdxToCorsePOS(s.corsePOSSeq[T]);

			if(T-1 >= 0 && currentAnnotation.equals("O")){
				String prevAnnotation = td.IdxToTag(s.assignment(T-1));

				if(prevAnnotation.endsWith("R")){
					if(currentCorseTag.startsWith("I"))
						return true;

				}
			}

			if(currentAnnotation.equals("BR") && currentCorseTag.startsWith("I"))
				return true;

			if(currentAnnotation.equals("IR") && currentCorseTag.startsWith("B"))
				return true;

			return false;
		}
	}
}



