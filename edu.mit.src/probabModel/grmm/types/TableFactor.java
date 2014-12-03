/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package probabModel.grmm.types;

import edu.umass.cs.mallet.base.types.Matrix;
import edu.umass.cs.mallet.base.util.Maths;
import edu.umass.cs.mallet.grmm.types.BidirectionalIntObjectMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created: Jan 4, 2006
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: TableFactor.java,v 1.8 2006/05/30 23:09:50 casutton Exp $
 */
public class TableFactor extends AbstractTableFactor {


  public static DiscreteFactor multiplyAll (Factor[] phis)
  {
    return multiplyAll (Arrays.asList (phis));
  }


  /**
   * Returns the product of a collection of multinomial potentials.
   */
  /// xxx once there are other types of potentials, this will need to
  /// be refactored into a Factors static-utilities class.
  public static TableFactor multiplyAll (Collection phis)
  {
    /* Get all the variables */
    HashSet varSet = new HashSet ();
    for (Iterator it = phis.iterator (); it.hasNext ();) {
      Factor phi = (Factor) it.next ();
      varSet.addAll (phi.varSet ());
    }

    /* define a new potential over the neighbors of NODE */
    TableFactor newCPF = new TableFactor (varSet);
    for (Iterator it = phis.iterator (); it.hasNext ();) {
      Factor phi = (Factor) it.next ();
      newCPF.multiplyBy (phi);
    }

    return newCPF;
  }


  public TableFactor (Variable var)
  {
    super (var);
  }

  public TableFactor (Variable var, double[] values)
  {
    super (var, values);
  }

  public TableFactor ()
  {
  }

  public TableFactor (BidirectionalIntObjectMap varMap)
  {
    super (varMap);
  }

  public TableFactor (Variable allVars [])
  {
    super (allVars);
  }

  public TableFactor (Collection allVars)
  {
    super (allVars);
  }

  public TableFactor (Variable[] allVars, double[] probs)
  {
    super (allVars, probs);
  }

  public TableFactor (VarSet allVars, double[] probs)
  {
    super (allVars, probs);
  }

  public TableFactor (Variable[] allVars, Matrix probsIn)
  {
    super (allVars, probsIn);
  }

  public TableFactor (AbstractTableFactor in)
  {
    super (in);
    probs = (Matrix) in.getValueMatrix ().cloneMatrix ();
  }

  public TableFactor (VarSet allVars, Matrix probsIn)
  {
    super (allVars, probsIn);
  }

  public TableFactor (AbstractTableFactor ptl, double[] probs)
  {
    super (ptl, probs);
  }


  /**************************************************************************/

  void setAsIdentity ()
  {
    setAll (1.0);
  }

  public Factor duplicate ()
  {
    return new TableFactor (this);
  }

  protected AbstractTableFactor createBlankSubset (Variable[] vars)
  {
    return new TableFactor (vars);
  }

  /**
   * Multiplies every entry in the potential by a constant
   * such that all the entries sum to 1.
   */
  public Factor normalize ()
  {
    probs.oneNormalize ();
    return this;
  }

  public double sum ()
  {
    return probs.oneNorm ();
  }

  public double logValue (AssignmentIterator it)
  {
    return Math.log (rawValue (it.indexOfCurrentAssn ()));
  }

  public double logValue (Assignment assn)
  {
    return Math.log (rawValue (assn));
  }

  public double logValue (int loc)
  {
    return Math.log (rawValue (loc));
  }

  public double value (Assignment assn)
  {
    return rawValue (assn);
  }

  public double value (int loc)
  {
    return rawValue (loc);
  }

  public double value (AssignmentIterator assn)
  {
    return rawValue (assn.indexOfCurrentAssn ());
  }

  protected Factor marginalizeInternal (AbstractTableFactor result)
  {

    result.setAll (0.0);

    int[] projection = largeIdxToSmall (result);

    /* Add each element of the single array of the large potential
to the correct element in the small potential. */
    for (int largeLoc = 0; largeLoc < probs.numLocations (); largeLoc++) {

      /* Convert a single-index from this distribution to
 one for the smaller distribution */
      int smallIdx = projection[largeLoc];

      /* Whew! Now, add it in. */
      double oldValue = this.probs.valueAtLocation (largeLoc);
      result.probs.incrementSingleValue (smallIdx, oldValue);
    }

    return result;
  }

