package preprocess.stemmer;

import io.importer.StemmerConfigHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import preprocess.stemmer.org.tartarus.snowball.ext.englishStemmer;
import utils.UtilMethods;

/**
 * A class stemming an input file using the snowball stemmer <a
 * href="http://snowball.tartarus.org/">Snowball Stemmer</a>.
 * 
 * @author ab
 */
public class EnglishStemmer {
	StemmerConfigHandler configs;

	EnglishStemmer(String configsPath) {
		configs = new StemmerConfigHandler(configsPath);
	}

	public void execute() {
		String[] inputFiles = UtilMethods.dirListing(configs.inputPath);

		for (int index = 0; index < inputFiles.length; index++) {
			String currentFilePath = configs.inputPath + "/"
					+ inputFiles[index];
			File inputFile = new File(currentFilePath);

			try {
				String outputFile = configs.outputPath + "/"
						+ inputFiles[index];
				BufferedWriter writer = new BufferedWriter(new FileWriter(
						new File(outputFile)));
				BufferedReader reader = new BufferedReader(new FileReader(
						inputFile));
				String line = reader.readLine();

				while (line != null) {

					writer.write(process(line));
					writer.write("\n");
					writer.flush();

					line = reader.readLine();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static String process(String line) {
		ArrayList<String> tokenized = new ArrayList<String>();

		line = UtilMethods.removeDigits(line).toLowerCase();
		line = UtilMethods.removePunctuation(line);
		String[] splitLine = line.split(" ");

		for (int tokenIndex = 0; tokenIndex < splitLine.length; tokenIndex++) {
			String stemmedToken = stem(splitLine[tokenIndex].trim());
			if (stemmedToken.length() <= 2)
				continue;
			tokenized.add(stemmedToken);
		}
		String updatedLine = UtilMethods.mergeStrings(tokenized).toLowerCase()
				.trim();

		return updatedLine.trim();
	}

	public static String stem(String token) {
		englishStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		stemmer.stem();
		return stemmer.getCurrent();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Incorrect params");
			System.exit(-1);
		}

		EnglishStemmer stem = new EnglishStemmer(args[0]);
		stem.execute();

		// System.out.println(process("1. Blood draw in 2-3 days to monitor INR and creatinine. Please"));
	}

}
