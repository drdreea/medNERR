/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package probabModel.grmm.types;

import edu.umass.cs.mallet.base.util.Maths;
import edu.umass.cs.mallet.base.types.SparseMatrixn;
import edu.umass.cs.mallet.base.types.Matrix;

import java.util.Collection;

/**
 * Created: Jan 4, 2006
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: LogTableFactor.java,v 1.6 2006/05/30 23:09:55 casutton Exp $
 */
public class LogTableFactor extends AbstractTableFactor {

  public LogTableFactor (AbstractTableFactor in)
  {
    super(in);
    probs = (Matrix) in.getLogValueMatrix ().cloneMatrix ();
  }

  public LogTableFactor (Variable var)
  {
    super(var);
  }

  public LogTableFactor (Variable[] allVars)
  {
    super(allVars);
  }

  public LogTableFactor (Collection allVars)
  {
    super(allVars);
  }

  // Create from
  //  Used by makeFromLogFactorValues
  private LogTableFactor (Variable[] vars, double[] logValues)
  {
    super(vars, logValues);
  }

  private LogTableFactor (Variable[] allVars, Matrix probsIn)
  {
    super(allVars, probsIn);
  }

  //**************************************************************************/

  public static LogTableFactor makeFromValues (Variable[] vars, double[] vals)
  {
    double[] vals2 = new double [vals.length];
    for (int i = 0; i < vals.length; i++) {
      vals2[i] = Math.log(vals[i]);
    }
    return makeFromLogValues(vars, vals2);
  }

  public static LogTableFactor makeFromLogValues (Variable[] vars, double[] vals)
  {
    return new LogTableFactor(vars, vals);
  }

  //**************************************************************************/

  void setAsIdentity ()
  {
    setAll(0.0);
  }

  public Factor duplicate ()
  {
    return new LogTableFactor(this);
  }

  protected AbstractTableFactor createBlankSubset (Variable[] vars)
  {
    return new LogTableFactor(vars);
  }

  public Factor normalize ()
  {
    double sum = logspaceOneNorm();
    for (int i = 0; i < probs.numLocations(); i++) {
      double val = probs.valueAtLocation(i);
      probs.setValueAtLocation(i, val - sum);
    }
    return this;
  }

  private double logspaceOneNorm ()
  {
    double sum = Double.NEGATIVE_INFINITY; // That's 0 in log space
    for (int i = 0; i < probs.numLocations(); i++) {
      sum = Maths.sumLogProb(sum, probs.valueAtLocation(i));
    }
    return sum;
  }

  public double sum ()
  {
    return Math.exp(logspaceOneNorm());
  }

  public double logsum ()
  {
    return logspaceOneNorm();
  }

  /**
   * Does the conceptual equivalent of this += pot.
   * Assumes that pot's variables are a subset of
   * this potential's.
   */
  protected void multiplyByInternal (DiscreteFactor ptl)
  {
    int[] projection = largeIdxToSmall(ptl);
    int numLocs = probs.numLocations();
    for (int singleLoc = 0; singleLoc < numLocs; singleLoc++) {
      int smallIdx = projection[singleLoc];
      double prev = this.probs.valueAtLocation(singleLoc);
      double newVal = ptl.logValue (smallIdx);
      double product = prev + newVal;
      this.probs.setValueAtLocation(singleLoc, product);
    }
  }

  // Does destructive divison on this, assuming this has all
// the variables in pot.
  protected void divideByInternal (DiscreteFactor ptl)
  {
    int[] projection = largeIdxToSmall(ptl);
    for (int singleLoc = 0; singleLoc < probs.numLocations(); singleLoc++) {
      int smallIdx = projection[singleLoc];
      double prev = this.probs.valueAtLocation(singleLoc);
      double newVal = ptl.logValue (smallIdx);
      double product = prev - newVal;
      /* by convention, let -Inf + Inf (corresponds to 0/0) be -Inf */
      if (Double.isInfinite(newVal)) {
        product = Double.NEGATIVE_INFINITY;
      }
      this.probs.setValueAtLocation(singleLoc, product);
    }
  }

  public double value (Assignment assn)
  {
    if (getNumVars() == 0) return 1.0;
    return Math.exp (rawValue(assn));
  }


  public double value (AssignmentIterator it)
  {
    return Math.exp (rawValue(it.indexOfCurrentAssn()));
  }


