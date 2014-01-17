package hptest;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

import qapub.utils.EMailUtil;
import qapub.utils.FileUtil;

public class RateDiffer extends Thread
{
	int _pairscount;
	File[] _allcasesfiles;
	RateDiffWorkPair[] _ratediffpairs;
	boolean _ignorePlansOrder = true;
	boolean _checkSponsors = false;
	boolean _checkRates = true;
	String _siteIdA;
	String _siteIdB;

	boolean _hasDiff = false;
	boolean _hasException = false;
	
	boolean _ifrun = true;
	String _runTag = "";
	String _emailTo = null;
	File _rootdir;
	File _runningflag;
	Calendar _startdatetime;
	
	public RateDiffer(int pc, File[] casefiles, String siteId1, String siteId2, 
										boolean checkSponsors, boolean checkRates, boolean ignorePlansOrder,
										String emailto, File rootdir, File flagfile) throws Exception
	{
		this._pairscount = pc;
		this._allcasesfiles = casefiles;
		if(_pairscount<1) _pairscount = 1;
		if(_pairscount>_allcasesfiles.length) _pairscount = _allcasesfiles.length;
		
		this._siteIdA = siteId1;
		this._siteIdB = siteId2;
		
		this._checkSponsors = checkSponsors;
		this._checkRates = checkRates;
		this._ignorePlansOrder = ignorePlansOrder;
		
		this._emailTo = emailto;
		this._rootdir = rootdir;
		this._runningflag = flagfile;
	}
	
	public static void main(String[] args) throws Exception 
	{
		File basedir = new File("work");
		File casesdir = new File(basedir, "cases");
		File[] casefiles = casesdir.listFiles();
		File flagfile = new File(basedir, "RUNNING.flag");
		RateDiffer rdfer = new RateDiffer(1, casefiles, "QA", "PD", false, true, true, "", basedir, flagfile);
		rdfer.start();
	}
	
	void initPairs(File runnerdir, String rundatestr) {
		_ratediffpairs = new RateDiffWorkPair[_pairscount];
		if(_pairscount==0) return;
		int filescount = _allcasesfiles.length;
		ArrayList<File>[] subfileslists = new ArrayList[_pairscount];
		int c=0;
		for(int i=0;i<filescount;i++){
			if(subfileslists[c]==null) subfileslists[c] = new ArrayList<File>();
			subfileslists[c].add(_allcasesfiles[i]);
			c++;
			if(c==_pairscount) c=0;
		}
		
		Properties nonplansProp = new Properties();
		//File propfile = new File(_rootdir, "nonplanstates.prop");
		//try{
		//	FileInputStream fis = new FileInputStream(propfile);
		//	nonplansProp.load(fis);
		//	fis.close();
		//}catch(Exception e){
		//	e.printStackTrace();
		//}
		
		File[] arsubfiles;
		for(int i=0;i<_pairscount;i++){
			arsubfiles = subfileslists[i].toArray(new File[subfileslists[i].size()]);
			_ratediffpairs[i] = 
				new RateDiffWorkPair(arsubfiles, this, "P"+i, 
														 runnerdir, rundatestr, nonplansProp);
		}
	}
	
