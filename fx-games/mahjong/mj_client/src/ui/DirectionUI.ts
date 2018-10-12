class DirectionUI extends UIBase{
	public constructor() {
		super('resource/UI_exml/Direction.exml');
	}
	/***
	 * 1 东 
	 * 2 南
	 * 3 西
	 * 4 北 
	 */
	private model:MjModel;
	private img_1:eui.Image;
	private img_2:eui.Image;
	private img_3:eui.Image;
	private img_4:eui.Image; //default dir
	private img_bg:eui.Image;
	private container:egret.Sprite;
	private rotationSp:egret.Sprite;
	protected uiLoadComplete():void
	{
		this.container = new egret.Sprite();
		this.container.addChild(this.img_bg);
		for(let i:number=1; i<=4; i++){
			this['img_'+i].visible = false;
			this.container.addChild(this['img_'+i]);
		}
		this.rotationSp = new egret.Sprite();
		var w:number = this.img_bg.width/2;
		var h:number = this.img_bg.height/2;
		this.rotationSp.addChild(this.container);
		this.container.x = -w;
		this.container.y = -h;
		this.addChild(this.rotationSp);
		this.rotationSp.x = w;
		this.rotationSp.y = h;
		//this.rotationSp.graphics.beginFill(0xff0000,1);
		//this.rotationSp.graphics.drawCircle(0,0,70);

		//this.addEventListener(egret.TouchEvent.TOUCH_TAP,this.testHandler,this);

	}
	/*private testHandler(evt:any):void{
		this.setDirection(Math.ceil(Math.random()*4));
		this.playCardDirection(Math.ceil(Math.random()*4));
	}*/
	protected bindEvent():void
	{
		this.model = TFacade.getProxy(MjModel.NAME);
	}
	/***
	 * 1 东 
	 * 2 南
	 * 3 西
	 * 4 北   之前做的是有东南西北字样的，所以用到了旋转
	 */
	public setDirection(meDir:number):void
	{
		switch(meDir)
		{
			case 1:
				this.rotationSp.rotation = 90;
				break;
			case 2:
				this.rotationSp.rotation = 180;
				break;
			case 3:
				this.rotationSp.rotation = -90;
				break;
			case 4:
				this.rotationSp.rotation = 0;
				break;
		}
	}
	/**
	 * 出牌的玩家方向显示高亮
	 */
	public playCardDirection(dir:number):void
	{
		let img:eui.Image;
		for(let i:number=1; i<=4; i++){
			img = this['img_'+i];
			img.visible = dir == i;
		}
	}
}