/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/** 
   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package probabModel.pipe;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.types.FeatureSequence;
import edu.umass.cs.mallet.base.types.Instance;
import java.io.*;

// This class does not insist on getting its own Alphabet because it can rely on getting
// it from the FeatureSequence input.
/**
 * Convert the data field from a (feature, value) sequence to a feature vector.
 * Modified from FeatureSequence2FeatureVector.java
 * Created on 12/9/06 by Fei Xia.
 */
public class FeatValueSequence2AugmentableFeatureVector extends Pipe implements Serializable
{

	public FeatValueSequence2AugmentableFeatureVector ()
	{
	}
	
	
	public Instance pipe (Instance carrier)
	{
		FeatValueSequence fs = (FeatValueSequence) carrier.getData();
		
		Alphabet dict = fs.getAlphabet();
		int[] features = fs.toFeatureIndexSequence();
		double[] values = fs.toFeatureValueSequence();

		carrier.setData(new FeatureVector (dict, features, values));
		return carrier;
	}

	// Serialization 
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 1;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
	}
}
