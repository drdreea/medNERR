package probabModel.types;

import java.lang.Math;
import java.util.ArrayList;

public class MalletLabelSeqWithProb implements Comparable
{
    ArrayList labelSeq;  // the label idx sequence.
    ArrayList probSeq;   // the prob sequence
    ArrayList logprobSeq;  // the logprob sequence (log with base 10)

    double logprobSum;  // the sum of log prob
    

    public MalletLabelSeqWithProb() 
    {
        labelSeq = new ArrayList();
	probSeq = new ArrayList();
	logprobSeq = new ArrayList();

        logprobSum = 0;
    }

    // clone an instance.
    public MalletLabelSeqWithProb(MalletLabelSeqWithProb other)
    {
        labelSeq = new ArrayList();
	probSeq = new ArrayList();
	logprobSeq = new ArrayList();
	
	logprobSum = other.logprobSum;

        for (int i = 0; i < other.labelSeq.size(); ++i) {
            labelSeq.add(other.labelSeq.get(i));
            probSeq.add(other.probSeq.get(i));
            logprobSeq.add(other.logprobSeq.get(i));
        }
    }
    
    
    public void add(Object o, double prob) 
    {
	//      System.out.print("Adding " + o.toString() + " with prob " + prob + 
	//                         " to sequence {" + this + "}");
        
	double logprob = Math.log(prob);
        labelSeq.add(o);
	probSeq.add(new Double(prob));
	logprobSeq.add(new Double(logprob));

        logprobSum += logprob;
        //      System.out.println(" Now it's become {" + this + "}");
        
    }
    
    public double getLogprobSum() 
    {
        return logprobSum;
    }

    public ArrayList getLabelSeq() 
    {
        return labelSeq;
    }
    
    public ArrayList getProbSeq() 
    {
        return probSeq;
    }

    public ArrayList getLogprobSeq() 
    {
        return logprobSeq;
    }

    public int compareTo(Object o)
    {
        // we want the ones with higher prob first
        // so the output order is opposite of the prob order

        MalletLabelSeqWithProb ols = (MalletLabelSeqWithProb) o;
        if (this.logprobSum > ols.logprobSum)
	    return -1;
        if (this.logprobSum < ols.logprobSum) 
	    return 1;
        
	return 0;
    }

    public String toString()
    {
        String s = "";
        for (Object o : labelSeq) {
            s += o.toString();
            s += " ";
        }
        s += "logprobSum: " + logprobSum;
        return s;
    }

}

