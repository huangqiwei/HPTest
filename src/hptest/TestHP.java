package hptest;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import org.json.*;

import qapub.utils.FileUtil;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.*;


public class TestHP {
	
	static void ggtest() throws Exception 
	{
		final WebClient client = new WebClient();
    //client.setUseInsecureSSL(true);
    client.setJavaScriptEnabled(false);
    client.setCssEnabled(false);
    
    Pattern ptn = Pattern.compile("q=(https?\\:\\/\\/.+?)&sa=", Pattern.CASE_INSENSITIVE);
    PrintWriter pw = new PrintWriter(new FileWriter("d:\\googleresult.txt"));
    int len = keywords.length;
    int i=0;
    for(String kw : keywords)
    {
    	i++;
    	if(!kw.contains("Insurance")) kw = kw +" Insurance";
    	
    	System.out.println(i+"/"+ len +" : "+ kw);
	    pw.println(kw);
	    HtmlPage quotepage = client.getPage("http://www.google.com/search?q="+ kw);
	    List<?> plandivs = quotepage.getByXPath("//div[@id=\"ires\"]/ol/li/h3/a");
	    //System.out.println(plandivs.size() +" plans");
	    int c=0;
	    for(int x=0; c<3 && x<plandivs.size();x++)
	    {
		    HtmlAnchor ank = (HtmlAnchor)plandivs.get(x);
		    String href = ank.getHrefAttribute();
		    Matcher mat = ptn.matcher(href);
		    if(mat.find()){
		    	pw.println(mat.group(1));
		    	c++;
		    }
	    }
	    pw.println("-------------------------------");
	    pw.flush();
	    Thread.sleep(2000);
    }
	}
	
