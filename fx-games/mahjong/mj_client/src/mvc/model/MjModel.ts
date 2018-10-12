class MjModel extends Proxy{
	public static NAME:string = 'MjModel';

	public static LOGIN_SUCC:string = 'LOGIN_SUCC';
	/**
	 * 输入房间秘密正确 进入房间
	 */
	public static PASSWORD_RIGHT:string = 'PASSWORD_RIGHT';
	public static INIT_CARD_SUCC:string = 'INIT_CARD_SUCC';
	/**
	 * 加入房间 生成周围玩家
	 */
	public static ENTER_ROOM_SUCC:string = 'ENTER_ROOM_SUCC';
	/**
	 * 有人出牌了
	 */
	public static SOMEONE_PLAY_CARD:string = 'SOMEONE_PLAY_CARD';
	/**
	 * 有人摸牌了
	 */
	public static SOMEONE_GET_CARD:string = 'SOMEONE_GET_CARD';
	/**
	 * 标签操作、碰、杠 等
	 */
	public static SING_OPERATE_SUCC:string = 'SING_OPERATE_SUCC';
	/**
	 * 服务端提示有标签出现，此处用途：停掉计时器
	 */
	public static SING_APPEAR:string = 'SING_APPEAR';
	/**
	 * 显示结算
	 */
	//public static SETTLE_SHOW:string = 'SETTLE_SUCC';
	/**
	 * 解散房间
	 */
	public static JIESAN_ROOM_SUCC:string = 'JIESAN_ROOM_SUCC';
	/**
	 * 退出房间
	 */
	public static EXIT_ROOM_SUCC:string = 'EXIT_ROOM_SUCC';
	/**
	 * 有人准备好了
	 */
	public static SOMEONE_READY_GAME:string = 'SOMEONE_READ_GAME';
	/**
	 * 结算后点击开始游戏
	 */
	public static GAME_START_AGAIN:string = 'GAME_START_AGAIN';
	/***
	 * 拉取了公告列表
	 */
	public static GET_GONGGAO_LIST:string = 'GET_GONGGAO_LIST';
	// temp data
	//private heroMsg:Object;
	public mainDir:number;

	//room 信息
	public roomId:number;
	public jushu:number;
	public currentJu:number;
	public currentIndex:number;
	// room 信息

	public playerData:Object; //HeroVO

	/**
	 * 结算
	 * SettleVO
	*/
	public settleData:Object;
	/**
	 * 牌局结束
	 * FinalSettleVO
	 */
	public overData:Object

	///公告列表
	public gonggaoList:any[];
	//邮件列表
	public mailList:any[];
	//版本信息
	public versionData:VersionVO;

	//痞子
	public card_pizi:CardVO;
	//癞子
	public card_laizi:CardVO;
	//谁打了牌，作用：防止自己出两次牌
	public whoPlayedCard:number;
	/**
	 *正在打的这张牌、判断吃的时候用到
	 */
	public playingCard:CardVO;
	/**
	 * 玩家摸到的牌
	 */
	public gotCard:CardVO;
	/**
	 * 剩余多少张牌
	 */
	public leftCard:number = 99;
	/***
	 * 是否存在标签  超时不操作让其过
	 */
	public signExist:boolean = false;

	public constructor() {
		super();
		this.playerData = {};
	//	this.initTempData();
	}

	public getHeromsg():Object
	{
	//	return this.heroMsg; 测试数据
		return this.playerData;
	}
	/**
	 * 相对主角是不是在右边
	 */
	public checkIsRight(mainDir:number,curDir:number):boolean
	{
		let temp:number = curDir - mainDir;
		return temp == 1 || temp == -3;
	}
	/**
	 * 相对主角是不是在上边
	 */
	public checkIsTop(mainDir:number,curDir:number):boolean
	{
		let temp:number = Math.abs(curDir - mainDir);
		return temp == 2;
	}
	/**
	 * 相对主角是不是在左边
	 */
	public checkIsLeft(mainDir:number,curDir:number):boolean
	{
		let temp:number = curDir - mainDir;
		return temp == 3 || temp == -1;
	}

	/**
	 * return 
	 */
	public getIconPByPosition(p:number):number
	{
		if(this.checkIsRight(this.mainDir,p)) return 1;
		if(this.checkIsTop(this.mainDir,p)) return 2;
		if(this.checkIsLeft(this.mainDir,p)) return 3;
		return 4;
	}

	public getInitP(p:number):egret.Point
	{
		let r:egret.Point = new egret.Point();
		if(this.checkIsRight(this.mainDir,p)) 		{
			r.x = 1108;
			r.y = 301;
		}
		if(this.checkIsTop(this.mainDir,p)){
			r.x = 582;
			r.y = 61;
		}
		if(this.checkIsLeft(this.mainDir,p)) {
			r.x = 156;
			r.y = 286;
		}

		return r;
	}

	

	public getIconStyle(p:number):string
	{
		return p == this.mainDir ? 'b' : 's';
	}

	

	public getOperateTexture(opValue:number):egret.Texture
	{
		switch(opValue)
		{
			case 1: //碰
				return SheetManage.getTextureFromOperate('op_peng');
			case 2: //暗杠
				return SheetManage.getTextureFromOperate('op_gang');
			case 3: //明杠
				return SheetManage.getTextureFromOperate('op_gang');
			case 4: //过路杠
				return SheetManage.getTextureFromOperate('op_gang');
			case 5: //胡
				return SheetManage.getTextureFromOperate('op_hu');
			case 6: //自摸
				return SheetManage.getTextureFromOperate('op_hu');
			case 7: //过
				return SheetManage.getTextureFromOperate('op_pass');
			case 8: //吃
				return SheetManage.getTextureFromOperate('op_eat');
		}
		return null;
	}

	public sortCardList(temp:any[]):void
	{
		/*temp.sort(this.sort1);
		temp.sort(this.sort2);*/
		temp.sort(this.sortMoreFun(["style","type"],[0,0]));
	}
	/*private sort1(c1:CardVO,c2:CardVO):number
	{
		if(c1.style < c2.style){
			return -1;
		}
		return 1;
	}
	private sort2(c1:CardVO,c2:CardVO):number
	{
		if(c1.style == c2.style){
			if(c1.type < c2.type) return -1;
			else return 1;
		}
		return 0;
	}*/

	private sortMoreFun(strarr:any[],sortarr:any[] = null){
		
		return function(obj1,obj2)
		{
			let temp:number;
			var valarr:number[] = [];
			var sorlen:number = 0;
			if(sortarr)sorlen = sortarr.length;
			var chanum:number;
			for(var b in strarr){
				temp = parseInt(b);
				chanum = parseInt(obj1[strarr[b]]) - parseInt(obj2[strarr[b]]);
				if(chanum == 0){
					continue;
				}else
				{
					if(sorlen > temp && sortarr[b] == 0) { 
						  return chanum;
					}else
					{
						return -chanum;
					}
				}
			}
			return 0;
		}
	}


	// private getDuiList(p:number):any[]
	// {
	// 	var playedTemp:any[] = [];
	// 	let card:CardVO;
	// 	for(let i:number=0; i<3; i++){
	// 		card = new CardVO();
	// 		card.id = 1000+i;
	// 		card.position = p;
	// 		card.style = Math.ceil(Math.random()*4);
	// 		let max:number = card.style <= 3 ? 9 : 7;
	// 		card.type = Math.ceil(Math.random()*max);
	// 		playedTemp.push(card);
	// 	}
	// 	return playedTemp;
	// }

	// private getOtherCard(p:number):any[]
	// {
	// 	var playedTemp:any[] = [];
	// 	let card:CardVO;
	// 	for(let i:number=0; i<10; i++){
	// 		card = new CardVO();
	// 		card.position = p;
	// 		playedTemp.push(card);
	// 		if(i == 9 && (p == 1|| p == 4)){
	// 			card.justGet = true;
	// 		}
	// 		else if(i == 0 && (p == 2 || p == 3)){
	// 			card.justGet = true;
	// 		}
	// 	}
	// 	return playedTemp;
	// }

	// private initTempData():void
	// {
	// 	var vo:HeroVO = new HeroVO();
	// 	var card:CardVO;
	// 	var temp:any[] = [];
	// 	var playedTemp:any[] = [];
	// 	for(let i:number=0; i<11; i++){
	// 		card = new CardVO();
	// 		card.id = 1000+i;
	// 		card.position = 1;
	// 		card.style = Math.ceil(Math.random()*4);
	// 		let max:number = card.style <= 3 ? 9 : 7;
	// 		card.type = Math.ceil(Math.random()*max);
	// 		temp.push(card);
	// 		if(i < 3){
	// 			playedTemp.push(card);
	// 		}
	// 		/*if(i == 9){
	// 			card.justGet = true;
	// 		}*/
	// 	}
	// 	this.sortCardList(temp);
	// 	//vo.isMainer = true;
	// 	vo.decodeTemp('tiger',true,'me',1,10,playedTemp.concat(),[]);
	// 	vo.ownCardList = temp;
	// 	vo.roleId = 10000;
	// 	this.heroMsg[vo.roleId] = vo;
	// 	this.mainDir = vo.dir;


	// 	playedTemp = this.getDuiList(2);
	// 	vo = new HeroVO();
	// 	vo.ownCardList = this.getOtherCard(2);
	// 	vo.roleId = 10001;
	// 	vo.decodeTemp('xiong',true,'xiong',2,10,playedTemp.concat(),[]);
	// 	this.heroMsg[vo.roleId] = vo;


	// 	playedTemp = this.getDuiList(3);
	// 	vo = new HeroVO();
	// 	vo.roleId = 10002;
	// 	vo.ownCardList = this.getOtherCard(3);
	// 	vo.decodeTemp('luo2',true,'luo2',3,10,playedTemp.concat(),[]);
	// 	this.heroMsg[vo.roleId] = vo;


	// 	playedTemp = this.getDuiList(4);
	// 	vo = new HeroVO();
	// 	vo.ownCardList = this.getOtherCard(4);
	// 	vo.roleId = 10003;
	// 	vo.decodeTemp('luo',true,'luo',4,10,playedTemp.concat(),[]);
	// 	this.heroMsg[vo.roleId] = vo;
	// }
}