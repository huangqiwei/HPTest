package hptest;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import qapub.utils.FileUtil;

public class RateDiffWorkPair extends Thread{
	File[] _casefiles;
	String[] _colors;
	String[] _resultfilesnames;
	
	RateInfoCollector _collector1;
	RateInfoCollector _collector2;
	String _siteIdA;
	String _siteIdB;
	
	boolean _ignorePlansOrder = false;
	//boolean _checkSponsors = false;
	//boolean _checkRates = false;

	boolean _hasDiff = false;
	boolean _hasException = false;
	boolean _hasUHCplanInProd = false;
	boolean _hasHMNplanInProd = false;

	boolean _ifrun = true;
	String _runTag = "";
	File _runnerdir;
	//File _runningflag;
	
	int _currentfileNum;
	int _casesNum, _doneCasesNum;
	Properties _nonplansProp;
	String _rundatestr;
	String _pairID;
	StringBuffer emailbuf = new StringBuffer();
	ArrayList<File> attachfiles = new ArrayList<File>();
	boolean _isdone;
	
	public RateDiffWorkPair(File[] casefiles, RateDiffer differ, String pairId, 
												 	//String siteId1, String siteId2, 
												 	//boolean checkSponsors, boolean checkRates, boolean ignorePlansOrder, 
												 	File workdir, String rundatestr, Properties nonplansprop)
	{
		this._casefiles = casefiles;
		this._colors = new String[casefiles.length];
		this._resultfilesnames = new String[casefiles.length];
		this._siteIdA = differ._siteIdA;
		this._siteIdB = differ._siteIdB;
		this._pairID = pairId;
		//this._checkSponsors = differ._checkSponsors;
		//this._checkRates = differ._checkRates;
		this._ignorePlansOrder = differ._ignorePlansOrder;
		
		this._runnerdir = workdir;
		//this._runningflag = flagfile;
		_nonplansProp = nonplansprop;
		_rundatestr = rundatestr;
	}
	
	public void startCollectors(){
		String urlA = getSiteUrl(_siteIdA);
		String urlB = getSiteUrl(_siteIdB);
		
		_collector1 = new RateInfoCollector(_siteIdA, urlA, _pairID+"A");
		_collector2 = new RateInfoCollector(_siteIdB, urlB, _pairID+"B");
		_collector1.start();
		_collector2.start();
		_isdone=false;
	}
	
	static String getSiteUrl(String id){
		if("QA".equalsIgnoreCase(id)) return "http://pengujian.healthpocket.com";
		else if("PD".equalsIgnoreCase(id)) return "http://www.healthpocket.com";
		return "";
	}
	
