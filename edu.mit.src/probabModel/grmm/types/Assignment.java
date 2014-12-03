/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package probabModel.grmm.types;

import edu.umass.cs.mallet.base.types.Matrixn;
import gnu.trove.TIntArrayList;
import gnu.trove.TObjectIntHashMap;

import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;


/**
 * An assignment to a bunch of variables.
 * <p/>
 * Note that outcomes are always integers.  If you
 * want them to be something else, then the Variables
 * all have outcome Alphabets; for example, see
 * {@link Variable#lookupOutcome}.
 * <p/>
 * Created: Tue Oct 21 15:11:11 2003
 *
 * @author <a href="mailto:casutton@cs.umass.edu">Charles Sutton</a>
 * @version $Id: Assignment.java,v 1.11 2006/05/08 19:01:16 casutton Exp $
 */
public class Assignment {

  /* Maps from vars => indicies */
  List vars = new ArrayList ();
  TObjectIntHashMap var2idx;

  /* Maps from indices => outcomes. */
  TIntArrayList values;

  /**
   * Creates an empty assignment.
   */
  public Assignment ()
  {
    var2idx = new TObjectIntHashMap ();
    values = new TIntArrayList ();
  }

  public Assignment (Variable var, int outcome)
  {
    this ();
    setValue (var, outcome);
  }

  /**
   * Creates an assignemnt for the given variables.
   */
  public Assignment (Variable[] vars, int[] outcomes)
  {
    var2idx = new TObjectIntHashMap ();
    values = new TIntArrayList (vars.length);
    setValues (vars, outcomes);
  }

  /**
   * Creates an assignemnt for the given variables.
   */
  public Assignment (List vars, int[] outcomes)
  {
    var2idx = new TObjectIntHashMap ();
    values = new TIntArrayList (vars.size ());
    setValues ((Variable[]) vars.toArray (new Variable[0]), outcomes);
  }

  /**
   * Creates an assignment over all Variables in a model.
   * The assignment will assign outcomes[i] to the variable
   * <tt>mdl.get(i)</tt>
   */
  public Assignment (FactorGraph mdl, int[] outcomes)
  {
    var2idx = new TObjectIntHashMap ();
    values = new TIntArrayList (mdl.numVariables ());
    Variable[] vars = new Variable [mdl.numVariables ()];
    for (int i = 0; i < vars.length; i++) vars[i] = mdl.get (i);
    setValues (vars, outcomes);
  }

  /**
   * Returns the union of two Assignments.  That is, the value of a variable in the returned Assignment
   *  will be as specified in one of the given assignments.
   * <p>
   * If the assignments share variables, the value in the new Assignment for those variables in
   *  undefined.
   *
   * @param assn1 One assignment.
   * @param assn2 Another assignment.
   * @return A newly-created Assignment.
   */
  public static Assignment union (Assignment assn1, Assignment assn2) {
    Assignment ret = assn1.duplicate ();
    ret.setValues (assn2);
    return ret;
  }

  /**
   * Returns a new assignment which only assigns values to those variabes in a given clique.
   * @param assn A large assignment
   * @param varSet Which variables to restrict assignment o
   * @return A newly-created Assignment
   */
  public static Assignment restriction (Assignment assn, VarSet varSet)
  {
    Variable[] vars = new Variable [varSet.size ()];
    int[] outcomes = new int [varSet.size()];
    for (int i = 0; i < outcomes.length; i++) {
      Variable var = varSet.get (i);
      outcomes[i] = assn.get (var);
      vars [i] = var;
    }
    return new Assignment (vars, outcomes);
  }

  /**
   * Sets one variable in the assignment.
   */
  public void setValue (Variable var, int outcome)
  {
    int vidx = var2idx.get (var);
    if (vidx < 0) {
      vars.add (var);
      vidx = values.size();
      var2idx.put (var, vidx);
    }

    if (vidx < values.size ()) {
      values.set (vidx, outcome);
    } else {
      values.insert (vidx, outcome);
    }
  }

