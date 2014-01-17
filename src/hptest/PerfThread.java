package hptest;

import java.util.*;
import java.util.regex.*;
import com.meterware.httpunit.*;

public class PerfThread extends Thread {
	String[] zipcodes;
	
	int errornum = 0;
	public PerfThread(String[] zips){
		zipcodes = zips;
	}
	
	static Pattern Ptn_ValidPage = Pattern.compile("Showing \\d+ results", Pattern.CASE_INSENSITIVE);
	
	public void run() 
	{
		int len = zipcodes.length;
		
		Pattern ptn = Pattern.compile("[A-Z]{2},\\d{5}");
		//HttpUnitOptions.setScriptingEnabled(false);
		
		WebClient wc = new WebConversation();
		wc.getClientProperties().setAcceptGzip(false);
		wc.getClientProperties().setIframeSupported(false);
		//wc.getClientProperties().setUserAgent("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.13)");
		
		Random r = new Random();
		
		while(!PerfTest.STOP)
		{
			String sz = zipcodes[r.nextInt(len)];
			
			if(!ptn.matcher(sz).matches()) continue;
			
			//System.out.println(this.getName() +" - "+ sz);
			
			String state = sz.substring(0,2);
			String zipcode = sz.substring(3);
			String url = getURL(state, zipcode);

			System.out.println(this.getName() +" - "+ url);
			
			try{
				WebResponse wresp = wc.getResponse(url);
				String content = wresp.getText();
				Matcher mat = Ptn_ValidPage.matcher(content);
				if(mat.find()){
					long fbtime = wresp.getResponseTime();
					
					if(!PerfTest.STOP)
						PerfTest.ReqTimesList.add(fbtime);
				}
				else
					errornum++;
				//System.out.println(fbtime +" ms.");
			}
			catch(Exception ex){
				ex.printStackTrace();
				errornum++;
			}
		}
		
		
	}
	
	String getURL(String state, String zipcode) {
		//return "http://www.baidu.com";
		StringBuffer sb = new StringBuffer(PerfTest.HostDomain);
		if("MC".equalsIgnoreCase(PerfTest.Type)){
			sb.append("/medicare/quote?zip=").append(zipcode);
		}
		else if("IFP".equalsIgnoreCase(PerfTest.Type)){
			
			//http://pengujian.healthpocket.com/individual-health-insurance/quote?zip=20001&state=DC&r=2012-12-15&P=1981-10-11MT&S=1984-02-02FF
			sb.append("/individual-health-insurance/quote?zip=").append(zipcode);
			sb.append("&state=").append(state);
			
			if(PerfTest.BirthDate){
				sb.append("&r=").append(getNextMonthToday());
				sb.append("&P=").append(randomBirthdate());
			}
		}
		return sb.toString();
			
	}
	
	static String randomBirthdate(){
		int year = 1965 + (int)(Math.random()*25);
		int m = 1+ (int)(Math.random()*10);
		String mm = m<10 ? ("0"+m) : (""+m);
		
		int d = 1+ (int)(Math.random()*26);
		String dd = d<10 ? ("0"+d) : (""+d);
		
		StringBuffer sb = new StringBuffer();
		sb.append(year).append('-').append(mm).append('-').append(dd);
		
		sb.append((Math.random()*10)>=5 ? 'F' : 'M');
		sb.append((Math.random()*10)>=7 ? 'T' : 'F');
		
		return sb.toString();
	}
	
	static String getNextMonthToday(){
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		Calendar cld = Calendar.getInstance();
		cld.add(Calendar.DATE, 31);
		return sdf.format(cld.getTime());
	}
}
