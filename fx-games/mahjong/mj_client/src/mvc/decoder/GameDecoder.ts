class GameDecoder extends TDecoder{
	private model:MjModel;
	/**
	 * //所有协议均有result，统一处理到data的第一位   不管是服务器推送还是请求后推送 如不用错误判断 则干掉第一位再操作
	 */
	public constructor() {
		super();
		this.types = [10003,10007,10008,10009,10010,10011,10012,10016,10018,
		               10013,10014,10015,10017,10019
					];

		this.model = TFacade.getProxy(MjModel.NAME);
	}

	/**
	 * 
	 * 10003  登陆游戏
c->s
	loginType 登陆类型 1=微信登陆 2=游客登陆
	pid 
	userId  客户端有缓存，就传userId过来，没有缓存传""
	machingId  机器的设备码 有唯一性
	code 只用作微信登陆并且没有userId没有缓存，游客登陆传""
s->c
	失败=[0,errorcode]
	成功=[
			1,
			nowTime, // 当前时间
			offset, // 当前时区
			[
				roleId,
				name,
				gender,
				diamond, // 钻石
				headimgUrl // 头像url地址 （游客登陆为""）
			]
			userId,
			ip
		]
	 */
	public f_10003(data:any[]):void
	{
		if(!this.checkSucc(data)){
			return;
		}
		var hvo:HeroVO = GlobalDefine.herovo;
		hvo.decode(data[3]);
		GlobalDefine.userId = data[4];
		GlobalDefine.ip = data[5];
		GlobalDefine.serverInfo.serverTime = data[1];
		GlobalDefine.wxCode = 'aaaa';
		this.model.simpleDispatcher(MjModel.LOGIN_SUCC);
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
			成功=[1,roomNum,jushu]
	 */
	public f_10007(data:any[]):void
	{
		if(!this.checkSucc(data)){
			return;
		}
		this.model.roomId = data[1];

		//自己创建的房间 直接怼进去
		var hvo:HeroVO = GlobalDefine.herovo;
		this.f_10008([1,data[1],data[2],[[1,hvo.roleId,hvo.name,hvo.ip]]]);
		//SendOperate.instance.requestEnterRoom(data[1]);
	}
	/**
	 * (10008)加入房间
		c->s
			[
			roomNum
			]
		s->c
			失败=[0,errorcode]
			成功=[1,
				roomNum,
				jushu
					[
					[index,id,name,ip].... 
					]
				]
	 */
	public f_10008(data:any[]):void
	{
		if(!this.checkSucc(data)){
			return;
		}
		this.model.roomId = data[1];
		this.model.currentJu = 0;
		this.model.jushu = data[2];
		let vo:HeroVO;
		this.model.playerData = {};
		for (var key in data[3]) {
			let temp:any[] = data[3][key];
			vo = new HeroVO();
			vo.decodeGameBase(temp);
			vo.ready = true;
			this.model.playerData[vo.roleId] = vo;
			if(vo.roleId == GlobalDefine.herovo.roleId){
				this.model.mainDir = vo.dir;
			}
		}
		TFacade.toggleUI(MainGameUI.NAME,1);
		TFacade.toggleUI(EnterRoomUI.NAME,0);
		this.model.simpleDispatcher(MjModel.ENTER_ROOM_SUCC);
	}
	/**
	 * 
	 * (10009)给客户端推送初始化的牌
		s->c
				[	
				  1,

			//痞子[
						[
						id, 牌的唯一id
						color, 牌的花色
						num, 牌的数值
						used
					]
					//癞子[
						id, 牌的唯一id
						color, 牌的花色
						num, 牌的数值
						used
					]
			]
			
			[//以下才是手牌
				[
					id, 牌的唯一id
					color, 牌的花色
					num, 牌的数值
					used
				]...
			]
		]
	 */
	public f_10009(data:any[]):void
	{
		//data[0]  result
		this.model.card_pizi = new CardVO();
		this.model.card_pizi.decode(data[1][0]);

		this.model.card_laizi = new CardVO();
		this.model.card_laizi.decode(data[1][1]);

		this.model.leftCard = 84;
		this.model.currentJu ++;
		
		var main:HeroVO = this.model.playerData[GlobalDefine.herovo.roleId];
		main.ownCardList = [];
		main.ready = false;
		let vo:CardVO;
		let key:any
		for (key in data[2]) {
			vo = new CardVO();
			let temp:any[] = data[2][key];
			vo.decode(temp);
			vo.position = main.dir;
			main.ownCardList.push(vo);
		}
		this.model.sortCardList(main.ownCardList);

		//初始化其它玩家的牌
		let hvo:HeroVO;
		for (key in this.model.playerData) {
			hvo = this.model.playerData[key];
			hvo.ready = false;
			if(hvo.roleId == main.roleId) continue;
			hvo.ownCardList = [];
			for(let i:number=0; i<13; i++){
				vo = new CardVO();
				vo.position = hvo.dir;
				hvo.ownCardList.push(vo);
			}
		}

		GlobalDefine.gameState = 1;
		this.model.simpleDispatcher(MjModel.INIT_CARD_SUCC);
	}
	/**
	 * 	(10010)客户端提示 碰，杠，胡 之类
		 *  s->c
		数组大小不固定，如果有碰和暗杠，则会返回
		[
			1,
			2,
			...
		]
      碰=1  暗杠=2  明杠=3  过路杠=4  胡=5  自摸=6  过=7   吃=8
	 */
	public f_10010(data:any[]):void
	{
		data.shift(); //所有协议均有result，统一处理到data的第一位
		TFacade.toggleUI(OperateUI.NAME,1).execute('operate',data);
		this.model.simpleDispatcher(MjModel.SING_APPEAR);
		this.model.signExist = true;
	}
	/**
	 * 
	 * 10011 打牌
		c->s
			paiId
		s->c
			失败=[0,errorcode]
			成功=[
					1,
					roleId,
					roleIndex,
					[
						id, 牌的唯一id
						color, 牌的花色
						num, 牌的数值
						used
					]
				]
				如果有提示标签的话，会推送10010
				如果没有提示标签的话。会推送10012
	 */
	public f_10011(data:any[]):void
	{
		if(!this.checkSucc(data)){
			return;
		}
		var vo:CardVO = new CardVO();
		vo.position = data[2];
		vo.decode(data[3]);

		this.model.whoPlayedCard = data[1];
		this.model.playingCard = vo;
		this.model.gotCard = null;
			
		let hvo:HeroVO = this.model.getHeromsg()[data[1]];
		//let hvo:HeroVO = this.model.playerData[data[1]];//正式数据

		for (var key in hvo.ownCardList) {//打过牌后 将刚摸的牌的状态改变
			let temp:CardVO = hvo.ownCardList[key];
			temp.justGet = false;
		} 

		if(hvo.roleId == GlobalDefine.herovo.roleId) //更新玩家手上的牌， 出过的牌在MainGameUI里修改，便于调用render方法 实现不同的添加方式
		{
			var list:any[] = hvo.ownCardList;
			var temp:CardVO;
			for (var key in list) {
				temp = list[key];
				if(temp.id == vo.id){
					list.splice(list.indexOf(temp),1); //只删除一个数据 可以在for循环里干
					break;
				}
			}
		}else{
			hvo.ownCardList.length = 13 - hvo.duiCardList.length;
		}

		if(hvo.roleId == GlobalDefine.herovo.roleId)
		{
			this.model.sortCardList(hvo.ownCardList);	//自己出过牌后 整理有序自己的牌面
		}

		this.model.simpleDispatcher(MjModel.SOMEONE_PLAY_CARD,vo);
	}
	/**
	 * 10012 推送摸到的牌(都推送) 摸牌
		s->c
			[
				1,
				roleId,
				roleIndex,
				[
					id, 牌的唯一id
					color, 牌的花色
					num, 牌的数值
					used
				]
			]
	 */
	public f_10012(data:any[]):void
	{
		var vo:CardVO = new CardVO();
		vo.position = data[2];
		this.model.currentIndex = data[2];
		this.model.currentIndex = vo.position;
		this.model.whoPlayedCard = -1;//摸牌后必定能出牌
		vo.decode(data[3]);
		this.model.gotCard= vo;
		vo.justGet = true;

		this.model.leftCard -- ;
		if(this.model.leftCard < 0){
			this.model.leftCard = 0;
		}

		this.model.simpleDispatcher(MjModel.SOMEONE_GET_CARD,vo);
	}
	/**
	 * 10013 麻将标签操作   有人操作就推送
		c->s
			signType 标签类型
		s->c
			失败=[0,errorcode]
			成功=[
					1,
					roleId,
					roleIndex,
					signType,
					[
						[
							id,
							color,
							num,
							used
						]...
					]
				]
		备：signType是胡牌类型的话，返回10016协议
	 */
	public f_10013(data:any[]):void
	{
		if(!this.checkSucc(data)){
			return;
		}
		this.model.signExist = false;
		if(data[3] == 7){ //过
			return;
		}
		this.model.currentIndex = data[2];
		var deleteIds:any[] = [];
		var temps:any[] = [];
		var cvo:CardVO;
		var key:any;
		for (key in data[4]) {
			cvo = new CardVO();
			cvo.position = data[2];
			cvo.decode(data[4][key]);
			temps.push(cvo);
			deleteIds.push(cvo.id);
		}

		if(temps.length > 0){ //不说别的，别人干了标签 那刚才出牌的那位是不是该把牌交出来了
			var xvo:HeroVO = this.model.playerData[this.model.whoPlayedCard];
			var xlist:any[] = xvo.playedCardList;
			for(let xt:number=0; xt<xlist.length; xt++){
				cvo = xlist[xt];
				if(cvo.id == this.model.playingCard.id){
					xlist.splice(xt,1);
					break;
				}
			}
		}

		var tt:any[] = [];
		var vo:HeroVO = this.model.playerData[data[1]];
		if(data[1] == GlobalDefine.herovo.roleId) //主角
		{
			vo.duiCardList = vo.duiCardList.concat(temps); //碰牌追加
			for (key in vo.ownCardList) 
			{
				cvo = vo.ownCardList[key];
				if(deleteIds.indexOf(cvo.id) != -1){ //从手牌上去除碰过的牌
					//let index:number = vo.ownCardList.indexOf(cvo); ///不要在for循环里删数据啊
					//vo.ownCardList.splice(index,1);
					tt.push(cvo);
				}
			}
			if(tt.length > 0){
				for(let j:number=0; j<tt.length; j++){
					cvo = tt[j];
					let index = vo.ownCardList.indexOf(cvo);
					if(index != -1){
						vo.ownCardList.splice(index,1);
					}
				}
			}
		}
		else 
		{
			vo.duiCardList = vo.duiCardList.concat(temps);
			vo.ownCardList.length = 13 - vo.duiCardList.length;
		}


		this.model.simpleDispatcher(MjModel.SING_OPERATE_SUCC,[vo.dir,data[3],this.model.playingCard.position]); ///抛事件、刷新对应玩家的牌面,顺便刷新出牌的那位
	}
	/**
	 * 10014 游戏未开始房主解散房间
		c->s
			
		s->c
			失败=[0,errorcode]
			成功=[
					1,
					code,name,id
				]
	 */
	public f_10014(data:any[]):void
	{
		if(!this.checkSucc(data)){
			return;
		}
		Tnotice.instance.popUpTip(data[1]);
		this.model.simpleDispatcher(MjModel.JIESAN_ROOM_SUCC);
		TFacade.toggleUI(MainGameUI.NAME,0);
	}
	/**
	 * 10015 游戏未开始别人退出房间
		c->s
			
		s->c
			失败=[0,errorcode]
			成功=[
					1
				]
			注意：退出成功会给其他玩家刷新房间列表，走10008协议
	 */
	public f_10015(data:any[]):void
	{
		if(!this.checkSucc(data))
		{
			return;
		}
		this.model.simpleDispatcher(MjModel.EXIT_ROOM_SUCC);
		TFacade.toggleUI(MainGameUI.NAME,0);
	}
	/**
	 (10016)每局的结算面板
		s->c
		[
			1
				[ 胡牌玩家列表，没人胡时为null
				winRoleId
				...
				],
			[
				roleId, 
				jifen总积分, 
				[玩家牌的列表
					[
						id,
						color,
						num,
						used
					]...
				]
			]...
		]
	 */
	public f_10016(data:any[]):void
	{
		data.shift();

		let huList:any[] = data[0];

		this.model.settleData = {};
		let vo:SettleVO;
		for (var key in data[1]) {
			let arr:any[] = data[1][key];
			vo = new SettleVO();
			vo.decode(arr);
			this.model.settleData[vo.roleId] = vo;
		}
		vo = this.model.settleData[GlobalDefine.herovo.roleId];
		let ss:string;
		if(huList == null){
			ss = 'ping';
		}else if(huList.indexOf(vo.roleId) != -1)
		{
			ss = 'win';
		}else{
			ss = 'lose';
		}
		TFacade.toggleUI(SettlementUI.NAME,1).execute('go',ss);
		//this.model.simpleDispatcher(MjModel.SETTLE_SHOW);
	}
	/***
	 * (10019)总战绩
		s->c
		失败=[0,errorcode]
		成功=[
			1,
			[
				index,roleId,name,jifen总积分,zimoCount,jiepaoCount,dianpaoCount,angangCount,minggangCount
			]...
		]
	 */
	public f_10019(data:any[]):void
	{
		data.shift();
		this.model.overData = {};
		let vo:FinalSettleVO;
		for (var key in data) {
			let arr:any[] = data[key];
			vo = new FinalSettleVO();
			vo.index = arr.shift();
			vo.roleid = arr.shift();
			vo.name = arr.shift();
			vo.score = arr.shift();
			vo.decode(arr);
			this.model.overData[vo.roleid] = vo;
		}
		GlobalDefine.gameState = 0;
		TFacade.toggleUI(GameOverUI.NAME,1).execute('go');
	}

	/**
	 * 10017 开始游戏
		c->s
		s->c
			失败=[0,errorcode]
			成功=[
				1,
				roleId
			]
		如果4个人都点了开始游戏，会给客户端返回10009 ，10012 ，10010 
	 */
	public f_10017(data:any[]):void
	{
		if(!this.checkSucc(data)){
			return;
		}
		GlobalDefine.gameState = 0; 
		var vo:HeroVO = this.model.playerData[data[1]];
		vo.ready = true;
		this.model.leftCard = 0;
		let tt:HeroVO;
		for (var key in this.model.playerData) {
			tt = this.model.playerData[key];
			tt.ownCardList = [];
			tt.duiCardList = [];
			tt.playedCardList = [];
		}
		this.model.simpleDispatcher(MjModel.GAME_START_AGAIN);

		
		this.model.simpleDispatcher(MjModel.SOMEONE_READY_GAME,vo.dir);

	}
	/**
	 * 
	 * (10018)断线重连
		s->c
		失败=[0,errorcode]
		成功=[
			1,
			roomNum,
			alreadyJuShu, 已经打了的局数
					totalJu,总局数
			sumIndex, 已经摸了多少张牌
			curIndex, 当前索引
			[
				index,玩家索引
				roleId,
				name,
				ip,
				jifen,
				[玩家自己牌的列表(其他玩家这里存放的是已经被用过的牌)
					[ 
						id,
						color,
						num,
						used
					]...
				]
				[打出来的牌
					[
						id,
						color,
						num,
						used
					]...
				]
			]...
 ]
	 */
	public f_10018(data:any[]):void
	{
		if(!this.checkSucc(data)){
			return;
		}
		this.model.roomId = data[1];
		this.model.currentJu = data[2];
		this.model.jushu = data[3];
		this.model.currentIndex = data[5];

		data = data.slice(6);
		let vo:CardVO;
		let key:any
		for (key in data) {
			let temp:any[] = data[key];
			let rid:number = temp[1];
			let hvo:HeroVO = this.model.playerData[rid];
			if(!hvo){
				if(rid == GlobalDefine.herovo.roleId){
					hvo = GlobalDefine.herovo;
				}else{
					hvo = new HeroVO();
				}
				hvo.dir = temp[0];
				hvo.roleId = rid;
				hvo.name = temp[2];
				hvo.ip = temp[3];
				hvo.jifen = temp[4];
				this.model.playerData[rid] = hvo;
			}

			hvo.ownCardList = [];
			hvo.duiCardList = [];
			hvo.playedCardList = [];

			let ownCardList:any[] = temp[5]; //手牌
			for (key in ownCardList) { //
				let tt:any[] = ownCardList[key];
				vo = new CardVO();
				vo.position = hvo.dir;
				vo.decode(tt);
				if(vo.used){
					hvo.duiCardList.push(vo);
				}else{
					hvo.ownCardList.push(vo);
				}
				
			}
			if(rid != GlobalDefine.herovo.roleId){ //是其它玩家，其手牌等于13张 - 对/杠过的牌
				var ownLen:number = 13-hvo.duiCardList.length;
				for(let i:number=0; i<ownLen; i++){
					vo = new CardVO();
					vo.position = hvo.dir;
					hvo.ownCardList.push(vo);
				}
			}
			let playedCardList:any[] = temp[6];//打出去的牌
			for (key in playedCardList) { //
				let cc:any[] = ownCardList[key];
				vo = new CardVO();
				vo.decode(cc);
				hvo.playedCardList.push(vo);
			}
		}
		GlobalDefine.gameState = 1;
		TFacade.toggleUI(MainGameUI.NAME,1);
		this.model.simpleDispatcher(MjModel.ENTER_ROOM_SUCC);
		this.model.simpleDispatcher(MjModel.INIT_CARD_SUCC);
	}
}