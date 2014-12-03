/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package probabModel.grmm;

//import edu.umass.cs.mallet.grmm.types.*;

import java.io.Serializable;

import probabModel.grmm.types.*;


/**
 *  Interface implemented by all inferencers, which are algorithms for
 *   computing (perhaps approximately) marginal distributions over
 *   nodes in the model.
 * <P>
 * If you are implementing a new inferencer, you may wish to consider
 *   subclassing {@link edu.umass.cs.mallet.grmm.inference.AbstractInferencer}, which implements this
 *   interface.
 * <p>
 * Created: Wed Oct  1 11:18:09 2003
 *
 * @author <a href="mailto:casutton@cs.umass.edu">Charles Sutton</a>
 * @version $Id: Inferencer.java,v 1.6 2006/02/03 04:25:32 casutton Exp $
 */
public interface Inferencer extends Serializable {

	/**
	 * Computes marginal distributions for a factor graph.
	 * @throws UnsupportedOperationException If this inferencer does
	 *  not support undirected models (unlikely).
	 */
	public void computeMarginals (FactorGraph mdl);

	/**
	 * Returns the computed marginal of a given variable.
	 * Before using this method, <tt>computeMarginals</tt> must have
	 * been previously called on the graphical model that contains <tt>v</tt>.
	 * @see #computeMarginals(FactorGraph)
	 */
	public Factor lookupMarginal (Variable v);


	/**
	 * Returns the computed marginal of a given clique in a graph.
	 * Before using this method, <tt>computeMarginals</tt> must have
	 * been previously called on the graphical model that contains the clique.
	 *
	 * @see #computeMarginals(edu.umass.cs.mallet.grmm.types.FactorGraph)
	 * @see #computeMarginals(JunctionTree)
	 * @throws UnsupportedOperationException If this inferencer does
	 *  not compute marginals for the size of clique given.
	 */
	public Factor lookupMarginal (VarSet varSet);

	/**
	 * Returns the joint probability of a given assignment,
	 *  computed in some factorized fashion.
	 * Before using this method, <tt>computeMarginals</tt> must have
	 *  been previously called on the graphical model that contains  
	 *  the variables of <tt>assn</tt>.
	 * @see #computeMarginals(edu.umass.cs.mallet.grmm.types.FactorGraph)
	 * @see #computeMarginals(JunctionTree)
	 */
	public double lookupJoint (Assignment assn);

	/**
	 * Returns the natural logarithm of the joint probability 
	 *   of a given assignment, computed in some factorized fashion.
	 * Before using this method, <tt>computeMarginals</tt> must have
	 * been previously called on the graphical model that contains
	 *  the variables of <tt>assn</tt>.
	 * <P>
	 * This method is less likely to underflow than
	 *  <code>Math.log (lookupJoint (assn))</code>.
	 * @see #computeMarginals(edu.umass.cs.mallet.grmm.types.FactorGraph)
	 * @see #computeMarginals(JunctionTree)
	 */
	public double lookupLogJoint (Assignment assn);

	/**
	 * Computes the marginal probability of a given assignment to
	 *  a small number of model variables.  This may require one
	 *  run of computeMarginals() for each variable in the assignment;
	 *  if the assigment has many variables, it may be more efficient
	 *  to use lookupJoint.
	 */
	public double query (FactorGraph mdl, Assignment assn);

	public Inferencer duplicate ();
	
} // Inferencer
