class SettlementUI extends UIBase{
	public static NAME:string = 'SettlementUI';
	public constructor() {
		super('resource/UI_exml/Settlement.exml','settle');

		this.centerFlag = true;
		this.isAloneShow = true;
	}
	private list:HPagelist;
	private model:MjModel;
	private bg:AsyImage;
	private img_titlewin:eui.Image;
	private img_titlehuang:eui.Image;
	private img_titlescore:eui.Image;
	private state:State;
	private btn_close:eui.Button;
	private btn_start:eui.Button;
	protected bindEvent():void
	{
		this.model = TFacade.getProxy(MjModel.NAME);
	}
	protected uiLoadComplete():void
	{
		this.bg = new AsyImage();
		this.addChildAt(this.bg,0);
		this.bg.setUrl(PathDefine.UI_IMAGE+'jiesuan_bg.png');

		this.list = new HPagelist(SettleRender,200,120,true,1);
		this.addChild(this.list);
		this.list.updateXY(25,100);

		this.state = new State();
		this.state.add('win',[this.img_titlewin]);
		this.state.add('ping',[this.img_titlehuang]);
		this.state.add('lose',[this.img_titlescore]);
		this.state.state = 'win';
		this.addChild(this.state);

		this.btn_close.addEventListener(egret.TouchEvent.TOUCH_TAP,this.hide,this);
		this.btn_start.addEventListener(egret.TouchEvent.TOUCH_TAP,this.startgame,this);
	}
	private startgame(evt:any):void
	{
		this.hide();
	}

	public sleep():void{
		SendOperate.instance.requestStartAgainGame();
	}

	/*private loaderOver():void
	{
		this.width = this.bg.width;
		this.height = this.bg.height;
		this.layoutP();
	}*/

	protected doExecute():void
	{
		var temp:any[] = [];
		for (var key in this.model.settleData) {
			temp.push(this.model.settleData[key]);
		}
		this.list.displayList(temp);

		this.state.state = this.params;

	}

}