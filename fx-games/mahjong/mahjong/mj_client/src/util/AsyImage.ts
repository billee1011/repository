class AsyImage extends egret.DisplayObjectContainer{
	private backFun:Function;
	private backObj:any;
	/**
	 * 是否缓存
	 */
	private isSave:boolean;
	private _img:eui.Image;
	public circleMask:boolean;
	public constructor(w:number=0, h:number=0,isSave:boolean = true) {
		super();
		this._img = new eui.Image();
		this.addChild(this._img);
		if(w) this._img.width = w;
		if(h) this._img.height = h;
		this.isSave = isSave;
	}
	/**
	 * 默认在resource的Icon下面，如果不是补全路劲
	 */
	public setUrl(url:string,backFun:Function=null,thisObj:any=null):void
	{
		url = url.toString();
		this.backFun = backFun;
		this.backObj = thisObj;
		if(url.indexOf('.') == -1){
			url = PathDefine.MJ_ICON + url + '.png';
		}
		var tex:egret.Texture = ResourceCache.instance.getImageFromCache(url);
		if(tex){
			this._img.source = tex;
			this.checkCircleMask();
			if(this.backFun != null){
				this.backFun.call(this.backObj);
			}
			return;
		}
		RES.getResByUrl(url,this.getSucc,this,RES.ResourceItem.TYPE_IMAGE);
	}

	public get width():number
	{
		return this._img.width;
	}
	public get height():number
	{
		return this._img.height;
	}

	public setScale(sx:number,sy:number):void
	{
		this._img.scaleX = sx;
		this._img.scaleY = sy;
	}
	public getImg():eui.Image
	{
		return this._img;
	}
	public clear():void
	{
		this._img.source = null;
		//this._img
	}


	public fullAll():void
	{
		this._img.left = 0;
		this._img.right = 0;
		this._img.top = 0;
		this._img.bottom = 0;
	}

	private getSucc(data:any,url:string):void
	{
		if(this.isSave){
			ResourceCache.instance.addImageToCache(url,data);
		}
		this._img.source = data;
		this.checkCircleMask();
		if(this.backFun != null){
			this.backFun.call(this.backObj);
		}
	}
	private maskShape:egret.Shape;
	private checkCircleMask():void
	{
		if(!this.circleMask) return;
		if(!this.maskShape){
			this.maskShape = new egret.Shape();
		}
		this.maskShape.graphics.clear();
		this.maskShape.graphics.beginFill(0,1);
		this.maskShape.graphics.lineStyle(1,0);
		var hh:number = this._img.width*.5;
		this.maskShape.graphics.drawCircle(hh,hh,hh);
		this.maskShape.graphics.endFill();
		this.addChild(this.maskShape);
		this.mask = this.maskShape;
	}
	public updateXY(xx:number,yy:number):void
	{
		this.x = xx;
		this.y = yy;
	}
}