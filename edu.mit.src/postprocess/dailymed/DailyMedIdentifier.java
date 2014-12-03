/**
 * 
 */
package postprocess.dailymed;

import io.importer.AbbrevConfigHandler;
import io.importer.DataImport;
import io.output.OutputPredictions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.Annotation.AnnotationType;

import utils.UtilMethods;

/**
 * @author ab
 * 
 */
public class DailyMedIdentifier {
	AbbrevConfigHandler configs;
	private DataImport importer;
	HashSet<String> dailyMed;

	public DailyMedIdentifier(String configsPath) {
		configs = new AbbrevConfigHandler(configsPath);
		importer = new DataImport(null);

		dailyMed = importer.importDailyMedNames(configs.abbrevPath, false);
	}

	private void execute() {
		// import the meds data
		HashMap<String, AnnotationFile> medsAnnotation = importer
				.importAnnotations(this.configs.medsPath);

		for (String file : medsAnnotation.keySet()) {

			String filePath = this.configs.rawFiles + "/"
					+ file.split("\\.")[0];

			ArrayList<String> fileContent = importer.readFile(filePath);
			ArrayList<AnnotationDetail> meds = getMeds(file, fileContent);
			ArrayList<AnnotationDetail> medsCopy = new ArrayList<AnnotationDetail>();
			medsCopy.addAll(meds);

			Iterator<AnnotationDetail> it = meds.iterator();

			while (it.hasNext()) {
				AnnotationDetail givenMed = it.next();

				for (AnnotationDetail annt : medsCopy){

					if (annt.overlapDiffLines(givenMed))
						if (annt.startLine != annt.endLine)
							it.remove();
				}

			}

			AnnotationFile value = medsAnnotation.get(file);
			ArrayList<Annotation> initialAnnotations = clearAnnotations(
					value.annotations, fileContent);

			for (Annotation annt : initialAnnotations) {
	        	AnnotationDetail anntMed = annt.annotationElements.get(AnnotationType.M);

				HashSet<AnnotationDetail> toAdd = new HashSet<AnnotationDetail>();
				toAdd.add(anntMed);

				it = meds.iterator();

				while (it.hasNext()) {
					AnnotationDetail medication = it.next();

					if (anntMed.equalsAnnotation(medication)) {
						it.remove();
						toAdd.add(anntMed);
					}
					else
						if (anntMed
								.overlapAnnotation(medication)) {
							it.remove();
							toAdd.add(anntMed);
						}
						else
							if (anntMed.overlapDiffLines(medication)) {

								if (anntMed.startLine != anntMed.endLine) {
									toAdd.add(anntMed);
									it.remove();
								}
								else {
									toAdd.remove(anntMed);
								}
							}
				}

				meds.addAll(toAdd);
			}

			// store the annotations
			OutputPredictions.storeAnnotations(this.configs.outputPath + "/"
					+ file, meds);
		}
	}

	/**
	 * @param annotations
	 * @param fileContent
	 * @return
	 */
	private ArrayList<Annotation> clearAnnotations(
			ArrayList<Annotation> annotations, ArrayList<String> fileContent) {
		Iterator<Annotation> it = annotations.iterator();
		ArrayList<Annotation> updated = new ArrayList<Annotation>();

		while (it.hasNext()) {
			Annotation annt = it.next();
        	AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

			String med = medication.content;
			String[] splitMed = medication.content.split(" ");

			if (medication.endOffset - medication.startOffset + 1 != splitMed.length
					&& medication.startLine == medication.endLine) {
				String line = fileContent.get(medication.startLine - 1)
						.toLowerCase();
				if (line.contains(med)) {
					String[] splitLine = line.split(" ");
					int start = -1;
					for (int index = 0; index < splitLine.length; index++) {
						String tmpMed = splitLine[index];
						if (med.startsWith(tmpMed)) {
							boolean found = true;
							for (int offset = 0; offset < splitMed.length; offset++) {
								if (index + offset < splitLine.length)
									if (!splitMed[offset]
											.equals(splitLine[index + offset]))
										found = false;
							}

							if (found) {
								start = index;
								break;
							}
						}
					}

					if (start != -1) {
						medication.startOffset = start;
						medication.endOffset = start + splitMed.length - 1;
						System.out.println(annt.annotation);
					}

				}
			}

			updated.add(annt);
		}

		return updated;
	}

	private ArrayList<AnnotationDetail> getMeds(String filePath,
			ArrayList<String> fileContent) {
		ArrayList<AnnotationDetail> abbvs = new ArrayList<AnnotationDetail>();

		int lineCounter = 1;

		for (int range = 0; range < fileContent.size() - 1; range++) {
			String line = fileContent.get(range).toLowerCase();
			String futureLine = fileContent.get(range + 1).toLowerCase();

			if (line.toLowerCase().contains("darbepoetin")
					&& lineCounter == 140)
				System.out.println("here");

			if (line == null || line.trim().isEmpty()) {
				lineCounter++;
				continue;
			}

			String mergedLine = UtilMethods.mergeStrings(line, futureLine);
			String[] splitLine = mergedLine.split(" ");
			String[] splitCurrentLine = line.split(" ");

			String prev = "";

			for (int index = 0; index < splitLine.length; index++) {
				String word = splitLine[index].trim();

				if (word.trim().isEmpty())
					continue;

				String tmpWord = UtilMethods.removePunctuation(word);
				if (tmpWord.isEmpty())
					continue;

				String merged = UtilMethods.mergeStrings(prev, tmpWord);

				if (this.dailyMed.contains(merged) && !prev.isEmpty()
						&& index <= splitCurrentLine.length) {
					AnnotationDetail anntDetail = getAnnotationDetail(prev,
							word, line, futureLine, lineCounter, index);
					if (anntDetail != null) {
						abbvs.add(anntDetail);
					}
				}
				else
					if (this.dailyMed.contains(tmpWord)
							&& line.contains(tmpWord)
							&& index < splitCurrentLine.length) {
						AnnotationDetail anntDetail = new AnnotationDetail(
								word, null);
						anntDetail.startLine = lineCounter;
						anntDetail.endLine = lineCounter;
						anntDetail.startOffset = index;
						anntDetail.endOffset = index;

						abbvs.add(anntDetail);
					}

				prev = word;
			}

			lineCounter++;
		}

		return abbvs;
	}

	/**
	 * @param prev
	 * @param word
	 * @param line
	 * @param prevLine
	 * @param lineCounter
	 * @return
	 */
	private AnnotationDetail getAnnotationDetail(String prev, String word,
			String line, String futureLine, int lineCounter, int index) {
		String merged = UtilMethods.mergeStrings(prev, word);

		AnnotationDetail anntDetail = new AnnotationDetail(merged, null);

		if (line.contains(merged)) {
			anntDetail.startLine = lineCounter;
			anntDetail.endLine = lineCounter;
			anntDetail.startOffset = index - 1;
			anntDetail.endOffset = index;
		}
		else {
			if (line.endsWith(prev)) {
				anntDetail.startLine = lineCounter;
				anntDetail.endLine = lineCounter + 1;
				anntDetail.startOffset = index - 1;
				anntDetail.endOffset = 0;
			}
			else
				return null;
		}
		return anntDetail;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Required configs path");
			System.exit(-1);
		}

		DailyMedIdentifier ident = new DailyMedIdentifier(args[0]);
		ident.execute();
	}

}
