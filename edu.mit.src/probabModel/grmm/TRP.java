package probabModel.grmm;

/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

import edu.umass.cs.mallet.base.util.MalletLogger;
import edu.umass.cs.mallet.grmm.types.DirectedModel;
import edu.umass.cs.mallet.grmm.types.Tree;
import gnu.trove.THashSet;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;

import java.util.logging.Logger;
import java.util.*;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org._3pq.jgrapht.UndirectedGraph;
import org._3pq.jgrapht.Graph;
import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.traverse.BreadthFirstIterator;
import org._3pq.jgrapht.graph.SimpleGraph;

import probabModel.grmm.types.*;


/**
 * Implementation of Wainwright's TRP schedule for loopy BP
 * in general graphical models.
 *
 * @author Charles Sutton
 * @version $Id: TRP.java,v 1.12 2006/02/03 04:25:32 casutton Exp $
 */
public class TRP extends AbstractBeliefPropagation {

  private static Logger logger = MalletLogger.getLogger (TRP.class.getName ());

  private static final boolean reportSpanningTrees = false;

  private TreeFactory factory;

  private TerminationCondition terminator;

  private Random random = new Random ();

  /* Make sure that we've included all edges before we terminate. */
  transient private TIntObjectHashMap factorTouched;

  transient private boolean hasConverged;

  transient private int iterUsed = 0;

  public TRP ()
  {
    this (null, null);
  }

  public TRP (TreeFactory f)
  {
    this (f, null);
  }

  public TRP (TerminationCondition cond)
  {
    this (null, cond);
  }

  public TRP (TreeFactory f, TerminationCondition cond)
  {
    factory = f;
    terminator = cond;
  }

  public static TRP createForMaxProduct ()
  {
    TRP trp = new TRP ();
    trp.setMessager (new MaxProductMessageStrategy ());
    return trp;
  }

  // Accessors

  public TRP setTerminator (TerminationCondition cond)
  {
    terminator = cond;
    return this;
  }

  // xxx should this be static?
  public void setRandomSeed (long seed) { random = new Random (seed); }

  public boolean isConverged () { return hasConverged; }

  public int iterationsUsed () { return iterUsed; }

  protected void initForGraph (FactorGraph m)
  {
    super.initForGraph (m);

    int numNodes = m.numVariables ();
    factorTouched = new TIntObjectHashMap (numNodes);
    hasConverged = false;

    if (factory == null) {
      factory = new AlmostRandomTreeFactory ();
    }

    if (terminator == null) {
      terminator = new DefaultConvergenceTerminator ();
    } else {
      terminator.reset ();
    }
  }

  private static edu.umass.cs.mallet.grmm.types.Tree graphToTree (Graph g) throws Exception
  {
    // Perhaps handle gracefully?? -cas
    if (g.vertexSet ().size () <= 0) {
      throw new RuntimeException ("Empty graph.");
    }
    Tree tree = new edu.umass.cs.mallet.grmm.types.Tree ();
    Object root = g.vertexSet ().iterator ().next ();
    tree.add (root);

    for (Iterator it1 = new BreadthFirstIterator (g, root); it1.hasNext();) {
      Object v1 = it1.next ();
      for (Iterator it2 = g.edgesOf (v1).iterator (); it2.hasNext ();) {
        Edge edge = (Edge) it2.next ();
        Object v2 = edge.oppositeVertex (v1);
          if (tree.getParent (v1) != v2) {
            tree.addNode (v1, v2);
            assert tree.getParent (v2) == v1;
          }
        }
      }

    return tree;
  }

  /**
   * Interface for tree-generation strategies for TRP.
   * <p/>
   * TRP works by repeatedly doing exact inference over spanning tree
   * of the original graph.  But the trees can be chosen arbitrarily.
   * In fact, they don't need to be spanning trees; any acyclic
   * substructure will do.  Users of TRP can tell it which strategy
   * to use by passing in an implementation of TreeFactory.
   */
  public interface TreeFactory extends Serializable {
    public edu.umass.cs.mallet.grmm.types.Tree nextTree (FactorGraph mdl);
  }

