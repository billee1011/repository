class UpdownUtil extends StageDele{
	private img:eui.Image;
	public constructor(skin:string) {
		super();
		this.img = new eui.Image();
		this.img.source = SheetManage.getTextureFromCenter(skin);
		this.addChild(this.img);

	}
	protected toStage(evt:any):void
	{
		egret.Tween.removeTweens(this.img);
		this.img.y = -40;
		this.img.x = -17;
		egret.Tween.get(this.img,{loop:true})
					.to({y:-25},600)
					.to({y:-40},600);
	}
	protected awayStage(evt:any):void
	{
		egret.Tween.removeTweens(this.img);
	}

}