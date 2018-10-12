class TButton {
	public skin:egret.DisplayObjectContainer;
	public diBm:egret.Bitmap;
	public ziBm:egret.Bitmap;//可能被包含在diBm里了
	public isToggleBtn:boolean;
	private _selected:boolean;
	private d1:egret.Texture;
	private d2:egret.Texture;
	private tex1:egret.Texture;
	private tex2:egret.Texture;
	public name:string;
	public _badage:eui.Image;
	private b_offX:number = 0;
	private b_offY:number = 0;
	public data:any;
	public constructor(skin:egret.DisplayObjectContainer,name:string='',isToggleBtn:boolean = false) {
		this.skin = skin;
		this.name = name;
		this.skin.name = name; //事件的target是skin 而不是Tbutton
		this.isToggleBtn = isToggleBtn;

		this.diBm = new egret.Bitmap();
		this.skin.addChildAt(this.diBm,0);
		this.diBm.width = skin.width;
		this.diBm.height = skin.height;

		/*if(this.isToggleBtn){
			skin.addEventListener(egret.TouchEvent.TOUCH_TAP,this.clicked,this,false,999);
		}*/
	}
	/**
	 * 相对右上角的偏移量 
	 */
	public setBadgeOffset(ox:number=0,oy:number=0):void
	{
		this.b_offX = ox;
		this.b_offY = oy;
	}
	public set badage(v:boolean)
	{
		if(this._badage)
		{
			this._badage.visible = v;
		}
		else if(v)
		{
			this._badage = new eui.Image();
			this._badage.x = this.skin.width + this.b_offX-13;
			this._badage.y = this.b_offY-5;
			//this._badage.texture = SheetManage.getTongyongTexture('redPoint');
			this.skin.addChild(this._badage);
		}
	}

	/*private clicked(evt:egret.TouchEvent):void
	{
		if(!this.selected){
			this.selected = true;
		}
	}*/
	/**
	 * 如果skin为component则添加
	 */
	public addBgIcon(d1:egret.Texture,d2:egret.Texture=null):void
	{
		this.d1 = d1;
		this.d2 = d2;
		this.diBm.texture = d1;
		if(this.isToggleBtn && !d2){
			this.d2 = this.d1;
		}
	}

	public set texture(tex:egret.Texture)
	{
		this.diBm.texture = tex;
	}

	public addTextIcon( t1:egret.Texture,t2:egret.Texture=null):void
	{
		if(!this.ziBm)
		{
			this.ziBm = new egret.Bitmap();
			this.skin.addChild(this.ziBm);
		}
		this.tex1 = t1;
		this.tex2 = t2;
		this.ziBm.texture = t1;
		this.ziBm.x = (this.skin.width - this.ziBm.width) >> 1;
		this.ziBm.y = (this.skin.height - this.ziBm.height) >> 1;
	}

	public get selected():boolean
	{
		return this._selected;
	}

	public set selected(bol:boolean)
	{
		if(!this.isToggleBtn) return;
		this._selected = bol;
		/*if(bol){
		}*/
		this.diBm.texture = bol ? this.d2 : this.d1;
		if(this.tex2)
			this.ziBm.texture = bol ? this.tex2 : this.tex1;
	}
}