package probabModel.ccm_code.HMMLearningAndInferenceWithConstraints;

import java.util.HashMap;


public class State {
	public double score;
	public int[] wordseq=null;
	public int[] isMed = null;
	public int[] nearbyMed = null;
	public int[] posSeq = null;
	public int[] reasonSeq = null;
	public int[] corsePOSSeq = null;
	
	public HashMap<String, int[]> features = null;
	
	public int state=-1;
	public int idx=0;
	public State prev=null;
	public String[][] allowableTags=null;//this for my extension for "Moises-style" local experts
	public int randomConstraint=-1;
	
	public boolean isComplete()
	{
		return  (idx == wordseq.length - 1);
	}
	
	public int assignment(int index){
		State pointer=this;
		while(pointer.idx!=index)
			pointer=pointer.prev;
		return pointer.state;
	}
	
/*	public void setAllowableTags(Classifier c,double margin,TaggingDict td){
		if(allowableTags!=null)
			return;
		String[] words=new String[this.wordseq.length];
		for(int i=0;i<wordseq.length;i++)
			words[i]=td.IdxToWord(wordseq[i]);
		allowableTags=LearnContextToStatePerceptron.allowableTags(c, words, margin);
	}*/
	
	public int[] toArr(){
		int[] res=new int[idx+1];
		State pointer=this;
		for(int i=idx;i>=0;i--){
			res[i]=pointer.state;
			pointer=pointer.prev;
		}
		return res;
	}
	
	public State(TaggingDict td,double _score,int[] _wordseq,int _state, 
			int[] _isMeds, int[] _nearbyMeds, int[] _pos, int[] _reason, 
			int[] _corsePOSSeq,
			HashMap<String, int[]> _features){
		idx=0;
		wordseq=_wordseq;
		score=_score;
		state=_state;
		isMed = _isMeds;
		nearbyMed = _nearbyMeds;
		posSeq = _pos;
		reasonSeq = _reason;
		corsePOSSeq = _corsePOSSeq;
		features = _features;
		
		if(Math.random()<Constraint.randomConstraintProb)
			randomConstraint=(int)(td.GetTagNum()*Math.random());
	}
	
	public State(State _prev,int _state){
		state=_state;
		idx=_prev.idx+1;
		wordseq=_prev.wordseq;
		isMed = _prev.isMed;
		nearbyMed = _prev.nearbyMed;
		posSeq = _prev.posSeq;
		reasonSeq = _prev.reasonSeq;
		corsePOSSeq = _prev.corsePOSSeq;
		features = _prev.features;
		
		score=_prev.score;
		prev=_prev;
	}
	public State(double _score,int[] _wordseq,int _state, int[] _meds, 
			int[] _nearbyMed, int[] _pos,
			int[] _reasons, int[] _sections,
			HashMap<String, int[]> _features){
		idx=0;
		wordseq=_wordseq;
		score=_score;
		state=_state;
		isMed = _meds;
		nearbyMed = _nearbyMed;
		posSeq = _pos;
		reasonSeq = _reasons;
		corsePOSSeq = _sections;
		features = _features;
	}
		
	
	public State(TaggingDict td, State _prev,int _state){
		state=_state;
		idx=_prev.idx+1;
		wordseq=_prev.wordseq;
		isMed = _prev.isMed;
		nearbyMed = _prev.nearbyMed;
		posSeq = _prev.posSeq;
		reasonSeq = _prev.reasonSeq;
		corsePOSSeq = _prev.corsePOSSeq;
		features = _prev.features;
		
		allowableTags=_prev.allowableTags;
		score=_prev.score;
		prev=_prev;
		if(Math.random()<Constraint.randomConstraintProb)
			randomConstraint=(int)(td.GetTagNum()*Math.random());
	}

	public void print(){
		System.out.println("====> idx: " + idx);

		State pointer=this;
		String res="";
		for (int i=0; i< idx; i++){
			res=pointer.state+" "+res;
			pointer=pointer.prev;
		}
		System.out.println(res);

		for (int i=0; i< wordseq.length; i++){
			System.out.print(" " + wordseq[i]);
		}
		System.out.println();
	}
	
	public void print(TaggingDict td){
		System.out.println("====> idx: " + idx);

		State pointer=this;
		String res="";
		for (int i=0; i< idx; i++){
			res=td.IdxToTag(pointer.state)+" "+res;
			pointer=pointer.prev;
		}
		System.out.println(res);

		for (int i=0; i< wordseq.length; i++){
			System.out.print(" " + td.IdxToWord(wordseq[i]));
		}
		System.out.println();
	}	
	

	
	/*int[] assignment = null;
	int[] wordseq = null;
	int idx = -1;

	public boolean isComplete()
	{
		if (idx == wordseq.length - 1)
			return true;
		else
			return false;
	}

	public State clone(){
		State res = new State();
		res.idx = this.idx;
		res.assignment = new int[this.wordseq.length];
		res.wordseq = new int[this.wordseq.length];
		System.arraycopy(this.wordseq, 0, res.wordseq, 0, wordseq.length);
		System.arraycopy(this.assignment, 0, res.assignment, 0, wordseq.length);
		return res;
	}

	public void print(){

		System.out.println("====> idx: " + idx);

		for (int i=0; i< assignment.length; i++){
			System.out.print(" " + assignment[i]);
		}
		System.out.println();

		for (int i=0; i< wordseq.length; i++){
			System.out.print(" " + wordseq[i]);
		}
		System.out.println();
	}*/
}
