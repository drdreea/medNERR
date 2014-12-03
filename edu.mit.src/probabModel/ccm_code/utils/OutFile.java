package probabModel.ccm_code.utils;


import java.io.FileWriter;
import java.io.PrintWriter;


public class OutFile {
	public PrintWriter out = null;
	
	public OutFile(String filename){
		try{
			out= new PrintWriter(new FileWriter(filename));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void println(String s){
		out.println(s);
	}
	public void print(String s){
		out.print(s);
	}
	public void close(){
		out.flush();
		out.close();
	}
}
