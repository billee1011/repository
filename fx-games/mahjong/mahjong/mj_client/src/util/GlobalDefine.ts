class GlobalDefine {
	public static stage:egret.Stage;

	public static WEB_SOCKE_COLLECTED:boolean = false;

	public static socket:TSocket
	

	public static tickTemp:boolean = true;

	public static serverInfo:ServerInfoVO = new ServerInfoVO();
	/**
	 * 主角自己的信息 登录时赋值
	 */
	public static herovo:HeroVO = new HeroVO();

	public static userId:string;
	public static ip:string;
	/**背景音乐 */
	public static backSoundChannel:egret.SoundChannel;
	/**音效大小 */
	public static playSoundVolume:number = 1;
	/**
	 * 0:游戏未开始
	 * 1：游戏开始
	 * 2：游戏开始了但在房间外面
	 */
	public static gameState:number = 0;
	/**
	 * 客户端快速进入游戏绿卡通道
	 */
	//public static clientQuickEnterGame:boolean = false;
	/**
	 * sdk传入
	 */
	public static wxCode:string;

	public static shebeima:string;
	public constructor() {
	}

	public static charge():void
	{
	}

	public static get stageW():number
	{
		return this.stage.stageWidth;
	}

	public static get stageH():number
	{
		return this.stage.stageHeight;
	}

	//this.label_des.textFlow = <Array<egret.ITextElement>>[ 
    //{ text:"Egret", style:{"textColor":0xFF0000, "size":30} }
//];
}