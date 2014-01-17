package hptest;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class RateInfoCollector extends Thread{
	HealthTestCase _case;
	boolean _ifrun = true;
	
	String _siteId;
	String _siteUrl;
	//String _siteUrl;
	//String _prodLine;
	boolean _isIFP;
	String _abid;
	
	HtmlPage _quotepage;
	
	List<String> _allPlansList = new ArrayList<String>();
	//List<String> _sponsors = new ArrayList<String>();
	int _status=0;
	Exception _exception = null;
	
	public RateInfoCollector(String siteid, String surl, String ricid){
		this._siteId = siteid;
		this._siteUrl= surl;

		//this._checkRates = checkRate;
		//this._checkSponsors = checksp;
		this._abid = ricid;
	}
	
	public synchronized void doCase(HealthTestCase testcase, String prodline){
		//if("Medicare".equalsIgnoreCase(prodline) || "MS".equals(prodline) || "MA".equals(prodline) || "MD".equals(prodline))
			//_prodLine = "MS"; 
		//else
		//	_prodLine= prodline;
		
		_isIFP = !("Medicare".equalsIgnoreCase(prodline) || "MS".equals(prodline) || "MA".equals(prodline) || "MD".equals(prodline));

		this._case = testcase;
		
		this.notify();
	}
	
	public synchronized void cancel(){
		this._ifrun = false;
		this._case = null;
		this.notify();
	}
	
	public synchronized void run()
	{
		WebClient client;
		String quoteurl;
		
		while(_ifrun)
    {
      try{wait();}
      catch(InterruptedException e){}

      if(!_ifrun || _case==null) continue;
      
      _status = 1;//running;
      _exception = null;
      
      _allPlansList.clear();
      
      //System.out.println("start job on "+ this._siteUrl);
      try{
      	client = new WebClient();
      	client.setTimeout(10000);
        //client.setUseInsecureSSL(true);
        client.setJavaScriptEnabled(false);
        client.setCssEnabled(false);
        
        if(_isIFP)
        	quoteurl = _siteUrl + "/individual-health-insurance/quote?zip=" + _case._zipcode;
        else
        	quoteurl = _siteUrl + "/medicare/quote?zip="+ _case._zipcode+ "&page&pageSize=300";
        
        System.out.println(quoteurl);
        
      	//if(_ifrun && _checkRates){
        
      	int allnum=0;
   			int retry=0;
   			while(retry<10)
   			{
     			try{
     				if(_isIFP)
     					_quotepage = quoteIFP(client, _case);
     				else
	      			_quotepage = client.getPage(quoteurl);
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
	      	retry=11;
	      	
	      	if(_isIFP)
	      		allnum = GetIFPPlansInfo(_quotepage, _allPlansList);
	      	else
	      		allnum = GetMedicareAllPlansInfo(_quotepage, _allPlansList);
      	}

     		System.out.println("Total "+ allnum +" Plans in "+ _siteId);
      	
  			_status = 2; // one case is done;
      }
      catch(Exception ex){
      	ex.printStackTrace();
      	this._exception = ex;
      	//this._exception.setStackTrace(ex.getStackTrace());
      	_status = 3; // exception;
      }
    }
		_status = 4; // thread finished;
	}
	
	public HtmlPage getPage(){
		return _quotepage;
	}
	

	HtmlPage quoteIFP(WebClient client, HealthTestCase bicase) throws Exception
	{
		Page page = client.getPage(_siteUrl);
    page = client.getPage(_siteUrl +"/api/home-count?zip="+ bicase._zipcode);
    //System.out.println(page.getWebResponse().getContentAsString());
    //System.out.println("-------------------");
    
    page = client.getPage(_siteUrl +"/api/clientData");
    String jsonstr =  page.getWebResponse().getContentAsString();
    //System.out.println(jsonstr);
    //System.out.println("-------------------");
    JSONObject profileJsonObj = new JSONObject(jsonstr);
    
    JSONObject census = profileJsonObj.getJSONObject("census");
    JSONArray members = bicase.buildMembersJsonArray(); //new JSONArray(); //census.getJSONArray("member");
    census.put("member", members);
    
    census.put("isCanonical", false);
    census.put("effective", bicase._effdatestr);
    
    profileJsonObj.put("pageSize", 300);
    
    String postbody = profileJsonObj.toString();
    //System.out.println(postbody);
    //System.out.println("-------------------");
    
    WebRequest post = new WebRequest(new java.net.URL(_siteUrl +"/api/clientData"), HttpMethod.POST);
    post.setAdditionalHeader("Content-Type", "application/json; charset=UTF-8");
    post.setAdditionalHeader("X-Requested-With", "XMLHttpRequest");
    post.setAdditionalHeader("Referer", "http://pengujian.healthpocket.com/individual-health-insurance/quote");
    post.setRequestBody(postbody);
    page = client.getPage(post);
    //jsonstr =  page.getWebResponse().getContentAsString();
    //System.out.println(jsonstr);
    //System.out.println("-------------------");
    
    
    HtmlPage quotepage = client.getPage(_siteUrl +"/individual-health-insurance/quote");
    return quotepage;
	}
	
	static int GetIFPPlansInfo(HtmlPage quotepage, List<String> plansList) throws Exception
	{
		List<?> plandivs = quotepage.getByXPath("//div[@class=\"planContainer\"]"); //and @data-id
		
		int plansnum = plandivs.size(); 
    //System.out.println(plandivs.size() +" plans");
    
    for(int x=0; x<plansnum; x++)
    {
	    HtmlElement div = (HtmlElement)plandivs.get(x);
	    
	    StringBuffer buf = new StringBuffer();
	    
	    HtmlElement plannamelnk = (HtmlElement)div.getFirstByXPath(".//div[@class='header']//h4/a");
	    String planid = plannamelnk.getAttribute("data-id");
	    if(planid==null) planid="NULL-00";
	    
	    buf.append('(').append(planid).append(')');
	    buf.append(plannamelnk.asText().trim());
	    buf.append('|');
	    
	    HtmlElement company = (HtmlElement)div.getFirstByXPath(".//div[@class='header']//p[@class='issuerName']");
	    buf.append(company.asText().trim());
	    //System.out.println("header :: "+ divheader.asText());
	    buf.append("; ");
	    
	    HtmlElement costelem = (HtmlElement)div.getFirstByXPath(".//div[@class='planCost']");
	    //System.out.println("cost :: "+ costelem.asText());
	    buf.append("Rate: ").append(costelem.asText().trim());
	    
	    buf.append("; ");
	    
	    HtmlElement detailelem = (HtmlElement)div.getFirstByXPath(".//div[@class='planDetailInfoWrapper']");
	    //System.out.println("detail :: "+ detailelem.asText());
	    buf.append(detailelem.asText().replaceAll("[\\n\\s]+", " ").trim());
	    buf.append(" ");
	    
	    List<?> hls = div.getByXPath(".//div[@class='creatures']/ul/li/span");
	    //System.out.println(hls.size() +" high-lights");
	    
	    for(int i=0; i<hls.size(); i++){
	    	HtmlElement span = (HtmlElement)hls.get(i);
	    	//System.out.println(span.getAttribute("title"));
	    	buf.append(span.getAttribute("title")).append(' ');
	    }
	    
	    plansList.add(buf.toString());
    }
    
    return plansnum;
	}
	
	static int GetMedicareAllPlansInfo(HtmlPage quotepage, List<String> plansList) throws Exception 
	{
		List<?> plandivs = quotepage.getByXPath("//div[@class=\"planContainer\" and @data-id]");
    //System.out.println(plandivs.size() +" plans");
		int plansnum = plandivs.size(); 
		
    for(int x=0; x<plansnum; x++)
    {
	    HtmlElement div = (HtmlElement)plandivs.get(x);
	    
	    StringBuffer buf = new StringBuffer();
	    
	    HtmlElement plannamelnk = (HtmlElement)div.getFirstByXPath(".//div[@class='header']//h4/a");
	    
	    String planid = div.getAttribute("data-id");
	    if(planid==null) planid="NULL-00";
	    
	    buf.append('(').append(planid).append(')');
	    buf.append(plannamelnk.asText().trim());
	    buf.append('|');
	    HtmlElement company = (HtmlElement)div.getFirstByXPath(".//div[@class='header']//p[@class='issuerName']");
	    buf.append(company.asText().trim());
	    //System.out.println("header :: "+ divheader.asText());
	    buf.append(' ');
	    //buf.append("; ");
	    
	    HtmlElement costelem = (HtmlElement)div.getFirstByXPath(".//p[@class='planCost']");
	    //System.out.println("cost :: "+ costelem.asText());
	    buf.append("Rate: ").append(costelem.asText().trim());
	    buf.append(' ');
	    //buf.append("; ");
	    
	    HtmlElement detailelem = (HtmlElement)div.getFirstByXPath(".//div[@class='planDetailInfoWrapper']");
	    //System.out.println("detail :: "+ detailelem.asText());
	    buf.append(detailelem.asText().replaceAll("[\\n\\s]+", " ").trim());
	    buf.append(' ');
	    //buf.append(" ");
	    
	    List<?> hls = div.getByXPath(".//ul[@class='planHighlights']/li/span");
	    //System.out.println(hls.size() +" high-lights");
	    
	    for(int i=0; i<hls.size(); i++){
	    	HtmlElement span = (HtmlElement)hls.get(i);
	    	//System.out.println(span.getAttribute("title"));
	    	buf.append(span.getAttribute("title")).append(' ');
	    }
	    
	    plansList.add(buf.toString());
    }
		
    return plansnum;
	}
	
	
	static JSONObject buildMemberJsonObj(String gender, String dob, String role, boolean smoker) throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("gender", gender);
		jo.put("dob", dob);
		jo.put("role", role);
		jo.put("smoker", smoker);
		
		return jo;
	}
	
	static String getNextMonthToday(){
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy");
		Calendar cld = Calendar.getInstance();
		cld.add(Calendar.DATE, 31);
		return sdf.format(cld.getTime());
	}
}