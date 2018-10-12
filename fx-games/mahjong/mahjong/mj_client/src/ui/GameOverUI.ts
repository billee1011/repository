class GameOverUI extends UIBase{
	public static NAME:string = 'GameOverUI';
	public constructor() {
		super('resource/UI_exml/GameOver.exml','settle');

		this.isAloneShow = true;
	}
	private btn_close:eui.Button;
	private list:HPagelist;
	private model:MjModel;
	protected uiLoadComplete():void
	{
		this.model = TFacade.getProxy(MjModel.NAME);

		this.list = new HPagelist(GameOverRender,236,404,false);
		this.addChild(this.list);
		this.list.updateXY(46,113);

		this.btn_close.addEventListener(egret.TouchEvent.TOUCH_TAP,this.hide,this);
	}

	public sleep():void
	{
		TFacade.toggleUI(MainGameUI.NAME,0);
		TFacade.toggleUI(MainFrameUI.NAME,1);
	}

	protected doExecute():void
	{
		var temp:any[] = [];
		for (var key in this.model.overData) {
			temp.push(this.model.overData[key]);
		}
		this.list.displayList(temp);
	}

	protected backGroundClick():void
	{

	}
}