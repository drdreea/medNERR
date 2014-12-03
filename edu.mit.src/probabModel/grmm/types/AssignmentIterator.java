/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package probabModel.grmm.types;

import java.util.Iterator;

/**
 * Iterates over the assignments to a set of variables.
 *  This is never instantiated by user code; instead, use
 *  one of the many assignmentIterator() methods.
 *
 *   DOCTODO: Add note about difference between using this class and iterating
 *    over assignments.
 *   DOCTODO: Explain why advance() is useful instead of next.
 *
 * Created: Sun Nov  9 21:04:03 2003
 *
 * @author <a href="mailto:casutton@cs.umass.edu">Charles Sutton</a>
 * @version $Id: AssignmentIterator.java,v 1.3 2005/12/15 22:37:05 casutton Exp $
 */public interface AssignmentIterator extends Iterator {

  void advance();

  int indexOfCurrentAssn ();

  Assignment assignment ();
}
