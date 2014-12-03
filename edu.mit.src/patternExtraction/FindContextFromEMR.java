/**
 * 
 */
package patternExtraction;

import io.importer.DataImport;
import io.importer.EMRPatternConfigHandler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import corpus.Annotation;
import corpus.AnnotationDetail;
import corpus.AnnotationFile;
import corpus.MedicalRecord;
import corpus.Annotation.AnnotationType;

import utils.UtilMethods;

/**
 * @author ab
 *
 */
public class FindContextFromEMR {
    public EMRPatternConfigHandler configs;
    public DataImport importer;
    public HashSet<String> globalPatterns;

    public FindContextFromEMR(String configPath) {
        configs = new EMRPatternConfigHandler(configPath);
        importer = new DataImport(null);
        globalPatterns = new HashSet<String>();

    }

    public void execute() {
        HashMap<String, AnnotationFile> annotation = 
                importer.importAnnotations(this.configs.mergedPath);

        for(String file : annotation.keySet()) {
            MedicalRecord newRecord = importer.readRecordData(file, configs);
            ArrayList<Annotation> annts = annotation.get(file).annotations;

            HashSet<String> patterns = getPattern(newRecord.rawFileLines, annts);
            globalPatterns.addAll(patterns);
        }

        storePatterns();
    }

    /**
     * @param globalPatterns2
     */
    private void storePatterns() {
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(this.configs.outputPath));

            for(String key : globalPatterns) {
                out.write(key + "\n");
                out.flush();

            }