  // Does destructive multiplication on this, assuming this has all
// the variables in pot.
  protected void multiplyByInternal (DiscreteFactor ptl)
  {
    int[] projection = largeIdxToSmall (ptl);
    for (int singleLoc = 0; singleLoc < probs.numLocations (); singleLoc++) {
      int smallIdx = projection[singleLoc];
      double prev = this.probs.valueAtLocation (singleLoc);
      double newVal = ptl.value (smallIdx);
      this.probs.setValueAtLocation (singleLoc, prev * newVal);
    }
  }

  // Does destructive divison on this, assuming this has all
  // the variables in pot.
  protected void divideByInternal (DiscreteFactor ptl)
  {
    int[] projection = largeIdxToSmall (ptl);
    for (int singleLoc = 0; singleLoc < probs.numLocations (); singleLoc++) {
      int smallIdx = projection[singleLoc];
      double prev = this.probs.valueAtLocation (singleLoc);
      double newVal = ptl.value (smallIdx);
      double product = prev / newVal;
      /* by convention, let dividing by zero just return 0 */
      if (Maths.almostEquals (newVal, 0)) {
        product = 0;
      }
      this.probs.setValueAtLocation (singleLoc, product);
    }
  }

  protected double rawValue (Assignment assn)
  {
    int numVars = getNumVars();
    int[] indices = new int[numVars];
    for (int i = 0; i < numVars; i++) {
      Variable var = getVariable (i);
      indices[i] = assn.get (var);
    }

    double value = rawValue (indices);
    return value;
  }

  private double rawValue (int[] indices)
  {
    // handle non-occuring indices specially, for default value is -Inf in log space.
    int singleIdx = probs.singleIndex (indices);
    return rawValue (singleIdx);
  }

  protected double rawValue (int singleIdx)
  {
    int loc = probs.location (singleIdx);
    if (loc < 0) {
      return 0;
    } else {
      return probs.valueAtLocation (loc);
    }
  }

  public void exponentiate (double power)
  {
    for (int loc = 0; loc < probs.numLocations (); loc++) {
      double oldVal = probs.valueAtLocation (loc);
      double newVal = Math.pow (oldVal, power);
      probs.setValueAtLocation (loc, newVal);
    }
  }

  /*
  protected AbstractTableFactor ensureOperandCompatible (AbstractTableFactor ptl)
  {
    if (!(ptl instanceof TableFactor)) {
      return new TableFactor (ptl);
    } else {
      return ptl;
    }
  }
  */

  public void setLogValue (Assignment assn, double logValue)
  {
      setRawValue (assn, Math.exp (logValue));
  }

  public void setLogValue (AssignmentIterator assnIt, double logValue)
  {
      setRawValue (assnIt, Math.exp (logValue));
  }

  public void setValue (AssignmentIterator assnIt, double value)
  {
    setRawValue (assnIt, value);
  }

  public void setLogValues (double[] vals)
  {
    for (int i = 0; i < vals.length; i++) {
      setRawValue (i, Math.exp(vals[i]));
    }
  }

  public void setValues (double[] vals)
  {
    for (int i = 0; i < vals.length; i++) {
      setRawValue (i, vals[i]);
    }
  }

  protected void plusEqualsAtLocation (int loc, double v)
  {
    double oldVal = valueAtLocation (loc);
    setRawValue (loc, oldVal + v);
  }

  public Matrix getValueMatrix ()
  {
    return probs;
  }

  public Matrix getLogValueMatrix ()
  {
    Matrix logProbs = (Matrix) probs.cloneMatrix ();
    for (int loc = 0; loc < probs.numLocations (); loc++) {
      logProbs.setValueAtLocation (loc, Math.log (logProbs.valueAtLocation (loc)));
    }
    return logProbs;
  }

  public double valueAtLocation (int idx)
  {
    return probs.valueAtLocation (idx);
  }
}
