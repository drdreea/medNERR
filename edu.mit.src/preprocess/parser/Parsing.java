package preprocess.parser;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import utils.SystemConstants;
import utils.UtilMethods;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class Parsing {
	
    LexicalizedParser parser;
    TokenizerFactory<CoreLabel> tokenizerFactory;
    public static final List<String> verbs = Arrays.asList("VBN", "VBD", "VBG", "VBZ",
	    "VB");
    public static final List<String> nouns = Arrays.asList("NN", "NNS", "NNP", "NNPS", "FW");
    
    public static final List<String> possesive = Arrays.asList("POS", "PRP$", "WP$");
    
    public static final List<String> adj = Arrays.asList("JJ", "JJR", "JJS", "RBR", "RBS");
    private HashMap<String, Boolean> verbList;

    public Parsing(String parserPath) {
	LexicalizedParser lp = null;
	lp = new LexicalizedParser(parserPath);
	verbList = new HashMap<String, Boolean>();

	this.parser = lp;

	tokenizerFactory = PTBTokenizer
		.factory(new CoreLabelTokenFactory(), "");

    }

    public String parseLine(String line, String med) {

	// if the line is null or empty we continue to next phrase
	if (line == null || line.isEmpty())
	    return med;

	List<CoreLabel> rawWords2 = tokenizerFactory.getTokenizer(
		new StringReader(line)).tokenize();
	Tree parse = parser.apply(rawWords2);

	TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

	for (TypedDependency td : tdl) {
	    if (td.gov().label().beginPosition() == -1)
		continue;

	    String gov = line.substring(td.gov().label().beginPosition(),
		    td.gov().label().endPosition()).toLowerCase();

	    String dep = line.substring(td.dep().label().beginPosition(),
		    td.dep().label().endPosition()).toLowerCase();
	    String relation = td.reln().getShortName();

	    String tmpResult = operateMedication(dep, gov, med, relation);
	    if (tmpResult != null)
		med = tmpResult;

	}

	return med;
    }

    public String operateMedication(String dep, String gov, String med,
	    String relation) {
	if (UtilMethods.checkQuantityModal(gov)
		|| UtilMethods.endsWithNumber(gov)
		|| UtilMethods.checkQuantityModal(dep)
		|| UtilMethods.endsWithNumber(dep))
	    return null;

	boolean found = false;
	String fst = null;
	String snd = null;

	if (med.endsWith(gov) || med.startsWith(gov)) {
	    fst = gov;
	    snd = dep;
	    found = true;

	}
	else
	    if (med.endsWith(dep) || med.startsWith(dep)) {
		fst = dep;
		snd = gov;
		found = true;
	    }

	// special case when the medication ends with a percentage
	boolean percentage = false;
	if (med.endsWith("%")) {
	    String[] splitMed = med.split(" ");
	    if (splitMed.length > 2) {
		if (splitMed[splitMed.length - 2].endsWith(gov)) {
		    fst = gov;
		    snd = dep;
		    found = true;
		    percentage = true;
		}
		else
		    if (splitMed[splitMed.length - 2].endsWith(dep)) {
			fst = dep;
			snd = gov;
			found = true;
			percentage = true;
		    }
	    }
	}

	if (found) {

	    if (!SystemConstants.medDependencies.contains(relation))
		return null;

	    if ((med.startsWith(fst) && !med.contains(snd)) || percentage) {
		return UtilMethods.mergeStrings(snd, med);

	    }
	    else
		if (med.endsWith(fst) && !med.contains(snd))
		    return UtilMethods.mergeStrings(med, snd);

	}

	return null;
    }

    public HashMap<String, String> parseInput(String input) {
	HashMap<String, String> tags = new HashMap<String, String>();

	try {
	    List<CoreLabel> rawWords2 = tokenizerFactory.getTokenizer(
		    new StringReader(input)).tokenize();
	    Tree parse = parser.apply(rawWords2);

	    List<TaggedWord> taggedWords = parse.taggedYield();

	    for (TaggedWord word : taggedWords) {
		String value = input.substring(word.beginPosition(), word
			.endPosition());

		tags.put(value, word.tag());
	    }
	}
	catch (Exception e) {

	}

	return tags;
    }

    public boolean isVerb(String content) {
	if (this.verbList.keySet().contains(content))
	    return this.verbList.get(content);

	HashMap<String, String> tags = this.parseInput(content);

	// we only care if there is one word inside the hahsmap
	if (tags.size() == 1) {
	    for (String key : tags.keySet()) {
		String tag = tags.get(key);
		if (Parsing.verbs.contains(tag) && !key.endsWith("id")) {
		    this.verbList.put(content, true);
		    return true;
		}

	    }
	}

	this.verbList.put(content, false);
	return false;
    }

}
