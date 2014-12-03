
/* Copyright (C) 2012 Massachusetts Institute of Technology, CSAIL
   This file is part of "MCORES" (Medical COreference REsolution System).
	http://people.csail.mit.edu/andreeab/mcores
   This software is provided under the terms of the MIT License,
	as published by http://www.opensource.org/licenses/MIT.  For further
   information, see the file `LICENSE' included with this distribution. */
package preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;

/**
 *
 * @author: Andreea Bodnari
 * @version: POSParser.java Jun 29, 2012 

 */

public class POSParser {
	final String PARSE_MODEL = "english-left3words-distsim.tagger";
	public final static String POS_FILENAME_EXTENSION = "pos";

	public static final String OUTPUT_DIRECTORY = "parse";

	String pathModel ;
	MaxentTagger tagger;

	public POSParser(String pathResources){
		pathModel = pathResources + "/" + PARSE_MODEL;

		Properties properties = new Properties();
		properties.setProperty("tokenize", "false");
		properties.setProperty("delimiter", "/");
		properties.setProperty("model", pathModel);

		TaggerConfig configs = new TaggerConfig(properties);
		try{
			tagger = new MaxentTagger(pathModel, configs);
		}catch(Exception e){
			e.printStackTrace();

		}

	}

	public void parseFile(String filePath, String outputPath){
		try{
			String outputFileName = outputPath + "/" + OUTPUT_DIRECTORY;

			// check if the directory exists, otherwise create it
			new File(outputFileName).mkdir();
			outputFileName = outputFileName + "/" + (new File(filePath)).getName()
					+ "." + POS_FILENAME_EXTENSION;

			if (!(new File(outputFileName).exists())){

				BufferedReader in = new BufferedReader(new FileReader(filePath));
				BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));

				tagger.runTagger(in, out, "", false);
			}

		}catch(Exception e){
			e.printStackTrace();
		}

	}
}
