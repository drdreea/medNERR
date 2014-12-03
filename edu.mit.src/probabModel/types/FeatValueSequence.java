/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */





package probabModel.types;

import edu.umass.cs.mallet.base.types.*;

import java.util.Arrays;
import java.io.*;

/** A FeatValueSequence is the same of a FeatureSequence except that
 **  each feature has a feature value.
 *
 *  Modified from FeatureSequence.java.
 *  Created on 12/9/2006
 *  @author Fei Xia <a href="mailto:fxia@u.washington.edu">fxia@u.washington.edu</a>
 */

public class FeatValueSequence implements Sequence, Serializable
{
    Alphabet dictionary;
    
    // The two arrays should have the same length.
    int[] features;
    double[] values;  // store the feature values. 

    int length;        // length is the real length

    /**
     *  Creates a FeatValueSequence given all of the objects in the
     *  sequence.
     *
     *  @param dict A dictionary that maps objects in the sequence
     *     to numeric indices.
     *  @param features An array where features[i] gives the index
     *     in dict of the ith element of the sequence.
     */
    public FeatValueSequence (Alphabet dict, int[] features,
			      double[] values)
    {
	this(dict, features.length);
	if(features.length != values.length)
	    throw new IllegalArgumentException("The features and values have different size");
	
	for (int i = 0; i < features.length; i++)
	    add(features[i], values[i]);
    }

    public FeatValueSequence (Alphabet dict, int[] features, 
			      double[] values, int len)
    {
	this(dict, len);
	if(features.length != values.length)
	    throw new IllegalArgumentException("The features and values have different size");

	if(features.length < len)
	    throw new IllegalArgumentException("The feature array is too short");

	for (int i = 0; i < len; i++)
	    add(features[i], values[i]);
    }

    public FeatValueSequence (Alphabet dict, int capacity)
    {
	dictionary = dict;
	int cap = capacity > 2 ? capacity : 2;
	features = new int[cap];
	values = new double[cap];
	
	length = 0;
    }

    public FeatValueSequence (Alphabet dict)
    {
	this (dict, 2);
    }
	
    public Alphabet getAlphabet () { return dictionary; }

    public final int getLength () { return length; }

    public final int size () { return length; }

    public final int getIndexAtPosition (int pos)
    {
	return features[pos];
    }

    public final double getValueAtPosition (int pos)
    {
	return values[pos];
    }

    public Object getObjectAtPosition (int pos)
    {
	return dictionary.lookupObject (features[pos]);
    }

    // xxx This method name seems a bit ambiguous?
    public Object get (int pos)
    {
	return dictionary.lookupObject (features[pos]);
    }

    public String toString ()
    {
	StringBuffer sb = new StringBuffer ();
	for (int fsi = 0; fsi < length; fsi++) {
	    Object o = dictionary.lookupObject(features[fsi]);
	    sb.append (fsi);
	    sb.append (": ");
	    sb.append (o.toString());
	    sb.append (" (");
	    sb.append (features[fsi]);

	    // Fei: append Feature value
	    sb.append ("val=");
	    double val = values[fsi];
	    sb.append (val);

	    sb.append (")\n");
	}
	return sb.toString();
    }

    protected void growIfNecessary ()
    {
	if (length == features.length) {
	    int[] newFeatures = new int[features.length * 2];
	    System.arraycopy (features, 0, newFeatures, 0, length);
	    features = newFeatures;
	    
	    double[] newValues = new double[features.length * 2];
	    System.arraycopy (values, 0, newValues, 0, length);
	    values = newValues;
	}
    }

    public void add (int featureIndex, double featureValue)
    {
	growIfNecessary ();
	assert (featureIndex < dictionary.size());
	features[length] = featureIndex;
	values[length] = featureValue;
	length ++;
    }

    public void add (Object key, double featureValue)
    {
	int fi = dictionary.lookupIndex (key);
	if (fi >= 0)
	    // This will happen if the dictionary is frozen,
	    // and key is not already in the dictionary.
	    add (fi, featureValue);
	// xxx Should we raise an exception if the appending doesn't happen?
    }

    public void addFeatureWeightsTo (double[] weights)
    {
	for (int i = 0; i < length; i++)
	    weights[features[i]]++;
    }

    public void addFeatureWeightsTo (double[] weights, double scale)
    {
	for (int i = 0; i < length; i++)
	    weights[features[i]] += scale;
    }

    public int[] toFeatureIndexSequence ()
    {
	int[] feats = new int[length];
	System.arraycopy (features, 0, feats, 0, length);
	return feats;
    }

    public double[] toFeatureValueSequence()
    {
	double[] vals = new double[length];
	System.arraycopy (values, 0, vals, 0, length);
	return vals;
    }

    public int[] toSortedFeatureIndexSequence ()
    {
	int[] feats = this.toFeatureIndexSequence ();
	java.util.Arrays.sort (feats);
	return feats;
    }


    // Serialization
		
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;
    private static final int NULL_INTEGER = -1;
    
    private void writeObject (ObjectOutputStream out) throws IOException {
	out.writeInt (CURRENT_SERIAL_VERSION);
	out.writeObject (dictionary);
	out.writeInt (features.length);
	for (int i = 0; i < features.length; i++){
	    out.writeInt (features[i]);
	    out.writeDouble (values[i]);
	}
	out.writeInt (length);
    }
	
    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
	int featuresLength;
	int version = in.readInt ();
	dictionary = (Alphabet) in.readObject ();
	featuresLength = in.readInt();
	features = new int[featuresLength];
	for (int i = 0; i < featuresLength; i++){
	    features[i] = in.readInt ();
	    values[i] = in.readDouble ();
	}
	length = in.readInt ();
    }
	
}
