/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package probabModel.grmm.types;

import edu.umass.cs.mallet.base.types.*;
import edu.umass.cs.mallet.base.util.Maths;
import edu.umass.cs.mallet.base.util.Random;
import edu.umass.cs.mallet.grmm.types.BidirectionalIntObjectMap;
import edu.umass.cs.mallet.grmm.util.GeneralUtils;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;



/**
 * Class for a multivariate multinomial distribution.
 * <p/>
 * Created: Mon Sep 15 17:19:24 2003
 *
 * @author <a href="mailto:casutton@cs.umass.edu">Charles Sutton</a>
 * @version $Id: AbstractTableFactor.java,v 1.11 2006/05/30 23:09:46 casutton Exp $
 */
public abstract class AbstractTableFactor implements DiscreteFactor {

  /**
   * Maps all of the Variable objects of this distribution
   * to an integer that says which dimension in the probs
   * matrix correspands to that var.
   */
  private BidirectionalIntObjectMap varMap;
  private VarSet vars;  // cache of varMap

  /**
   * Number of variables in this potential.
   */
  private int numVars;

  public Matrix probs;

  protected AbstractTableFactor (BidirectionalIntObjectMap varMap)
  {
    initVars (varMap);
    setAsIdentity ();
  }

  private void initVars (BidirectionalIntObjectMap allVars)
  {
    initVars (Arrays.asList (allVars.toArray ()));
  }

  private void initVars (Variable allVars[])
  {
    int sizes[] = new int[allVars.length];

    varMap = new BidirectionalIntObjectMap ();
    vars = new HashVarSet ();

//    Arrays.sort (allVars);
    for (int i = 0; i < allVars.length; i++) {
      varMap.lookupIndex (allVars[i]);
      vars.add (allVars[i]);
      sizes[i] = allVars[i].getNumOutcomes ();
    }

    probs = new Matrixn (sizes);
    if (probs.numLocations () == 0) {
      System.err.println ("Warning: empty potential created");
    }

    numVars = allVars.length;
  }

  private void initVars (Collection allVars)
  {
    initVars ((Variable[]) allVars.toArray (new Variable[allVars.size ()]));
  }

  private void setProbs (double[] probArray)
  {
    if (probArray.length != probs.numLocations ()) {
      /* This shouldn't be a runtime exception. So sue me. */
      throw new RuntimeException
              ("Attempt to initialize potential with bad number of prababilities.\n"
              + "Needed " + probs.numLocations () + " got " + probArray.length);
    }

    for (int i = 0; i < probArray.length; i++) {
      probs.setValueAtLocation (i, probArray[i]);
    }
  }

  /**
   * Creates an identity potential over the given variable.
   */
  public AbstractTableFactor (Variable var)
  {
    initVars (new Variable[]{var});
    setAsIdentity ();
  }

  public AbstractTableFactor (Variable var, double[] values)
  {
    initVars (new Variable[]{var});
    setProbs (values);
  }

  /**
   * Creates an identity potential over NO variables.
   */
  public AbstractTableFactor ()
  {
    initVars (new Variable[]{});
    setAsIdentity ();
  }

  /**
   * Creates an identity potential with the given variables.
   */
  public AbstractTableFactor (Variable allVars [])
  {
    initVars (allVars);
    setAsIdentity ();
  }

  /**
   * Creates an identity potential with the given variables.
   *
   * @param allVars A collection containing the Variables
   *                of this distribution.
   */
  public AbstractTableFactor (Collection allVars)
  {
    initVars (allVars);
    setAsIdentity ();
  }

  /**
   * Creates a potential with the given variables and
   * the given probabilities.
   *
   * @param allVars Variables of the potential
   * @param probs   All phi values of the potential, in row-major order.
   */
  public AbstractTableFactor (Variable[] allVars, double[] probs)
  {
    initVars (allVars);
    setProbs (probs);
  }

