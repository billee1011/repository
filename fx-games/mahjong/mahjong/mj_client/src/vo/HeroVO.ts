class HeroVO{
	public roleId:number;
	public gender:number = 1;
	public name:string = '';
	public head:string = '';
	public diamond:number = 0;
	/**
	 * true：准备好了，，false：游戏开始了
	 */
	public ready:boolean;
	public dir:number;
	public ip:string;
	/**
	 * 剩余牌的数量  （针对其他玩家不显示牌面）
	 */
	//public leftCardLen:number = 0;
	/**
	 * 对过的牌
	 */
	public duiCardList:any[] = [];
	/**
	 * 打出去的牌
	 */
	public playedCardList:any[] = [];
	/**
	 * 主角的牌面
	 */
	public ownCardList:any[];

	public jifen:number;
	public constructor() {
		//this.leftCardLen = 13;
	}
	/**
	 * [
			roleId,
			name,
			gender,
			diamond, // 钻石
			headimgUrl // 头像url地址 （游客登陆为""）
		]
	 */
	public decode(data:Array<any>):void
	{
		var i:number = 0;
		this.roleId= data[i++];
		this.name = data[i++];
		this.gender = data[i++];
		this.diamond = data[i++];
		this.head = data[i++];
	}

	public decodeGameBase(arr:any[]):void
	{
		this.dir = arr[0];
		this.roleId = arr[1];
		this.name = arr[2];
		this.ip  = arr[3];
	}

	public decodeTemp(name:any,ready:any,head:any,dir:any,leftCard:number,duiList:any[],playedList:any[]):void
	{
		this.name = name;
		this.ready = ready;
		this.head = head;
		this.dir = dir;
		//this.leftCardLen = leftCard;
		this.duiCardList = duiList;
		this.playedCardList = playedList;
	}
}