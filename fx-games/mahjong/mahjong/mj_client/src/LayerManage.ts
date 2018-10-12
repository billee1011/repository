class LayerManage {
	public constructor() {
		
	}
	public static _instance:LayerManage;

	public static get instance():LayerManage
	{
		if(!this._instance){
			this._instance = new LayerManage();
		}
		return this._instance;
	}
	/**
	 * 2d世界 包括 地图 和 单位
	 */
	public world2D:egret.Sprite;
	/**
	 * 场景特效层
	 */
	public effectLayer:egret.Sprite;
	/**
	 * 面板层
	 */
	public panelLayer:egret.Sprite;
	/**
	 * 面板一层
	 */
	public panelChildLayer1:egret.Sprite;
	/**
	 * 面板二层
	 *主界面固定最上层面板
	 */
	public panelChildLayer2:egret.Sprite;
	/**
	 * 提示层
	 */
	public tipLayer:egret.Sprite;

	public initLayer(stage:eui.UILayer):void
	{

		this.world2D = new egret.Sprite();
		stage.addChild(this.world2D);

		
		this.effectLayer = new egret.Sprite();
		this.world2D.addChild(this.effectLayer);

		this.panelLayer = new egret.Sprite();
		stage.addChild(this.panelLayer);
		this.panelChildLayer1 = new egret.Sprite();
		this.panelLayer.addChild(this.panelChildLayer1);
		this.panelChildLayer2 = new egret.Sprite();
		this.panelLayer.addChild(this.panelChildLayer2);
		
		this.tipLayer = new egret.Sprite();
		stage.addChild(this.tipLayer);

	}

	public notice:Notice;
	public playGonggao(content:string,loop:boolean=false,mask:egret.Shape=null):void
	{
		//if(!this.notice){
		this.notice = new Notice();
		//}
		this.tipLayer.addChild(this.notice);
		if(mask){
			var gp:egret.Point = mask.localToGlobal(0,0);
			this.notice.x = gp.x;
			this.notice.y = gp.y+5;
			this.notice.addChild(mask);
			mask.x = 0;
			mask.y = 0;
			this.notice.mask = mask;
		}else{
			this.notice.x = 0;
			this.notice.y = 3;
		}
		this.notice.play(content,loop,mask);
	}
	public hideGonggao():void
	{
		DisplayUtil.removeDisplay(this.notice);
	}

	private panelBgMask:egret.Sprite;
	public showPanelMask(index:number):void
	{
		if(!this.panelBgMask)
		{
			this.panelBgMask = new egret.Sprite();
			this.panelBgMask.touchEnabled = true;
			this.panelBgMask.addEventListener(egret.TouchEvent.TOUCH_TAP,this.maskClickHandler,this);
		}
		this.panelBgMask.graphics.clear();
		this.panelBgMask.graphics.beginFill(0,0.7);
		this.panelBgMask.graphics.drawRect(0,0,GlobalDefine.stageW,GlobalDefine.stageH);
		this.panelBgMask.graphics.endFill();
		if(!this.panelBgMask.stage){
			this.panelChildLayer2.addChildAt(this.panelBgMask,index);
		}else{
			this.panelChildLayer2.addChildAt(this.panelBgMask,index-1);
		}
	}
	public hidePanelMask():void
	{
		if(this.panelBgMask)
		{
			this.panelBgMask.graphics.clear();
			DisplayUtil.removeDisplay(this.panelBgMask);	
		}
	}
	private maskClickHandler(evt:any):void
	{
		TFacade.facade.simpleDispatcher(ConstDefine.EMPTY_MASK_CLICK);
	}
	private _updown:UpdownUtil;
	public getUpDown():UpdownUtil
	{
		if(!this._updown){
			this._updown = new UpdownUtil('outmjtile_sign');
		}
		return this._updown;
	}
}