  ;

  // This works around what appears to be a bug in OpenJGraph
  // connected sets.
  private static class SimpleUnionFind {

    private Map obj2set = new THashMap ();

    private Set findSet (Object obj)
    {
      Set container = (Set) obj2set.get (obj);
      if (container != null) {
        return container;
      } else {
        Set newSet = new THashSet ();
        newSet.add (obj);
        obj2set.put (obj, newSet);
        return newSet;
      }
    }

    private void union (Object obj1, Object obj2)
    {
      Set set1 = findSet (obj1);
      Set set2 = findSet (obj2);
      set1.addAll (set2);
      for (Iterator it = set2.iterator (); it.hasNext ();) {
        Object obj = it.next ();
        obj2set.put (obj, set1);
      }
    }

    public boolean noPairConnected (VarSet varSet)
    {
      for (int i = 0; i < varSet.size (); i++) {
        for (int j = i + 1; j < varSet.size (); j++) {
          Variable v1 = varSet.get (i);
          Variable v2 = varSet.get (j);
          if (findSet (v1) == findSet (v2)) {
            return false;
          }
        }

      }
      return true;
    }

    public void unionAll (Factor factor)
    {
      VarSet varSet = factor.varSet ();
      for (int i = 0; i < varSet.size (); i++) {
        Variable var = varSet.get (i);
        union (var, factor);
      }
    }

  }


  /**
   * Always adds edges that have not been touched, after that
   * adds random edges.
   */
  public class AlmostRandomTreeFactory implements TreeFactory {

    public Tree nextTree (FactorGraph fullGraph)
    {
      SimpleUnionFind unionFind = new SimpleUnionFind ();
      ArrayList edges = new ArrayList (fullGraph.factors ());
      ArrayList goodEdges = new ArrayList (fullGraph.numVariables ());
      Collections.shuffle (edges, random);

      // First add all edges that haven't been used so far
      try {
        for (Iterator it = edges.iterator (); it.hasNext ();) {
          Factor factor = (Factor) it.next ();
          VarSet varSet = factor.varSet ();
          if (!isFactorTouched (factor) && unionFind.noPairConnected (varSet)) {
            goodEdges.add (factor);
            unionFind.unionAll (factor);
            it.remove ();
          }
        }

        // Now add as many other edges as possible
        for (Iterator it = edges.iterator (); it.hasNext ();) {
          Factor factor = (Factor) it.next ();
          VarSet varSet = factor.varSet ();
          if (unionFind.noPairConnected (varSet)) {
            goodEdges.add (factor);
            unionFind.unionAll (factor);
          }
        }

        for (Iterator it = goodEdges.iterator (); it.hasNext ();) {
          Factor factor = (Factor) it.next ();
          touchFactor (factor);
        }

        UndirectedGraph g = new SimpleGraph ();
        for (Iterator it = fullGraph.variablesIterator (); it.hasNext ();) {
          Variable var = (Variable) it.next ();
          g.addVertex (var);
        }

        for (Iterator it = goodEdges.iterator (); it.hasNext ();) {
          Factor factor = (Factor) it.next ();
          g.addVertex (factor);
          for (Iterator vit = factor.varSet ().iterator (); vit.hasNext ();) {
            Variable var = (Variable) vit.next ();
            g.addEdge (factor, var);
          }
        }

        Tree tree = graphToTree (g);
        if (reportSpanningTrees) {
          System.out.println ("********* SPANNING TREE *************");
          tree.dump();
          System.out.println ("********* END TREE *************");
        }

        return tree;
      } catch (Exception e) {
        e.printStackTrace ();
        throw new RuntimeException (e);
      }
    }
  }

  ;

  /**
   * Generates spanning trees cyclically from a predefined collection.
   */
  static public class TreeListFactory implements TreeFactory {

    private List lst;
    private Iterator it;

    public TreeListFactory (List l)
    {
      lst = l;
      it = lst.iterator ();
    }

