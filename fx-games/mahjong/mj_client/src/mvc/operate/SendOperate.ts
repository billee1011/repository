class SendOperate {
	public static _instance:SendOperate;
	public static get instance():SendOperate
	{
		if(!this._instance){
			this._instance = new SendOperate();
		}
		return this._instance;
	}
	private socket:TSocket;
	public constructor() {
		this.socket = GlobalDefine.socket;
	}
	/**
	 * 10003  登陆游戏
		C -> S
		[loginType	Int	登录类型	1：微信登录，2: 游客登录
		pid	string	平台ID	
		uid	string		客户端有缓存，就传userId过来，没有缓存传""
		machingId	string	设备码	
		code	string		只用作微信登陆并且没有userId没有缓存，游客登陆传""
		]
	 * 
	 */
	public requestLogin():void
	{
		let t:string = GlobalDefine.shebeima ? GlobalDefine.shebeima : 'fd'+Math.floor(Math.random()*100000);
		this.socket.sendData(10003,[2,'','',t,'']);
	}
	/**
	 * (10007)创建房间
		c->s
			[
			jushu, 局数
			costType, 1：房主付费 2：AA付费
			]
		s->c
			失败=[0,errorcode]
			成功=[1,roomNum]
	 */
	public requestCreateRoom(ju:number,costType:number):void
	{
		this.socket.sendData(10007,[ju,costType]);
	}
	/**
	 * 10008 加入房间
		c->s
			roomNum
	 */
	public requestEnterRoom(roomId:number):void
	{
		this.socket.sendData(10008,roomId);
	}

	public requestPlayCard(id:number):void
	{
		this.socket.sendData(10011,id);
	}
	/**
	 * 碰=1  暗杠=2  明杠=3  胡=4  自摸=5
	 */
	public requestOperate(type:number):void
	{
		this.socket.sendData(10013,type);
	}
	/**
	 * 吃
	 */
	public requestEatCard(arr:any[]):void
	{
		this.socket.sendData(10022,arr);
	}

	public requestJiesanRoom():void
	{
		this.socket.sendData(10014);
	}
	public requestExitRoom():void
	{
		this.socket.sendData(10015);
	}

	public requestStartAgainGame():void
	{
		this.socket.sendData(10017);
	}
}