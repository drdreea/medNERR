/**
 * Created on Jan 16, 2012
 * 
 * @author ab Contact andreeab dot mit dot edu
 * 
 */
package eval;

import io.importer.ConceptEvalConfigHandler;
import io.log.ExtractMedicationLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.Annotation.AnnotationType;

import classifier.TrainTestCreator.CorpusType;
import classifier.vector.AttributeVector;

import utils.UtilMethods;

/**
 * This class compares the system output to the gold standard and identifies
 * where the two differ The differences are printed to file
 * 
 * @author ab
 * 
 */
public class OutputCompare {
	ConceptEvalConfigHandler configs;
	final static int requiredArgs = 1;
	ExtractMedicationLog log;

	enum Operation {
		COMPARE, DIFF
	};

	enum Type {
		MEDICATION, REASON
	};

	enum Match{
		EXACT, INEXACT
	}

	HashMap<String, Integer> incorrect;
	HashMap<String, Integer> missed;

	String[] systemFiles;
	String[] gsFiles;
	List<String> gsFilesAsList;

	public static Match matchForComparison = Match.INEXACT;

	public OutputCompare(String configsPath) {
		incorrect = new HashMap<String, Integer>();
		missed = new HashMap<String, Integer>();

		this.configs = new ConceptEvalConfigHandler(configsPath);
		this.log = new ExtractMedicationLog(configs.outputPath);
		// read the system annotation
		systemFiles = UtilMethods
				.dirListing(this.configs.systemPath);

		// read the gold standard annotations
		gsFiles = UtilMethods.dirListing(this.configs.gsPath);
		gsFilesAsList = Arrays.asList(gsFiles);
	}

	int missedTotal = 0;
	int incorrectTotal = 0;
	int correctTotal = 0;

	/**
	 * Iterate through all the files and perform a check on all of them
	 */
	public void compare(Operation action, Type neType) {
		for (String systemFile : systemFiles) {
			if (systemFile.startsWith("."))
				continue; // ignore the system files

			// check if the gs has the current file
			if (!gsFilesAsList.contains(systemFile)) {
				System.out.println("File not in gs output " + systemFile);

				continue;
			}

			log.writeError("\n\nComparing file : " + systemFile);
			//			System.out.println("\n\nComparing file : " + systemFile);

			// read the system and gs file content
			String gsAnnotationsPath = UtilMethods.joinPaths(
					this.configs.gsPath, systemFile);
			ArrayList<String> gsLines = UtilMethods
					.readFileLines(gsAnnotationsPath);
			ArrayList<Annotation> gsAnnotations = new ArrayList<Annotation>();

			for (String line : gsLines) {
				Annotation result = Annotation.parseAnnotationLine(line, CorpusType.I2B2);
				if (result != null)
					gsAnnotations.add(result);
			}

			String systemAnnotationspath = UtilMethods.joinPaths(
					this.configs.systemPath, systemFile);
			ArrayList<String> systemLines = UtilMethods
					.readFileLines(systemAnnotationspath);
			ArrayList<Annotation> systemAnnotations = new ArrayList<Annotation>();

			for (String line : systemLines) {
				Annotation result = Annotation.parseAnnotationLine(line, CorpusType.I2B2);
				if (result != null)
					systemAnnotations.add(result);
			}

			switch (action) {
			default:
			case COMPARE:
				switch (neType) {
				case MEDICATION:
					this.compareFiles(systemAnnotations, gsAnnotations);
					break;
				case REASON:
					this.compareReasons(systemAnnotations, gsAnnotations, systemFile);
					break;
				}
				break;
			}
		}

		double precision = correctTotal*1. /(correctTotal + incorrectTotal);
		double recall = correctTotal *1. /(correctTotal + missedTotal);
		double f_measure = 0;

		if(precision != 0. && recall != 0.)
			f_measure = 2*precision*recall /(precision + recall);

		System.out.println("Missed\tincorrect\tcorrect");
		System.out.println(String.valueOf(missedTotal) + "\t" + String.valueOf(incorrectTotal) + "\t" + 
				String.valueOf(correctTotal));

		System.out.println("Precision\trecall\tf-measure: ");
		System.out.println(String.valueOf(precision) +"\t" + String.valueOf(recall) + "\t" + 
				String.valueOf(f_measure));
	}

