class AsySoundManager {
	public static instance = new AsySoundManager();
	private loader:egret.URLLoader;
	public constructor() {
		this.loader = new egret.URLLoader();
		this.loader.dataFormat = egret.URLLoaderDataFormat.SOUND;
		this.loader.addEventListener(egret.Event.COMPLETE,this.onLoadComplete,this);
	}
	public first:QueenVO;
	public last:QueenVO;
	public loadSound(name:string):void
	{
		var vo:QueenVO = new QueenVO();
		vo.data = name;
		if(!this.last){
			this.first = this.last = vo;
		}else
		{
			this.last.next = vo;
			vo.pre = this.last;
			this.last = vo;
		}
		this.startLoad();
	}
	private startLoad():void
	{
		this.loader.load(new egret.URLRequest(PathDefine.MJ_SOUND+this.first.data+'.mp3'));
	}

	private onLoadComplete(evt:egret.Event):void
	{
		var sound:egret.Sound = <egret.Sound>this.loader.data;
		var c:egret.SoundChannel = sound.play(0,1);
		c.volume = GlobalDefine.playSoundVolume;
		

		this.first = this.first.next;
		if(!this.first){
			this.first = this.last = null;
			return;
		}
		this.startLoad();

		
	}
}