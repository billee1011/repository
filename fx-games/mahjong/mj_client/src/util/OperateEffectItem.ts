class OperateEffectItem extends egret.Sprite{
	private econ:egret.Sprite;
	private img_eff:eui.Image;
	public constructor() {
		super();

		this.econ = new egret.Sprite;
		this.addChild(this.econ);

		this.img_eff = new eui.Image;
		this.econ.addChild(this.img_eff);
	}
	private tw:egret.Tween;
	private comOk(evt:egret.Event):void
	{

	}
	public playEffect(type:number):void
	{
		var model:MjModel = TFacade.getProxy(MjModel.NAME);
		this.img_eff.addEventListener(egret.Event.COMPLETE,this.comOk,this)
		this.img_eff.source = model.getOperateTexture(type);
		this.img_eff.x = -83*.5;
		this.img_eff.y = -79*.5;

		this.econ.alpha = 0;
		this.econ.scaleX = 2;
		this.econ.scaleY = 2;
		this.tw = egret.Tween.get( this.econ );
		this.tw.to({alpha:1,scaleX:1,scaleY:1},600)
				.wait(500)
				.to({alpha:0,scaleX:1.2,scaleY:1.2},400)
				.call(this.playEnd,this);
	}

	private playEnd():void
	{
		this.img_eff.source = null;
		DisplayUtil.removeDisplay(this);
	}

	public updateXY(xx:number,yy:number):void
	{
		this.x = xx;
		this.y = yy;
	}
}