/**
 * 
 */
package postprocess.bing;

import io.importer.DataImport;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.code.bing.search.client.BingSearchClient;
import com.google.code.bing.search.client.BingSearchClient.SearchRequestBuilder;
import com.google.code.bing.search.client.BingSearchServiceClientFactory;
import com.google.code.bing.search.schema.SearchResponse;
import com.google.code.bing.search.schema.SourceType;
import com.google.code.bing.search.schema.web.WebResult;

/**
 * @author ab
 *
 */
public class BingSearches  {  
    SearchRequestBuilder builder;
    BingSearchClient client;
    final static String appId = "99B37C8EE16FB8CDFA5FFC7923A4A1935B682C20";
    DataImport importer;
    WebSample searchWeb;

    public HashMap<ArrayList<String>, HashMap<String, Double>> previousQueries;
    public HashMap<String, HashMap<String, Double>> patternQueries;

    public BingSearches(String pcmPath, String patternsPath) {

	searchWeb = new WebSample();

	BingSearchServiceClientFactory factory = BingSearchServiceClientFactory.newInstance();
	client = factory.createBingSearchClient();
	builder = client.newSearchRequestBuilder();
	builder.withAppId(appId);
	builder.withSourceType(SourceType.WEB);
	builder.withTranslationRequestSourceLanguage("");
	builder.withVersion("2.0");
//	builder.withAdultOption(AdultOption.STRICT);
	builder.withNewsRequestOffset(0L);
	builder.withNewsRequestCount(10L);

	importer = new DataImport(null);

	if(pcmPath == null)
	    previousQueries = new HashMap<ArrayList<String>, HashMap<String, Double>>();
	else
	    previousQueries = importer.loadSearchCounts(pcmPath);

	if(patternsPath == null)
	    patternQueries = new HashMap<String, HashMap<String, Double>>();
	else
	    patternQueries = importer.loadPatternQueries(patternsPath);
    }


    public HashMap<String, Double> execute(ArrayList<String> searchTerms, 
	    String query, boolean isStrict) {

	if(this.previousQueries.containsKey(searchTerms) && !isStrict) 
	    return this.previousQueries.get(searchTerms);

	HashMap<String, Double> results = new HashMap<String, Double>();

	for(String term : searchTerms) {
	    results.put(term, 0.);
	}

	try {
	    builder.withQuery(query);	
	    SearchResponse response = client.search(builder.getResult());

	    if(response.getWeb() != null)
		for(WebResult rsl : response.getWeb().getResults()) {
		    String content = rsl.getDescription();
		    if(content == null) continue;

		    content = content.toLowerCase();

		    for(String term : results.keySet()) {
			if(content.contains(term))
			    if(isStrict ) {
				if(!content.contains("..."))
				    results.put(term, results.get(term)+1);
			    }
			    else
				results.put(term, results.get(term)+1);
		    }
		}
	}catch(Exception e) {

	    e.printStackTrace();
	}

	for(String term : results.keySet()) {
	    results.put(term, results.get(term)*1./10);

	}

	return results;
    }

    public HashMap<String, Double> executePatternSearch(ArrayList<String> searchTerms, 
	    String query, String queryContent) {
	
	// remove punctuation signs
	if(query.contains(";") || query.contains(".")) {
	    query = query.replaceAll("\\.|;", "");
	    queryContent = queryContent.replaceAll("\\.|;", "");
	    
	}
	
	HashMap<String, Double> results = new HashMap<String, Double>();

	for(String term : searchTerms) {
	    results.put(term, 0.);
	}

	try {
	    builder.withQuery(query);	
	    SearchResponse response = client.search(builder.getResult());

	    if(response.getWeb() != null)
		for(WebResult rsl : response.getWeb().getResults()) {
		    String content = rsl.getDescription();
		    if(content == null) continue;

		    content = content.toLowerCase();
		    if(!content.contains(queryContent))
			continue;
		    
		    for(String term : results.keySet()) {
			if(content.contains(term.replaceAll("\\.|;", "")))
			    results.put(term, results.get(term)+1);
		    }
		}
	}catch(Exception e) {

	    e.printStackTrace();
	}

	for(String term : results.keySet()) {
	    results.put(term, results.get(term)*1./10);

	}

	return results;
    }

    public static void main(String[] args) {
	BingSearches search = new BingSearches(null, null);

	ArrayList<String> searchTerms = new ArrayList<String>();
	searchTerms.add("lasix");
	searchTerms.add("follow up");

	String query = "+ ";

	for(String term : searchTerms) {
	    query += " \"" + term + "\"";
	}

	query = query.trim();

	search.execute(searchTerms, query, false);
    }

    /**
     * @param content
     * @param content2
     * @return
     */
    public HashMap<String, Double> execute(String content, String content2) {
	ArrayList<String> searchTerms = new ArrayList<String>();
	searchTerms.add(content);
	searchTerms.add(content2);

	String query = "+ ";

	for(String term : searchTerms) {
	    query += " \"" + term + "\"";
	}

	query = query.trim();

	HashMap<String, Double> results = execute(searchTerms, query, false);
	previousQueries.put(searchTerms, results);


	return results;
    }

    /**
     * @param content
     * @param content2
     * @return
     */
    public HashMap<String, Double> executeStrict(String reason, String med) {
	ArrayList<String> searchTerms = new ArrayList<String>();
	searchTerms.add(reason);
	searchTerms.add(med);

	String query = "+ \"if " + reason + "\" \"" + med + "\"";


	query = query.trim();

	HashMap<String, Double> results = execute(searchTerms, query, true);
	previousQueries.put(searchTerms, results);

	return results;
    }


    /**
     * @param pattern
     * @param reason
     * @param med
     * @return
     */
    public HashMap<String, Double> executePatternSearch(String pattern,
	    String reason, String med) {
	ArrayList<String> searchTerms = new ArrayList<String>();
	searchTerms.add(reason);
	searchTerms.add(med);

	String query = "+ \"" + med + " " + pattern  + " " + reason + " \"";
	String queryContent = med + " " + pattern  + " " + reason;

	if(this.patternQueries.containsKey(query))
	    return this.patternQueries.get(query);

	System.out.println(query);

	query = query.trim();
	HashMap<String, Double> result = executePatternSearch(searchTerms, query, queryContent);
//	HashMap<String, Double> result = execute(searchTerms, query, true);

	this.patternQueries.put(query, result);

	return result;
    }

    public HashMap<String, Double> containsPattern(String pattern, 
	    String reason, String med){
	String query = "+ \"" + med + " " + pattern  + " " + reason + " \"";

	if(this.patternQueries.containsKey(query))
	    return this.patternQueries.get(query);

	return null;

    }
}
