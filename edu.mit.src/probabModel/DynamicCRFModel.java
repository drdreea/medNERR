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
import probabModel.pipe.AcrfData2TokenSequence;

import edu.umass.cs.mallet.base.classify.Classifier;
import edu.umass.cs.mallet.base.classify.ClassifierTrainer;
import edu.umass.cs.mallet.base.classify.MaxEntTrainer;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.TokenSequence2FeatureVectorSequence;
import edu.umass.cs.mallet.base.pipe.TokenSequenceParseFeatureString;
import edu.umass.cs.mallet.base.pipe.iterator.LineGroupIterator;
import edu.umass.cs.mallet.base.pipe.iterator.PipeInputIterator;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.Labeling;
import edu.umass.cs.mallet.base.types.Labels;
import edu.umass.cs.mallet.base.types.LabelsSequence;
import edu.umass.cs.mallet.base.util.BshInterpreter;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.FileUtils;
import edu.umass.cs.mallet.base.util.Timing;
import edu.umass.cs.mallet.grmm.learning.ACRFTrainer.LogEvaluator;
import edu.umass.cs.mallet.grmm.learning.ACRFTrainer.TestResults;
import edu.umass.cs.mallet.grmm.learning.GenericAcrfData2TokenSequence;
import edu.umass.cs.mallet.grmm.learning.GenericAcrfTui;
import edu.umass.cs.mallet.grmm.learning.templates.SimilarTokensTemplate;

/**
 * @author ab
 *
 */
public class DynamicCRFModel {

    private static CommandOption.File modelFile = new CommandOption.File
            (GenericAcrfTui.class, "model-file", "FILENAME", true, null, "Text file describing model structure.", null);

    private static CommandOption.File trainFile = new CommandOption.File
            (GenericAcrfTui.class, "training", "FILENAME", true, null, "File containing training data.", null);

    private static CommandOption.File testFile = new CommandOption.File
            (GenericAcrfTui.class, "testing", "FILENAME", true, null, "File containing testing data.", null);

    private static CommandOption.Integer numLabelsOption = new CommandOption.Integer
            (GenericAcrfTui.class, "num-labels", "INT", true, -1,
                    "If supplied, number of labels on each line of input file." +
                            "  Otherwise, the token ---- must separate labels from features.", null);

    private static CommandOption.String inferencerOption = new CommandOption.String
            (GenericAcrfTui.class, "inferencer", "STRING", true, "TRP",
                    "Specification of inferencer.", null);

    private static CommandOption.String maxInferencerOption = new CommandOption.String
            (GenericAcrfTui.class, "max-inferencer", "STRING", true, "TRP.createForMaxProduct()",
                    "Specification of inferencer.", null);

    private static CommandOption.String evalOption = new CommandOption.String
            (GenericAcrfTui.class, "eval", "STRING", true, "LOG",
                    "Evaluator to use.  Java code grokking performed.", null);

    static CommandOption.Boolean cacheUnrolledGraph = new CommandOption.Boolean
            (GenericAcrfTui.class, "cache-graphs", "true|false", true, false,
                    "Whether to use memory-intensive caching.", null);

    static CommandOption.Boolean useTokenText = new CommandOption.Boolean
            (GenericAcrfTui.class, "use-token-text", "true|false", true, false,
                    "Set this to true if first feature in every list is should be considered the text of the " +
                            "current token.  This is used for NLP-specific debugging and error analysis.", null);

    static CommandOption.Integer randomSeedOption = new CommandOption.Integer
            (GenericAcrfTui.class, "random-seed", "INTEGER", true, 0,
                    "The random seed for randomly selecting a proportion of the instance list for training", null);

    private static BshInterpreter interpreter = setupInterpreter ();

    /**
     * @param args
     */
    public DynamicCRFModel(String[] args){
        doProcessOptions (GenericAcrfTui.class, args);

    }

    /**
     * 
     * @throws Exception
     */
    void execute() throws Exception{
        Timing timing = new Timing ();

        AcrfData2TokenSequence basePipe;
        // the AcrfData2TokenSequence param represents the number 
        // of labels in the test set
        basePipe = new AcrfData2TokenSequence (2);

        basePipe.setFeaturesIncludeToken(false);
        basePipe.setIncludeTokenText(true);

        Pipe pipe = new SerialPipes (new Pipe[] {
                basePipe,
                new TokenSequence2FeatureVectorSequence (false, true),
                //                                new TokenSequenceParseFeatureString (true)
        });

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

        ACRF.Template[] tmpls = new ACRF.Template[] {
                new ACRF.BigramTemplate (0),
                new ACRF.BigramTemplate (1),
                new ACRF.PairwiseFactorTemplate (0,1),
        };

        ACRF acrf = new ACRF (pipe, tmpls);
//        acrf.setInferencer (inf);
//        acrf.setViterbiInferencer (maxInf);
//
//        ACRFTrainer trainer = new ACRFTrainer ();
//        
//        trainer.train (acrf, training, null, null, eval, 100);
//        // original no. iterations = 9999
//        test(acrf, testing, eval);

        timing.tick ("Training");

        FileUtils.writeGzippedObject (new File ("acrf.ser.gz"), acrf);
        timing.tick ("Serializing");

        System.out.println ("Total time (ms) = " + timing.elapsedTime ());
    }

