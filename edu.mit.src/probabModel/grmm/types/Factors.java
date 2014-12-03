/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package probabModel.grmm.types;

import edu.umass.cs.mallet.base.types.*;
import edu.umass.cs.mallet.base.util.Maths;

import java.util.*;

import gnu.trove.TIntArrayList;
import gnu.trove.TDoubleArrayList;

/**
 * A static utility class containing utility methods for dealing with factors,
 *  especially TableFactor objects.
 * 
 * Created: Mar 17, 2005
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: Factors.java,v 1.6 2006/05/29 03:41:52 casutton Exp $
 */
public class Factors {


  /**
   * A version of slice() that doesn't require knowing the set that the new potential
   *  will be over.
   * @param factor Factor to take the slice of
   * @param assn Variables to hold as fixed
   * @return A new factor over VARS(factor)\VARS(assn)
   */
  public static Factor slice (Factor factor, Assignment assn)
  {
    HashVarSet clique = new HashVarSet (factor.varSet ());
    clique.removeAll (Arrays.asList (assn.getVars ()));
    return slice (clique.toVariableArray (), factor, assn);
  }

  /**
   * Creates a new potential from another by restricting it to a given assignment.
   * @param var Variable the new potential will be over
   * @param ptl The old potential.  Its varSet() must contain var.
   * @param observed Evidence to restrict to.  Must give values for all variables in ptl.varSet() except for var.
   * @return A DiscretePotential over var
   */
  public static Factor slice (Variable var, Factor ptl, Assignment observed)
  {
    boolean inLogSpace = ptl instanceof LogTableFactor; // hack
    Assignment assn = observed.duplicate();
    double[] vals = new double [var.getNumOutcomes ()];
    for (int i = 0; i < var.getNumOutcomes (); i++) {
      assn.setValue (var, i);
      if (inLogSpace) {
        vals[i] = ptl.logValue (assn);
      } else {
        vals[i] = ptl.value (assn);
      }
    }

    Factor ret;
    if (inLogSpace) {
      ret = LogTableFactor.makeFromLogValues (new Variable[] { var }, vals);
    } else {
      ret = new TableFactor (var, vals);
    }

    return ret;
  }

  public static Factor slice (Variable v1, Variable v2, Factor ptl, Assignment observed)
  {
    boolean inLogSpace = ptl instanceof LogTableFactor; // hack
    Assignment assn = observed.duplicate();

    int N1 = v1.getNumOutcomes ();
    int N2 = v2.getNumOutcomes ();
    int[] szs = new int[] { N1, N2 };

    double[] vals = new double [N1 * N2];
    for (int i = 0; i < N1; i++) {
      assn.setValue (v1, i);
      for (int j = 0; j < N2; j++) {
        assn.setValue (v2, j);
        int idx = Matrixn.singleIndex (szs, new int[] { i, j }); // Inefficient, but much less error prone

        double val = inLogSpace ? ptl.logValue (assn) : ptl.value (assn);

        vals[idx] = val;
      }
    }

    Factor ret;
    if (inLogSpace) {
      ret = LogTableFactor.makeFromLogValues(new Variable[] { v1, v2 }, vals);
    } else {
      ret = new TableFactor (new Variable[] { v1, v2 }, vals);
    }

    return ret;
  }


  public static Factor slice (Variable[] vars, Factor ptl, Assignment observed)
  {
    // Special case for speed
    if (vars.length == 1) {
      return slice (vars[0], ptl, observed);
    } else if (vars.length == 2) {
      return slice (vars[0], vars[1], ptl, observed);
    } else {
      return sliceInternal (vars, ptl, observed);
    }
  }

  private static Factor sliceInternal (Variable[] vars, Factor ptl, Assignment observed)
  {
    boolean inLogSpace = ptl instanceof LogTableFactor; // hack
    Assignment assn = observed.duplicate();
    VarSet varSet = new HashVarSet (vars);
    double[] vals = new double [varSet.weight ()];

    AssignmentIterator it = varSet.assignmentIterator ();
    while (it.hasNext ()) {
      assn.setValues (it.assignment ());

      double val = inLogSpace ? ptl.logValue (assn) : ptl.value (assn);

      vals[it.indexOfCurrentAssn ()] = val;
      it.advance ();
    }

    Factor ret;
    if (inLogSpace) {
      ret = LogTableFactor.makeFromLogValues(vars, vals);
    } else {
      ret = new TableFactor (vars, vals);
    }

    return ret;
  }

  public static CPT normalizeAsCpt (AbstractTableFactor ptl, Variable var)
  {
    double[] sums = new double [ptl.numLocations()];
    Arrays.fill (sums, Double.NEGATIVE_INFINITY);

    // Compute normalization factor for each neighbor assignment
    VarSet neighbors = new HashVarSet (ptl.varSet ());
    neighbors.remove (var);

    AssignmentIterator it = neighbors.assignmentIterator ();
    while (it.hasNext ()) {
      Assignment assn = it.assignment ().duplicate ();
      for (int i = 0; i < var.getNumOutcomes (); i++) {
        assn.setValue (var, i);
        int idx = it.indexOfCurrentAssn ();
//        sums[idx] += ptl.phi (assn);
        sums[idx] = Maths.sumLogProb (ptl.logValue (assn), sums[idx]);
      }
      it.advance ();
    }

    // ...and then normalize potential
    it = neighbors.assignmentIterator ();
    while (it.hasNext ()) {
      Assignment assn = it.assignment ().duplicate ();
      for (int i = 0; i < var.getNumOutcomes (); i++) {
        assn.setValue (var, i);
        double oldVal = ptl.logValue (assn);
//        double oldVal = ptl.phi (assn);
        double logZ = sums[it.indexOfCurrentAssn ()];
//        ptl.setPhi (assn, oldVal / logZ);
        ptl.setLogValue (assn, oldVal - logZ);
      }
      it.advance ();
    }

    return new CPT (ptl, var);
  }