	public void run()
	{
		_isdone=false;
		
		ArrayList<HealthTestCase> caseslist=null;
		String prodline;
		boolean issbg;
		String casefn;
		String nonplanstates;
		//boolean semiAnnual;
		HealthTestCase bnfcase, bnfcase2;
		boolean casefileIsDiff;
				
		try{ sleep(1000); }
		catch(InterruptedException iexp){}
				
		File txtFile, htmFile;
		PrintWriter txtfw, htmlfw;
 
		String diffStr;
		String ssfnA, ssfnB;
		
		for(_currentfileNum=0; _ifrun && _currentfileNum<_casefiles.length; _currentfileNum++)
		{
			casefn = _casefiles[_currentfileNum].getName();
			
			prodline = casefn.substring(0, casefn.indexOf('.'));
			issbg = prodline.startsWith("SBG");
			casefn = casefn.substring(0, casefn.lastIndexOf('.'));
			
			nonplanstates = this._nonplansProp.getProperty(prodline);
			
			casefileIsDiff = false;
			txtFile = new File(_runnerdir, casefn +'_'+ _siteIdA +'_'+ _siteIdB +"_Results_"+ _rundatestr +".txt");
			htmFile= new File(_runnerdir, casefn +".html");
			
			emailbuf.append("<tr><td>"+ casefn +"</td>");
			try{
				caseslist = HealthTestCase.buildHealthCases(_casefiles[_currentfileNum]);
				txtfw = new PrintWriter(new FileWriter(txtFile));
				htmlfw = new PrintWriter(new FileWriter(htmFile));
			}
			catch(Exception ex){
				_hasException = true;
				emailbuf.append("<td><PRE>");
				for(StackTraceElement ste : ex.getStackTrace()){
					emailbuf.append(ste.toString());
				}
				emailbuf.append("</PRE></td></tr>");
				break;
			}
			
			_casesNum = caseslist.size();
			_doneCasesNum = 0;
			
			txtfw.println(prodline +" Results:");
			htmlfw.println("<H3>"+ prodline +" Rate Diff Results</H3><TABLE border=1>");

			_hasUHCplanInProd = false;
			_hasHMNplanInProd = false;
			
			for(_doneCasesNum=0; _ifrun && _doneCasesNum<_casesNum; _doneCasesNum++)
			{
				bnfcase = caseslist.get(_doneCasesNum);
				bnfcase2= bnfcase.clone();
				
				txtfw.print(bnfcase._State +' '+ bnfcase._zipcode +':');
				htmlfw.print("<tr><th>"+ bnfcase._State +' '+ bnfcase._zipcode +"</th><td>");
				
				System.out.println(String.format("%1$s %2$s %3$s %4$s / %5$s", casefn, bnfcase._State, bnfcase._zipcode, _doneCasesNum+1, _casesNum));
				ssfnA = casefn +'_'+ bnfcase._State +'_'+ bnfcase._zipcode +"A.html";
				ssfnB = casefn +'_'+ bnfcase._State +'_'+ bnfcase._zipcode +"B.html";
				
				_collector1.doCase(bnfcase,  prodline);
				_collector2.doCase(bnfcase2, prodline);
				do{
					try{ sleep(300); }
					catch(InterruptedException iexp){}
				}
				while(_ifrun && (_collector1._status==1 || _collector2._status==1));
				
				if(!_ifrun) break;

				try{
					FileUtil.savePage(new File(_runnerdir, ssfnA), _collector1.getPage(), null);
					FileUtil.savePage(new File(_runnerdir, ssfnB), _collector2.getPage(), null);
					
					if(_collector1._exception!=null){
						_hasException = true;
						emailbuf.append("<td><PRE>");
						emailbuf.append(_collector1._exception.getMessage());
						for(StackTraceElement ste : _collector1._exception.getStackTrace()){
							emailbuf.append("\r\n "+ ste.toString());
						}
						emailbuf.append("</PRE></td></tr>");
						
						htmlfw.print("<PRE>");
						_collector1._exception.printStackTrace(htmlfw);
						htmlfw.println("</PRE>");
						
						txtfw.println();
						_collector1._exception.printStackTrace(txtfw);
						//try{FileUtil.savePage(new File(_dir, "Ex1.html"), _collector1.getPage(), "", false);}catch(Exception e){}
					}
					if(_collector2._exception!=null){
						_hasException = true;
						emailbuf.append("<td><PRE>");
						emailbuf.append(_collector2._exception.getMessage());
						for(StackTraceElement ste : _collector2._exception.getStackTrace()){
							emailbuf.append("\r\n "+ ste.toString());
						}
						emailbuf.append("</PRE></td></tr>");
						
						htmlfw.print("<PRE>");
						_collector2._exception.printStackTrace(htmlfw);
						htmlfw.println("</PRE>");
						
						txtfw.println();
						_collector2._exception.printStackTrace(txtfw);
						//try{FileUtil.savePage(new File(_dir, "Ex2.html"), _collector2.getPage(), "", false);}catch(Exception e){}
					}
					
					if(!_ifrun || _hasException){
						htmlfw.print("</td><td>");
						htmlfw.print("<A target=\"_blank\" href=\""+ ssfnA +"\">PageA</A> ");
						htmlfw.print("<A target=\"_blank\" href=\""+ ssfnB +"\">PageB</A>");
						htmlfw.print("</td></tr>");
						htmlfw.flush();
						break;
					}
					
					boolean expectedNonPlan = (nonplanstates!=null && nonplanstates.contains(bnfcase._casedata[0]));
					
					StringBuffer buf2 = new StringBuffer();
					if(_collector1._allPlansList.size()==0){
						if(!expectedNonPlan)
							buf2.append("\t Warning: No Any Plan In The Quote Page of "+ this._siteIdA +"\r\n");
					}else{
						if(expectedNonPlan)
							buf2.append("\t Warning: Unexpected Plan Found In "+ this._siteIdA +"\r\n");
					}
						
					if(_collector2._allPlansList.size()==0){
						if(!expectedNonPlan)
							buf2.append("\t Warning: No Any Plan In The Quote Page of "+ this._siteIdB +"\r\n");
					}else{
						if(expectedNonPlan)
							buf2.append("\t Warning: Unexpected Plan Found In "+ this._siteIdB +"\r\n");
					}
						
					if(this._ignorePlansOrder)
						buf2.append(comparePlansIgnoreOrder(_collector1._allPlansList, _collector2._allPlansList, issbg));
					else
						buf2.append(comparePlansByOrder(_collector1._allPlansList, _collector2._allPlansList, issbg));
						
					diffStr = buf2.toString();
					
					
					if(diffStr.length()==0){
						txtfw.println(" Rates Match.");
						htmlfw.print("Rates Match.</td><td bgcolor=\"lime\">Match ");
					}
					else{
						casefileIsDiff = true;
						_hasDiff = true;
						txtfw.println();
						txtfw.println("==========");
						txtfw.println(diffStr);
						
						htmlfw.print("<pre>");
						htmlfw.print(diffStr);
						htmlfw.print("</pre>");
						htmlfw.print("</td><td bgcolor=\"orangered\">");
					}
					
					htmlfw.print("<A target=\"_blank\" href=\""+ ssfnA +"\">PageA</A> ");
					htmlfw.print("<A target=\"_blank\" href=\""+ ssfnB +"\">PageB</A>");
					htmlfw.println("</td></tr>");

					txtfw.flush();
					htmlfw.flush();
				}
				catch(Exception ex){
					_hasException = true;
					emailbuf.append("<td><PRE>");
					emailbuf.append(ex.getMessage());
					for(StackTraceElement ste : ex.getStackTrace()){
						emailbuf.append("\r\n "+ ste.toString());
					}
					emailbuf.append("</PRE></td></tr>");
					
					htmlfw.print("<PRE>");
					ex.printStackTrace(htmlfw);
					htmlfw.println("</PRE>");
					
					txtfw.println();
					ex.printStackTrace(txtfw);
					
					break;
				}
			} // end the loop for cases
			htmlfw.println("</TABLE>");
			
			/**
			if(issbg && ("prod".equalsIgnoreCase(_siteIdA) || "prod".equalsIgnoreCase(_siteIdB)) )
			{
				if(!this._hasUHCplanInProd && !"SBGVS".equals(prodline)){
					diffStr1 = "There is not any UHC plan online in Prod";
					casefileIsDiff = true;
					_hasDiff = true;
					txtfw.println();
					txtfw.println("==========");
					txtfw.println(diffStr1);
					htmlfw.print("<p>");
					htmlfw.print(diffStr1);
					htmlfw.print("</p>");
				}
				if(!this._hasHMNplanInProd && !"SBGVS".equals(prodline)){
					diffStr1 = "There is not any Humana plan online in Prod";
					casefileIsDiff = true;
					_hasDiff = true;
					txtfw.println();
					txtfw.println("==========");
					txtfw.println(diffStr1);
					htmlfw.print("<p>");
					htmlfw.print(diffStr1);
					htmlfw.print("</p>");
				}
			}
			**/
			txtfw.flush();
			htmlfw.flush();
			txtfw.close();
			htmlfw.close();
			attachfiles.add(txtFile);
			
			_resultfilesnames[_currentfileNum] = _runnerdir.getName() +'/'+ htmFile.getName();

			if(!_ifrun || _hasException) break;
			
			if(!casefileIsDiff){
				emailbuf.append("<td style=\"color:green\">Match</td></tr>");
				_colors[_currentfileNum] = "LimeGreen";
			}
			else{
				emailbuf.append("<td style=\"color:red\">Mismatch</td></tr>");
				_colors[_currentfileNum] = "OrangeRed";
			}
			emailbuf.append("</td></tr>");
		}// end the loop for test files
		
		_collector1._ifrun = false;
		_collector2._ifrun = false;
		_collector1.doCase(null,"");
		_collector2.doCase(null,"");
		
		_isdone = true;
		System.out.println("==== END ====");
	}
	
