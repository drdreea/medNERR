/**
 * 
 */
package postprocess.bing;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

// Map prefixes to Namespace URIs
public class APINameSpaceContext implements NamespaceContext 
{
	static final String WEB_NAMESPACE =  "http://schemas.microsoft.com/LiveSearch/2008/04/XML/web";
	static final String API_NAMESPACE = "http://schemas.microsoft.com/LiveSearch/2008/04/XML/element";
	static final String SPELL_NAMESPACE = "http://schemas.microsoft.com/LiveSearch/2008/04/XML/spell";
	static final String RS_NAMESPACE = "http://schemas.microsoft.com/LiveSearch/2008/04/XML/relatedsearch";
	static final String PB_NAMESPACE = "http://schemas.microsoft.com/LiveSearch/2008/04/XML/phonebook";
	static final String MM_NAMESPACE = "http://schemas.microsoft.com/LiveSearch/2008/04/XML/multimedia";
	static final String AD_NAMESPACE = "http://schemas.microsoft.com/LiveSearch/2008/04/XML/ads";
	static final String IA_NAMESPACE = "http://schemas.microsoft.com/LiveSearch/2008/04/XML/instantanswer";
	static final String NEWS_NAMESPACE = "http://schemas.microsoft.com/LiveSearch/2008/04/XML/news";
	static final String ENCARTA_NAMESPACE = "http://schemas.microsoft.com/LiveSearch/2008/04/XML/encarta";

	public String getNamespaceURI(String prefix) 
	{
		if (prefix == null) throw new NullPointerException("Null prefix");
		else if ("api".equals(prefix)) return API_NAMESPACE;
		else if ("web".equals(prefix)) return WEB_NAMESPACE;
		return XMLConstants.NULL_NS_URI;
	}
	
	// This method isn't necessary for XPath processing.
	public String getPrefix(String uri) 
	{
		throw new UnsupportedOperationException();
	}
	
	public Iterator<String> getPrefixes(String arg0) 
	{
		throw new UnsupportedOperationException();
	}
}