  public static Factor average (Factor ptl1, Factor ptl2, double weight)
  {
    // complete hack
    TableFactor mptl1 = (TableFactor) ptl1;
    TableFactor mptl2 = (TableFactor) ptl2;
    return TableFactor.hackyMixture (mptl1, mptl2, weight);
  }

  public static double oneDistance (Factor bel1, Factor bel2)
  {
    Set vs1 = bel1.varSet ();
    Set vs2 = bel2.varSet ();

    if (!vs1.equals (vs2)) {
      throw new IllegalArgumentException ("Attempt to take distancebetween mismatching potentials "+bel1+" and "+bel2);
    }

    double dist = 0;
    for (AssignmentIterator it = bel1.assignmentIterator (); it.hasNext ();) {
      Assignment assn = it.assignment ();
      dist += Math.abs (bel1.value (assn) - bel2.value (assn));
      it.advance ();
    }

    return dist;
  }


  public static TableFactor retainMass (DiscreteFactor ptl, double alpha)
  {
    int[] idxs = new int [ptl.numLocations ()];
    double[] vals = new double [ptl.numLocations ()];
    for (int i = 0; i < idxs.length; i++) {
      idxs[i] = ptl.indexAtLocation (i);
      vals[i] = ptl.logValue (i);
    }

    RankedFeatureVector rfv = new RankedFeatureVector (new Alphabet(), idxs, vals);
    TIntArrayList idxList = new TIntArrayList ();
    TDoubleArrayList valList = new TDoubleArrayList ();

    double mass = Double.NEGATIVE_INFINITY;
    double logAlpha = Math.log (alpha);
    for (int rank = 0; rank < rfv.numLocations (); rank++) {
      int idx = rfv.getIndexAtRank (rank);
      double val = rfv.value (idx);
      mass = Maths.sumLogProb (mass, val);
      idxList.add (idx);
      valList.add (val);
      if (mass > logAlpha) {
        break;
      }
    }

    int[] szs = computeSizes (ptl);
    SparseMatrixn m = new SparseMatrixn (szs, idxList.toNativeArray (), valList.toNativeArray ());

    TableFactor result = new TableFactor (computeVars (ptl));
    result.setValues (m);

    return result;
  }

  public static int[] computeSizes (Factor result)
  {
    int nv = result.varSet ().size();
    int[] szs = new int [nv];
    for (int i = 0; i < nv; i++) {
      Variable var = result.getVariable (i);
      szs[i] = var.getNumOutcomes ();
    }
    return szs;
  }

  public static Variable[] computeVars (Factor result)
  {
    int nv = result.varSet ().size();
    Variable[] vars = new Variable [nv];
    for (int i = 0; i < nv; i++) {
      Variable var = result.getVariable (i);
      vars[i] = var;
    }
    return vars;
  }

  /**
   * Given a joint distribution over two variables, returns their mutual information.
   * @param factor A joint distribution.  Must be normalized, and over exactly two variables.
   * @return The mutual inforamiton
   */
  public static double mutualInformation (Factor factor)
  {
    VarSet vs = factor.varSet ();
    if (vs.size() != 2) throw new IllegalArgumentException ("Factor must have size 2");
    Factor marg1 = factor.marginalize (vs.get (0));
    Factor marg2 = factor.marginalize (vs.get (1));

    double result = 0;
    for (Iterator it = factor.assignmentIterator (); it.hasNext(); ) {
      Assignment assn = (Assignment) it.next ();
      result += (factor.value (assn)) * (factor.logValue (assn) - marg1.logValue (assn) - marg2.logValue (assn));
    }
    return result;
  }

  public static double KL (AbstractTableFactor f1, AbstractTableFactor f2)
  {
    double result = 0;
    // assumes same var set
    for (int loc = 0; loc < f1.numLocations (); loc++) {
      double val1 = f1.valueAtLocation (loc);
      double val2 = f2.value (f1.indexAtLocation (loc));
      if (val1 > 1e-5) {
        result += val1 * Math.log (val1 / val2);
      }
    }
    return result;
  }

  /**
   * Returns a new Factor <tt>F = alpha * f1 + (1 - alpha) * f2</tt>.
   */
   public static Factor mix (AbstractTableFactor f1, AbstractTableFactor f2, double alpha)
  {
    return AbstractTableFactor.hackyMixture (f1, f2, alpha);
  }

  public static double euclideanDistance (AbstractTableFactor f1, AbstractTableFactor f2)
  {
    double result = 0;
    // assumes same var set
    for (int loc = 0; loc < f1.numLocations (); loc++) {
      double val1 = f1.valueAtLocation (loc);
      double val2 = f2.value (f1.indexAtLocation (loc));
      result += (val1 - val2) * (val1 - val2);
    }
    return Math.sqrt (result);
  }

  public static double l1Distance (AbstractTableFactor f1, AbstractTableFactor f2)
  {
    double result = 0;
    // assumes same var set
    for (int loc = 0; loc < f1.numLocations (); loc++) {
      double val1 = f1.valueAtLocation (loc);
      double val2 = f2.value (f1.indexAtLocation (loc));
      result += Math.abs (val1 - val2);
    }
    return result;
  }
}
