class CachePool {
	public constructor() {
	}
	private static instance:CachePool;
		
	/** 
	 *  作为对象池的词典dict 
	 */
	private objPoolDict:Object=new Object();
	
	
	public static getInstance():CachePool
	{
		
		if (this.instance == null) 
		{
			
			this.instance=new CachePool;
			
		}
		
		return this.instance;
		
	}
	
	/** 
	 * 向对象池中放入对象，以便重复利用 
	 * @param disObj 要的放入对象 
	 
		*/
	public reBack(oldObj:Object):void
	{
		
		var objName:string=egret.getQualifiedClassName(oldObj);
		
		if (oldObj == null) 
		{
			
			return;
			
		}
		
		if (this.objPoolDict[objName] == null) 
		{
			
			this.objPoolDict[objName]=[];
			
		}
		if(this.objPoolDict[objName].lenth > 50){
			return;
		}
		
		this.objPoolDict[objName].push(oldObj);
		
	}
	
	/** 
	 * 从对象池中取出需要的对象 
	 * @param targetObj 需要的对象类类名，没必要必须是类实例名 类名就可以 
	 * @return 取出的相应对象 
	 * 
	 */
	public getObject(targetObj:any):Object 
	{
		
		var objName:string = egret.getQualifiedClassName(targetObj);
		
		if (this.objPoolDict[objName] != null && this.objPoolDict[objName].length > 0)
		{
			
			return this.objPoolDict[objName].pop()  as  Object;
			
		}
 		//var objClass:any = egret.getDefinitionByName(objName);
		
		var obj:Object = new targetObj();
		
		return obj;
		
	}
}