	public String getWorkProgress()
	{
		StringBuffer buf = new StringBuffer();
		for(int i=0;i<_currentfileNum;i++){
			buf.append("<tr bgcolor=\""+ _colors[i] +"\"><td width=100>"+ _casefiles[i].getName() +"</td><td width=100><a href=\""+ _resultfilesnames[i] +"\" target=\"_blank\">Done</a></td><td width=100>100%</td></tr>");
		}
		
		if(_currentfileNum<_casefiles.length){
			if(_hasException)
				buf.append("<tr bgcolor=\"OrangeRed\"><td width=100>"+ _casefiles[_currentfileNum].getName() +"</td><td width=100><a href=\""+ _resultfilesnames[_currentfileNum] +"\" target=\"_blank\">Exception</a></td><td width=100>"+ _doneCasesNum +" / "+ _casesNum +"</td></tr>");
			else
				buf.append("<tr bgcolor=\"#7FFFD4\"><td width=100>"+ _casefiles[_currentfileNum].getName() +"</td><td width=100>Running</td><td width=100>"+ _doneCasesNum +" / "+ _casesNum +"</td></tr>");
		}
		
		if(!_hasException){
			for(int i=_currentfileNum+1;i<_casefiles.length;i++){
				buf.append("<tr bgcolor=\"#e0e0e0\"><td width=100>"+ _casefiles[i].getName() +"</td><td width=100>Queueing</td><td width=100>0%</td></tr>");
			}
		}
		
		return buf.toString();
	}
	
	
	String spondiffmt = "\t %1$s Sponsorship: %2$s\r\n\t %3$s Sponsorship: %4$s\r\n\t --------\r\n";
	String compareSponsors(List<String> splist1, List<String> splist2){
		StringBuffer buf = new StringBuffer();
		String sp1, sp2;
		int i=0;
		int size1 = splist1.size();
		int size2 = splist2.size();
		
		for(i=0; i<size1; i++)
		{
			sp1 = splist1.get(i);
			if(i>=size2){
				buf.append(String.format(spondiffmt, _siteIdA, sp1, _siteIdB, "None."));
			}
			else{
				sp2 = splist2.get(i);
				if(!sp1.equals(sp2)){
					buf.append(String.format(spondiffmt, _siteIdA, sp1, _siteIdB, sp2));
				}
			}
		}
		
		for(int j=i; j<size2; j++){
			sp2 = splist2.get(j);
			buf.append(String.format(spondiffmt, _siteIdA, "None.", _siteIdB, sp2));
		}
		
		return buf.toString();
	}
	
	
	String PlanFmt = "%1$s; %2$s; %3$s";// %4$s";