	public void run()
	{
		_startdatetime = Calendar.getInstance();
		
		String datestr = String.format("%1$tF", _startdatetime);
		String runnername = _siteIdA + '_' + _siteIdB +'_'+ String.format("%1$tm_%1$td_%1$tH_%1$tM", _startdatetime);
		File runnerdir = new File(_rootdir, runnername);
		
		initPairs(runnerdir, datestr);
		
		runnerdir.mkdir();
		
		for(int i=0;i<_ratediffpairs.length;i++){
			_ratediffpairs[i].startCollectors();
			_ratediffpairs[i].start();
		}
		
		StringBuffer emailbuf = new StringBuffer(10240);
		StringBuffer titlebuf = new StringBuffer();
				
		boolean alldone = false;
		
		while(!alldone){
			try{sleep(1000);}catch(InterruptedException iexp){}
			alldone = true;
			for(int i=0;alldone && i<_ratediffpairs.length;i++){
				if(!_ratediffpairs[i]._isdone) alldone = false;
				if(_ratediffpairs[i]._hasDiff) _hasDiff = true;
				if(_ratediffpairs[i]._hasException) _hasException = true;
			}
		}
		
		if(!_ifrun){
			_runningflag.delete();
			return;
		}
		
		if(_checkSponsors) titlebuf.append("Sponsorship ");
		if(_checkRates) titlebuf.append("Rate ");
		titlebuf.append("Diff Test Results [" + _siteIdA + " vs. " + _siteIdB +"] - "+ datestr);
		
		emailbuf.append("<P style=\"font-size:14px;ont-family:Arial;\"><b>");
		emailbuf.append(titlebuf.toString());
		emailbuf.append("</b></p>");
		
		emailbuf.append("<P style=\"font-size:11px;font-weight:bold;font-family:Arial;\">Check Sponsorships = ");
		emailbuf.append(this._checkSponsors ? "Yes":"No");
		emailbuf.append(", Check Rates = ");
		emailbuf.append(this._checkRates ? "Yes":"No");
		emailbuf.append(", Ignore Plans Order = ");
		emailbuf.append(this._ignorePlansOrder ? "Yes":"No");
		emailbuf.append("</P><table border=1 cellspacing=0 cellpadding=3>");
		
		for(int i=0;i<_ratediffpairs.length;i++){
			emailbuf.append(_ratediffpairs[i].emailbuf);
		}
		
		emailbuf.append("</table>");
		
		if(!_ifrun){
			_runningflag.delete();
			return;
		}
		
		try{
			FileUtil.textW(new File(_rootdir, "lastresult.html"), this.getProgressHtmlTable());
		}catch(Exception e){
			e.printStackTrace();
		}
		
		ArrayList<File> allattachfiles = new ArrayList<File>();
		for(int i=0;i<_ratediffpairs.length;i++){
			allattachfiles.addAll(_ratediffpairs[i].attachfiles);
		}
		
		if(!_ifrun){
			_runningflag.delete();
			return;
		}

		if(this._emailTo!=null){
			if(_hasException){
				titlebuf.append(" -- Exception");
				_emailTo = "wayne.huang@ehealth-china.com; flame.guo@ehealth-china.com; willis.bao@ehealth-china.com; alex.xiu@ehealth-china.com; cliff.zhu@ehealth-china.com;";
			}
			else if(_hasDiff) titlebuf.append(" -- Mismatch");
			else titlebuf.append(" -- Match");
			
			//try{
			//	EMailUtil.sendEmailByEH(
			//			"\"Wayne Huang\" <wayne.huang@ehealth-china.com>", _emailTo, 
			//			titlebuf.toString(), emailbuf.toString(), allattachfiles);
			//}
			//catch(Exception e){
			//	e.printStackTrace();
			//}
		}
		
		if(!_ifrun){
			_runningflag.delete();
			return;
		}

//		if(!_hasException) {
//			for(File f : allattachfiles){
//				try{
//					FileUtil.copyFile_Cha(f, new File("/home/release/post_release", f.getName()));
//					//f.delete();
//				}catch(Exception e){
//					e.printStackTrace();
//				}
//			}
//		}
		
		_runningflag.delete();
	}
	
	
	public String getProgressHtmlTable()
	{
		StringBuffer buf = new StringBuffer();
		String yn1 = _checkSponsors ? "Yes":"No";
		String yn2 = _checkRates ? "Yes":"No";
		String yn3 = _ignorePlansOrder ? "Yes":"No";
		buf.append("<P style=\"font-size:16px;font-weight:bold;font-family:Arial;\"><b>Rate/Sponsorship Diff Test ");
		buf.append(_siteIdA);
		buf.append(" vs. ");
		buf.append(_siteIdB);
		buf.append("</b></P>\n");
		buf.append("<P style=\"font-size:11px;font-weight:bold;font-family:Arial;\">Check Sponsorships = ");
		buf.append(yn1);
		buf.append(", Check Rates = ");
		buf.append(yn2);
		buf.append(", Ignore Plans Order = ");
		buf.append(yn3);
		buf.append("</P>\n");
		buf.append("<P style=\"font-size:11px;font-weight:bold;font-family:Arial;\">Start DateTime = ");
		buf.append(String.format("%1$tF %1$tT", _startdatetime));
		buf.append("</P>\n");
		buf.append("<table border=0 cellpadding=4 cellspacing=1 bgcolor=black style=\"font-family:verdana; font-size:11px; font-weight:bold;\">");
		
		for(int i=0;i<_ratediffpairs.length;i++){
			buf.append(_ratediffpairs[i].getWorkProgress());
		}
		
		buf.append("</table>");
		
		return buf.toString();
	}
	
	public void cancel(){
		this._ifrun = false;
		for(int i=0;i<_ratediffpairs.length;i++)
			_ratediffpairs[i]._ifrun = false;
		
		do{
			try{sleep(1000);}catch(InterruptedException ex){}
		}
		while(_runningflag.exists());
	}
	
	public boolean is_checkRates() {
		return _checkRates;
	}

	public boolean is_checkSponsors() {
		return _checkSponsors;
	}

	public boolean is_ignorePlansOrder() {
		return _ignorePlansOrder;
	}
}