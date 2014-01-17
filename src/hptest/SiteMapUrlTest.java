package hptest;

import java.io.*;

import qapub.utils.CommonData;

import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

public class SiteMapUrlTest {
	static WebConversation _WC;
	static PrintWriter _TestResultFileWriter;
	static int _UrlsCount = 0;
	public static void main(String[] args) throws Exception {
		CommonData.trustHttpsCertificates();
		HttpUnitOptions.setScriptingEnabled(false);
		HttpUnitOptions.setExceptionsThrownOnErrorStatus(false);
		
		_WC = new WebConversation();
		_WC._connectTimeout = 9000;
		_WC._readTimeout = 9000;
		_WC.getClientProperties().setAcceptGzip(false);
		_WC.getClientProperties().setUserAgent("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0)");
		_WC.getClientProperties().setIframeSupported(false);
		
		_TestResultFileWriter = new PrintWriter("URLS.Error.txt");
		
		BufferedReader br = new BufferedReader(new FileReader("URLS.All.txt"));
		String url;
		while((url=br.readLine())!=null)
		{
			if(!url.startsWith("http://") && !url.startsWith("https://")) continue;
			
			System.out.println(url);
			
			int retry=0;
			WebResponse resp = null;
			while(retry<9)
			{
				try{
					resp = _WC.getResponse(url);
					int code = resp.getResponseCode();
					System.out.println(code);
					if(code>=400){
						_TestResultFileWriter.println(url +" : "+ code);
						_TestResultFileWriter.flush();
					}
				}
				catch(Exception ex){
					System.out.println("Error:: "+ ex.getMessage());
					retry++;
					if(retry>=9){
						_TestResultFileWriter.println(url +" : "+ ex.getMessage());
						_TestResultFileWriter.flush();
					}
					else{
						System.out.println("Retry:: "+ retry +"/9");
						continue;
					}
				}
				retry = 12;
			}
		}
		
		br.close();
		_TestResultFileWriter.close();
		System.out.println("--- [END] ---");
	}
	
	
}
