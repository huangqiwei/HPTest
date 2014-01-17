package hptest;

import java.io.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import qapub.utils.*;

public class HealthTestCase {
	String[] _casedata;
	String _zipcode, _county, _carrierid, _planid;
	String _effdatestr = "";
	boolean _executed = false;
	boolean _found = false;
	
	String _effenddate=null;
	boolean _is_st_single=false;
	String _State="";
	
	public HealthTestCase(String[] args){
		_casedata = args;
		_zipcode = _casedata[1].trim();
		_State = _casedata[0].trim();
			
		//if(_casedata[9].length()==2) _stuSchoolState = _casedata[9];
		
		int dashidx = _casedata[51].indexOf('-');
		if(dashidx>0){
			_carrierid = _casedata[51].substring(0,dashidx);
			_planid = _casedata[51].substring(dashidx+1);
		}
		
		this._effdatestr = makeDateString(_casedata[52], _casedata[53], _casedata[54]);
		
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		Date d1 = null;
		try{
			d1 = sdf.parse(this._effdatestr);
		}catch(Exception e){
			e.printStackTrace();
		}
		Calendar cld = Calendar.getInstance();
		if(d1==null || d1.getTime() < cld.getTimeInMillis()){
			cld.add(Calendar.DATE, 30);
			this._effdatestr = sdf.format(cld.getTime());
		}
		/**
		if(_casedata.length>58 && StringUtil.parseInt(_casedata[55])!=null && StringUtil.parseInt(_casedata[56])!=null && StringUtil.parseInt(_casedata[57])!=null){
			this._is_st_single = true;
			this._effenddate = makeDateString(_casedata[55], _casedata[56], _casedata[57]);
			
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy");
			
			try{
				Date d1 = sdf.parse(this._effdatestr);
				Date d2 = sdf.parse(this._effenddate);
				Calendar cld = Calendar.getInstance();
				if(d1.getTime() < cld.getTimeInMillis()){
					long betweenms = d2.getTime() - d1.getTime();
					long betweendays = betweenms/(1000*3600*24);

					this._effdatestr = sdf.format(cld.getTime());
					cld.add(Calendar.DATE, (int)betweendays);
					this._effenddate = sdf.format(cld.getTime());
				}
				//log("<p>set single payment: "+ betweendays +" days</p>");
			}catch(Exception e){
				
			}
		}
		**/
	}
	
	public boolean censusEquals(HealthTestCase case2){
		//return (_zipcode.equals(case2._zipcode) && _county.equals(case2._county));
		if(case2._casedata.length<51) return false;
		if(!_zipcode.equals(case2._zipcode)) return false;
		if(!_county.equals(case2._county)) return false;
		if(!_effdatestr.equals(case2._effdatestr)) return false;
		
		if(_is_st_single != case2._is_st_single) return false;
		if(_is_st_single && _effenddate!=null && !_effenddate.equals(case2._effenddate)) return false;
		
		for(int i=2;i<51;i++){
			if(!_casedata[i].equalsIgnoreCase(case2._casedata[i])) return false;
		}
		
		return true;
	}
	
	
	public String toString(){
		StringBuffer buf = new StringBuffer();
		for(String str : _casedata){
			if(str==null) continue;
			str = str.trim();
			if(str.length()==0) continue;
			buf.append(str);
			buf.append(", ");
		}
		return buf.toString();
	}
	
	public HealthTestCase clone(){
		return new HealthTestCase(this._casedata);
	}
	
	public static String makeDateString(String m, String d, String y){
		StringBuffer sb = new StringBuffer();
		sb.append(y);
		sb.append('-');
		
		if(m.length()==1) sb.append('0');
		sb.append(m);
		sb.append('-');
		
		if(d.length()==1) sb.append('0');
		sb.append(d);
		
		return sb.toString();
	}
	
	public static ArrayList<HealthTestCase> buildHealthCases(File csvfile) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(csvfile));
		String line;
		String[] ar;
		ArrayList<HealthTestCase> caselist = new ArrayList<HealthTestCase>();
		while((line=reader.readLine())!=null){
			ar = line.split(",");
			if(ar.length>2){
				caselist.add(new HealthTestCase(ar));
			}
		}
		reader.close();
		return caselist;
	}
	
	String makeIFPQuoteUri()
	{
		int x=0;
		
		boolean smoke, student;
		
		StringBuffer buf = new StringBuffer();
		buf.append("zip=").append(this._zipcode)
			 .append("&state=").append(this._State)
			 .append("&r=").append(this._effdatestr);
		buf.append('&');
		
		String head;
		for(int j=2;j<51;j+=7)
		{
			x++;
			if(!"Male".equalsIgnoreCase(_casedata[j]) && !"Female".equalsIgnoreCase(_casedata[j])) continue;
			
			if(x==1) head = "P";
			else if(x==2) head = "S";
			else head = "C"+ (x-2);
			
			buf.append(head).append('=');
			buf.append(makeDateString(_casedata[j+1], _casedata[j+2], _casedata[j+3]));
			buf.append(_casedata[j].toUpperCase().charAt(0));
			
			smoke   = _casedata[j+5].equalsIgnoreCase("SMOKER");
			//student = _casedata[j+6].equalsIgnoreCase("STUDENT");
			buf.append(smoke ? 'T':'F');

		}
		
		
		return buf.toString();
	}
	
	JSONArray buildMembersJsonArray() throws Exception
	{
		int x=0;
		
		boolean smoke, student;
		
		JSONArray members = new JSONArray();
		
		String head;
		for(int j=2;j<51;j+=7)
		{
			x++;
			if(!"Male".equalsIgnoreCase(_casedata[j]) && !"Female".equalsIgnoreCase(_casedata[j])) continue;
			
			if(x==1) head = "P";
			else if(x==2) head = "S";
			else head = "C"; //+ (x-2);
			
			smoke   = _casedata[j+5].equalsIgnoreCase("SMOKER");

			JSONObject jo = new JSONObject();
			jo.put("gender", _casedata[j].toUpperCase().substring(0, 1));
			jo.put("dob", makeDateString(_casedata[j+1], _casedata[j+2], _casedata[j+3]));
			jo.put("role", head);
			jo.put("smoker", smoke);
			
			members.put(jo);
		}
				
		return members;
	}
	
	public String get_carrierid() {
		return _carrierid;
	}

	public String get_county() {
		return _county;
	}

	public String get_effdatestr() {
		return _effdatestr;
	}

	public String get_planid() {
		return _planid;
	}

	public String get_zipcode() {
		return _zipcode;
	}
}