	String comparePlansByOrder(List<String> planList1, List<String> planList2, boolean sbg)
	{
		String info1, info2;
		StringBuffer buf = new StringBuffer();
		
		//if(sbg){
		//	treatSbgExternalPlans(planList1, true);
		//	treatSbgExternalPlans(planList2, false);
		//}
		
		int i=0;
		int size1 = planList1.size();
		int size2 = planList2.size();
		
		for(i=0;i<size1;i++)
		{
			info1 = planList1.get(i);
			
			if(i>=size2){
				buf.append("\t "+ _siteIdA +" Rate: ");
				buf.append(info1);
				buf.append("\r\n\t "+ _siteIdB +" Rate: None.\r\n\t --------\r\n");
			}
			else{
				info2 = planList2.get(i);
				if(!info1.equals(info2)) {
					buf.append("\t "+ _siteIdA +" Rate: ");
					buf.append(info1);
					buf.append("\r\n\t "+ _siteIdB +" Rate: ");
					buf.append(info2);
					buf.append("\r\n\t --------\r\n");
				}
			}
		}
		
		for(int j=i; j<size2; j++){
			info2 = planList2.get(j);
			buf.append("\t "+ _siteIdA +" Rate: None.\r\n\t "+ _siteIdB +" Rate: ");
			buf.append(info2);
			buf.append("\r\n\t --------\r\n");
		}
		
		return buf.toString();
	}
	