            out.close();
        }catch(Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param rawFileLines
     * @param annts
     * @return
     */
    private HashSet<String> getPattern(ArrayList<String> rawFileLines,
            ArrayList<Annotation> annts) {
        HashSet<String> patterns = new HashSet<String>();

        for(Annotation annt : annts) {
        	AnnotationDetail reason = annt.annotationElements.get(AnnotationType.R);
        	AnnotationDetail medication = annt.annotationElements.get(AnnotationType.M);
        	
            if(reason == null || 
                    reason.content == null || reason.content.equals("nm"))
                continue;

            String pattern = getContent(rawFileLines, medication, 
            		reason, true);

            if(pattern != null){
                patterns.add(pattern);
            }

        }

        return patterns;
    }
    
    /**
     * @param rawFileLines
     * @param medication
     * @param reason
     * 
     * @return
     */
    public static ArrayList<AnnotationDetail> getContentObjects(Object[] rawFileLines,
            AnnotationDetail medication, AnnotationDetail reason) {
        ArrayList<AnnotationDetail> contexts = new ArrayList<AnnotationDetail>();
        
        AnnotationDetail start = null;
        AnnotationDetail end = null;
        String content = "";
        
        // order the two concepts
        if(medication.endLine > reason.endLine) {
            start = reason;
            end = medication;
        }else if (medication.endLine < reason.endLine) {
            start = medication;
            end = reason;
        }else if(medication.endLine == reason.endLine) {
            if(medication.endOffset < reason.endOffset) {
                start = medication;
                end = reason;
            }else {
                start = reason;
                end = medication;
            }
        }

        if(start == null || end == null) 
            return null;

        int startLine = start.startLine;

        if(startLine != start.endLine) 
            startLine = start.endLine;


        for(int index = startLine - 1; index < end.startLine; index ++) {
            String line = (String) rawFileLines[index];

            if(index == startLine - 1 && index == end.startLine-1) {
                String[] splitLine = line.split(" ");
                for(int offset = start.endOffset+1; offset < end.startOffset && offset < splitLine.length; offset ++) {
                    AnnotationDetail annt = new AnnotationDetail();
                    annt.content = splitLine[offset];
                    annt.startLine = index + 1;
                    annt.startOffset = offset;
                    annt.endLine = index + 1;
                    annt.endOffset = offset;
                    contexts.add(annt);
                    content = UtilMethods.mergeStrings(content, splitLine[offset]);
                }
            }else if(index ==  startLine-1) {
                String[] splitLine = line.split(" ");
                for(int offset = start.endOffset + 1; offset < splitLine.length; offset ++) {
                    AnnotationDetail annt = new AnnotationDetail();
                    annt.content = splitLine[offset];
                    annt.startLine = index + 1;
                    annt.startOffset = offset;
                    annt.endLine = index + 1;
                    annt.endOffset = offset;
                    contexts.add(annt);
                    content = UtilMethods.mergeStrings(content, splitLine[offset]);
                }
            }else if(index == end.startLine-1) {
                String[] splitLine = line.split(" ");

                for(int offset = 0; offset < end.startOffset && 
                        offset < splitLine.length; offset ++) {
                    AnnotationDetail annt = new AnnotationDetail();
                    annt.content = splitLine[offset];
                    annt.startLine = index + 1;
                    annt.startOffset = offset;
                    annt.endLine = index + 1;
                    annt.endOffset = offset;
                    contexts.add(annt);
                    content = UtilMethods.mergeStrings(content, splitLine[offset]);
                }
            }else{
                String[] splitLine = line.split(" ");
                
                for(int span = 0; span < splitLine.length; span ++){
                    AnnotationDetail annt = new AnnotationDetail();
                    annt.content = splitLine[span];
                    annt.startLine = index + 1;
                    annt.startOffset = span;
                    annt.endLine = index + 1 ;
                    annt.endOffset = span;
                    contexts.add(annt);
                }
                
                content = UtilMethods.mergeStrings(content, line);
            }
        }

        return contexts;
    }
    /**
     * @param rawFileLines
     * @param medication
     * @param reason
     * @return
     */
    public static String getContent(ArrayList<String> rawFileLines,
            AnnotationDetail medication, AnnotationDetail reason, 
            boolean strict) {
        AnnotationDetail start = null;
        AnnotationDetail end = null;
        String content = "";
        
        // order the two concepts
        if(medication.endLine > reason.endLine) {
            start = reason;
            end = medication;
        }else if (medication.endLine < reason.endLine) {
            start = medication;
            end = reason;
        }else if(medication.endLine == reason.endLine) {
            if(medication.endOffset < reason.endOffset) {
                start = medication;
                end = reason;
            }else {
                start = reason;
                end = medication;
            }
        }

        if(start == null || end == null) 
            return null;

        int startLine = start.startLine;

        if(startLine != start.endLine) 
            startLine = start.endLine;


        for(int index = startLine - 1; index < end.startLine; index ++) {
            String line = rawFileLines.get(index);

            if(index == startLine - 1 && index == end.startLine-1) {
                String[] splitLine = line.split(" ");
                for(int offset = start.endOffset+1; offset < end.startOffset && offset < splitLine.length; offset ++) {
                    content = UtilMethods.mergeStrings(content, splitLine[offset]);
                }
            }else if(index ==  startLine-1) {
                String[] splitLine = line.split(" ");
                for(int offset = start.endOffset + 1; offset < splitLine.length; offset ++) {
                    content = UtilMethods.mergeStrings(content, splitLine[offset]);
                }
            }else if(index == end.startLine-1) {
                String[] splitLine = line.split(" ");

                for(int offset = 0; offset < end.startOffset && 
                        offset < splitLine.length; offset ++) {
                    content = UtilMethods.mergeStrings(content, splitLine[offset]);
                }
            }else
                content = UtilMethods.mergeStrings(content, line);
        }

        if(content.split(" ").length > 6 && strict)
            return null;

        String[] splitContent = content.split(" ");
        if(splitContent.length > 0  
                && ( UtilMethods.isNumber(splitContent[0]) || 
                        UtilMethods.isPunctuation(splitContent[0]))
                        && strict)
            return null;


        return content;
    }

    public static void main(String[] args) {

        FindContextFromEMR context = new FindContextFromEMR(args[0]);
        context.execute();
    }
}
