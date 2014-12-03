/**
 * Created on Jan 16, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package utils;

import java.util.ArrayList;

import corpus.Indexes;


/**
 * @author ab
 *
 */
public class PhrasePair {
	public ArrayList<String> candidates;
	public String line;
	public String prevLine;
	public Boolean isList;
	public Indexes indexes;
	
	public PhrasePair(String line, String prev, Boolean list){
		this.candidates = new ArrayList<String>();
		this.line = line;
		this.prevLine = prev;
		this.isList = list;
		this.indexes = new Indexes();
		
	}
}
