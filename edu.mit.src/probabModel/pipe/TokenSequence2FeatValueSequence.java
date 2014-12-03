/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */





package probabModel.pipe;

import edu.umass.cs.mallet.base.pipe.Pipe;

import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.types.FeatureSequence;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.Instance;
import java.io.*;
/**
 * Convert the token sequence in the data field each instance to a 
   (feature,value) sequence.
   Each line is of the input has the format
    "name target featname1 featval1 featname2 featval2 ....".

   The class is modified from the TokenSequence2FeatValueSequence class.
   Created on 12/9/2006.

   @author Fei Xia <a href="mailto:fxia@u.washington.edu">fxia@u.washington.edu</a>
 */

public class TokenSequence2FeatValueSequence extends Pipe
{
	public TokenSequence2FeatValueSequence (Alphabet dataDict)
	{
		super (dataDict, null);
	}

	public TokenSequence2FeatValueSequence ()
	{
		super(Alphabet.class, null);
	}
	
	public Instance pipe (Instance carrier)
	{
		TokenSequence ts = (TokenSequence) carrier.getData();
		if(ts.size() % 2 != 0)
		    throw new IllegalArgumentException ("The token sequence should have an even number of tokens");
		
		int seq_leng = ts.size() / 2;

		FeatValueSequence ret =
			new FeatValueSequence ((Alphabet)getDataAlphabet(), seq_leng);
		for (int i = 0; i < seq_leng; i++) {
		    String tokenStr = ts.getToken(2*i).getText();
		    String valueStr = ts.getToken(2*i+1).getText();
		    double val = Double.parseDouble(valueStr);
		    ret.add (tokenStr, val);
		}
		carrier.setData(ret);
		return carrier;
	}

}
