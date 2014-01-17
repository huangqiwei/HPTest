package hptest;

import java.util.*;
import java.io.*;
import org.apache.commons.jxpath.JXPathContext;
import org.w3c.dom.*;

import qapub.utils.*;

import com.meterware.httpunit.*;

public class SiteMapUrlCollect {
	static WebConversation _WC;
	static PrintWriter _UrlsFileWriter;
	static int _UrlsCount = 0;
	public static void main(String[] args) throws Exception {
		CommonData.trustHttpsCertificates();
		HttpUnitOptions.setScriptingEnabled(false);
		_WC = new WebConversation();
		_WC._connectTimeout = 9000;
		_WC._readTimeout = 9000;
		_WC.getClientProperties().setAcceptGzip(false);
		_WC.getClientProperties().setUserAgent("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0)");
		_WC.getClientProperties().setIframeSupported(false);
		
		_UrlsFileWriter = new PrintWriter("URLS.All.txt");
		//List<String> urlist = new ArrayList<String>();
		collectSiteMapUrls("http://www.healthpocket.com/sitemapIndex.xml");
		
		_UrlsFileWriter.close();
		System.out.println("--- [END] ---");
	}
	
	static void collectSiteMapUrls(String sitemapurl) throws Exception 
	{
		System.out.println("Collecting SiteMap:: "+ sitemapurl);
		int retry=0;
		WebResponse resp = null;
		while(retry<10)
		{
			try{
				resp = _WC.getResponse(sitemapurl);
			}
			catch(Exception ex){
				System.out.println("Error:: "+ ex.getMessage());
				retry++;
				if(retry>=10){
					throw ex;
				}
				else{
					System.out.println("Retry:: "+ retry +"/9");
					continue;
				}
			}
			retry = 12;
		}
		
		Document doc = resp.getDOM();
		System.out.println("----- site map dom ok ------");
		
		//XMLUtil.prettyPrint(doc, new PrintWriter(System.out));
		
		JXPathContext context = JXPathContext.newContext(doc);
  	context.registerNamespace("ns", "http://www.sitemaps.org/schemas/sitemap/0.9");

    List<Node> urlnodes = context.selectNodes("//ns:URL/ns:LOC/text()");
    for (Node urlnd : urlnodes) {
    	String url = urlnd.getNodeValue();
    	_UrlsFileWriter.println(url);
    	_UrlsCount++;
    	System.out.println(_UrlsCount +" :: "+ url);
    }
    _UrlsFileWriter.flush();
    
    List<Node> smlist = context.selectNodes("//ns:SITEMAP/ns:LOC/text()");
    
    for (Node nd : smlist) {
    	String smxmlurl = nd.getNodeValue();
    	if(smxmlurl.endsWith(".xml"))
    		collectSiteMapUrls(smxmlurl);
    }
	}
	
	
}
