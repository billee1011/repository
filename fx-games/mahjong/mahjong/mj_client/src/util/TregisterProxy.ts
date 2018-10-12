class TregisterProxy {
	private facade:Facade;
	public constructor() {
		this.facade = TFacade.facade;
		this.reg(MjModel.NAME,MjModel);
	}

	private reg(name:string,proxy:any):void
	{
		this.facade.registerProxy(name,proxy);
	}
}