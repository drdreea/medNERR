/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package probabModel.pipe;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.AugmentableFeatureVector;
import edu.umass.cs.mallet.base.types.FeatureSequence;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Instance.Iterator;
import edu.umass.cs.mallet.base.types.Label;
import edu.umass.cs.mallet.base.types.LabelAlphabet;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.util.PropertyList;
//import edu.umass.cs.mallet.base.types.*;
import edu.umass.cs.mallet.base.extract.StringTokenization;
import edu.umass.cs.mallet.base.extract.StringSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import utils.UtilMethods;

/**
 * Generic pipe that takes a linegroup of the form:
 * <pre>
 *  LABEL1 LABEL2 ... LABELk word feature1 feature2 ... featuren
 * </pre>
 * and converts it into an input FeatureVectorSequence and target LabelsSequence.
 * <p>
 * If the number of labels at each sequence position could vary, then use this format instead:
 *  <pre>
 *  LABEL1 LABEL2 ... LABELk ---- word feature1 feature2 ... featuren
 *  </pre>
 * The four dashes ---- must be there to separate the features from the labels.
 * Whitespace is ignored.
 * The difference between this pipe and {@link edu.umass.cs.mallet.users.casutton.experiments.dcrf.GenericDcrfPipe} is that this pipe
 *  allows for a different number of labels at each sequence position.
 * <p>
 * Explicitly specifying which word is the token allows the use of the HTML output from
 *  the extract package.
 *
 * Created: Aug 22, 2005
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: GenericAcrfData2TokenSequence.java,v 1.10 2006/06/01 17:36:32 casutton Exp $
 */
public class MaxEntData2TokenSequence extends Pipe {

	private ArrayList labelDicts = new ArrayList ();
	private int numLabels = -1;
	private boolean includeTokenText = true;
	private String textFeaturePrefix = "WORD=";
	private boolean featuresIncludeToken = true;
	private boolean labelsAtEnd = false;

	public MaxEntData2TokenSequence ()
	{
		super (Alphabet.class, LabelAlphabet.class);
	}

	public MaxEntData2TokenSequence (int numLabels)
	{
		super (Alphabet.class, LabelAlphabet.class);
		this.numLabels = numLabels;
	}

	public void setIncludeTokenText (boolean includeTokenText)
	{
		this.includeTokenText = includeTokenText;
	}

	/**
	 * If true, then the first feature in the list is considered to be the token's text.
	 * If false, then no feature is designated as the token text.
	 * @param featuresIncludeToken
	 */
	public void setFeaturesIncludeToken (boolean featuresIncludeToken)
	{
		this.featuresIncludeToken = featuresIncludeToken;
	}

	public void setTextFeaturePrefix (String textFeaturePrefix)
	{
		this.textFeaturePrefix = textFeaturePrefix;
	}

	public LabelAlphabet getLabelAlphabet (int lvl)
	{
		return (LabelAlphabet) labelDicts.get (lvl);
	}

	public int numLevels ()
	{
		return labelDicts.size();
	}

	public Instance pipe (Instance carrier)
	{
		String input;
		if (carrier.getData () instanceof CharSequence) {
			input = String.valueOf(carrier.getData ());
		} else {
			throw new ClassCastException("Needed a String; got "+carrier.getData());
		}


		StringBuffer buf = new StringBuffer ();

		Alphabet dict = getDataAlphabet ();

		String line = input;
		String[] toks = line.split ("\\s+");

		int j = 0;
		Label lbls = null;

		if (!labelsAtEnd) {
			while (!isLabelSeparator (toks, j)) {
				j++;
			}
			if ((j < toks.length) && toks[j].equals ("----")) j++;
			lbls  = labelForTok (toks[j-1], j );
		}

		int maxFeatureIdx = (labelsAtEnd) ? toks.length - numLabels : toks.length;

		String text = "*???*";
		if (featuresIncludeToken) {
			if (j < maxFeatureIdx) {
				text = toks [j++];
			}
		}

		int start = buf.length ();
		buf.append (text);
		int end = buf.length ();
		buf.append (" ");

		StringSpan span = new StringSpan (buf, start, end);
		HashMap<String, Double> values = new HashMap<String, Double>();

		// skip the word feature
		j ++;

		while (j < maxFeatureIdx) {

			try{
				span.setFeatureValue (toks[j], 1.0);
				values.put(toks[j], 1.0);
			}catch(Exception e1){
				e1.printStackTrace();
			}

			j = j+2;

		}

		if (includeTokenText) {
			span.setFeatureValue ((textFeaturePrefix+text).intern(), 1.0);
		}

		if (labelsAtEnd) {
			int firstLblIdx = j;
			while (j < toks.length) {
				j++;
			}
			lbls = labelForTok (toks[j], j - firstLblIdx);
		}

		Token spans = span;

		StringTokenization tokenization = new StringTokenization (buf);
		tokenization.add (spans);

		carrier.setData (values);

		carrier.setTarget (lbls);

		return carrier;
	}

	private Label labelForTok (String tok, int lvl)
	{
		while (labelDicts.size() <= lvl) {
			labelDicts.add (new LabelAlphabet ());
		}
		LabelAlphabet dict = (LabelAlphabet) labelDicts.get (lvl);
		return dict.lookupLabel (tok);
	}

	private boolean isLabelSeparator (String[] toks, int j)
	{
		if (numLabels > 0) {
			// if fixed numLabels, just return whether we have enough.
			return j >= numLabels;
		} else {
			// otherwise, use the dynamic labels separator
			return toks[j].equals ("----");
		}
	}

	// Serialization garbage

	// version 1.0 == returned a feature vector sequence
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 2;

	private void writeObject (ObjectOutputStream out) throws IOException
	{
		out.defaultWriteObject ();
		out.writeInt (CURRENT_SERIAL_VERSION);
	}


	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject ();
		int version = in.readInt ();
		if (version <= 1) {
			featuresIncludeToken = true;
		}
	}

	public boolean isLabelsAtEnd ()
	{
		return labelsAtEnd;
	}

	public void setLabelsAtEnd (boolean labelsAtEnd)
	{
		this.labelsAtEnd = labelsAtEnd;
	}
}
