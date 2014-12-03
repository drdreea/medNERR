package probabModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import probabModel.grmm.ACRF;
import probabModel.grmm.ACRFEvaluator;
import probabModel.grmm.ACRFTrainer;
import probabModel.grmm.AcrfSerialEvaluator;
import probabModel.grmm.Inferencer;
import probabModel.grmm.MultiSegmentationEvaluatorACRF;
import probabModel.grmm.TRP;
import probabModel.pipe.FeatValueSequence2AugmentableFeatureVector;
import probabModel.pipe.LineGroupIterator;
import probabModel.pipe.MaxEntData2TokenSequence;
import probabModel.pipe.TokenSequence2FeatValueSequence;

import edu.umass.cs.mallet.base.classify.Classifier;
import edu.umass.cs.mallet.base.classify.ClassifierTrainer;
import edu.umass.cs.mallet.base.pipe.CharSequence2TokenSequence;
import edu.umass.cs.mallet.base.pipe.FeatureSequence2AugmentableFeatureVector;
import edu.umass.cs.mallet.base.pipe.FeatureSequence2FeatureVector;
import edu.umass.cs.mallet.base.pipe.Input2CharSequence;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.Target2Label;
import edu.umass.cs.mallet.base.pipe.TokenSequence2FeatureSequence;
import edu.umass.cs.mallet.base.pipe.TokenSequence2FeatureVectorSequence;
import edu.umass.cs.mallet.base.pipe.TokenSequenceLowercase;
import edu.umass.cs.mallet.base.pipe.TokenSequenceParseFeatureString;
import edu.umass.cs.mallet.base.pipe.iterator.PipeInputIterator;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.Labeling;
import edu.umass.cs.mallet.base.types.Labels;
import edu.umass.cs.mallet.base.types.LabelsSequence;
import edu.umass.cs.mallet.base.util.BshInterpreter;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.FileUtils;
import edu.umass.cs.mallet.base.util.Timing;

/**
 * @author ab
 *
 */
public class MaxEntModel {

    private static CommandOption.File modelFile = new CommandOption.File
            (MaxEntModel.class, "model-file", "FILENAME", true, null, "Text file describing model structure.", null);

    private static CommandOption.File trainFile = new CommandOption.File
            (MaxEntModel.class, "training", "FILENAME", true, null, "File containing training data.", null);

    private static CommandOption.File testFile = new CommandOption.File
            (MaxEntModel.class, "testing", "FILENAME", true, null, "File containing testing data.", null);

    private static CommandOption.Integer numLabelsOption = new CommandOption.Integer
            (MaxEntModel.class, "num-labels", "INT", true, -1,
                    "If supplied, number of labels on each line of input file." +
                            "  Otherwise, the token ---- must separate labels from features.", null);

    private static CommandOption.String inferencerOption = new CommandOption.String
            (MaxEntModel.class, "inferencer", "STRING", true, "TRP",
                    "Specification of inferencer.", null);

    private static CommandOption.String maxInferencerOption = new CommandOption.String
            (MaxEntModel.class, "max-inferencer", "STRING", true, "TRP.createForMaxProduct()",
                    "Specification of inferencer.", null);

    private static CommandOption.String evalOption = new CommandOption.String
            (MaxEntModel.class, "eval", "STRING", true, "LOG",
                    "Evaluator to use.  Java code grokking performed.", null);

    static CommandOption.Boolean cacheUnrolledGraph = new CommandOption.Boolean
            (MaxEntModel.class, "cache-graphs", "true|false", true, false,
                    "Whether to use memory-intensive caching.", null);

    static CommandOption.Boolean useTokenText = new CommandOption.Boolean
            (MaxEntModel.class, "use-token-text", "true|false", true, false,
                    "Set this to true if first feature in every list is should be considered the text of the " +
                            "current token.  This is used for NLP-specific debugging and error analysis.", null);

    static CommandOption.Integer randomSeedOption = new CommandOption.Integer
            (MaxEntModel.class, "random-seed", "INTEGER", true, 0,
                    "The random seed for randomly selecting a proportion of the instance list for training", null);

    /**
     * @param args
     */
    public MaxEntModel(String[] args){
        doProcessOptions (MaxEntModel.class, args);

    }

