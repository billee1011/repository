class GoodsSlot extends RenderBase{
	/**
	 * 品质框
	 */
	protected rareSkin:eui.Image;
	protected label_name:eui.Label;
	private iconW:number;
	protected img_icon:AsyImage;
	/**
	 * 默认皮肤、比如装备槽的底
	 */
	protected _defaultSkin:eui.Image;
	/**
	 * 是否点击弹出物品tip
	 */
	private _tipable:boolean = false;

	private _badage:eui.Image;
	private b_offX:number = 0;
	private b_offY:number = 0;

	private label_count:eui.Label;
	private _vo:ItemVO;
	public constructor(w:number=60,h:number=60) {
		super('');

		this._defaultSkin = new eui.Image();
		this._defaultSkin.width = w;
		this._defaultSkin.height = w;
		this.addChild(this._defaultSkin);

		this.iconW = w;
		this.rareSkin = new eui.Image();
		this.rareSkin.width = w;
		this.rareSkin.height = w;
		this.addChild(this.rareSkin);
		var t:number = 12;
		this.img_icon = new AsyImage(w-t,w-t);
		this.img_icon.x = t/2;
		this.img_icon.y = t/2;
		this.addChild(this.img_icon);

		this.label_count = new eui.Label();
		this.label_count.size = 14;
		this.label_count.fontFamily = '微软雅黑';
		this.addChild(this.label_count);
		CommonTool.addStroke(this.label_count,1,0x000000);
		this.label_count.touchEnabled = false;

		this.label_name = new eui.Label();
		this.addChild(this.label_name);
		this.label_name.y = w;
		//this.label_name.textAlign = 'center';
		this.label_name.size = 16;
		this.label_name.fontFamily = '微软雅黑';
		CommonTool.addStroke(this.label_name,1,0x000000);

		//this._skin.touchChildren = this._skin.touchEnabled = false;
	}

	public set rarelevel(v:number)
	{
		 var r:string = ColorDefine.rareYinshe[v];
		// this.rareSkin.source = SheetManage.getTongyongTexture(r);
	}

	 protected dataChanged(): void
	 {
		 if(!this.data) return;
		this._vo = this.data;
		this.rarelevel = this.data.quality;
		var url:string = PathDefine.MJ_ICON + this.data.looks + '.png';
		this.img_icon.setUrl(url);
		this.count = this.data.count;
		if(this._vo.type == 25){
			this.showEquipLevel();
		}
		this.doData();
	 }

	protected doData():void
	{
		//override
	}

	public showEquipLevel():void
	{
		this.label_count.text = 'lv.'+ this._vo.level;
		this.label_count.x = this.iconW - this.label_count.width - 4;
		this.label_count.y = this.iconW - this.label_count.height - 2;
	}

	public set count(value:number)
	{
		if(this._vo.type == 25){
			return;
		}
		if(value > 1)
		{
			this.label_count.text = CommonTool.getMoneyShow(value);
			this.label_count.x = this.iconW - this.label_count.width - 4;
			this.label_count.y = this.iconW - this.label_count.height - 2;
		}
		else{
			this.label_count.text = '';
		}
	}

	public set label(value:string)
	{
		this.label_name.text = value;
		this.label_name.textColor = ColorDefine.rareColr[this.data.quality];
		this.label_name.x = (this.iconW - this.label_name.width) >> 1;
	}

	/*public set icontouch(value:boolean)
	{
		this.img_icon.touchEnabled = value;
	}*/

	public set tipable(value:boolean)
	{
		this._tipable = value;
		if(value){
			this.img_icon.touchEnabled = true;
			this.img_icon.addEventListener(egret.TouchEvent.TOUCH_TAP,this.showTips,this);
		}
		else{
			this.img_icon.touchEnabled = false;
			this.img_icon.removeEventListener(egret.TouchEvent.TOUCH_TAP,this.showTips,this);
		}
	}
	private showTips(evt:egret.TouchEvent):void
	{
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
			this._badage.x = this.iconW + this.b_offX-13;
			this._badage.y = this.b_offY-6;
			//this._badage.texture = SheetManage.getTongyongTexture('redPoint');
			this.addChild(this._badage);
		}
	}
	public setBadgeOffset(ox:number=0,oy:number=0):void
	{
		this.b_offX = ox;
		this.b_offY = oy;
	}
}