	void compareReasons(ArrayList<Annotation> systemAnnotations,
			ArrayList<Annotation> gsAnnotations, String file) {

		// check which annotations were missed
		HashSet<String> reasonComment = new HashSet<String>();

		// check which system annotations are incorrect
		int counter = 1;

		for (Annotation annt : systemAnnotations) {
			Annotation found = null;
			AnnotationDetail reason = annt.annotationElements.get(AnnotationType.R);

			for (Annotation gsAnnt : gsAnnotations) {
				AnnotationDetail gsReason = gsAnnt.annotationElements.get(AnnotationType.R);

				if (reason != null && reason.content != null && gsReason != null)
					try {
						if (reason.equalsAnnotation(gsReason) )
							found = gsAnnt;

						if(matchForComparison == Match.INEXACT && 
								reason.overlapAnnotation(gsReason) ||
								reason.overlapDiffLines(gsReason)) {
							found = gsAnnt;
						}
					}
				catch (Exception e) {
					e.printStackTrace();
				}

			}

			// if we found a correct reason
			if (found != null) {

				if (!annt.annotation.contains("r=\"nm\"")) {
					log.writeError(" CORRECT "
							+ reason.printString());
					//					System.out.println(String.valueOf(counter) + " CORRECT "
					//							+ annt.reason.printString(AnnotationType.r));
					counter++;
					correctTotal ++;
				}
				reasonComment.add(AttributeVector.printComments(
						found.annotationElements.get(AnnotationType.R),""));

				gsAnnotations.remove(found);
			}
			else {
				if (annt.annotation != null ) {
					if (annt.annotation.contains("r=\"nm\""))
						continue;

					if(reason != null)
						log.writeError("INCORRECT " + reason.printString());
					else{ 
						for(AnnotationType det : annt.annotationElements.keySet())
							if(det != AnnotationType.M)
								log.writeError("INCORRECT " + 
							annt.annotationElements.get(det).printString());
					}

					//					System.out.println(String.valueOf(counter) + " INCORRECT "
					//							+ annt.reason.printString(AnnotationType.r));
					counter++;
					incorrectTotal ++;
				}
			}
		}


		for (Annotation annt : gsAnnotations) {
			AnnotationDetail reason = annt.annotationElements.get(AnnotationType.R);

			if (annt.annotation.contains("r=\"nm\"") ||
					reason.content.equals("nm"))
				continue;


			String comment = AttributeVector.printComments(reason, "");
			if(reasonComment.contains(comment))
				continue;
			else
				reasonComment.add(comment);

			log.writeError("MISSED " + reason.printString());
			System.out.println(String.valueOf(counter) + "MISSED "
					+ reason.printString());

			missedTotal ++;
			counter++;
		}
	}

	void compareFiles(ArrayList<Annotation> systemAnnotations,
			ArrayList<Annotation> gsAnnotations) {
		for (Annotation annt : systemAnnotations) {
			AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

			Annotation found = null;

			for (Annotation gsAnnt : gsAnnotations) {
				AnnotationDetail gsMed = gsAnnt.annotationElements.get(AnnotationType.M);

				if (medication != null && medication.content != null)
					try {
						if (medication.content
								.equals(gsMed.content)
								&& medication.startLine == gsMed.startLine
								&& medication.startOffset == gsMed.startOffset) {
							found = gsAnnt;
						}
						else
							if(matchForComparison == Match.INEXACT){
								if (medication
										.overlapAnnotation(gsMed)) {
									found = gsAnnt;
								}

								else
									if (medication.overlapDiffLines(gsMed)) {
										found = gsAnnt;
									}
							}
					}catch (Exception e) {
						e.printStackTrace();
					}

				if (found != null)
					break;
			}

			if (found != null) {
				correctTotal ++;
				log.writeError("CORRECT " + annt.annotation);

				gsAnnotations.remove(found);
			}
			else {
				if (annt.annotation != null) {
					try {
						incorrectTotal ++;
						log.writeError("INCORRECT " + annt.annotation);

						if (incorrect.containsKey(medication.content))
							incorrect.put(medication.content, incorrect
									.get(medication.content) + 1);
						else
							incorrect.put(medication.content, 1);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		// check which annotations were missed
		for (Annotation annt : gsAnnotations) {
			AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

			missedTotal ++;
			log.writeError("MISSED " + annt.annotation);

			if (missed.containsKey(medication.content))
				missed.put(medication.content, missed
						.get(medication.content) + 1);
			else
				missed.put(medication.content, 1);
		}

	}

	public static void main(String[] args) {
		if (args.length != requiredArgs) {
			System.out
			.println("Incorrect arguments! Required configuration file path.");
			System.exit(-1);
		}
		OutputCompare comparison = new OutputCompare(args[0]);

		System.out.println("Comparing output at " + comparison.configs.systemPath);
		System.out.println("Against GS at " + comparison.configs.gsPath);

		matchForComparison = Match.INEXACT;
		comparison.compare(Operation.COMPARE, Type.REASON);
	}
}
