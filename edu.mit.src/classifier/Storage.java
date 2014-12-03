package classifier;

import io.importer.ClassifierConfigHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import cc.mallet.classify.Classifier;

/**
 * Class handling file storage
 * @author ab
 *
 */
public class Storage {
    ClassifierConfigHandler configs ;
    ClassifierBase classifier;
    
    String trainFile = "/trainSamples.txt";
    String testFile = "/testSamples.txt";
    String commentsFile = "/comments.txt";
    
    public Storage(ClassifierConfigHandler configs, ClassifierBase classifyInstances ){
        this.configs = configs;
        this.classifier = classifyInstances;
        
        trainFile = configs.classifierOutputPath + trainFile;
        testFile = configs.classifierOutputPath + testFile;
        commentsFile = configs.classifierOutputPath + commentsFile;
    }
    
    public Storage(){
    	
    }
    
    /**
     * 
     * @param classifier
     */
    void storeClassifier(Classifier classifier){
        // store the classifier to file
        try{
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream (new File(
                            this.configs.classifierOutputPath + "/model.mallet")));
            oos.writeObject(classifier);
            oos.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * @param outputLines
     */
    public void storeTrain(ArrayList<String> outputLines) {
        String outputFile = configs.classifierOutputPath + "/trainSamples.txt";

        storeFile(outputFile, outputLines);
    }

    /**
     * 
     * @param outputLines
     */
    public void storeParam(ArrayList<String> outputLines) {
        String outputFile = configs.classifierOutputPath + "/paramSamples.txt";

        storeFile(outputFile, outputLines);

    }

    /**
     * @param outputLines
     */
    public void storeTest(ArrayList<String> outputLines) {

        storeFile(testFile, outputLines);

        if(!classifier.trainTestCreator.outputComments.isEmpty()){
            storeFile(commentsFile, classifier.trainTestCreator.outputComments);
        }

    }

    /**
     * @param outputLines
     */
    public void storeTrainMallet(ArrayList<String> outputLines, String file) {
        String outputFile = configs.classifierOutputPath + "/" + file;

        storeFile(outputFile, outputLines);

    }

    /**
     * 
     * @param outputLines
     */
    public void storePredictionLines(ArrayList<String> outputLines) {
        String file = configs.classifierOutputPath + "predictionLines";

        storeFile(file, outputLines);

    }
    
    public void storePredictionTags(ArrayList<String> tags, ArrayList<String> words ){
    	
    }
    
    /**
     * @param outputLines
     */
    public void storeTestMallet(ArrayList<String> outputLines, 
    		String file) {
        String outputFile = configs.classifierOutputPath + "/" + file;

        storeFile(outputFile, outputLines);

    }
    
    public void storeTestPredictionsMallet(ArrayList<String> outputLines) {
        String outputFile = configs.classifierOutputPath + "/testPredictions_mallet.txt";

        storeFile(outputFile, outputLines);

    }

    /**
     * @param outputFile
     * @param outputLines
     */
    public static void storeFile(String outputFile, ArrayList<String> outputLines) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));

            for(String line : outputLines) {
                out.write(line + "\n");
            }

            out.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 
     * @param previousQueries
     */
    public void storeCache(HashMap<ArrayList<String>, 
            Integer> previousQueries) {
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(this.configs.pcmPath));

            for(ArrayList<String> keys : previousQueries.keySet()) {
                int count = previousQueries.get(keys);
                String merged = "";

                for(String val : keys)
                    merged = merged + "||" + val;

                merged = merged + "||" + String.valueOf(count);

                merged = merged.replaceFirst("\\|\\|", "");

                out.write(merged + "\n");
                out.flush();

            }

            out.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}
