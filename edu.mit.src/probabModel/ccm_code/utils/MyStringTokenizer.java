package probabModel.ccm_code.utils;


import java.util.StringTokenizer;

public class MyStringTokenizer {
	StringBuffer buf=null;
	String _s=null;
	String delims="\n\t\r ";
	String nextToken=null;
	String[] tokens;
	
	public MyStringTokenizer(String s){
		_s=s;
		buf=new StringBuffer(s);
		prepareNextToken();
		tokens=new String[10000];
		for(int i=0;i<buf.length()/100;i++)
			tokens[i]=buf.substring(i*100, i*100+100);
	}
	public boolean hasMoreTokens(){
		return nextToken!=null;
	}
	public void prepareNextToken()
	{
		StringBuffer res=new StringBuffer();
		int pos=0;
		while((pos<buf.length())&&(delims.indexOf(buf.charAt(pos))==-1))
		{
			res.append(buf.charAt(pos));
			pos++;
		}
		while((pos<buf.length())&&(delims.indexOf(buf.charAt(pos))!=-1))
			pos++;
		buf.delete(0, pos);
		nextToken=new String(res);
		if(nextToken.length()==0)
			nextToken=null;
	}
	
	public  String nextToken(){
		String res=nextToken;
		prepareNextToken();
		return res;
	}
	public  static void main(String[] args)
	{
		String lala="lala\n   \nlala\r\r     lalalala      ";
		MyStringTokenizer st=new MyStringTokenizer(lala);
		System.out.println(st.hasMoreTokens());
		System.out.println(st.nextToken());
		System.out.println(st.nextToken());
		System.out.println(st.hasMoreTokens());
		System.out.println(st.nextToken());
		System.out.println(st.hasMoreTokens());
	}
}
