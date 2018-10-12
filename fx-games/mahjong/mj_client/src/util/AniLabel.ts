class AniLabel extends egret.BitmapText{
	public constructor(fnt:string,space:number=0) {
		super();
		var bf:egret.BitmapFont = RES.getRes(fnt);
		this.font = bf;

		this.letterSpacing = space;
	}
	public updateXY(xx:number,yy:number):void
	{
		this.x = xx;
		this.y = yy;
	}
}