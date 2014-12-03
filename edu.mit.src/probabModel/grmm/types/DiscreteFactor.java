/* Copyright (C) 2006 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://mallet.cs.umass.edu/
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package probabModel.grmm.types;

import edu.umass.cs.mallet.base.util.Random;

/**
 * $Id: DiscreteFactor.java,v 1.1 2006/01/25 23:56:44 casutton Exp $
 */
public interface DiscreteFactor extends Factor {
  
  int sampleLocation (Random r);

  double value (int index);

  int numLocations ();

  double valueAtLocation (int loc);

  int indexAtLocation (int loc);

  double[] toValueArray ();

  int singleIndex (int[] smallDims);
}
