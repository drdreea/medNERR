package probabModel.ccm_code.utils;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Keyboard {
	public static BufferedReader standard = new BufferedReader(new InputStreamReader(System.in));
	
	public static String readLine() throws IOException{
		return  standard.readLine();
	}
}
