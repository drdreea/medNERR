/**
 * 
 */
package postprocess;

import io.importer.DataImport;
import io.importer.UMLSConceptConfigHandler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import conceptExtraction.NERRecognition;
import corpus.Annotation;
import corpus.AnnotationFile;
import corpus.Annotation.AnnotationType;

import utils.UtilMethods;

/**
 * @author ab
 *
 */
public class GetUMLSConcept {
    UMLSConceptConfigHandler configs ;
    HashMap<String, String> mappings;
    HashMap<String, List<String>> semanticTypes;
    DataImport importer;
    NERRecognition recognizer;

    /**
     * 
     * @param configPath
     */
    public GetUMLSConcept(String configPath) {
        this.configs = new UMLSConceptConfigHandler(configPath);
        mappings = new HashMap<String, String>();
        importer = new DataImport(null);
        recognizer = new NERRecognition(null);

        semanticTypes = new HashMap<String, List<String>>();

    }

    /**
     * 
     */
    public void execute() {
        HashMap<String, AnnotationFile> reasonAnnotations = 
                importer.importAnnotations(this.configs.reasonsPath);

        for(String key : reasonAnnotations.keySet()) {
            getConcepts(reasonAnnotations.get(key).annotations);
        }

        storeMappings();
    }

    /**
     * Get the semantic types of the concepts stored in file
     */
    public void getSemantics(){
        HashMap<String, AnnotationFile> reasonAnnotations = 
                importer.importAnnotations(this.configs.reasonsPath);

        HashMap<String, AnnotationFile> gsAnnotations = 
                importer.importAnnotations(this.configs.gsPath);

        for(String key : reasonAnnotations.keySet()) 
            getSemanticTypes(reasonAnnotations.get(key).annotations);


        for(String key : gsAnnotations.keySet()) 
            getSemanticTypes(gsAnnotations.get(key).annotations);


        storeMappingsSemantics();
    }


    /**
     * Store the retrieved mappings
     */
    private void storeMappings() {
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(this.configs.outputPath));

            for(String key : this.mappings.keySet()) {
                out.write(key + "||"+ this.mappings.get(key) + "\n");
                out.flush();

            }

            out.close();
        }catch(Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Store the retrieved mappings
     */
    private void storeMappingsSemantics() {
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(this.configs.outputPath));

            for(String key : this.semanticTypes.keySet()) {
                List<String> values = this.semanticTypes.get(key);
                String merged = key ;

                for(String el : values)
                    merged = UtilMethods.mergeStrings(merged, el, "||");

                out.write(merged + "\n");
                out.flush();
            }
            out.close();
        }catch(Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param annotations
     */
    private void getConcepts(ArrayList<Annotation> annotations) {
        for(Annotation annt : annotations) {

            if(annt.annotationElements.get(AnnotationType.R) != null) {
                String reason = annt.annotationElements.get(AnnotationType.R).content;
                if(reason == null) continue;
                if(this.mappings.containsKey(reason)) continue;

                String concept = this.recognizer.getConcept(reason);

                if(concept != null) {
                    this.mappings.put(reason, concept.toLowerCase());
                }
            }

            if(annt.annotationElements.get(AnnotationType.M) != null) {
                String medication = annt.annotationElements.get(AnnotationType.M).content;
                if(medication == null) continue;
                if(this.mappings.containsKey(medication)) continue;

                String concept = this.recognizer.getConcept(medication);

                if(concept != null) {
                    this.mappings.put(medication, concept.toLowerCase());
                }
            }
        }

    }

    /**
     * @param annotations
     */
    private void getSemanticTypes(ArrayList<Annotation> annotations) {
        for(Annotation annt : annotations) {
            if(annt.annotationElements.get(AnnotationType.R) != null) {
                String reason = annt.annotationElements.get(AnnotationType.R).content;
                if(reason != null) {
                    if(!this.mappings.containsKey(reason)){

                        List<String> concept = this.recognizer.getConceptCUI(reason);

                        if(concept != null) {
                            this.semanticTypes.put(reason, concept);
                        }
                    }
                }
            }

            if(annt.annotationElements.get(AnnotationType.M) != null){
                String medication = annt.annotationElements.get(AnnotationType.M).content;
                if(medication != null) {

                    if(!this.mappings.containsKey(medication)){

                        List<String> concept = this.recognizer.getConceptCUI(medication);

                        if(concept != null) {
                            this.semanticTypes.put(medication, concept);
                        }
                    }
                }
            }
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Incorrect arguments");
            System.exit(-1);
        }

        GetUMLSConcept conceptRecognizer = new GetUMLSConcept(args[0]);
        //		conceptRecognizer.execute();
        conceptRecognizer.getSemantics();
    }

}
