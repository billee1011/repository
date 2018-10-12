package Test;

import java.util.Date;

public class Test {
	
	
	public static void getInt32(int num) {
		
		char[] chs = new char[Integer.SIZE];
		for (int i = 0; i < Integer.SIZE; i++)
		{
		chs[Integer.SIZE - 1 - i] = (char) (((num >> i) & 1) + '0');
		
		System.out.println(i+"："+chs[Integer.SIZE - 1 - i]);
		}
		System.out.println(chs.length);
		System.out.println(new String(chs)) ;
	}
	
	
	public static void main(String[] args) {
//		System.out.println(32/10);
		Date date = new Date(1521066600000L);
		System.out.println(date);
	}
}