    /**
     * 
     * @param acrf
     * @param testing
     * @param eval
     */
    void test(ACRF acrf, InstanceList testing,ACRFEvaluator eval){
        ACRFEvaluator[] evals = new ACRFEvaluator[]{eval};

        File outputPrefix = new File ("testingResults.txt");
        File resultsFile = new File ("results.txt");
        ArrayList<String> resultLabels = new ArrayList<String>();

        @SuppressWarnings("unchecked")
        List<LabelsSequence> pred = acrf.getBestLabels (testing);
        ArrayList<ArrayList<ArrayList<Double>>> labelsProb = acrf.getLabelsProbabilities(testing);
        
        for(int seqIndex = 0; seqIndex < pred.size(); seqIndex ++){
            LabelsSequence ls = pred.get(seqIndex);
            ArrayList<ArrayList<Double>> predictions = labelsProb.get(seqIndex);
            
            for(int index = 0; index < ls.size(); index ++){
                Labels label = ls.getLabels(index);
                String tag = "";
                
                tag = label.toString();
               
                tag += " " + predictions.get(index).toString();
                resultLabels.add(tag);
            }
        }

        for (int i = 0; i < evals.length; i++) {
            evals[i].setOutputPrefix (outputPrefix);
            evals[i].test (testing, pred, "Testing");
        }

        TestResults results = LogEvaluator.computeTestResults (testing, pred);

        try {
            PrintWriter writer = new PrintWriter (new FileWriter (outputPrefix, true));
            results.print ("Testing", writer);
            writer.close ();
        } catch (Exception e) {
            e.printStackTrace ();
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
     * @return
     */
    private static BshInterpreter setupInterpreter ()
    {
        BshInterpreter interpreter = CommandOption.getInterpreter ();
//        try {
//            interpreter.eval ("import edu.umass.cs.mallet.base.extract.*");
//            interpreter.eval ("import edu.umass.cs.mallet.grmm.inference.*");
//            interpreter.eval ("import edu.umass.cs.mallet.grmm.learning.*");
//            interpreter.eval ("import edu.umass.cs.mallet.grmm.learning.templates.*");
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException (e);
//        }

        return interpreter;
    }

    /**
     * 
     * @param mdlFile
     * @return
     * @throws Exception
     */
    private static ACRF.Template[] parseModelFile (File mdlFile) throws Exception
    {
        BufferedReader in = new BufferedReader (new FileReader (mdlFile));

        List tmpls = new ArrayList ();
        String line = in.readLine ();
        while (line != null) {
//            Object tmpl = interpreter.eval (line);
//            if (!(tmpl instanceof ACRF.Template)) {
//                throw new RuntimeException ("Error in "+mdlFile+" line "+
//                        in.toString ()+":\n  Object "+tmpl+" not a template");
//            }
//            tmpls.add (tmpl);
            line = in.readLine ();
        }

        return (ACRF.Template[]) tmpls.toArray (new ACRF.Template [0]);
    }

    /**
     * 
     * @param spec
     * @return evaluator
     * @throws EvalError
     */
//    public static ACRFEvaluator createEvaluator (String spec) throws EvalError
//    {
//        if (spec.indexOf ('(') >= 0) {
//            // assume it's Java code, and don't screw with it.
//            return (ACRFEvaluator) interpreter.eval (spec);
//        } else {
//            LinkedList toks = new LinkedList (Arrays.asList (spec.split ("\\s+")));
//            return createEvaluator (toks);
//        }
//    }

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
     * @param spec
     * @return
     * @throws EvalError
     */
//    private static Inferencer createInferencer (String spec) throws EvalError
//    {
////        String cmd;
//        if (spec.indexOf ('(') >= 0) {
//            // assume it's Java code, and don't screw with it.
//            cmd = spec;
//        } else {
//            cmd = "new "+spec+"()";
//        }
//
//        // Return whatever the Java code says to
//        Object inf = interpreter.eval (cmd);
//
//        if (inf instanceof Inferencer)
//            return (Inferencer) inf;
//
//        else throw new RuntimeException ("Don't know what to do with inferencer "+inf);
        
        //TO DO: for now just return a new TRP
//        return new TRP();
//    }

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
        DynamicCRFModel crf = new DynamicCRFModel(args);
        try{
            crf.execute();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
