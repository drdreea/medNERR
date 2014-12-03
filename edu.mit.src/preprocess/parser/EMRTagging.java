package preprocess.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import utils.UtilMethods;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;

public class EMRTagging {
	LexicalizedParser parser;
	TokenizerFactory<CoreLabel> tokenizerFactory;
	
	/**
	 * annotate given dir files for tags and parsed tree
	 * @param inputDir
	 * @param outputDir
	 */
	void execute(String inputDir, String outputDir){
		parser = new LexicalizedParser("grammar/englishPCFG.ser.gz");

		String[] rawFiles = UtilMethods.dirListing(inputDir);
		
		for(int index = 0; index < rawFiles.length; index ++){
			if(rawFiles[index].startsWith(".")) continue;
			
			String input = inputDir + "/" + rawFiles[index];
			String outputParse = outputDir + "/" + rawFiles[index] + ".parsed";
			String outputTag = outputDir + "/" + rawFiles[index] + ".tagged";
			processFile(input, outputParse, outputTag);
		}
	}
	
	/**
	 * tag and parse given file
	 * @param inputFile
	 * @param outputParse
	 * @param outputTag
	 */
	void processFile(String inputFile, String outputParse, String outputTag){
		tokenizerFactory = PTBTokenizer.factory( new CoreLabelTokenFactory(), "");
		
		try{
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(outputParse)));
			BufferedWriter outTag = new BufferedWriter(new FileWriter(new File(outputTag)));

			PrintWriter pw = new PrintWriter(out);
			
			outTag.write("<tag doc=\""+ inputFile + "\">\n");
			pw.write("<parse doc=\""+ inputFile + "\">\n");
			
			// You could also create a tokenier here (as below) and pass it
			// to DocumentPreprocessor
			int sentenceCount = 0;
			
			for (List<HasWord> sentence : new DocumentPreprocessor(inputFile)) {
				pw.write("<sentence id=\"" + String.valueOf(sentenceCount) + "\">");
				outTag.write("<sentence id=\"" + String.valueOf(sentenceCount) + "\">");
				sentenceCount ++;
				
				pw.write("<value>" + sentence.toString() + "</value>\n");
				outTag.write("<value>" + sentence.toString() + "</value>\n");
 
				Tree parse = parser.apply(sentence);
				parse.pennPrint();

				List<TaggedWord> taggedWords = parse.taggedYield();
				
				outTag.write("<content>\n");
				for (TaggedWord word : taggedWords) 
					outTag.write(word.value() + "||" + word.tag() + "\n");
				outTag.write("</content>\n");

			    pw.write("<content>\n");   
			    TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
			    tp.printTree(parse, pw);
			    pw.write("\n</content>\n");   
			    
			    pw.write("</sentence>\n");
			    outTag.write("</sentence>\n");			    
			}
			
			outTag.write("</tag>");
			pw.write("</parse>");
			
			pw.flush();
			outTag.flush();
			
		    pw.close();
		    out.close();
		    outTag.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void main(String[] args){

		if(args.length != 2){
			System.out.println("Incorrect arguments. " +
					"Required input directory and output directory");
			System.exit(-1);
		}
		
		EMRTagging tagger = new EMRTagging();
		tagger.execute(args[0], args[1]);
	}

}
