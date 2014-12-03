/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package probabModel.grmm.types;

import edu.umass.cs.mallet.base.types.LabelAlphabet;
import edu.umass.cs.mallet.base.util.PropertyList;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;


/**
 *  Class for a discrete random variable in a graphical model.
 *
 * Created: Thu Sep 18 09:32:25 2003
 *
 * @author <a href="mailto:casutton@cs.umass.edu">Charles Sutton</a>
 * @version $Id: Variable.java,v 1.3 2006/02/03 04:25:32 casutton Exp $
 */
public class Variable implements Comparable {

  private String label;  // name of this variable
  private LabelAlphabet outcomes;

  private static int counter = 0;

  /**
   * Creates a new variable with the given outcomes.
   */
  public Variable (LabelAlphabet outs) {
    this.outcomes = outs;
    if (outs.size() < 1) {
      throw new IllegalArgumentException
        ("Attempt to create variable with "+outs.size()+" outcomes.");
    }
    setIndex();
  }

  public Variable (int numOutcomes)
  {
    if (numOutcomes < 1) {
      throw new IllegalArgumentException
        ("Attempt to create variable with "+numOutcomes+" outcomes.");
    }

    outcomes = new LabelAlphabet ();
    /* Setup default outcomes */
    for (int i = 0; i < numOutcomes; i++) {
      outcomes.lookupIndex (new Integer (i));
    }
    setIndex();
  }

  private void setIndex()
  {
    setLabel ("VAR" + (counter++));
  }


  public String getLabel ()
  {
    return label;
  }

  public void setLabel (String label)
  {
    this.label = label;
  }

  public int getNumOutcomes () {
    return outcomes.size();
  }

  public Object lookupOutcome (int i) {
    return outcomes.lookupObject (i);
  }

  public LabelAlphabet getLabelAlphabet ()
  {
    return outcomes;
  }

  public int compareTo(Object o)
  {
    /*
    Variable var = (Variable) o;
    return getLabel().compareTo (var.getLabel());
    */
    int index = hashCode();
    int index2 = ((Variable)o).hashCode();

    if (index == index2) {
      return 0;
    } else if (index < index2) {
      return -1;
    } else {
      return 1;
    }
    /**/
  }

  transient private PropertyList properties = null;

  public void setNumericProperty (String key, double value)
  {
    properties = PropertyList.add (key, value, properties);
  }

  public double getNumericProperty (String key)
  {
    return properties.lookupNumber (key);
  }

  public String toString ()
  {
    return label;
  }

  // Serialization garbage

  private static final long serialVersionUID = 1;
  private static final int CURRENT_SERIAL_VERSION = 1;

  private void writeObject (ObjectOutputStream out) throws IOException
  {
    out.defaultWriteObject ();
    out.writeInt (CURRENT_SERIAL_VERSION);
  }


  private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException
  {
    in.defaultReadObject ();
    in.readInt ();
  }


}
