class Tnotice {
	public static _instance:Tnotice;
	public static get instance():Tnotice
	{
		if(!this._instance){
			this._instance = new Tnotice();
		}
		return this._instance;
	}
	public first:QueenVO;
	public last:QueenVO;
	private timer:egret.Timer;

	public constructor() {
		this.timer = new egret.Timer(100,0);
		this.timer.addEventListener(egret.TimerEvent.TIMER,this.startCheck,this);
	}

	public popUpTip(vv:string):void
	{
		var vo:QueenVO = new QueenVO(vv);
		if(!this.last){
			this.first = this.last = vo;
		}else
		{
			this.last.next = vo;
			vo.pre = this.last;
			this.last = vo;
		}
		if(this.first)
		{
			this.timer.start();
		}
	}

	private startCheck(evt:egret.TimerEvent):void
	{
		var vo:QueenVO = this.first;
		ToastManager.instance.showTips(vo.tips);
		this.first = vo.next;
		if(vo.next){
			vo.next = null;
		}
		if(vo.pre){
			vo.pre = null;
		}
		if(!this.first){
			this.first = this.last = null;
			this.timer.stop();
		}
	}

	public showMsg(msg:string):void
	{
		TFacade.toggleUI(CommonSureUI.NAME,1).execute('msg',msg);
	}
	public showSureMsg(msg:string,backFun:Function,thisObj:any):void
	{
		TFacade.toggleUI(CommonSureUI.NAME,1).execute('buy',[msg,backFun,thisObj]);
	}
	public showSureNoCancelMsg(msg:string,backFun:Function,thisObj:any):void
	{
		TFacade.toggleUI(CommonSureUI.NAME,1).execute('noCancel',[msg,backFun,thisObj]);
	}

	public playGonggao(content:string,loop:boolean=false,mask:egret.Shape=null):void
	{
		this.hideGonggao();
		LayerManage.instance.playGonggao(content,loop,mask);
	}
	public hideGonggao():void
	{
		LayerManage.instance.hideGonggao();
	}
}