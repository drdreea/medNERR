/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package probabModel.grmm.types;

import gnu.trove.THashSet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;



/**
 *  A clique is a collection of nodes in a graph that are all
 *   adjacent.  We implement it cheaply by delegating to a HashSet.
 *
 * Created: Wed Sep 17 12:50:01 2003
 *
 * @author <a href="mailto:casutton@cs.umass.edu">Charles Sutton</a>
 * @version $Id: HashVarSet.java,v 1.1 2006/02/03 04:25:32 casutton Exp $
 */
// xxx Perhaps this should just use an alphabet and not implement Set.
public class HashVarSet implements VarSet {

	private THashSet verts = new THashSet();
	private ArrayList vertsList = new ArrayList ();

  /**
	 * Create an empty clique.
	 */
	public HashVarSet () {
		super ();
	} // Clique constructor

	/**
	 * Create a two-clique given an edge in a graph.
	 *
	public HashVarSet (Edge e) {
		super ();
		add (e.getVertexA());
		add (e.getVertexB());
	}
  */

  /**
	 * Create a Clique given a Collection of nodes.
	 */
	public HashVarSet (Collection c) {
		super();
		addAll (c);
	}

	public HashVarSet (Variable[] vars) {
		super();
		addAll (Arrays.asList (vars));
	}

	public Variable get (int idx) {
		return (Variable) vertsList.get (idx);
	}

	public String getLabel () {
		return toString ();
	}

	/**
	 * Returns the intersection of two cliques.
	 */
	public Set intersection (VarSet c)
	{
		THashSet set = new THashSet (vertices());
		set.retainAll (c.vertices());
		return set;
	}


// Code for delegation of java.util.AbstractSet methods to verts

  /* Can't delegate to THashMap, because in early versions of Trove (that we are frozen at)
   *  the THashMap.hashCode() isn't consistent with equals.  This is workaround, which may
   *  be removed when we upgrade Trove. */
  public int hashCode() {
    int ret = 39;
    for (Iterator it = verts.iterator (); it.hasNext ();) {
      Object o = it.next ();
      ret = 59 * ret + o.hashCode ();
    }
    return ret;
	}

	public boolean equals(Object object) {
		return verts.equals(object);
	}

	public boolean removeAll(Collection collection) {
		return verts.removeAll(collection);
	}

	public Set vertices() {
		return Collections.unmodifiableSet (verts);
	}
 
	public Variable[] toVariableArray () 
	{
		// Cannot just do (Variable[]) vertsList.toArray() because that
		// would cause a ClassCastException.  I suppose that's why
		// toArray is overloaded...
		return (Variable[]) vertsList.toArray (new Variable[] {});
	}

// Code for delegation of java.util.AbstractCollection methods to verts

	public String toString() 
	{
		String val = "(C";
		for (Iterator it = vertsList.iterator(); it.hasNext();) {
			val += " ";
			val += it.next().toString();
		}
		val += ")";
		return val;
	}

	public boolean addAll(Collection collection) {
		vertsList.addAll(collection);
		return verts.addAll(collection);
	}

	/** Returns the variables in this clique as an array.  If the
	 * clique is not modified, then the ordering will remain consistent
	 * across calls.
	 */
	public Object[] toArray(Object[] objectArray) {
		// Using vertsList here assures that toArray() always returns the
		// same ordering. 
		return vertsList.toArray(objectArray);
	}


	/** Returns the variables in this clique as an array.  If the
	 * clique is not modified, then the ordering will remain consistent
	 * across calls.
	 */
	public Object[] toArray() {
		// Using vertsList here assures that toArray() always returns the
		// same ordering.
		return vertsList.toArray();
	}

	public boolean containsAll(Collection collection) {
		return verts.containsAll(collection);
	}

	public boolean retainAll(Collection collection) {
		return verts.retainAll(collection);
	}
	
// Code for delegation of java.util.HashSet methods to verts

	public Object clone() {
		return verts.clone();
	}

	public boolean add(Object object) {
		vertsList.add (object);
		return verts.add (object);
	}

	public boolean contains(Object object) {
		return verts.contains(object);
	}

	public int size() {
		return verts.size();
	}

	// Returns the total size of a dense discrete variable over this clique.
	public int weight () {
		int tot = 1;
		for (int i = 0; i < vertsList.size(); i++) {
			Variable var = (Variable) vertsList.get (i);
			tot *= var.getNumOutcomes ();
		}
		return tot;
	}

	public Iterator iterator() {
		return verts.iterator();
	}

	public boolean remove(Object object) {
    vertsList.remove (object);
    return verts.remove(object);
	}

	public void clear() {
    vertsList.clear ();
    verts.clear();
	}

	public boolean isEmpty() {
		return verts.isEmpty();
	}

	// Iterating over assignments

	public AssignmentIterator assignmentIterator ()
	{
		return new DenseAssignmentIterator (this);
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
    int version = in.readInt ();
  }

} // Clique
