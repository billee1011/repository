class TabItem {
	public tb:eui.ToggleButton;
	private tex1:egret.Texture;
	private tex2:egret.Texture;
	private bm:egret.Bitmap;
	public name:string;
	public constructor(skin:eui.ToggleButton,n:string=null) {
		throw new Error('并没有用到的类');
		/*this.tb = skin;
		this.bm = new egret.Bitmap();
		this.tb.addChild(this.bm);
		this.name = n;*/
	}
	/**
	 * t1默认皮肤    
	 * t2选中皮肤
	 */
	public addIcon(t1:egret.Texture,t2:egret.Texture=null):void
	{
		this.tex1 = t1;
		this.tex2 = t2;
		this.bm.texture = t1;
		this.bm.x = (this.tb.width - this.bm.width) >> 1;
		this.bm.y = (this.tb.height - this.bm.height) >> 1;
	}
	public set select(bol:boolean)
	{
		this.tb.selected = bol;
		if(this.tex2)
		{
			this.bm.texture = bol ? this.tex2 : this.tex1;
		}
	}
	public get select():boolean
	{
		return this.tb.selected;
	}

	public set icon(tex:egret.Texture)
	{
		this.tb.icon = tex;
	}
}