	public static void main(String[] args) throws Exception
	{
		//ggtest();
		
		
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "false");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "ERROR");
		System.out.println(System.currentTimeMillis());
		
    
		IFPtest();
		//medicaretest();
		
		//JSONObject jsonObj = new JSONObject(FileUtil.text(new File("d:\\temp\\xxx.txt")));
		//System.out.println(jsonObj.getString("pageSize"));
	}
	
	static JSONObject buildMemberJsonObj(String gender, String dob, String role, boolean smoker) throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("gender", gender);
		jo.put("dob", dob);
		jo.put("role", role);
		jo.put("smoker", smoker);
		
		return jo;
	}
	
	public static void IFPtest() throws Exception {
		final WebClient client = new WebClient();
    //client.setUseInsecureSSL(true);
    client.setJavaScriptEnabled(false);
    client.setCssEnabled(false);

    //client.setWebConnection(new SuryaniWrapper(client));
    Page page = client.getPage("http://pengujian.healthpocket.com");
    page = client.getPage("http://pengujian.healthpocket.com/api/home-count?zip=90001");
    System.out.println(page.getWebResponse().getContentAsString());
    System.out.println("-------------------");
    
    page = client.getPage("http://pengujian.healthpocket.com/api/clientData");
    String jsonstr =  page.getWebResponse().getContentAsString();
    System.out.println(jsonstr);
    System.out.println("-------------------");
    
    JSONObject jsonObj = new JSONObject(jsonstr);
    
    JSONObject census = jsonObj.getJSONObject("census");
    JSONArray members = new JSONArray(); //census.getJSONArray("member");
    members.put(buildMemberJsonObj("M","1980-11-12","P",false));
    members.put(buildMemberJsonObj("S","1980-11-12","S",false));
    members.put(buildMemberJsonObj("C","2000-10-10","C",false));
    
    census.put("member", members);
    census.put("isCanonical", false);
    census.put("effective", "2013-07-01");
    jsonObj.put("pageSize", 300);
    
    String postbody = jsonObj.toString();
    System.out.println(postbody);
    System.out.println("-------------------");
    
    WebRequest post = new WebRequest(new java.net.URL("http://pengujian.healthpocket.com/api/clientData"), HttpMethod.POST);
    post.setAdditionalHeader("Content-Type", "application/json; charset=UTF-8");
    post.setAdditionalHeader("X-Requested-With", "XMLHttpRequest");
    post.setAdditionalHeader("Referer", "http://pengujian.healthpocket.com/individual-health-insurance/quote");
    post.setRequestBody(postbody);
    page = client.getPage(post);
    jsonstr =  page.getWebResponse().getContentAsString();
    System.out.println(jsonstr);
    
    System.out.println("-------------------");
    
    
    int allnum=0;
    int plannum;
    int pagenum=1;
    //do{
      HtmlPage quotepage = client.getPage("http://pengujian.healthpocket.com/individual-health-insurance/quote");
      		//client.getPage("http://pengujian.healthpocket.com/medicare/quote?zip=60001&page&pageSize=300");
      		//client.getPage("http://pengujian.healthpocket.com/individual-health-insurance/quote?zip=99701&state=AK&r=2013-05-15&P=1990-01-01MT&page="+ page +"&pageSize=50");
      System.out.println("-------------------");
  		//System.out.println(System.currentTimeMillis());
      
      List<?> plandivs = quotepage.getByXPath("//div[@class=\"planContainer\"]"); //and @data-id
      plannum = plandivs.size();
      allnum += plannum;
            
      for(int x=0; x<plannum; x++)
      {
  	    HtmlElement div = (HtmlElement)plandivs.get(x);
  	    
  	    String xpath = div.getCanonicalXPath();
  	    //DomNode dn = div.cloneNode(true);
  	    //System.out.println(xpath);
  	    
  	    StringBuffer buf = new StringBuffer();
  	    
  	    HtmlElement plannamelnk = (HtmlElement)div.getFirstByXPath(".//div[@class='header']//h4/a");
  	    String planid = plannamelnk.getAttribute("data-id");
  	    if(planid==null) planid="NULL-00";
  	    //String href = plannamelnk.getAttribute("href");
  	    //System.out.println(href);
  	    //Matcher mat = ptn.matcher(href);
  	    
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
  	    
  	    System.out.println(buf);
      }
      pagenum++;
    //}
    //while(plannum==50);
    
    
    System.out.println(allnum +" plans");
    
    
   
    
    
    
	}
	
	public static void medicaretest() throws Exception {

    final WebClient client = new WebClient();
    //client.setUseInsecureSSL(true);
    client.setJavaScriptEnabled(false);
    client.setCssEnabled(false);
    
		HtmlPage quotepage = 
    		client.getPage("http://pengujian.healthpocket.com/medicare/quote?zip=10001&page&pageSize=300");
    		//client.getPage("http://pengujian.healthpocket.com/individual-health-insurance/quote?zip=90210&state=CA&r=2009-04-01P=1990-01-01MT&page&pageSize=300");
    System.out.println("-------------------");
		System.out.println(System.currentTimeMillis());
    
    List<?> plandivs = quotepage.getByXPath("//div[@class=\"planContainer\" and @data-id]");
    System.out.println(plandivs.size() +" plans");
    
    //Pattern ptn = Pattern.compile("/detail/(\\w+)/plan/(\\d+)/");
    
    for(int x=0; x<plandivs.size();x++)
    {
	    HtmlElement div = (HtmlElement)plandivs.get(x);
	    
	    String xpath = div.getCanonicalXPath();
	    //DomNode dn = div.cloneNode(true);
	    //System.out.println(xpath);
	    
	    StringBuffer buf = new StringBuffer();
	    
	    HtmlElement plannamelnk = (HtmlElement)quotepage.getFirstByXPath(xpath+ "//div[@class='header']//h4/a");
	    String planid = div.getAttribute("data-id");
	    if(planid==null) planid="NULL-00";
	    
	    //System.out.println(href);
	    //Matcher mat = ptn.matcher(href);
	    
	    //String planid="NULL-00";
	    //if(mat.find())
	    //	planid = mat.group(1)+"-"+mat.group(2);
	    
	    buf.append(planid).append(' ');
	    buf.append(plannamelnk.asText().trim());
	    
	    HtmlElement company = (HtmlElement)quotepage.getFirstByXPath(xpath+ "//div[@class='header']//p[@class='issuerName']");
	    buf.append(company.asText().trim());
	    //System.out.println("header :: "+ divheader.asText());
	    
	    buf.append("; ");
	    
	    DomNode costelem = (DomNode)quotepage.getFirstByXPath(xpath+ "//p[@class='planCost']");
	    //System.out.println("cost :: "+ costelem.asText());
	    buf.append("Rate: ").append(costelem.asText().trim());
	    
	    buf.append("; ");
	    
	    DomNode detailelem = (DomNode)quotepage.getFirstByXPath(xpath+ "//div[@class='planDetailInfoWrapper']");
	    //System.out.println("detail :: "+ detailelem.asText());
	    buf.append(detailelem.asText().replaceAll("[\\n\\s]+", " ").trim());
	    buf.append(" ");
	    
	    List<?> hls = quotepage.getByXPath(xpath+ "//ul[@class='planHighlights']/li/span");
	    //System.out.println(hls.size() +" high-lights");
	    
	    for(int i=0; i<hls.size(); i++){
	    	HtmlElement span = (HtmlElement)hls.get(i);
	    	//System.out.println(span.getAttribute("title"));
	    	buf.append(span.getAttribute("title")).append(' ');
	    }
	    
	    System.out.println(buf);
    }
	}
	
	static String[] keywords = new String[]{
		/*
		"MultiPlan",
		  "First Choice Health",
		  "Emblem Health",
		  "Commercial Insurance Company",
		  "Medicaid",
		  "POMCO Group",
		  "Wells Fargo TPA",
		  "Tricare",
		  "Worker's Compensation",
		  "Uniform Medical Plan",
		  "HealthNet",
		  "WellPoint",
		  "Champ VA",
		  "Simplifi",
		  "USI Affinity",
		  "Benesys",
		  "Thrivent Financial",
		  "National Elevator",
		  "Principle Life Insurance",
		  "The Guardian Life Insurance Company of America",
		  "Delta Dental",
		  "United Concordia",
		  "Ameritas",
		  "Mail Handlers Benefit Plan",
		  "United Healthcare Dental",
		  "Centene",
		  "Harvard Pilgrim Health Care",
		  "Guardian",
		  "Travelers",
		  "Assurant Health",
		  "Champus/Tricare",
		  "CoreSource",
		  "HealthSmart Holdings",
		  "NGS American",
		  "Bakery Confectionary Union Plan",
		  "DenteMax Dental",
		  "MetLife",
		  "GEHA",
		  "Pacific Source",
		  "Culinary Health Fund",
		  "AARP",
		  "Accepts most major Health Plans",
		  "Medico",
		  "MedHealthInsurance",
		  "Delta Health System",
		  "American Enterprise Group",
		  "TriWest Champus",
		  "Principal Financial",
		  "Planned Administration Inc",
		  "Veteran Administration Plan",
		  "Accepts most insurance",
		  "EBS-RMSCO",
		  "Old Surety Life Ins Company",
		  "UnitedHealth Group",
		  "We do not accept health insurance",
		  "ODS Health Network",
		  "Sagamore Health Network",
		  "FMH Benefit Services",
		  "Locals (any local)",
		  "MedCost",
		  "Brown Toland Physicians",
		  "Delta Premier",
		  "Mutual of Omaha",
		  "Scott White Health Plan",
		  "Humana Gold Plus",
		  "Aultman Health Foundation",
		  "Marion Polk Community Health Plan",
		  "PHP-Physicians Health Plan",
		  "Amalgamated Clothing Textile Workers Union",
		  "All Care Insurance Services",
		  "United Medical Resource",
		  "MagnaCare",
		  "Liberty Mutual Insurance Company",
		  "Bankers Life and Casualty",
		  "Excellus BCBS",
		  "PA Insurance Services",
		  "Bluegrass Family Health",
		  "Coventry Health Care of OK HMO",
		  "Community Health Network",
		  "Tufts Health Plans",
		  "Capital Blue Cross",
		  "FEP",
		  "Magellan Health Services",
		  "Mercy Health Plans",
		  "United HealthCare",
		  "Beech Street Corporation",
		  "Affinity Insurance Services",
		  "Emerald Health Network",
		  "LifeGuard Health",
		  "Empire Physicians Medical Group",
		  "National Association of Preferred Providers",
		  "National Benefit Plans",
		  "National Preferred Provider Network",
		  "National Healthcare Alliance",
		  "Coventry Health Care of LA HMO",
		  "Center Care",
		  "CHA Health",
		  "Health Link",
		  "WellPath Select",
		  "Three Rivers Provider Network",
		  "Principal Financial Group",
		  "HIP Health Insurance",
		  "Managed Care (Non-HMO)",
		  "Healthspan",
		  "Integrated Health Plan",
		  "Frontpath Health Coalition",
		  "Consumer Health Network",
		  "Neighborhood Health Providers",
		  "Univera Healthcare",
		  "BCN Medicare Advantage",
		  "MidMichigan Health",
		  "US Health and Life",
		  "Midland Health Plan",
		  "HealthPlus Medicare Advantage",
		  "Zurich",
		  "Applied Risk Management Solutions",
		  "Concentra",
		  "Encompass Medical Group",
		  "Gallagher Basset",
		  "Prudential",
		  "Blue Shield of California",
		  "BPS Healthcare (PPO)",
		  "Conventry/First Health",
		  "CappCare (PPO)",
		  "CorVel Corporation",
		  "Health Payors Organization",
		  "Horizon Managed Care",
		  "Joint Benefit Trust (PPO)",
		  "Interplan Health Group",
		  "Managed Care Administrators",
		  "Pacific Foundation for Medical Care",
		  "Networks By Design (PPO)",
		  "Western Health Advantage (HMO)",
		  "York Risk Services (PPO)",
		  "Pacific Health Alliance",
		  "DenteMax",
		  "PHCS Par-Multi Plan",
		  "Quality Health Plans",
		  "United Health Plan",
		  "Great West",
		  "Foundation Health Corporation",
		  "Devon Health",
		  "Intergroup",
		  "Prime Health Services",
		  "HealthEOS",
		  "Arise Health Plan",
		  "Security Health Plan (SHP)",
		  "North Central Healthcare Alliance (NCHA)",
		  "Wisconsin Educator's Association (WEA)",
		  "Today's Option",
		  "The Alliance",
		  "WPS Health Insurance (WPS)",
		  "American Postal Workers Union Health Plan",
		  "Blue Cross Blue Shield Managed Care",
		  "Private HealthCare Systems (PHCS)",
		  "Vytra Health Plans",
		  "Texas True Choice",
		  "True Choice",
		  "Affordable Health Benefits",
		  "Blue Cross/Blue Shield Dental",
		  "America's Health Choice",
		  "American Health Network",
		  "Global Medical Management PPO",
		  "HealthSource of Ohio",
		  "Physicians' Care Network",
		  "Employee Benefit Administrators",
		  "Blue Cross Federal",
		  "Blue Cross out of state",
		  "Meritain",
		  "State Farm",
		  "Teamsters or other Unions",
		  "UMR",
		  "Blue Access",
		  "Blue Advantage",
		  "Blue Advantage Plus",
		  "Blue Care HMO",
		  "Blue Cross Medicare Select",
		  "Cenpatico Behavioral Health",
		  "Cigna Behavioral Health",
		  "Compassionate Care Network",
		  "Comp Results",
		  "Coventry Advantra",
		  "Evercare",
		  "Family Health Partners",
		  */
		  "Freedom Network",
		  "Healthcare Preferred",
		  "HCA Midwest Comp Care",
		  "Healthcare USA",
		  "New Directions Behavioral Health",
		  "Healthe Exchange",
		  "Missouri Care",
		  "Preferred Care Blue",
		  "Preferred Health Professionals",
		  "Preferred Mental Health Management",
		  "Private Health Care Systems",
		  "Premier",
		  "Value Options",
		  "RockPort Health Care",
		  "Secure Horizons",
		  "Viant",
		  "Savility",
		  "Accountable Health Plans of America",
		  "Alta Health Network",
		  "AM/RAN",
		  "American Republic",
		  "Benesight",
		  "Anthem Bluecard Of St Louis",
		  "Blue Cross Arkansas",
		  "Blue Cross Blue Shield Health Select",
		  "Blue Cross Blue Shield of Indiana",
		  "Blue Cross Blue Shield Maryland",
		  "Blue Cross Blue Shield of TN",
		  "Blue Cross Blue Shield of TN-Network C",
		  "Blue Cross Blue Shield of TN-Network P",
		  "Blue Cross Blue Shield of TN-Network S",
		  "Blue Cross Idaho",
		  "Blue Cross of Alabama",
		  "Blue Cross Preferred Plus-Ford",
		  "Blue Cross of Illinois",
		  "Blue Cross Preferred Plus-GM",
		  "Blue Cross Trigon",
		  "Blue Cross Utah",
		  "Business Men's Associates",
		  "Megalife Insurance",
		  "SouthCare",
		  "Preferred Health Systems",
		  "Anthem MCARE Replacement",
		  "Bluegrass Health Network - Workers Comp",
		  "Choice Care",
		  "Humana National Transplant Network - BMT",
		  "Indiana Health Network",
		  "KY Medicaid",
		  "Indiana Medicaid",
		  "MD Wise Indiana Medicaid",
		  "Passport Advantage Health Plan",
		  "Passport Health Plan",
		  "Passport Home Health",
		  "Preferred Health Plan",
		  "Private Healthcare System",
		  "United Church of Christ",
		  "Southern Indiana Health Organization",
		  "USA Healthnet",
		  "AIG",
		  "United Teachers Association",
		  "American General",
		  "American Life Health Ins. Co.",
		  "BC MCR HMO",
		  "CIGNA FLEXCARE",
		  "Fortis Benefits Insurance Company",
		  "Employee Health Systems",
		  "National Association of Letter Carriers",
		  "Humana Health Care Plan",
		  "Pacific Life Annuity Company",
		  "Trust Mark",
		  "Wausa",
		  "Signature Health Alliance",
		  "ChoiceCare Network",
		  "Dunn Associates",
		  "Key Benefits Inc.",
		  "Pekin",
		  "Today's Options",
		  "Encore",
		  "HealthLink",
		  "National Heritage Insurance",
		  "Advantra",
		  "Community Care Network",
		  "Heritage Provider Network",
		  "United Food and Commercial Workers",
		  "CorVel Workers Compensation",
		  "MIPA HealthNet",
		  "Saint Francis Health Network",
		  "New York State Health Insurance Program",
		  "Lovelace Community Health Plan",
		  "Advantage Care Network (ACN)",
		  "CompPsych",
		  "Oregon Dental Service",
		  "United Horizons",
		  "Sedgwick CMS",
		  "Galaxy Health Network",
		  "Evolutions Healthcare Systems",
		  "HealthStar",
		  "NovaNet",
		  "AccessCare",
		  "Admar",
		  "Advantage 2000",
		  "Advocate Associate",
		  "Aetna Partners",
		  "Aetna Retired Teachers",
		  "Affiliated Health Funds",
		  "Affiliated Health Systems",
		  "Affiliated Healthcare",
		  "AHCCCS - AZ Health Care Cost Containment System",
		  "AIM",
		  "Alfa Insurance Company",
		  "Allegheny International Services",
		  "All Florida PPO Inc",
		  "Allegiance Health Plans",
		  "Alliant Health Plans",
		  "Alliance Health Care",
		  "Allied Insurance Group",
		  "Allina Advantage",
		  "AllKids Health Insurance",
		  "Alternative Health Delivery System",
		  "Allstate",
		  "American Benefit",
		  "America's Health Insurance Plans",
		  "American Community",
		  "American CareSource",
		  "American Commercial Barge Line",
		  "American Exchange",
		  "American Family Life Accident",
		  "American Federation of Television and Radio Artists",
		  "American Healthcare Providers Insurance Services Company",
		  "American Heritage",
		  "American Insurance Consultants",
		  "American Imaging Management (AIM)",
		  "American National Supplement",
		  "American Lifecare",
		  "American National",
		  "American Progressive Life Insurance/Health Insurance",
		  "AmeriPlan Discount Card",
		  "Amerisource",
		  "Anthem Senior Advantage",
		  "Amerivantage",
		  "Anthem BC Gov Wide",
		  "Anthem Group Services Corp.",
		  "Arcadian - Spokane Community Care",
		  "Apria Healthcare",
		  "APS Healthcare",
		  "Argonaut",
		  "Arizona Foundation for Medical Care",
		  "Arizona Health Care Cost Containment System",
		  "Asi Flex PPO Incorporated",
		  "Arizona Medical Network",
		  "ARTA Health Network",
		  "August Healthcare Services",
		  "Associates for Health Care",
		  "Avesis",
		  "AXIS",
		  "Baptist Health Services Group",
		  "AZ Benefit Options",
		  "AzMed",
		  "Bell South",
		  "AZFMC - AZ Foundation Medical Care",
		  "Behavioral Health Network",
		  "Baxter Health Care",
		  "Benefit Management",
		  "Benefit Panel Services",
		  "Benefit Planners",
		  "Blue Bell Benefit Trust",
		  "Benefit Trust Life Ins Co",
		  "Best Life Health Insurance",
		  "Bluegrass Family HMO",
		  "Berkshire Life Insurance Company of America",
		  "Blue Care Network-Blue Choice",
		  "Blue Care Network III",
		  "Boilermakers National Health Welfare Fund",
		  "Boon-Chapman",
		  "Champva",
		  "Bridgeway Advantage Medicare",
		  "Brown Williamson",
		  "Clarendon National",
		  "Bristol Park Medical Group",
		  "Chandler Group",
		  "Chesapeake Life Insurance Co",
		  "Clear One Health Plans",
		  "CNA",
		  "CNA HealthPro",
		  "Columbia Accountable Care Network",
		  "CoastalComp HealthNetworks",
		  "Colgate Palmolive/Aetna",
		  "Community Care of Southern Indiana",
		  "Commission for Children with Special Needs",
		  "Community Choice Medicaid",
		  "Community Health Choice",
		  "Community Health Network of WA",
		  "Confinity",
		  "Comp Choice",
		  "Comprehensive Medical Dental Plan",
		  "Companion Benefit Alternatives",
		  "CONNECTICOMP - WORKERS COMPENSATION",
		  "Confinity Health Network",
		  "CONNECTICARE - HMO",
		  "CONNECTICUT HEALTH PLAN - PPO",
		  "Conseco",
		  "Corporate Health Administrators",
		  "CONSUMER HEALTH NETWORK - PPO",
		  "Core Star",
		  "Cooperative Care and Hospital Agreement",
		  "Health Care Partners Medical Group and Affiliated Physicans",
		  "CSX Railroad",
		  "CUNA Mutual Group",
		  "Healthcare Select",
		  "Medica Prime Solution",
		  "EBC",
		  "First All America",
		  "Harrington Health",
		  "Physician's Mutual",
		  "Preferred Plan",
		  "United Mine Workers",
		  "USA Health and Wellness Network",
		  "United Postal Worker's Union",
		  "USA Managed Care Organization",
		  "Wausau Benefits",
		  "J. P. Farley of Ohio",
		  "Physicians Mutual",
		  "Vista Health Plans",
		  "UHP Healthcare",
		  "Florida Health Administrators",
		  "Hill Physicians",
		  "United Payors United Providers",
		  "Mutual Medical",
		  "Humana Cancer Care",
		  "Fortified Provider Network",
		  "StayWell",
		  "(GENERAL) - EPO",
		  "(GENERAL) - HMO",
		  "HEALTH NET - MEDICAID",
		  "HEALTHY KIDS - MEDICAID",
		  "MEDRISK, INC. - WORKERS COMPENSATION",
		  "National Provider Network",
		  "REVIEWCO - WORKERS COMPENSATION",
		  "USA HEALTH NETWORK - PPO",
		  "OXFORD COMMERCIAL - HMO",
		  "POMCO (TPA) - PPO",
		  "McLaren Health Plan",
		  "Care 1st",
		  "NCPPO",
		  "Medifocus",
		  "Maryland Physicians Care",
		  "Priority Partners",
		  "One Net",
		  "HealthSouth",
		  "CAP Management Systems",
		  "Priority Medicare Advantage",
		  "Department of Junvenile Justice-Commonwealth of KY",
		  "Direct Care America",
		  "Hospice Palliative Care Services,Inc.",
		  "Owensboro Community Health Network",
		  "Methodist Hospital Community Care Network",
		  "Kentucky Racing Health and Welfare",
		  "Optimum United Healthcare Bone Marrow Transplant",
		  "Dept. of US Army Medical Active Duty",
		  "Indiana Medicaid Assignment",
		  "United Parcel Service",
		  "University of Louisville Employee Health",
		  "Western American",
		  "Wal-Mart",
		  "Winflex",
		  "Dimension Health",
		  "Wells Fargo Insurance",
		  "Carecentrix Health Plan",
		  "HFN",
		  "Virginia Health Network",
		  "Virginia Premier",
		  "Caremore Medical Group",
		  "MedCare International",
		  "C and O Employee's Hospital Association",
		  "Central Virginia Health Network",
		  "CenVaNet",
		  "Employers Health Network",
		  "First Health Life and Health Insurance",
		  "MVP Health Plan Inc",
		  "Pyramid Today's Option",
		  "Optima Life Insurance Co",
		  "Souther Health",
		  "Va Coordinated Care for the Uninsured (VCC)",
		  "Motion Picture Industry",
		  "PPO Next",
		  "Keenan Associates",
		  "PCS Health Systems",
		  "Sloans Lake Health Insurance",
		  "Emdeon",
		  "Triwest",
		  "Mountain Medical Affiliates",
		  "One Call Medical (MRI CT only) - GA",
		  "Great West (One Health)",
		  "Care Management Resources",
		  "Employer's Health Network",
		  "Employers Health Welfare Plan",
		  "Government Employees Health Assurance",
		  "Humana Care Plus",
		  "Equitable",
		  "Integrated Medical Solutions",
		  "John Hancock",
		  "Integrated Medical Systems",
		  "Mass Mutual",
		  "Metropolitan Life Insurance",
		  "Metropolitian",
		  "Pipefitters",
		  "National Business Association Trust",
		  "Principle Life or Mutual",
		  "Physician Health Network",
		  "Prunet",
		  "SelectNet",
		  "Global Health Care Network",
		  "Total Health Choice",
		  "Memorial Health Network",
		  "Memorial Healthcare System",
		  "HealthPlus Medicaid",
		  "Vita Health",
		  "Memorial Hermann Physician Network",
		  "Sparks Health System",
		  "Secure Care",
		  "HealthCare Partners",
		  "Humana Medicare",
		  "ECOH",
		  "Fox Everett",
		  "Tenet HealthSystem Medical",
		  "Evolutions",
		  "Health Span",
		  "Medicare Replacement (Humana Replacement)",
		  "Starmark",
		  "Central Benefits",
		  "UNITED HEALTHCARE - WORKERS COMPENSATION",
		  "Humana MBP",
		  "Humana Freedom Plus",
		  "Guardian Dental",
		  "Renaissance Physicians Organization",
		  "Provider Select",
		  "Mass Behav Health Partnership",
		  "Texas Workers' Compensation",
		  "Kentucky Medical Assistance Assignment",
		  "NortonOne",
		  "TEACHERS RETIREMENT SYSTEM (TRS)",
		  "Children's Health Insurance Program (CHIP)",
		  "Union Pacific Railroad",
		  "Farm Bureau",
		  "Fidelity Life",
		  "Florida Hospital Healthcare System",
		  "Nation Wide",
		  "Farmers Insurance",
		  "Interwest Health Network",
		  "Essence Medicare Advantage",
		  "Washington State Labor Industries",
		  "Idaho Physicians Network",
		  "Idaho Medicaid",
		  "Sterling Medicare Advantage",
		  "Montana State Medicaid",
		  "Wenatchee Valley Med Center Health Plans",
		  "Oregon Medicaid (OMAP)",
		  "PHCS AZ",
		  "Sierra Health Nevada",
		  "Title V",
		  "Humana Veterans",
		  "United Secure Horizons",
		  "MCM Maxcare",
		  "Group Pension Admin Inc",
		  "El Paso First Health Network",
		  "Indian Health Service",
		  "OSMA Health",
		  "Oklahoma Health Network",
		  "HealthCrest",
		  "PCC Select",
		  "Sanus Health",
		  "Health Care District of Palm Beach County",
		  "Monarch Healthcare",
		  "Inland Empire Health Plan",
		  "PPO Plus",
		  "Houston Healthcare Purchasing Organization",
		  "Union Provider Services",
		  "United Gas Pipeline",
		  "Value Care Health Systems",
		  "Deseret Mutual (DMBA)",
		  "Public Employees Health Plan (PEHP)",
		  "Select Choice",
		  "Select Med",
		  "Public Employees Health Plan",
		  "Preferred Network Access",
		  "Delphi",
		  "United Healthcare WEST",
		  "San Francisco Health Plan",
		  "Western Health Advantage (WHA)",
		  "Medicaid of Indiana",
		  "Lifewise Health Plan of Arizona",
		  "Motorola",
		  "Medlife",
		  "Lincoln",
		  "Mercy Care (AHCCCS)",
		  "Maricopa Health Plan",
		  "Health Choice of Arizona (AHCCSS)",
		  "Phoenix Health Plan",
		  "Midwest Life",
		  "NASE",
		  "Unum",
		  "PacifiCare Health Systems",
		  "Pacific Mutual",
		  "Noridian Medicare",
		  "Provider Networks of America",
		  "Operating Engineers Health",
		  "Care2",
		  "Family Choice Healthcare",
		  "Name of Insurance Unknown",
		  "Precise",
		  "Pro America",
		  "State Mutual",
		  "Trustcare",
		  "The Wellness Plan",
		  "SmartCare",
		  "Kentucky Physician Plan Advantage",
		  "Kentucky Physicians Plan",
		  "HMO of KY",
		  "Teamcare",
		  "The Physicians, Inc",
		  "CBA Health Insurance",
		  "Dart Container Corporation",
		  "Eagle Administrators",
		  "EBM",
		  "Equicor",
		  "Health Care Preferred",
		  "Health Claim Services",
		  "Hoosier Healthcare Network",
		  "Healthwise",
		  "IBA",
		  "Kanawha",
		  "Kentucky Access",
		  "Kentucky Childrens Health Insurance",
		  "Kentucky Health and Welfare",
		  "Kentucky Kare Select",
		  "Leggett and Platt",
		  "Mayfair",
		  "Mercer Transportation Company",
		  "Medicaid of Kentucky",
		  "Mutual of New York",
		  "Mutual Protective Medicore",
		  "North American Admin",
		  "NTCA",
		  "NWL",
		  "Nyhart",
		  "Olin Direct",
		  "Nylcare",
		  "Pennacle",
		  "Patoka General",
		  "Pillsbury",
		  "Pioneer",
		  "Pittman",
		  "Planvista",
		  "PPO of Kentucky",
		  "Preferred Option",
		  "Provident Agency",
		  "Primary Physician Care",
		  "Seabury and Smith",
		  "Restat Insurance",
		  "Public Service Indiana",
		  "Stanfast",
		  "Tower Life",
		  "TPA",
		  "UARCO",
		  "Trans General",
		  "In childrens Special",
		  "MCMC",
		  "Medicaid Anthem HIP",
		  "Medicaid Care Select",
		  "Pam Transport",
		  "CalOptima",
		  "Health Marketing",
		  "KMG America",
		  "MedCorp Southwest",
		  "Lakewood Regional Medical Center",
		  "Managed Healthcare Northwest",
		  "International Brotherhood of Electrical Workers",
		  "Delaware Physicians Care",
		  "Keystone Mercy Health Plan",
		  "Health Care Alliance Pool",
		  "Caremark Pharmacy",
		  "Elder Health Pennsylvania",
		  "Georgia1st",
		  "Oregon Health Services (OHS)",
		  "State Comp Fund",
		  "Principal Life",
		  "Oregon Medicaid (DMAP)",
		  "Exclusive Healthcare",
		  "Care First (AHCCCS) w/prior authorization",
		  "Union Hospital",
		  "California Foundation for Medical Care",
		  "Dakota Care",
		  "Desert Oasis Healthcare",
		  "Desert Regional Medical Center",
		  "Edinger Medical Group",
		  "Georgia Baptist Health Care System",
		  "Georgia Correctional HealthCare",
		  "ETHIX SOUTHWEST",
		  "Harriman Jones Medical Group",
		  "HEALTH CONNECTICUT - PPO",
		  "Health Plan of San Joaquin",
		  "Health Utah",
		  "HEALTHCHOICE OF CT - PPO",
		  "Healthline",
		  "Mississippi Health Partners",
		  "Henry Ford Health System",
		  "Navistar",
		  "MESSA",
		  "Old Security Life",
		  "NORTHEAST HEALTH DIRECT - PPO",
		  "Preferred Care PPO",
		  "Short Term (GAP) Insurance",
		  "Sparrow Phys Health Network",
		  "Spartan Stores",
		  "Weyco",
		  "Tencon Health Plan",
		  "TEXAS MUNICIPAL LEAGUE",
		  "Texas Medical Association",
		  "IHP",
		  "Network Platinum Plus"
	};
}