    /**
     * 
     * @throws Exception
     */
    void execute() throws Exception{
        Timing timing = new Timing ();

        MaxEntData2TokenSequence basePipe;
        // the AcrfData2TokenSequence param represents the number 
        // of labels in the test set
        basePipe = new MaxEntData2TokenSequence (1);

        basePipe.setFeaturesIncludeToken(false);
        basePipe.setIncludeTokenText(true);

        Pipe pipe = new SerialPipes (new Pipe[] {
                new Target2Label (),
                basePipe,
                new TokenSequence2FeatValueSequence(),
                new FeatValueSequence2AugmentableFeatureVector()
        });
        
        Alphabet al = pipe.getTargetAlphabet();
        
        PipeInputIterator trainSource = new LineGroupIterator (
                new FileReader (trainFile.value), 
                Pattern.compile ("^\\s*$"), 
                true);

        PipeInputIterator testSource = new LineGroupIterator (
                new FileReader (testFile.value), 
                Pattern.compile ("^\\s*$"), 
                true);


        InstanceList training = new InstanceList (pipe);
       
        training.add (trainSource);

        InstanceList testing = new InstanceList (pipe);
        testing.add (testSource);
        
        ClassifierTrainer maxEnt = new MaxEntTrainer();
        Classifier classifier = maxEnt.train(training);

        testMaxEnt(classifier, testing);

        timing.tick ("Training");

        System.out.println ("Total time (ms) = " + timing.elapsedTime ());
    }

    private void testMaxEnt(Classifier maxEnt, InstanceList testing) {
        Iterator instances = testing.iterator();
        File resultsFile = new File ("results.txt");
        ArrayList<String> resultLabels = new ArrayList<String>();
        
        while (instances.hasNext()) {
            Labeling labeling = maxEnt.classify(instances.next()).getLabeling();

            // print the labels with their weights in descending order (ie best first)                     
            String label = "";
            double maxValue = Double.MIN_NORMAL;
            
            for (int rank = 0; rank < labeling.numLocations(); rank++){
                if(labeling.getValueAtRank(rank) > maxValue){
                    maxValue = labeling.getValueAtRank(rank);
                    label = labeling.getLabelAtRank(rank).toString();
                }
//                label += labeling.getLabelAtRank(rank) + ":" +
//                                 labeling.getValueAtRank(rank) + " ";
            }
            
            resultLabels.add(label);

        }
        
        
        try {
            PrintWriter writer = new PrintWriter (new FileWriter (resultsFile, true));
            for(String label : resultLabels){
                writer.write(label + "\n");
            }
            writer.close ();
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }


    /**
     * 
     * @param toks
     * @return
     */
    private static ACRFEvaluator createEvaluator (LinkedList toks)
    {
        String type = (String) toks.removeFirst ();

        if (type.equalsIgnoreCase ("SEGMENT")) {
            int slice = Integer.parseInt ((String) toks.removeFirst ());
            if (toks.size() % 2 != 0)
                throw new RuntimeException ("Error in --eval : " +
                        "Every start tag must have a continue.");
            int numTags = toks.size () / 2;
            String[] startTags = new String [numTags];
            String[] continueTags = new String [numTags];

            for (int i = 0; i < numTags; i++) {
                startTags[i] = (String) toks.removeFirst ();
                continueTags[i] = (String) toks.removeFirst ();
            }

            return new MultiSegmentationEvaluatorACRF (startTags, continueTags, slice);

        } else if (type.equalsIgnoreCase ("LOG")) {
            return new ACRFTrainer.LogEvaluator ();

        } else if (type.equalsIgnoreCase ("SERIAL")) {
            List evals = new ArrayList ();
            while (!toks.isEmpty ()) {
                evals.add (createEvaluator (toks));
            }
            return new AcrfSerialEvaluator (evals);

        } else {
            throw new RuntimeException ("Error in --eval  : illegal evaluator "+type);
        }
    }

  
    /**
     * 
     * @param childClass
     * @param args
     */
    public static void doProcessOptions (Class childClass, String[] args)
    {
        CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
        options.add (childClass);
        options.process (args);
        options.logOptions (Logger.getLogger (""));
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args){
        MaxEntModel maxEnt = new MaxEntModel(args);
        try{
            maxEnt.execute();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
