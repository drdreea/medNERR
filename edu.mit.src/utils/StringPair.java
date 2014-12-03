/**
 * Created on Jan 20, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package utils;

import java.util.HashMap;

/**
 * @author ab
 *
 */
public class StringPair {
	public String el1;
	public String el2;
	public HashMap<String, Boolean> wordsMap;
	public int startIndex;
	public int endIndex;
	
	public StringPair(String e1, String e2) {
		this.el1 = e1;
		this.el2 = e2;
	}
	
	public void setIndexes(int start, int end) {
		this.startIndex = start;
		this.endIndex = end;
	}

}
