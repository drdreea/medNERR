package postprocess;

import io.importer.AbbrevConfigHandler;
import io.importer.DataImport;
import io.output.OutputPredictions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.Annotation.AnnotationType;

import utils.UtilMethods;

public class AbbreviationIdentifier {
	AbbrevConfigHandler configs;
	private DataImport importer;
	HashSet<String> abbreviations;

	public AbbreviationIdentifier(String configsPath) {
		configs = new AbbrevConfigHandler(configsPath);
		importer = new DataImport(null);

		abbreviations = importer.importAbbreviations(configs.abbrevPath);
	}

	private void execute() {
		// import the meds data
		HashMap<String, AnnotationFile> medsAnnotation = importer
				.importAnnotations(this.configs.medsPath);

		for (String file : medsAnnotation.keySet()) {

			ArrayList<Annotation> abbv = getAbbv(file);
			AnnotationFile value = medsAnnotation.get(file);

			for (Annotation annt : value.annotations) {
				boolean unique = true;

				for (Annotation abb : abbv) {

					if (annt.annotationElements.get(AnnotationType.M).equalsAnnotation(
							abb.annotationElements.get(AnnotationType.M))) {
						unique = false;
						abbv.remove(abb);
						abbv.add(annt);
						break;
					}

					if (annt.annotationElements.get(AnnotationType.M)
							.overlapAnnotation(abb.annotationElements.get(AnnotationType.M))) {
						unique = false;
						abbv.remove(abb);
						abbv.add(annt);
						break;
					}
				}

				if (unique)
					abbv.add(annt);
			}

			// store the annotations
			//	    OutputPredictions.storeAnnotations(this.configs.outputPath + "/"
			//		    + file, abbv);
		}
	}

	private ArrayList<Annotation> getAbbv(String filePath) {
		ArrayList<Annotation> abbvs = new ArrayList<Annotation>();

		filePath = this.configs.rawFiles + "/" + filePath.split("\\.")[0];
		File rawFiles = new File(filePath);
		if (!rawFiles.exists())
			return abbvs;

		ArrayList<String> fileContent = DataImport.readFile(filePath);
		int lineCounter = 1;

		for (String line : fileContent) {

			if (line == null || line.trim().isEmpty()) {
				lineCounter++;
				continue;
			}

			String[] splitLine = line.toLowerCase().split(" ");
			String prev = "";

			for (int index = 0; index < splitLine.length; index++) {
				String word = splitLine[index];

				if (word.trim().isEmpty())
					continue;

				String tmpWord = UtilMethods.removePunctuation(word);

				if (this.abbreviations.contains(tmpWord)) {
					AnnotationDetail anntDetail = new AnnotationDetail(word,
							null);
					anntDetail.startLine = lineCounter;
					anntDetail.endLine = lineCounter;
					anntDetail.startOffset = index;
					anntDetail.endOffset = index;

//					Annotation annt = new Annotation(line);
//					annt.medication = anntDetail;
//					annt.isList = false;
//					abbvs.add(annt);
				}
				else
					if (this.abbreviations.contains(UtilMethods.mergeStrings(
							prev, word))) {
						AnnotationDetail anntDetail = new AnnotationDetail(
								UtilMethods.mergeStrings(prev, word), null);
						anntDetail.startLine = lineCounter;
						anntDetail.endLine = lineCounter;
						anntDetail.startOffset = index - 1;
						anntDetail.endOffset = index;

//						Annotation annt = new Annotation(line);
//						annt.medication = anntDetail;
//						annt.isList = false;
//						abbvs.add(annt);
					}

				prev = word;
			}

			lineCounter++;
		}

		return abbvs;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Required configs path");
			System.exit(-1);
		}

		AbbreviationIdentifier ident = new AbbreviationIdentifier(args[0]);
		ident.execute();
	}

}
