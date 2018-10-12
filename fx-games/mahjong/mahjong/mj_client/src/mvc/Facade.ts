class Facade {
	private classObj:Object;
	private instanceObj:Object;
	public currentShowUI:UIBase;
	private proxyObj:Object;
	private protocolObj:Object;
	private listenerObj:Object;
	//private groupObj:Object;
	public constructor() {
		this.classObj = {};
		this.instanceObj = {};
		this.proxyObj = {};
		this.protocolObj = {};
		this.listenerObj = {};
		//this.groupObj = {};
	}

	//public getGroup

	/**
     * 注册UI类名
     */
	public registerUI(name:string,cls:any):void
	{
		this.classObj[name] = cls;
	}
	/**
     * 取UI类名
     */
	public getUIClass(name:string):any
	{
		var t:any = this.classObj[name];
		if(t == null)
		{
			throw new Error("请先注册UI");
		}
		return this.classObj[name];
	}
	/**
     * 注册UI实例
     */
	public registerInstance(name:string,cls:eui.Component):void
	{
		this.instanceObj[name] = cls;
	}
	/**
     * 取UI实例
     */
	public getInstance(name:string):UIBase
	{
		return this.instanceObj[name];
	}
	/**
     * 注册prxoy
     */
	public registerProxy(name:string,cls:any):void
	{
		var proxy:Proxy = this.proxyObj[name];
		if(!proxy)
		{
			proxy = new cls as Proxy;
		}else{
			throw new Error("重复注册Proxy");
		}
		this.proxyObj[name] = proxy;
	}
	/**
     * 取proxy
     */
	public getProxy(name:string):Proxy
	{
		return this.proxyObj[name];
	}
	/**
     * 注册协议函数 data:[function,this]
     */
	public registerProtocol(id:number,data:Array<any>):void
	{
		var ff:Array<any> = this.protocolObj[id];
		if(!ff){
			this.protocolObj[id] = data;
		}else{
			throw new Error("重复注册Protocol");
		}
	}
	/**
     * 获得协议函数
     */
	public getProtocolFun(id:number):Array<any>
	{
		return this.protocolObj[id];
	}
	/**
     * 添加事件监听
     */
	public addListener(type:string,listenArr:Array<any>):void
	{
		var arr:Array<any> = this.listenerObj[type];
		if(!arr){
			this.listenerObj[type] = arr = [];
			arr.push(listenArr);
		}else if(arr.indexOf(listenArr) == -1){
			arr.push(listenArr);
		}
	}
	/**
     * 移除监听
     */
	public removeListener(type:string,listerArr:Array<any>):void
	{
		var arr:Array<any> = this.listenerObj[type];
		if(!arr || arr.length == 0){
			return;
		}
		var index:number = arr.indexOf(listerArr);
		if(index != -1){
			arr.splice(index,1)
		}
	}
	/**
     * 简单抛事件
     */
	public simpleDispatcher(type:string,data:any=null)
	{
		var funList:Array<any> = this.listenerObj[type];
		if(funList)
		{
			for(var i in funList){
				var arr:Array<any> = funList[i];
				arr[0].call(arr[1],data);
			}
		}
	}
}