  /**
   * Creates a potential with the given variables and
   * the given probabilities.
   *
   * @param allVars Variables of the potential
   * @param probs   All phi values of the potential, in row-major order.
   */
  private AbstractTableFactor (BidirectionalIntObjectMap allVars, double[] probs)
  {
    initVars (allVars);
    setProbs (probs);
  }


  /**
   * Creates a potential with the given variables and
   * the given probabilities.
   *
   * @param allVars Variables of the potential
   * @param probs   All phi values of the potential, in row-major order.
   */
  public AbstractTableFactor (VarSet allVars, double[] probs)
  {
    initVars (allVars.toVariableArray ());
    setProbs (probs);
  }


  /**
   * Creates a potential with the given variables and
   * the given probabilities.
   *
   * @param allVars Variables of the potential
   * @param probsIn All the phi values of the potential.
   */
  public AbstractTableFactor (Variable[] allVars, Matrix probsIn)
  {
    initVars (allVars);
    probs = (Matrix) probsIn.cloneMatrix ();
  }

  /**
   * Creates a potential with the given variables and
   * the given probabilities.
   *
   * @param allVars Variables of the potential
   * @param probsIn All the phi values of the potential.
   */
  private AbstractTableFactor (BidirectionalIntObjectMap allVars, Matrix probsIn)
  {
    initVars (allVars);
    probs = (Matrix) probsIn.cloneMatrix ();
  }

  /**
   * Copy constructor.
   */
  public AbstractTableFactor (AbstractTableFactor in)
  {
    //xxx Could be dangerous! But these should never be modified
    varMap = in.varMap;
    vars = in.vars;
    numVars = in.numVars;
  }

  /**
   * Creates a potential with the given variables and
   * the given probabilities.
   *
   * @param allVars Variables of the potential
   * @param probsIn All the phi values of the potential.
   */
  public AbstractTableFactor (VarSet allVars, Matrix probsIn)
  {
    initVars (allVars.toVariableArray ());
    probs = (Matrix) probsIn.cloneMatrix ();
  }

  /**
   * Creates a potential with the same variables as another, but different probabilites.
   * @param ptl
   * @param probs
   */
  public AbstractTableFactor (AbstractTableFactor ptl, double[] probs)
  {
    this (ptl.varMap, probs);
  }


  /**************************************************************************
   *  STATIC FACTORY METHODS
   **************************************************************************/

  public static Factor makeIdentityFactor (AbstractTableFactor copy)
  {
    return new TableFactor (copy.varMap);
  }


