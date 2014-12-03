package probabModel.ccm_code.utils;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.util.Vector;

public class InFile {
	public static boolean convertToLowerCaseByDefault=true;
	public BufferedReader  in = null;
	
	public InFile(String filename){
		try{
			in= new BufferedReader(new FileReader(filename));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public String readLine(){
		try{
			String s=in.readLine();
			if(s==null)
				return null;
			if(convertToLowerCaseByDefault)
				return s.toLowerCase().trim();
			return s;
		}catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		return null;
	}
	
	public Vector<String> readLineTokens(){
		return tokenize(readLine());
	}	
	public static Vector<String> tokenize(String s,String delim){
		StringTokenizer st=new StringTokenizer(s,delim);
		Vector<String> res=new Vector<String>();
		while(st.hasMoreTokens())
			res.addElement(st.nextToken());
		return res;
	}	
	public static Vector<String> tokenize(String s){
		if(s==null)
			return null;
		StringBuffer snorm=new StringBuffer((int)(s.length()*1.5));
		for(int i=0;i<s.length();i++){
			boolean flag=false;
			if((s.charAt(i)=='?')||(s.charAt(i)=='!')||(s.charAt(i)==','))
			{
				snorm.append(" "+s.charAt(i)+" ");
				flag=true;
			}
			if(i<s.length()-1)
				if((s.charAt(i)=='.')&&((s.charAt(i+1)>'9')||(s.charAt(i+1)<'0')))
				{
					snorm.append(" "+s.charAt(i)+" ");
					flag=true;
				}
			if((s.charAt(i)=='\''))
			{
				snorm.append(" "+s.charAt(i));
				flag=true;
			}
			if(!flag)
					snorm.append(s.charAt(i));
		}
		s=snorm.toString();
		Vector<String> res=new Vector<String>();
		StringTokenizer st=new StringTokenizer(s);
		while(st.hasMoreTokens())
			res.addElement(st.nextToken());//.trim());
		res.trimToSize();
		return res;		
	}
	
	public void close(){
		try{
			this.in.close();
		}catch(Exception E){}
	}
}
