/**
 * Created on Jan 27, 2012
 * @author ab
 * Contact andreeab dot mit dot edu
 * 
 */
package utils;

/**
 * @author ab
 *
 */
public class StringNumberPair {
	private int number;
	private String string;
	
	public StringNumberPair(String str, int nr) {
		this.setString(str);
		this.setNumber(nr);
	}

	/**
	 * @return the number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @param number the number to set
	 */
	public void setNumber(int number) {
		this.number = number;
	}

	/**
	 * @return the string
	 */
	public String getString() {
		return string;
	}

	/**
	 * @param string the string to set
	 */
	public void setString(String string) {
		this.string = string;
	}

}
