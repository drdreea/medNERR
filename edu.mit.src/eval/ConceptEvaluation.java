package eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.Annotation.AnnotationType;

import classifier.vector.AttributeVector;

import io.importer.ConceptEvalConfigHandler;
import io.importer.DataImport;

public class ConceptEvaluation {
    ConceptEvalConfigHandler configs;
    public DataImport importer;
    public HashMap<String, ArrayList<String>> conceptSemanticTypes;

    public ConceptEvaluation(String configsPath){
        configs = new ConceptEvalConfigHandler(configsPath);
        importer = new DataImport(null);

        this.conceptSemanticTypes = importer.importSemanticType(configs.semanticTypesPath);

    }

    /**
     * 
     */
    public void execute(){
        // import the gs and the system annotations
        HashMap<String, AnnotationFile> gsAnnotations = 
                importer.importAnnotations(this.configs.gsPath);
        HashMap<String, AnnotationFile> systemAnnotations = 
                importer.importAnnotations(this.configs.systemPath);

        HashMap<String, ArrayList<AnnotationDetail>> gsReasons = 
                new HashMap<String, ArrayList<AnnotationDetail>>();
        HashMap<String, ArrayList<AnnotationDetail>> systemReasons = 
                new HashMap<String, ArrayList<AnnotationDetail>>();

        // get the system and the gs reasons
        for(String file : gsAnnotations.keySet()){
            ArrayList<AnnotationDetail> annts = new ArrayList<AnnotationDetail>();

            for(Annotation annt : gsAnnotations.get(file).annotations){
    			AnnotationDetail reason = annt.annotationElements.get(AnnotationType.R);

                if(reason == null || reason.content.equals("nm"))
                    continue;
                annts.add(reason);
            }

            gsReasons.put(file, annts);
        }

        for(String file : systemAnnotations.keySet()){
            ArrayList<AnnotationDetail> annts = new ArrayList<AnnotationDetail>();

            for(Annotation annt : systemAnnotations.get(file).annotations){
    			AnnotationDetail reason = annt.annotationElements.get(AnnotationType.R);

            	if(reason == null || reason.content.equals("nm"))
                    continue;
                
                annts.add(reason);
            }

            systemReasons.put(file, annts);
        }
        
        // remove the gs duplicates
        for(String file : gsReasons.keySet()){
            ArrayList<AnnotationDetail> gs = gsReasons.get(file);
            ArrayList<AnnotationDetail> updatedGS = new ArrayList<AnnotationDetail>();
            ArrayList<String> annotationComment = new ArrayList<String>();
            
            for(AnnotationDetail annt : gs){
                if(annotationComment.contains(
                		AttributeVector.printComments(annt, annt, file)))
                    continue;
                annotationComment.add(
                		AttributeVector.printComments(annt, annt, file));
                updatedGS.add(annt);
            }
            
            gsReasons.put(file, updatedGS);
        }

        int truePositives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;

        // compare the system to the gs reasons
        for(String file : gsReasons.keySet()){
            if(! systemReasons.containsKey(file)) continue;

            ArrayList<AnnotationDetail> gs = gsReasons.get(file);
            ArrayList<AnnotationDetail> system = systemReasons.get(file);
            falsePositives += system.size();
            
            int common = compareCommonConcepts(gs, system);

            truePositives += common;
            falseNegatives += gs.size() - common;
            falsePositives -=  common;
        }

        double precision = truePositives*1.0/(truePositives + falsePositives);
        double recall = truePositives*1.0/(truePositives + falseNegatives);

        double f_Measure = 2.0*precision*recall/(precision + recall);
        
        System.out.println("True positives: " + String.valueOf(truePositives));
        System.out.println("False positives: " + String.valueOf(falsePositives));
        System.out.println("False negatives: " + String.valueOf(falseNegatives));
        
        System.out.println("precision\trecall\tF");
        System.out.println(String.valueOf(precision) + "\t" + String.valueOf(recall) + "\t" 
                + String.valueOf(f_Measure));
    }

    /**
     * 
     * @param gs
     * @param system
     * 
     * @return count of common concepts
     */
    public int compareCommonConcepts(ArrayList<AnnotationDetail> gs, 
            ArrayList<AnnotationDetail> system){
        Iterator<AnnotationDetail> it = gs.iterator();
        int count = 0;
                
        while(it.hasNext()){
            AnnotationDetail currentAnnt = it.next();
            AnnotationDetail foundAnnt = null;

            for(AnnotationDetail annt : system){
                if(annt.equalsAnnotation(currentAnnt) 
                        ||
                        annt.overlapAnnotation(currentAnnt) 
                        ||
                        annt.overlapDiffLines(currentAnnt)
                        ){
                    foundAnnt = annt;
                    break;
                }
            }

            if(foundAnnt != null){
                system.remove(foundAnnt);
                count ++;
            }else
            	System.out.println("MISSED " + currentAnnt.printString());
        }
        
        return count;
    }

    
    public static void main(String[] args){
        if(args.length != 1){
            System.out.println("Incorrect arguments");
            System.exit(-1);
        }

        ConceptEvaluation eval = new ConceptEvaluation(args[0]);

        eval.execute();
    }
}       
