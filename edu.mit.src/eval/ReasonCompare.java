/**
 * 
 */
package eval;

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
public class ReasonCompare extends Matcher{
    final static int requiredArgs = 1;
	int totalMissed = 0;
	int totalCorrect = 0;
	int totalIncorrect = 0;
	
    /**
     * @param path
     */
    public ReasonCompare(String path) {
        super(path);
    }

    public void execute() {
        for(String file : this.reasonAnnotation.keySet()) {
            if(!this.goldAnnotations.containsKey(file)) {
                System.out.println("File not in medication files " + file);
                continue;
            }

            this.log.writeError("\n\nCOMPARING FILE :  " + file );

            compareFiles(this.reasonAnnotation.get(file).annotations, 
                    this.goldAnnotations.get(file).annotations);
        }
    }

    /**
     * @param annotations
     * @param annotations2
     */
    private void compareFiles(ArrayList<Annotation> systemAnnts,
            ArrayList<Annotation> gsAnnts) {
        ArrayList<AnnotationDetail> gsReasons = new ArrayList<AnnotationDetail>();
        ArrayList<AnnotationDetail> systemReasons = new ArrayList<AnnotationDetail>();

        for(Annotation annt : systemAnnts) {
            systemReasons.add(annt.annotationElements.get(AnnotationType.R));
        }

        for(Annotation annt: gsAnnts) {
            gsReasons.add(annt.annotationElements.get(AnnotationType.R));
        }


        // remove duplicates from the gsAnnotations
        Iterator<AnnotationDetail> it = gsReasons.iterator();

        while(it.hasNext()) {
            AnnotationDetail annt = it.next();

            for(AnnotationDetail annt2 : gsReasons) {
                if(annt.equals(annt2))
                    continue;
                if(annt.equalsAnnotation(annt2)) {
                    it.remove();
                    break;
                }
            }
        }

        // iterate through the system reasons and find the equivalent 
        // medication reason
        it = systemReasons.iterator();

        while(it.hasNext()) {
            AnnotationDetail currentAnnt = it.next();
            AnnotationDetail match = null;

            boolean foundMatch  = false;
            for(AnnotationDetail annt : gsReasons) {
                if(annt.equalsAnnotation(currentAnnt)) {
                    foundMatch = true;
                    match = annt;
                    this.log.writeError("CORRECT :" + 
                            annt.printString());
                    break;
                }

                if(annt.overlapAnnotation(currentAnnt) ||
                        annt.overlapDiffLines(currentAnnt)) {
                    foundMatch = true;
                    match = annt;
                    this.log.writeError("PARTIAL :" + 
                            annt.printString());
                    this.log.writeError("PARTIAL :" + 
                            currentAnnt.printString());
                    break;
                }
            }

            if(foundMatch) {
                gsReasons.remove(match);
                it.remove();
            }
        }

        for(AnnotationDetail annt : systemReasons) {
            this.log.writeError("INCORRECT :" + annt.printString());
        }

        for(AnnotationDetail annt : gsReasons) {
            if(annt.content.equals("nm"))
                continue;
            this.log.writeError("MISSED :" + 
                    annt.printString());
        }
    }

    public static void main(String[] args) {
        if (args.length != requiredArgs) {
            System.out
            .println("Incorrect arguments! Required configuration file path.");
            System.exit(-1);
        }
        ReasonCompare compare = new ReasonCompare(args[0]);

        compare.execute();
    }
}