  void setAll (double val)
  {
    for (int i = 0; i < probs.numLocations (); i++) {
      probs.setSingleValue (i, val);
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // ABSTRACT METHODS
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Forces this potential to be the identity (all 1s).
   */
  abstract void setAsIdentity ();

  public abstract Factor duplicate ();

  public abstract Factor normalize ();

  public abstract double sum ();

  protected abstract AbstractTableFactor createBlankSubset (Variable[] vars);

  private AbstractTableFactor createBlankSubset (Collection vars)
  {
    return createBlankSubset ((Variable[]) vars.toArray (new Variable [vars.size ()]));
  }

  private AbstractTableFactor createBlankSubset (BidirectionalIntObjectMap varMap)
  {
    return createBlankSubset ((Variable[]) varMap.toArray (new Variable [varMap.size ()]));
  }

  public Matrix getProbs(){
      return probs;
  }
  
  protected int getNumVars ()
  {
    return numVars;
  }

  ///////////////////////////////////////////////////////////////////////////

  // This method is inherently dangerous b/c variable ordering issues.
  // Consider using setPhi(Assignment,double) instead.
  public void setValues (Matrix probs)
  {
    if (this.probs.singleSize () != probs.singleSize ())
      throw new UnsupportedOperationException
              ("Trying to reset prob matrix with wrong number of probabilities.  Previous num probs: "+
              this.probs.singleSize ()+"  New num probs: "+probs.singleSize ());
    if (this.probs.getNumDimensions () != probs.getNumDimensions ())
      throw new UnsupportedOperationException
              ("Trying to reset prob matrix with wrong number of dimensions.");
    this.probs = probs;
  }

  /**
   * Returns true iff this potential is over the given variable
   */
  public boolean containsVar (Variable var)
  {
    return varMap.contains (var);
  }

  /**
   * Returns set of variables in this potential.
   */
  public VarSet varSet ()
  {
    return new UnmodifiableVarSet (vars);
  }

  public AssignmentIterator assignmentIterator ()
  {
    if (probs instanceof SparseMatrixn) {
      return new SparseAssignmentIterator (vars, ((SparseMatrixn) probs).getIndices ());
    } else {
      return new DenseAssignmentIterator (vars);
    }
  }

  public void setRawValue (Assignment assn, double value)
  {
    int[] indices = new int[numVars];
    for (int i = 0; i < numVars; i++) {
      Variable var = (Variable) varMap.lookupObject (i);
      indices[i] = assn.get (var);
    }

    probs.setValue (indices, value);
  }

  public void setRawValue (AssignmentIterator it, double value)
  {
    probs.setSingleValue (it.indexOfCurrentAssn (), value);
  }

  protected void setRawValue (int loc, double value)
  {
    probs.setSingleValue (loc, value);
  }


  public abstract double value (Assignment assn);


  // Special function to do normalization in log space

  // Computes sum if this potential is in log space

  public double logsum ()
  {
    return Math.log (probs.oneNorm ());
  }

  public double entropy ()
  {
    double h = 0;
    double p;
    for (AssignmentIterator it = assignmentIterator (); it.hasNext ();) {
      p = logValue (it);
      if (!Double.isInfinite (p))
        h -= p * Math.exp (p);
      it.advance ();
    }
    return h;
  }


  //  PROJECTION OF INDICES


  // Maps potentials --> int[]
/* Be careful about this thing, however.  It gets shallow copied whenever
   *  a potential is duplicated, so if a potential were modified (e.g.,
   *  by expandToContain) while this was being shared, things could
   *  get ugly.  I think everything is all right at the moment, but keep
   *  it in mind if inexplicable bugs show up in the future. -cas
   */
  transient private TIntObjectHashMap projectionCache = new TIntObjectHashMap ();

  private void clearProjectionCache ()
  {
    projectionCache = new TIntObjectHashMap (varMap.size ());
  }

  /*  Returns a hash value for subsets of this potential's variable set.
   *   Note that the hash value depends only on the set's membership
   *   (not its order), so that this hashing scheme would be unsafe
   *   for the projection cache unless potential variables were always
   *   in a canonical order, which they are.
   */
  private int computeSubsetHashValue (DiscreteFactor subset)
  {
    // If potentials have more than 32 variables, we need to use an
    // expandable bitset, but then again, you probably wouldn't have
    // enough memory to represent the potential anyway
    assert varMap.size () <= 32;
    int result = 0;
    double numVars = subset.varSet ().size ();
    for (int i = 0; i < numVars; i++) {
      Object var = subset.getVariable (i);
      int myidx = this.varMap.lookupIndex (var, false);
      if (myidx < 0) throw new RuntimeException ("Attempt to marginalize potentinal "+this+" over "+subset);
      result |= (1 << myidx);
    }
    return result;
  }

  /* For below, I tried special casing this as:
     if (smallPotential.numVars == 1) {

      int projection[] = new int[probs.singleSize ()];
      int largeDims[] = new int[numVars];
      Variable smallVar = (Variable) smallPotential.varMap.lookupObject (0);
      int largeDim = this.varMap.lookupIndex (smallVar, false);
      assert largeDim != -1 : smallVar;

      for (int largeIdx = 0; largeIdx < probs.singleSize (); largeIdx++) {
        probs.singleToIndices (largeIdx, largeDims);
        projection[largeIdx] = largeDims[largeDim];
      }

      return projection;

    }
    
    but this didn't seem to make a huge performance gain. */

  private int[] computeLargeIdxToSmall (DiscreteFactor smallPotential)
//	private int largeIdxToSmall (int largeIdx, MultinomialPotential smallPotential)
  {
    int projection[] = new int[probs.numLocations ()];
    int largeDims[] = new int[numVars];
    int smallNumVars = smallPotential.varSet().size();
    int smallDims[] = new int[smallNumVars];

    for (int largeLoc = 0; largeLoc < probs.numLocations (); largeLoc++) {
      int largeIdx = probs.indexAtLocation (largeLoc);
      probs.singleToIndices (largeIdx, largeDims);
      for (int smallDim = 0; smallDim < smallNumVars; smallDim++) {
        Variable smallVar = smallPotential.getVariable (smallDim);
        int largeDim = this.varMap.lookupIndex (smallVar, false);
        assert largeDim != -1 : smallVar;
        smallDims[smallDim] = largeDims[largeDim];
      }
      projection[largeLoc] = smallPotential.singleIndex (smallDims);
    }

    return projection;
  }

  int[] largeIdxToSmall (DiscreteFactor smallPotential)
          //	private int cachedlargeIdxToSmall (int largeIdx, MultinomialPotential smallPotential)
  {
// Special case where smallPtl has only one variable.  Here
//  since ordering is not a problem, we can use a set-based
//  hash key.
    if (smallPotential.varSet ().size () == 1) {
      return cachedLargeIdxToSmall (smallPotential);
    } else {
      return computeLargeIdxToSmall (smallPotential);
    }
  }


  // Cached version of computeLargeIdxToSmall for ptls with a single variable.
  //  This code is designed to work if smallPotential has multiple variables,
  //  but it breaks if it's called with two potentials with the same
  //  variables in different orders.
  // TODO: Make work for multiple variables (canonical ordering?)
  private int[] cachedLargeIdxToSmall (DiscreteFactor smallPotential)
  {
    int hashval = computeSubsetHashValue (smallPotential);
    Object ints = projectionCache.get (hashval);
    if (ints != null) {
      return (int[]) ints;
    } else {
      int[] projection = computeLargeIdxToSmall (smallPotential);
      projectionCache.put (hashval, projection);
      return projection;
    }
  }

  /**
   * Returns the marginal of this distribution over the given variables.
   */
  public Factor marginalize (Variable vars[])
  {
    assert varSet ().containsAll (Arrays.asList (vars)); // Perhaps throw exception instead
    return marginalizeInternal (createBlankSubset (vars));
  }

  public Factor marginalize (Collection vars)
  {
    assert varSet ().containsAll (vars);  // Perhaps throw exception instead
    return marginalizeInternal (createBlankSubset (vars));
  }

  public Factor marginalize (Variable var)
  {
    assert varSet ().contains (var);  // Perhaps throw exception instead
    return marginalizeInternal (createBlankSubset (new Variable[]{var}));
  }

  public Factor marginalizeOut (Variable var)
  {
    Set newVars = new HashVarSet (vars);
    newVars.remove (var);
    return marginalizeInternal (createBlankSubset (newVars));
  }


  protected abstract Factor marginalizeInternal (AbstractTableFactor result);

  public Factor extractMax (Variable var)
  {
    return extractMaxInternal (createBlankSubset (new Variable[] { var }));
  }

  public Factor extractMax (Variable[] vars)
  {
    return extractMaxInternal (createBlankSubset (vars));
  }

  public Factor extractMax (Collection vars)
  {
    return extractMaxInternal (createBlankSubset (vars));
  }

  private Factor extractMaxInternal (AbstractTableFactor result)
  {

    result.setAll (Double.NEGATIVE_INFINITY);

    int[] projection = largeIdxToSmall (result);
    /* Add each element of the single array of the large potential
       to the correct element in the small potential. */
    for (int largeLoc = 0; largeLoc < probs.numLocations (); largeLoc++) {

      /* Convert a single-index from this distribution to
         one for the smaller distribution */
      int smallIdx = projection[largeLoc];

      /* Whew! Now, add it in. */
      double largeValue = this.probs.valueAtLocation (largeLoc);
      double smallValue = result.probs.singleValue (smallIdx);
      if (largeValue > smallValue) {
        result.probs.setValueAtLocation (smallIdx, largeValue);
      }
    }

    return result;
  }

  private void expandToContain (DiscreteFactor pot)
  {
    // Check if we're adding variables
    Set newVarSet = new HashSet (pot.varSet ());
    newVarSet.addAll (varSet ());

    // if so, expand this potential. this is not pretty
    if (newVarSet.size () > numVars) {
      AbstractTableFactor newPtl = createBlankSubset (newVarSet);
      newPtl.multiplyByInternal (this);
      varMap = newPtl.varMap;
      vars = newPtl.vars;
      probs = newPtl.probs;
      numVars = newPtl.numVars;
      clearProjectionCache ();
    }
  }

  /**
   * Does the conceptual equivalent of this *= pot.
   * Assumes that pot's variables are a subset of
   * this potential's.
   */
  public void multiplyBy (Factor pot)
  {
    DiscreteFactor pot1 = (DiscreteFactor) pot; // cheating
    expandToContain (pot1);
    pot1 = ensureOperandCompatible (pot1);
    multiplyByInternal (pot1);
  }

  /**
   * Ensures that <tt>this.inLogSpace == ptl.inLogSpace</tt>. If this is
   * not the case, return a copy of ptl logified or delogified as appropriate.
   *
   * @param ptl
   * @return A potential equivalent to ptl, possibly logified or delogified.
   *         ptl itself could be returned.
   */
  protected DiscreteFactor ensureOperandCompatible (DiscreteFactor ptl) { return ptl; };

  // Does destructive multiplication on this, assuming this has all
  // the variables in pot.
  protected abstract void multiplyByInternal (DiscreteFactor ptl);

  /**
   * Returns the elementwise product of this potential and
   * another one.
   */
  public Factor multiply (Factor dist)
  {
    Factor result = duplicate ();
    result.multiplyBy (dist);
    return result;
  }

  /**
   * Does the conceptual equivalent of this /= pot.
   * Assumes that pot's variables are a subset of
   * this potential's.
   */
  public void divideBy (Factor pot)
  {
    DiscreteFactor pot1 = (DiscreteFactor) pot; // cheating
    expandToContain (pot1);
    pot1 = ensureOperandCompatible (pot1);
    divideByInternal (pot1);
  }


  // Does destructive divison on this, assuming this has all
  // the variables in pot.
  protected abstract void divideByInternal (DiscreteFactor ptl);


  // xxx Should return an assignment
  public int argmax ()
  {
    int bestIdx = 0;
    double bestVal = probs.singleValue (0);

    for (int idx = 1; idx < probs.numLocations (); idx++) {
      double val = probs.singleValue (idx);
      if (val > bestVal) {
        bestVal = val;
        bestIdx = idx;
      }
    }

    return bestIdx;
  }

  private static final double EPS = 1e-5;

  public Assignment sample (Random r)
  {
    int loc = sampleLocation (r);
    return location2assignment (loc);
  }

  private Assignment location2assignment (int loc)
  {
    return new DenseAssignmentIterator (vars, loc).assignment ();
  }

  public int sampleLocation (Random r)
  {
    double sum = sum();
    double sampled = r.nextUniform () * sum;

    double cum = 0;
    for (int idx = 0; idx < probs.numLocations (); idx++) {
      double val = value (idx);
        cum += val;

      if (sampled <= cum + EPS) {
        return idx;
      }
    }

    throw new RuntimeException
            ("Internal errors: Couldn't sample from potential "+this+"\n"+dump ()+"\n Using value "+sampled);
  }


  public boolean almostEquals (Factor p)
  {
    return almostEquals (p, Maths.EPSILON);
  }

  public boolean almostEquals (Factor p, double epsilon)
  {
    if (!(p instanceof AbstractTableFactor)) {
      return false;
    }

    DiscreteFactor p2 = (DiscreteFactor) p;
    if (!varSet ().containsAll (p2.varSet ())) {
      return false;
    }
    if (!p2.varSet ().containsAll (varSet ())) {
      return false;
    }

/* TODO: fold into probs.almostEqauals() if variable ordering
     *  issues ever resolved.  Also, consider using this in all
     *  those hasConverged() functions.
     */
    int[] projection = largeIdxToSmall (p2);
    for (int loc1 = 0; loc1 < probs.numLocations (); loc1++) {
      int idx2 = projection[loc1];
      double v1 = valueAtLocation (loc1);
      double v2 = p2.value (idx2);
      if (Math.abs (v1 - v2) > epsilon) {
        return false;
      }
    }

    return true;
  }



  public Object clone ()
  {
    return duplicate ();
  }

  public String toString ()
  {
    StringBuffer s = new StringBuffer (1024);
    s.append ("[");
    s.append (GeneralUtils.classShortName(this));
    s.append (" : ");
    s.append (varSet ());
    s.append ("]");
    return s.toString ();
  }

  public String dump ()
  {
    StringBuffer s = new StringBuffer (1024);
    s.append (this.toString ());
    s.append ("\n");

    int indices[] = new int[numVars];
    for (int loc = 0; loc < probs.numLocations (); loc++) {
      int idx = probs.indexAtLocation (loc);
      probs.singleToIndices (idx, indices);
      for (int j = 0; j < numVars; j++) {
        s.append (indices[j]);
        s.append ("  ");
      }
      double val = probs.singleValue (idx);
      s.append (val);
      s.append ("\n");
    }
    s.append (" Sum = ").append (sum ()).append ("\n");

    return s.toString ();
  }

  public boolean isNaN ()
  {
    return probs.isNaN ();
  }

  public void printValues ()
  {
    System.out.print ("[");
    for (int i = 0; i < probs.numLocations (); i++) {
      System.out.print (probs.valueAtLocation (i));
      System.out.print (", ");
    }
    System.out.print ("]");
  }

  public void printSizes ()
  {
    int[] sizes = new int[numVars];
    probs.getDimensions (sizes);
    System.out.print ("[");
    for (int i = 0; i < numVars; i++) {
      System.out.print (sizes[i] + ", ");
    }
    System.out.print ("]");
  }

  public Variable findVariable (String name)
  {
    for (int i = 0; i < varMap.size (); i++) {
      Variable var = (Variable) varMap.lookupObject (i);
      if (var.getLabel().equals (name)) return var;
    }
    return null;
  }

  public int numLocations ()
  {
    return probs.numLocations ();
  }

  public int indexAtLocation (int loc)
  {
    return probs.indexAtLocation (loc);
  }

  public Variable getVariable (int i)
  {
    return (Variable) varMap.lookupObject (i);
  }


  // Serialization
  private static final long serialVersionUID = 1;

  // If seralization-incompatible changes are made to these classes,
  //  then smarts can be added to these methods for backward compatibility.
  private void writeObject (ObjectOutputStream out) throws IOException {
     out.defaultWriteObject ();
   }

  private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
     in.defaultReadObject ();
    // rerun initializers of transient fields
     projectionCache = new TIntObjectHashMap ();
  }

  public void divideBy (double v)
  {
    probs.divideEquals (v);
  }

  /** Use of this method is discouraged. */
  public abstract void setLogValue (Assignment assn, double logValue);

  /** Use of this method is discouraged. */
  public abstract void setLogValue (AssignmentIterator assnIt, double logValue);

  /** Use of this method is discouraged. */
  public abstract void setValue (AssignmentIterator assnIt, double logValue);

  static Factor hackyMixture (AbstractTableFactor ptl1, AbstractTableFactor ptl2, double weight)
  {
    // check that alphabets match
    if (ptl1.varMap.size() != ptl2.varMap.size()) {
      throw new IllegalArgumentException ();
    }
    for (int i = 0; i < ptl2.varMap.size(); i++) {
      if (ptl1.varMap.lookupObject (i) != ptl2.varMap.lookupObject (i)) {
        throw new IllegalArgumentException ();
      }
    }
    if (ptl1.ensureOperandCompatible (ptl2) != ptl2)
      throw new IllegalArgumentException ();

    AbstractTableFactor result = new TableFactor (ptl1.varMap);
    for (int loc1 = 0; loc1 < ptl1.numLocations (); loc1++) {
      double val1 = ptl1.valueAtLocation (loc1);
      int idx = ptl1.indexAtLocation (loc1);
      double val2 = ptl2.value (idx);
      result.setRawValue (idx, weight * val1 + (1 - weight) * val2);
    }

    /*
    TIntHashSet indices = new TIntHashSet ();
    for (int loc = 0; loc < ptl1.probs.numLocations (); loc++) {
      indices.add (ptl1.probs.indexAtLocation (loc));
    }
    for (int loc = 0; loc < ptl2.probs.numLocations (); loc++) {
      indices.add (ptl2.probs.indexAtLocation (loc));
    }

    int[] idxs = indices.toArray ();
    Arrays.sort (idxs);

    double[] vals = new double[idxs.length];
    if (ptl1 instanceof LogTableFactor) {  // hack
      for (int i = 0; i < idxs.length; i++) {
        vals[i] = weight * Math.exp (ptl1.probs.singleValue (idxs[i])) + (1 - weight) * Math.exp (ptl2.probs.singleValue (idxs[i]));
        vals[i] = Math.log (vals[i]);
      }

    } else {
      for (int i = 0; i < idxs.length; i++) {
        vals[i] = weight * ptl1.probs.singleValue (idxs[i]) + (1 - weight) * ptl2.probs.singleValue (idxs[i]);
      }
    }

    int[] szs = new int [ptl1.probs.getNumDimensions ()];
    ptl1.probs.getDimensions (szs);
    SparseMatrixn m = new SparseMatrixn (szs, idxs, vals);

    AbstractTableFactor result = ptl1.createBlankSubset (ptl1.varMap);
    result.setValues (m);
      */

    if (!ptl1.isNaN () && !ptl2.isNaN () && result.isNaN ()) {
      System.err.println ("Oops! NaN in averaging.\n   P1"+ptl1.isNaN ()+"\n  P2:"+ptl2.isNaN ()+"\n  Result:"+result.isNaN ());
    }
    return result;
  }


  protected abstract double rawValue (int singleIdx);

  public double[] toValueArray () {
    Matrix matrix = getValueMatrix ();
    double[] arr = new double [matrix.numLocations ()];
    for (int i = 0; i < arr.length; i++) {
      arr[i] = matrix.valueAtLocation (i);
    }
    return arr;
  }

  public int singleIndex (int[] smallDims)
  {
    return probs.singleIndex (smallDims);
  }

  public abstract Matrix getValueMatrix ();

  public abstract Matrix getLogValueMatrix ();

  public abstract void setLogValues (double[] vals);

  public abstract void setValues (double[] vals);

  public double[] getLogValues ()
  {
    return ((Matrixn)getLogValueMatrix ()).toArray ();
  }

  public double[] getValues ()
  {
    return ((Matrixn)getValueMatrix ()).toArray ();
  }

  /** Adds a constant to all values in the table.  This is most useful to add a small constant to avoid zeros. */
  public void plusEquals (double v)
  {
    for (int loc = 0; loc < numLocations (); loc++) {
       plusEqualsAtLocation (loc, v);
    }
  }

  protected abstract void plusEqualsAtLocation (int loc, double v);

}
