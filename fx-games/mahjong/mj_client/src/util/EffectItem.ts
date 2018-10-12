/**
 * 加载moveiclip文件
 */
class EffectItem extends egret.DisplayObjectContainer{
	public constructor(playOnce=false,mcname:string = 'main') {
		super();
		this.playOnce = playOnce;
		this.mcname = mcname;
	}
	private playOnce:boolean;
	private pngFlag:boolean;
	private jsonFlag:boolean;
	private pngData:any;
	private jsonData:any;
	private mc:egret.MovieClip;
	private mcname:string;
	private _isSave:boolean;
	public get isSave(): boolean {
	    return this._isSave;
	}
	public set isSave(value: boolean) {
	    this._isSave = value;
	}
	private path:string;
	/**
	 * 不要后缀,二者名字最好相同
	 */
	public setUrl(png:string,json:string=null,isSave:boolean=false,path:string=PathDefine.EFFECT):void
	{
		this.isSave = isSave;
		this.pngFlag = this.jsonFlag = false;

		if(!json){
			json = png;
		}
		png += '.png';
		json += '.json';
		this.path = path+png;

		this.mc = ResourceCache.instance.getMcFromCache(this.path);//get cache
		if(this.mc)
		{
			this.addChild(this.mc);
			this.mc.gotoAndStop(1);
			if(this.playOnce){
				this.mc.play(1);
				this.mc.addEventListener(egret.MovieClipEvent.COMPLETE,this.playComplete,this);
			}else{
				this.mc.play(-1);
			}
			
		}else{
			RES.getResByUrl(path + png,this.pngOver,this,RES.ResourceItem.TYPE_IMAGE);
			RES.getResByUrl(path + json,this.jsonOver,this,RES.ResourceItem.TYPE_JSON);
		}


		this.touchEnabled = false;
		this.addEventListener(egret.Event.REMOVED_FROM_STAGE,this.goAwayFromStage,this);
		this.addEventListener(egret.Event.ADDED_TO_STAGE,this.addToStage,this);
	}
	private pngOver(data:any):void
	{
		this.pngData = data;
		this.pngFlag = true;
		this.doubleOver();
	}
	private jsonOver(data:any):void
	{
		this.jsonData = data;
		this.jsonFlag = true;
		this.doubleOver();
	}
	private doubleOver():void
	{
		if(this.jsonFlag && this.pngFlag)
		{
			this.clear();
			var mcFactory = new egret.MovieClipDataFactory(this.jsonData,this.pngData);
       	 	this.mc = new egret.MovieClip(mcFactory.generateMovieClipData(this.mcname));
			this.addChild(this.mc);
			if(this.playOnce){
				this.mc.play(1);
				this.mc.addEventListener(egret.MovieClipEvent.COMPLETE,this.playComplete,this);
			}else{
				this.mc.play(-1);
			}
			if(this.isSave){
				ResourceCache.instance.addMcToCache(this.path,this.mc)
			}
		}
	}

	public clear():void
	{
		if(!this.mc) return;

		this.mc.stop();
		DisplayUtil.removeDisplay(this.mc);
		this.mc = null;
	}

	private playComplete(evt:egret.MovieClipEvent):void
	{
		if(this.mc) {
			this.mc.removeEventListener(egret.MovieClipEvent.COMPLETE,this.playComplete,this);
			DisplayUtil.removeDisplay(this.mc);
			DisplayUtil.removeDisplay(this);
		}
	}

	public setScale(c1:number,c2:number):void
	{
		this.scaleX = c1;
		this.scaleY = c2;
	}

	public updateXY(x1:number,y1:number):void
	{
		this.x = x1;
		this.y = y1;
	}

	private goAwayFromStage(evt:egret.Event):void
	{
		if(this.mc){
			this.mc.stop();
			DisplayUtil.removeDisplay(this.mc);
			//this.mc = null;
		}
		//this.jsonData = this.pngData = this.jsonFlag = this.pngFlag = false;
	}
	private addToStage(evt:egret.Event):void
	{
		if(this.mc){
			if(!this.playOnce){
				this.mc.play(-1);
			}
			this.addChild(this.mc);
			//this.mc = null;
		}
		///this.jsonData = this.pngData = this.jsonFlag = this.pngFlag = false;
	}
}