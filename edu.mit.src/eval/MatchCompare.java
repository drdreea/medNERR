/**
 * 
 */
package eval;

import io.log.ExtractMedicationLog;

import java.util.ArrayList;
import java.util.Iterator;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.Annotation.AnnotationType;

import matchMedAttributesRuleBased.Matcher;



/**
 * @author ab
 *
 */
public class MatchCompare extends Matcher{
	final static int requiredArgs = 1;
	int totalMissed = 0;
	int totalCorrect = 0;
	int totalIncorrect = 0;

	enum Match{
		EXACT, INEXACT
	}

	public static Match matchType ;

	/**
	 * @param path
	 */
	public MatchCompare(String path) {
		super(path);
	}
	
	public MatchCompare(String goldPath, String systemPath, String outputPath) {
		super();
		
		reasonAnnotation = importer.importAnnotations(systemPath);
		goldAnnotations = importer.importAnnotations(goldPath);
		log = new ExtractMedicationLog(outputPath );

	}

	public double execute() {
		for(String file : this.reasonAnnotation.keySet()) {
			if(!this.goldAnnotations.containsKey(file)) {
				System.out.println("File not in medication files " + file);
				continue;
			}

			this.log.writeError("\n\nCOMPARING FILE :  " + file );

			compareFiles(this.reasonAnnotation.get(file).annotations, 
					this.goldAnnotations.get(file).annotations);
		}


		totalMissed = 291 - totalCorrect;
//				totalMissed = 1637 - totalCorrect;

		System.out.println("Total correct " + String.valueOf(totalCorrect));
		System.out.println("Total incorrect " + String.valueOf(totalIncorrect));
		System.out.println("Total missed " + String.valueOf(totalMissed));


		double precision = totalCorrect*1. /(totalCorrect + totalIncorrect);
		double recall = totalCorrect *1. /(totalCorrect + totalMissed);
		double f_measure = 0;

		if(precision != 0. && recall != 0.)
			f_measure = 2*precision*recall /(precision + recall);

		System.out.println("Missed\tincorrect\tcorrect");
		System.out.println(String.valueOf(totalMissed) + "\t" + String.valueOf(totalIncorrect) + "\t" + 
				String.valueOf(totalCorrect));

		System.out.println("Precision\trecall\tf-measure: ");
		System.out.println(String.valueOf(precision) +"\t" + String.valueOf(recall) + "\t" + 
				String.valueOf(f_measure));
		
		return f_measure;
	}

	/**
	 * @param annotations
	 * @param annotations2
	 */
	public void compareFiles(ArrayList<Annotation> systemAnnts,
			ArrayList<Annotation> gsAnnts) {
		ArrayList<Annotation> gsPredictions = new ArrayList<Annotation>();
		ArrayList<Annotation> systemPrediction = new ArrayList<Annotation>();

		for(Annotation annt : systemAnnts) {
			systemPrediction.add(annt);
		}

		for(Annotation annt: gsAnnts) {
			gsPredictions.add(annt);
		}



		// iterate through the system reasons and find the equivalent 
		// medication reason
		Iterator<Annotation> it = systemPrediction.iterator();

		while(it.hasNext()) {
			Annotation currentAnnt = it.next();
			AnnotationDetail currentMedication = currentAnnt.annotationElements.get(AnnotationType.M);

			Annotation match = null;

			boolean foundMatch  = false;
			for(Annotation annt : gsPredictions) {
				AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

				if(medication.equalsAnnotation(currentMedication) ||
						(matchType == Match.INEXACT &&
						(medication.overlapAnnotation(currentMedication) ||
								medication.overlapDiffLines(currentMedication)))
						) {
					foundMatch = true;
					match = annt;

					AnnotationDetail reason = annt.annotationElements.get(AnnotationType.R);
					AnnotationDetail currentReason = currentAnnt.annotationElements.get(AnnotationType.R);

					if(reason.content.equals("nm") && 
							currentReason.content.equals("nm"))
						break;

					if(!reason.content.equals("nm") && 
							currentReason.content.equals("nm")){
						this.log.writeError("MISSED :" + Annotation.printString(annt));

						totalMissed ++;
						break;
					}

					if(reason.content.equals("nm") && 
							!currentReason.content.equals("nm")){
						totalIncorrect ++;
						this.log.writeError("EXTRA : " + Annotation.printString(currentAnnt));						
//						this.log.writeError("---- (gs): " + Annotation.printString(annt));		

						break;
					}

					if(reason.equalsAnnotation(currentReason)){
//						this.log.writeError("CORRECT :" + Annotation.printString(annt));
						totalCorrect ++;
					}
					else if(reason.overlapAnnotation(currentReason) || 
							reason.overlapDiffLines(currentReason)){ 
//						this.log.writeError("PARTIAL :" + Annotation.printString(annt));
						totalCorrect ++;
					}else{
						this.log.writeError("INCORRECT : " + Annotation.printString(currentAnnt));						
//						this.log.writeError("---- (gs): " + Annotation.printString(annt));		

						totalIncorrect ++;
					}

					break;
				}
			}

			if(foundMatch) {
				gsPredictions.remove(match);
				it.remove();
			}
		}

		for(Annotation annt : systemPrediction) {
			AnnotationDetail reason = annt.annotationElements.get(AnnotationType.R);

			if(!reason.content.equals("nm")){
				this.log.writeError("INCORRECT :" + Annotation.printString(annt));
				totalIncorrect ++;
			}
		}

		for(Annotation annt : gsPredictions) {
			AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);

			if(medication.content.equals("nm"))
				continue;
			this.log.writeError("MISSED med:" + Annotation.printString(annt));
			totalMissed ++;
		}
	}

	public static void main(String[] args) {
		if (args.length != requiredArgs) {
			System.out
			.println("Incorrect arguments! Required configuration file path.");
			System.exit(-1);
		}

		matchType = Match.INEXACT;

		MatchCompare compare = new MatchCompare(args[0]);

		compare.execute();
	}
}