    public TreeListFactory (edu.umass.cs.mallet.grmm.types.Tree[] arr)
    {
      lst = new ArrayList (java.util.Arrays.asList (arr));
      it = lst.iterator ();
    }

    public edu.umass.cs.mallet.grmm.types.Tree nextTree (FactorGraph mdl)
    {
      // If no more trees, rewind.
      if (!it.hasNext ()) {
        it = lst.iterator ();
      }
      return (edu.umass.cs.mallet.grmm.types.Tree) it.next ();
    }

  }

  ;

  // Termination conditions

  // will this need to be subclassed from outside?  Will such
  // subclasses need access to the private state of TRP?
  static public interface TerminationCondition extends Cloneable, Serializable {
    // This takes the instances of trp as a parameter so that if a
    //  TRP instance is cloned, and the terminator copied over, it
    //  will still work.
    public boolean shouldContinue (TRP trp);

    public void reset ();

    // boy do I hate Java cloning
    public Object clone () throws CloneNotSupportedException;
  }

  static public class IterationTerminator implements TerminationCondition {
    int current;
    int max;

    public void reset () { current = 0; }

    public IterationTerminator (int m)
    {
      max = m;
      reset ();
    }

    ;

    public boolean shouldContinue (TRP trp)
    {
      current++;
      if (current >= max) {
        logger.finest ("***TRP quitting: Iteration " + current + " >= " + max);
      }
      return current <= max;
    }

    ;

    public Object clone () throws CloneNotSupportedException
    {
      return super.clone ();
    }
  }

  //xxx Delta is currently ignored.
  public static class ConvergenceTerminator implements TerminationCondition {
    double delta = 0.01;

    public ConvergenceTerminator () {}

    ;

    public ConvergenceTerminator (double delta) { this.delta = delta; }

    public void reset ()
    {
    }

    public boolean shouldContinue (TRP trp)
    {
/*
			if (oldMessages != null) 
				retval = !checkForConvergence (trp);
			copyMessages(trp);
			
			return retval;
			*/
      boolean retval = !trp.hasConverged (delta);
      trp.copyOldMessages ();
      return retval;
    }

    public Object clone () throws CloneNotSupportedException
    {
      return super.clone ();
    }

  }

  // Runs until convergence, but doesn't stop until all edges have
  // been used at least once, and always stops after 1000 iterations.
  public static class DefaultConvergenceTerminator implements TerminationCondition {
    ConvergenceTerminator cterminator;
    IterationTerminator iterminator;

    String msg;

    public DefaultConvergenceTerminator () { this (0.001, 1000); }

    public DefaultConvergenceTerminator (double delta, int maxIter)
    {
      cterminator = new ConvergenceTerminator (delta);
      iterminator = new IterationTerminator (maxIter);
      msg = "***TRP quitting: over " + maxIter + " iterations";
    }

    public void reset ()
    {
      iterminator.reset ();
      cterminator.reset ();
    }

    // Terminate if converged or at insanely high # of iterations
    public boolean shouldContinue (TRP trp)
    {
      boolean notAllTouched = !trp.allEdgesTouched ();

      if (!iterminator.shouldContinue (trp)) {
        logger.warning (msg);
        if (notAllTouched) {
          logger.warning ("***TRP warning: Not all edges used!");
        }
        return false;
      }

      if (notAllTouched) {
        return true;
      } else {
        return cterminator.shouldContinue (trp);
      }
    }

    public Object clone () throws CloneNotSupportedException
    {
      DefaultConvergenceTerminator dup = (DefaultConvergenceTerminator)
              super.clone ();
      dup.iterminator = (IterationTerminator) iterminator.clone ();
      dup.cterminator = (ConvergenceTerminator) cterminator.clone ();
      return dup;
    }

  }

  // And now, the heart of TRP:

  public void computeMarginals (FactorGraph m)
  {
    resetMessagesSentAtStart ();
    initForGraph (m);

    int iter = 0;
    while (terminator.shouldContinue (this)) {
      logger.finer ("TRP iteration " + (iter++));
      edu.umass.cs.mallet.grmm.types.Tree tree = factory.nextTree (m);
      propagate (tree);
//      dump();
    }
    iterUsed = iter;
    logger.info ("TRP used " + iter + " iterations.");

    doneWithGraph (m);
  }


