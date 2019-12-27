package cc.lym.util;

public class Util {
	public static long fold(long val, long min, long max)
	{
		val=(val-min)%(max-min);
		if(val<0)val+=(max-min);
		return val+min;
	}
	
	public static double fold(double val, long min, long max)
	{
		val=(val-(double)min)%(max-min);
		if(val<0)val+=(max-min);
		return val+min;
	}
}
