package hptest;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

import com.meterware.httpunit.HttpUnitOptions;

import qapub.utils.FileUtil;

public class PerfTest {
	
	static final List<Long> ReqTimesList = Collections.synchronizedList(new ArrayList<Long>());
	
	static String HostDomain = "http://pengujian.healthpocket.com";
	static String Type = "MC";
	static boolean BirthDate = false;
	static boolean STOP = false;
	
	public static void main(String[] args) throws Exception 
	{
		HttpUnitOptions.setScriptingEnabled(false);
		
		int n = 0;
		int c = 0;
		
		String[] zipcodes = FileUtil.text(new File("zipcodes.txt")).split("[\\n\\r]+");
		
		for(int i=0; i<args.length; i+=2){
			if("-n".equals(args[i])) 
				n = Integer.valueOf(args[i+1]).intValue();
			else if("-c".equals(args[i])) 
				c = Integer.valueOf(args[i+1]).intValue();
			else if("-t".equals(args[i])) 
				Type = args[i+1];
			else if("-zipcode".equals(args[i]))
				zipcodes = new String[]{args[i+1]};
			else if("-bd".equals(args[i]))
				BirthDate = "Y".equalsIgnoreCase(args[i+1]) || "YES".equalsIgnoreCase(args[i+1]);  
		}
		
		if(n==0 || c==0) {
			System.out.println("wrong request / concurrency numbers");
			return;
		}
		
		System.out.println(zipcodes.length +" zipcodes.");
		
		PerfThread[] threads = new PerfThread[c];
		for(int i=0;i<c;i++){
			threads[i] = new PerfThread(zipcodes);
		}
		
		long startime = System.currentTimeMillis();
		
		for(int i=0;i<c;i++){
			threads[i].start();
		}
		
		int s = ReqTimesList.size();
		int m = 50;
		while(s<n){
			Thread.sleep(300);
			s = ReqTimesList.size();
			if(s>=m){
				System.out.println(m+ " requests completed.");
				m+=50;
			}
		}
		STOP = true;
		
		Thread.sleep(500);
		/////
		Collections.sort(ReqTimesList);
		s = ReqTimesList.size();
		
		System.out.println(s + " Completed Requests");
		
		int errcount = 0;
		for(int i=0;i<c;i++){
			errcount += threads[i].errornum;
		}
		System.out.println(errcount + " Failed Requests");
		
		long endtime = System.currentTimeMillis();
		
		BigDecimal bd_et = new BigDecimal(endtime-startime).divide(new BigDecimal(1000), 2, BigDecimal.ROUND_HALF_UP);
		
		System.out.println("Elapsed Time (sec):: "+ bd_et.doubleValue());
		
		BigDecimal bd_reqps = new BigDecimal(s).divide(bd_et, 2, BigDecimal.ROUND_HALF_UP);
		
		System.out.println("Requests / Second:: "+ bd_reqps.doubleValue());
		
		int top25 = (int)s/4;
		int low25 = s-top25;
		if(low25==s) low25 = s-1;
		
		int low10 = s- (int)s/10;
		if(low10==s) low10 = s-1;
		
		int low5 = s-(int)s/20;
		if(low5==s) low5 = s-1;
		
		int low1 = s-(int)s/100;
		if(low1==s) low1 = s-1;
		
		System.out.println("Average (sec):: "+ cal_average(0, s) );
		System.out.println("Fastest 25% (sec):: "+ cal_average(0, top25+1) );
		System.out.println("Median 50% (sec):: "+ cal_average(top25, low25+1) );
		System.out.println("Slowest 25% (sec):: "+ cal_average(low25, s) );
		System.out.println("Slowest 10% (sec):: "+ cal_average(low10, s) );
		System.out.println("Slowest 5% (sec):: "+ cal_average(low5, s) );
		System.out.println("Slowest 1% (sec):: "+ cal_average(low1, s) );
		System.out.println("Slowest (sec):: "+ cal_average(s-1, s) );
	}
	

	static double cal_average(int i1, int i2)
	{
		if(i2 <= i1) i2 = i1+1;
		
		long total = 0;
		for(int i=i1; i<i2; i++)
		{
			total += ReqTimesList.get(i);
		}
		
		int len = i2-i1;
		
		BigDecimal bd_tot = new BigDecimal(total);
		BigDecimal bd_len = new BigDecimal(len*1000);
		
		BigDecimal bd = bd_tot.divide(bd_len, 2, BigDecimal.ROUND_HALF_UP);
		
		return bd.doubleValue();
	}
}