  private void propagate (edu.umass.cs.mallet.grmm.types.Tree tree)
  {
    Object root = tree.getRoot ();
    lambdaPropagation (tree, null, root);
    piPropagation (tree, root);
  }


  private void lambdaPropagation (edu.umass.cs.mallet.grmm.types.Tree tree, Object parent, Object child)
  {
    logger.finer ("TRP lambdaPropagation from " + parent);
    Iterator it = tree.getChildren (child).iterator ();
    while (it.hasNext ()) {
      Object gchild = it.next ();
      lambdaPropagation (tree, child, gchild);
    }
    if (parent != null) {
      sendMessage (mdlCurrent, child, parent);
    }
  }


  private void piPropagation (edu.umass.cs.mallet.grmm.types.Tree tree, Object parent)
  {
    logger.finer ("TRP piPropagation from " + parent);
    Iterator it = tree.getChildren (parent).iterator ();
    while (it.hasNext ()) {
      Object child = it.next ();
      sendMessage (mdlCurrent, parent, child);
      piPropagation (tree, child);
    }
  }

  private void sendMessage (FactorGraph fg, Object parent, Object child)
  {
    if (parent instanceof Factor) {
      sendMessage (fg, (Factor) parent, (Variable) child);
    } else if (parent instanceof Variable) {
      sendMessage (fg, (Variable) parent, (Factor) child);
    }
  }

  private boolean allEdgesTouched ()
  {
    Iterator it = mdlCurrent.factorsIterator ();
    while (it.hasNext ()) {
      Factor factor = (Factor) it.next ();
      int idx = mdlCurrent.getIndex (factor);
      int numTouches = getNumTouches (idx);
      if (numTouches == 0) {
        logger.finest ("***TRP continuing: factor " + idx
                + " not touched.");
        return false;
      }
    }
    return true;
  }

  private void touchFactor (Factor factor)
  {
    int idx = mdlCurrent.getIndex (factor);
    incrementTouches (idx);
  }

  private boolean isFactorTouched (Factor factor)
  {
    int idx1 = mdlCurrent.getIndex (factor);
    return (getNumTouches (idx1) > 0);
  }

  private int getNumTouches (int idx1)
  {
    Integer integer = (Integer) factorTouched.get (idx1);
    return (integer == null) ? 0 : integer.intValue ();
  }

  private void incrementTouches (int idx1)
  {
    int nt = getNumTouches (idx1);
    factorTouched.put (idx1, new Integer (nt + 1));
  }

  public Factor query (DirectedModel m, Variable var)
  {
    throw new UnsupportedOperationException
            ("GRMM doesn't yet do directed models.");
  }

  //xxx could get moved up to AbstractInferencer, if mdlCurrent did.
  public Assignment bestAssignment ()
  {
    int[] outcomes = new int [mdlCurrent.numVariables ()];
    for (int i = 0; i < outcomes.length; i++) {
      Variable var = mdlCurrent.get (i);
      TableFactor ptl = (TableFactor) lookupMarginal (var);
      outcomes[i] = ptl.argmax ();
    }

    return new Assignment (mdlCurrent, outcomes);
  }

  // Deep copy termination condition
  public Object clone ()
  {
    try {
      TRP dup = (TRP) super.clone ();
      if (terminator != null) {
        dup.terminator = (TerminationCondition) terminator.clone ();
      }
      return dup;
    } catch (CloneNotSupportedException e) {
      // should never happen
      throw new RuntimeException (e);
    }
  }

  // Serialization
  private static final long serialVersionUID = 1;

  // If seralization-incompatible changes are made to these classes,
  //  then smarts can be added to these methods for backward compatibility.
  private void writeObject (ObjectOutputStream out) throws IOException
  {
    out.defaultWriteObject ();
  }

  private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException
  {
    in.defaultReadObject ();
  }

}