	String comparePlansIgnoreOrder(List<String> planList1, List<String> planList2, boolean sbg)
	{
		String info1, info2;
		String name1, name2, price1, price2;
		int x$;
		StringBuffer buf = new StringBuffer();
		
		//if(sbg){
		//	treatSbgExternalPlans(planList1, "prod".equalsIgnoreCase(this._siteIdA));
		//	treatSbgExternalPlans(planList2, "prod".equalsIgnoreCase(this._siteIdB));
		//}
		
		for(int i=0;i<planList1.size();i++)
		{
			info1 = planList1.get(i);
			
			x$ = info1.indexOf('|');
			if(x$>0){
				name1 = info1.substring(0, x$);
				price1 = info1.substring(x$+1);
			}else{
				name1 = info1;
				price1= "";
			}
			
			for(int j=0;j<planList2.size();j++)
			{
				info2 = planList2.get(j);
				x$ = info2.indexOf('|');
				if(x$>0){
					name2 = info2.substring(0, x$);
					price2= info2.substring(x$+1);
				}else{
					name2 = info2;
					price2= "";
				}
				
				if(!name1.equals(name2)) continue;
				
				if(!price1.equals(price2)){
					buf.append("\t "+ _siteIdA +" Rate: ");
					buf.append(info1);
					buf.append("\r\n\t "+ _siteIdB +" Rate: ");
					buf.append(info2);
					buf.append("\r\n\t --------\r\n");
				}
				
				planList2.remove(j);
				planList1.remove(i);
				i--;
				break;
			}
		}
		
		//System.out.println("---- Only In List1 -------");
		for(String info : planList1){
			buf.append("\t "+ _siteIdA +" Rate: ");
			buf.append(info);
			buf.append("\r\n\t "+ _siteIdB +" Rate: None.\r\n\t --------\r\n");
		}
		
		//System.out.println("---- Only In List2 -------");
		for(String info : planList2){
			buf.append("\t "+ _siteIdA +" Rate: None.\r\n\t "+ _siteIdB +" Rate: ");
			buf.append(info);
			buf.append("\r\n\t --------\r\n");
		}
		
		return buf.toString();
	}
	
	Pattern ptn_uhc = Pattern.compile("(Humana|UnitedHealthcare)\\(cid:\\d+\\);");
	void treatSbgExternalPlans(List<String> planList, boolean isprod)
	{
		String plan, car;
		ArrayList<String> pickedout = new ArrayList<String>();
		Matcher mat;
		
		for(int i=0;i<planList.size();i++)
		{
			plan = planList.get(i);
			if(plan.endsWith("Offline")) continue;
			
			mat = ptn_uhc.matcher(plan);
			if(mat.lookingAt()){
				car = mat.group();
				if(isprod){
					if(car.startsWith("UnitedHealthcare")){
						this._hasUHCplanInProd = true;
					}
					else { //the regular exp can make sure the else is started with Humana
						this._hasHMNplanInProd = true;
					}
				}
				car = car + " Offline";
				if(!pickedout.contains(car)) pickedout.add(car);
				planList.remove(i);
				i--;
			}
		}
		
		planList.addAll(pickedout);
	}
	/*
	String comparePlanInfo(String[] info1, String[] info2){
		if(!info1[0].equals(info2[0])) return "different cid";
		if(!info1[1].equals(info2[1])) return "different pid";
		if(!info1[2].equals(info2[2])) return "different rate";
		if(!info1[3].equals(info2[3])) return "different sr";
		return null;
	}
	*/
	
	//class ExternalPlansAvailability {
	//	b
	//}
}