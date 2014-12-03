/**
 * 
 */
package postprocess.pcm;

import io.importer.DataImport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author ab
 *
 */
public class QueryPCM {
	final String pmcURL = "http://www.ncbi.nlm.nih.gov/pmc/?cmd=DetailsSearch";
	DataImport importer;
	public HashMap<ArrayList<String>, Integer> previousQueries;

	public QueryPCM(String pcmPath) {
		importer = new DataImport(null);
		previousQueries = new HashMap<ArrayList<String>, Integer>();
		previousQueries = importer.loadPCMCounts(pcmPath);
	}

	/**
	 * 
	 * @param s1
	 * @param s2
	 * @return the number of pattern matches
	 */
	public int queryCount(String s1, String s2) {
		ArrayList<String> queryFor = new ArrayList<String>();
		queryFor.add(s1);
		queryFor.add(s2);

		return queryCount(queryFor);
	} 
	
	/**
	 * 
	 * @param queryFor
	 * @return the number of pattern matches
	 */
	public int queryCount(ArrayList<String> queryFor) {
		int count = 0;

		if(this.previousQueries.containsKey(queryFor))
			return this.previousQueries.get(queryFor);

		String queryResult = retrieveQueryResult(queryFor);

		if(queryResult !=null) {
			try {
				String countString = importer.parsePCMResults(queryResult);
				if(countString!= null) {
					count = Integer.valueOf(countString);
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}

		previousQueries.put(queryFor, count);

		return count;
	}


	public String retrieveQueryResult(ArrayList<String> queryFor) {
		
		String updatedURL = pmcURL + "&term=";
		String result = null;
		

		for(String query : queryFor){
			updatedURL = 
					updatedURL + "\"" +query.replace(" ", "%20")+ "\"%5BAll%20Fields%5D";
			updatedURL = updatedURL + "+AND+";
		}

		updatedURL = updatedURL.substring(0, updatedURL.length()-5);

		HttpURLConnection connection = null;
		BufferedReader rd  = null;
		StringBuilder sb = null;
		String line = null;

		URL serverAddress = null;

		try {
			serverAddress = new URL(updatedURL);
			connection = null;

			//Set up the initial connection
			connection = (HttpURLConnection)serverAddress.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setReadTimeout(10000);

			connection.connect();

			//read the result from the server
			rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			sb = new StringBuilder();

			while ((line = rd.readLine()) != null)
			{
				sb.append(line + '\n');
			}

			result = sb.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			//close the connection, set all objects to null
			connection.disconnect();
			rd = null;
			sb = null;
			connection = null;
		}

		return result;
	}    


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		QueryPCM qpcm = new QueryPCM("aPath");
		ArrayList<String> queryFor = new ArrayList<String>();
		queryFor.add("cephradine");
		queryFor.add("wound infection");

		qpcm.queryCount(queryFor);


	}

}