  public double value (int idx)
  {
    return Math.exp (rawValue (idx));
  }

  public double logValue (AssignmentIterator it)
  {
    return rawValue (it.indexOfCurrentAssn());
  }

  public double logValue (int idx)
  {
    return rawValue (idx);
  }

  public double logValue (Assignment assn)
  {
    return rawValue (assn);
  }

  protected Factor marginalizeInternal (AbstractTableFactor result)
  {

    result.setAll(Double.NEGATIVE_INFINITY);
    int[] projection = largeIdxToSmall(result);

    /* Add each element of the single array of the large potential
to the correct element in the small potential. */
    for (int largeLoc = 0; largeLoc < probs.numLocations(); largeLoc++) {

      /* Convert a single-index from this distribution to
 one for the smaller distribution */
      int smallIdx = projection[largeLoc];

      /* Whew! Now, add it in. */
      double oldValue = this.probs.valueAtLocation(largeLoc);
      double currentValue = result.probs.singleValue(smallIdx);
      result.probs.setValueAtLocation(smallIdx,
                                      Maths.sumLogProb(oldValue, currentValue));

    }

    return result;
  }

  protected double rawValue (Assignment assn)
  {
    int numVars = getNumVars();
    int[] indices = new int[numVars];
    for (int i = 0; i < numVars; i++) {
      Variable var = getVariable(i);
      indices[i] = assn.get(var);
    }

    return rawValue(indices);
  }

  private double rawValue (int[] indices)
  {
    // handle non-occuring indices specially, for default value is -Inf in log space.
    int singleIdx = probs.singleIndex(indices);
    return rawValue(singleIdx);
  }

  protected double rawValue (int singleIdx)
  {
    int loc = probs.location(singleIdx);
    if (loc < 0) {
      return Double.NEGATIVE_INFINITY;
    } else {
      return probs.valueAtLocation(loc);
    }
  }

  public void exponentiate (double power)
  {
    probs.timesEquals(power);
  }

  /*
  protected AbstractTableFactor ensureOperandCompatible (AbstractTableFactor ptl)
  {
    if (!(ptl instanceof LogTableFactor)) {
      return new LogTableFactor(ptl);
    } else {
      return ptl;
    }
  }
  */

  public void setLogValue (Assignment assn, double logValue)
  {
    setRawValue(assn, logValue);
  }

  public void setLogValue (AssignmentIterator assnIt, double logValue)
  {
    setRawValue(assnIt, logValue);
  }

  public void setValue (AssignmentIterator assnIt, double value)
  {
    setRawValue(assnIt, Math.log (value));
  }

  public void setLogValues (double[] vals)
  {
    for (int i = 0; i < vals.length; i++) {
      setRawValue (i, vals[i]);
    }
  }

  public void setValues (double[] vals)
  {
    for (int i = 0; i < vals.length; i++) {
      setRawValue (i, Math.log(vals[i]));
    }
  }

  protected void plusEqualsAtLocation (int loc, double v)
  {
    double oldVal = logValue (loc);
    setRawValue (loc, Maths.sumLogProb (oldVal, Math.log (v)));
  }

  public static LogTableFactor makeFromValues (Variable var, double[] vals2)
  {
    return makeFromValues (new Variable[] { var }, vals2);
  }

  public static LogTableFactor makeFromMatrix (Variable[] vars, SparseMatrixn values)
  {
    SparseMatrixn logValues = (SparseMatrixn) values.cloneMatrix ();
    for (int i = 0; i < logValues.numLocations (); i++) {
      logValues.setValueAtLocation (i, Math.log (logValues.valueAtLocation (i)));
    }
    return new LogTableFactor (vars, logValues);
  }

  public static LogTableFactor makeFromLogValues (Variable v, double[] vals)
  {
    return makeFromLogValues (new Variable[] { v }, vals);
  }

  public Matrix getValueMatrix ()
  {
    Matrix logProbs = (Matrix) probs.cloneMatrix ();
    for (int loc = 0; loc < probs.numLocations (); loc++) {
      logProbs.setValueAtLocation (loc, Math.exp (logProbs.valueAtLocation (loc)));
    }
    return logProbs;
  }

  public Matrix getLogValueMatrix ()
  {
    return probs;
  }

  public double valueAtLocation (int idx)
  {
    return Math.exp (probs.valueAtLocation (idx));
  }
}
