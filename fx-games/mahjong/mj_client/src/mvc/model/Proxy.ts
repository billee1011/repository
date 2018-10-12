class Proxy extends egret.EventDispatcher {
	protected _socket:TSocket;
	protected facade:Facade;
	public constructor() {
		super();
		this.facade = TFacade.facade;
	}
	public get socket():TSocket
	{
		return GlobalDefine.socket;
	}
	public simpleDispatcher(type:string,data:any=null):void
	{
		this.facade.simpleDispatcher(type,data);
	}

}