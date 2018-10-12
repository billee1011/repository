package com.cai.expression;

public class CEService
{
	private static ICalculateExpression instance = null;

	public synchronized static void regiesterICEservice(ICalculateExpression ceServiceInstance)
	{
		instance = ceServiceInstance;
	}
	
	public final static boolean  doSystemWork1(){
		return instance.doSystemWork1();
	}
	
	public final static boolean  doSystemWork2(){
		return instance.doSystemWork2();
	}
	


	
	
}