  /**
   * Add lots of variables to the assignment.
   */
  public void setValues (Variable[] vars, int[] outcomes)
  {
    for (int i = 0; i < vars.length; i++) {
      setValue (vars[i], outcomes[i]);
    }
  }

  /**
   * Modify this assignment so that it agrees with another Assignment.
   */
  public void setValues (Assignment assn)
  {
    for (int i = 0; i < assn.var2idx.size (); i++) {
      Variable var = (Variable) assn.vars.get (i);
      int value = assn.values.get (i);
      this.setValue (var, value);
    }
  }

  /**
   * Returns the value of var in this assigment.
   */
  public int get (Variable var)
  {
    int idx = var2idx.get (var);
    if (idx == -1)
      throw new IndexOutOfBoundsException
              ("Assignment does not give a value for variable " + var);

    return values.get (idx);
  }

  public Object getObject (Variable var)
  {
    return var.lookupOutcome (get (var));
  }

  public Variable getVariable (int i)
  {
    return (Variable) vars.get (i);
  }

  /** Returns all variables which are assigned to. */
  public Variable[] getVars () {
    return (Variable[]) vars.toArray (new Variable [0]);
  }

  public int size ()
  {
    return values.size ();
  }

  public static Assignment makeFromSingleIndex (VarSet clique, int idx)
  {
    int N = clique.size ();
    Variable[] vars = clique.toVariableArray ();
    int[] idxs = new int [N];
    int[] szs = new int [N];

    // compute sizes
    for (int i = 0; i < N; i++) {
      Variable var = vars[i];
      szs[i] = var.getNumOutcomes ();
    }

    Matrixn.singleToIndices (idx, idxs, szs);
    return new Assignment (vars, idxs);
  }

  /**
   * Converts this assignment into a unique integer.
   * All different assignments to the same variables are guaranteed to
   * have unique integers.  The range of the index will be between
   * 0 (inclusive) and M (exclusive), where M is the product of all
   * cardinalities of all variables in this assignment.
   *
   * @return An integer
   */
  public int singleIndex ()
  {
    // these could be cached
    int[] szs = new int [values.size ()];
    for (int i = 0; i < values.size (); i++) {
      Variable var = (Variable) vars.get (i);
      szs[i] = var.getNumOutcomes ();
    }

    int[] idxs = values.toNativeArray ();
    return Matrixn.singleIndex (szs, idxs);
  }

  public Assignment duplicate ()
  {
    Assignment ret = new Assignment ();
    ret.var2idx = (TObjectIntHashMap) var2idx.clone ();
    ret.vars = new ArrayList (vars);
    ret.values = (TIntArrayList) values.clone ();
    return ret;
  }

  public void dump ()
  {
    dump (new PrintWriter (new OutputStreamWriter (System.out), true));
  }

  public void dump (PrintWriter out)
  {
    for (int i = 0; i < var2idx.size (); i++) {
      Variable var = (Variable) vars.get (i);
      Object obj = getObject (var);
      out.println (var + " " + obj);
    }
  }


  public void dumpNumeric ()
  {
    for (int i = 0; i < var2idx.size (); i++) {
      Variable var = (Variable) vars.get (i);
      int outcome = get (var);
      System.out.println (var + " " + outcome);
    }
  }

  void setValues (int[] outcomes)
  {
    if (outcomes.length != var2idx.size ())
      throw new IllegalArgumentException
              ("Bad number of outcomes: expected " + var2idx.size () + " got " + outcomes.length);
    for (int i = 0; i < outcomes.length; i++) {
      values.set (i, outcomes[i]);
    }
  }

  /** Returns true if this assignment specifies a value for <tt>var</tt> */
  public boolean containsVar (Variable var)
  {
    int idx = var2idx.get (var);
    return (idx != -1);
  }
}
