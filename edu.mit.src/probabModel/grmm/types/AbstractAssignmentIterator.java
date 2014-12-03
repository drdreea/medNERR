/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package probabModel.grmm.types;

import edu.umass.cs.mallet.base.types.Matrixn;

/**
 * Created: Dec 15, 2005
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: AbstractAssignmentIterator.java,v 1.2 2006/02/03 04:25:32 casutton Exp $
 */
abstract class AbstractAssignmentIterator implements AssignmentIterator {

  protected VarSet vertsList;
  protected int max = 1;
  private int[] sizes;
  private Assignment assn = null;


  protected AbstractAssignmentIterator (VarSet verts)
  {
    vertsList = verts;
    initSizes ();
  }

  private void initSizes ()
  {
    sizes = new int [vertsList.size()];
    for (int i = 0; i < sizes.length; i++) {
      sizes[i] = vertsList.get (i).getNumOutcomes ();
    }
    max = vertsList.weight ();
  }

  protected Assignment constructAssignment ()
  {
    int current = indexOfCurrentAssn ();
    if (sizes == null) initSizes ();  // Lazily build sizes array
    int[] outcomes = new int [sizes.length];
    Matrixn.singleToIndices (current, outcomes, sizes);
    Variable[] vars = (Variable[]) vertsList.toArray (new Variable [0]);
    return new Assignment (vars, outcomes);
  }

  public void remove() {
    throw new UnsupportedOperationException
      ("Attempt to remave assignment from Clique.");
  }

  public Assignment assignment ()
  {
    if (assn == null) {
      assn = constructAssignment ();
      return assn;
    } else {
      int current = indexOfCurrentAssn ();
      int[] outcomes = new int [sizes.length];
      Matrixn.singleToIndices (current, outcomes, sizes);
      assn.setValues (outcomes);
      return assn;
    }
  }

  public Object next()
  {
    Assignment assn = assignment ();
    advance ();
    return assn;
  }
}
