class MainGameUI extends UIBase{
	public static NAME:string = 'MainGameUI';
	public constructor() {
		super('resource/UI_exml/MainGame.exml');
		this.closeOther = false;
	}
	private label_room:eui.Label;
	private model:MjModel;
	private deleObj:Object = {};
	private dirUI:DirectionUI;
	private tickTool:AniTickTool;
	private btn_back:eui.Button;
	private btn_set:eui.Button;
	private label_jushu:eui.Label;
	private laizi:DuiCardRender;
	private pizi:DuiCardRender;
	private gonggaoMask:egret.Shape;
	private btn_wxInvite:eui.Button;
	private img_wxzi:eui.Image;
	private state:State;
	private label_leftCard:eui.Label;
	protected bindEvent():void
	{
		this.model = TFacade.getProxy(MjModel.NAME);
		this._eventList[MjModel.ENTER_ROOM_SUCC] = [this.updatePlayer,this];
		this._eventList[MjModel.INIT_CARD_SUCC] = [this.refreshPlayerCard,this];
		this._eventList[MjModel.SOMEONE_GET_CARD] = [this.someoneGetCard,this];
		this._eventList[MjModel.SOMEONE_PLAY_CARD] = [this.somonePlayedCard,this];
		this._eventList[MjModel.SING_OPERATE_SUCC] = [this.singOperate,this];
		this._eventList[MjModel.SOMEONE_READY_GAME] = [this.someReady,this];
		this._eventList[MjModel.GAME_START_AGAIN] = [this.gamestartAgain,this];
		this._eventList[MjModel.SING_APPEAR] = [this.appearSign,this];
	}
	protected uiLoadComplete():void
	{
		this.deleObj = {};

		let bg:AsyImage = new AsyImage(0,0,true);
		this.addChildAt(bg,0);
		bg.setUrl(PathDefine.UI_IMAGE+'desk.jpg');

		//bg.fullAll();

		this.pizi = new DuiCardRender();
		this.pizi.updateXY(45,101);
		this.addChild(this.pizi);

		this.laizi = new DuiCardRender();
		this.laizi.updateXY(109,101);
		this.addChild(this.laizi);


		this.dirUI = new DirectionUI();
		this.dirUI.x = 568;
		this.dirUI.y = 288;
		this.addChild(this.dirUI);

		this.state = new State();
		this.addChild(this.state);

		var greenLabel:AniLabel = new AniLabel('mj_redFont_fnt') //其实是黄色字体
		this.addChild(greenLabel);
		/*var redLabel:AniLabel = new AniLabel('mj_redFont_fnt');
		redLabel.updateXY(638,341); 
		this.addChild(redLabel);*/
		this.tickTool = new AniTickTool(greenLabel,null,5000,640,346);
		this.tickTool.backFun = this.countdownTimeOver;
		this.tickTool.funThis = this;
		greenLabel.text = '15';
		greenLabel.x = 640 - greenLabel.width*.5;
		greenLabel.y = 346;

		this.state.add('game',[this.dirUI,greenLabel,this.label_leftCard]);
		this.state.add('wait',[this.btn_wxInvite,this.img_wxzi])
		this.state.state = 'wait';

		this.dirUI.setDirection(1);
		this.dirUI.playCardDirection(1);

		GlobalDefine.stage.addEventListener(egret.TouchEvent.TOUCH_BEGIN,this.touchBeginHandler,this);
		GlobalDefine.stage.addEventListener(egret.TouchEvent.TOUCH_MOVE,this.touchMoveHandler,this);
		GlobalDefine.stage.addEventListener(egret.TouchEvent.TOUCH_END,this.touchEndHandler,this);

		//this.dirUI.addEventListener(egret.TouchEvent.TOUCH_TAP,this.test,this);

		this.btn_back.addEventListener(egret.TouchEvent.TOUCH_TAP,this.backToHall,this);
		this.btn_set.addEventListener(egret.TouchEvent.TOUCH_TAP,this.showSetHandler,this);

		
	}

	private showGonggao(msg:string):void
	{
		if(!this.gonggaoMask){
			this.gonggaoMask = new egret.Sprite();
			this.addChild(this.gonggaoMask);
			this.gonggaoMask.graphics.beginFill(0,1);
			this.gonggaoMask.graphics.drawRect(0,0,630,22);
			this.gonggaoMask.x = 355;
			this.gonggaoMask.y = 13;
		}
		this.gonggaoMask.x = 362;
		this.gonggaoMask.y = 13;
		this.addChild(this.gonggaoMask);
		Tnotice.instance.playGonggao(msg,false,this.gonggaoMask);
		LayerManage.instance.notice.label.size = 18;
	}

	private showSetHandler(evt:any):void
	{
		TFacade.toggleUI(SettingUI.NAME);
		this.showGonggao('你点击了设置');
	}

	private countdownTimeOver():void
	{
		if(this.model.signExist){
			SendOperate.instance.requestOperate(7); //过
			return;
		}
		if(this.model.mainDir == this.model.currentIndex && this.model.gotCard)	
		{
			SendOperate.instance.requestPlayCard(this.model.gotCard.id);
		}
	}

	private backToHall(evt:egret.TouchEvent):void
	{
		Tnotice.instance.popUpTip('现在不能离开哦');
		return;
		if(GlobalDefine.gameState == 0){
			Tnotice.instance.showSureMsg('确定解散房间？',this.sureJiesan,this)
			return;
		}
		this.hide();
	}
	private sureJiesan():void
	{
		SendOperate.instance.requestJiesanRoom();
	}



	/*private testIndex:number = 10000;
	private testDir:number = 1;
	private test(evt:any):void
	{
			let style = Math.ceil(Math.random()*4);
			let max:number = style <= 3 ? 9 : 7;
			let type = Math.ceil(Math.random()*max);
		GlobalDefine.socket.simulateReceiveMsg(10011,[1,this.testIndex,this.testDir,[Math.floor(Math.random()*10000),style,type,false]])

		this.testIndex ++;
		this.testDir ++;
		if(this.testIndex > 10003){
			this.testIndex = 10000;
		}
		if(this.testDir > 4){
			this.testDir = 1;
		}
	//	TFacade.toggleUI(OperateUI.NAME,1).execute('aaa',[1,2,5]);
		//GlobalDefine.socket.simulateReceiveMsg(10016,null);
	}*/


	public awake():void
	{
		this.updateRoom();

		this.updatePlayer();

		this.refreshPlayerCard();

		this.updateLeftCard();
	}

	private updateRoom():void
	{
		this.label_room.text = this.model.roomId.toString();
		this.label_jushu.text = CommonTool.replaceStr('局数:{0}/{1}',this.model.currentJu,this.model.jushu);
	}

	private appearSign():void
	{
		this.tickTool.stopTick();
	}

	private singOperate(dd:any[]):void
	{
		var dele:PlayerBaseRender = this.deleObj[dd[0]];
		dele.refreshRender();

		this.dirUI.playCardDirection(this.model.currentIndex);

		dele.showOperateEffect(dd[1]);

		this.tickTool.stopTick();

		if(dd[2]){
			dele = this.deleObj[dd[2]];
			dele.refreshPlayedCard();
		}
	}
	/**
	 * 结算面板点击开始游戏，清理所有人的牌
	 */
	private gamestartAgain():void
	{
		var dele:PlayerBaseRender;
		for (var key in this.deleObj) {
			dele = this.deleObj[key];
			dele.refreshRender();
		}
		this.pizi.data = null;
		this.laizi.data = null;
		this.updateLeftCard();
	}
	/**
	 * 有人准备好了
	 */
	private someReady(dir:number):void
	{
		var dele:PlayerBaseRender = this.deleObj[dir];
		var vo:HeroVO = dele.data;
		if(vo && vo.ready){
			dele.showReadyHand();
		}

		//dele.refreshRender();

	}
	/**
	 * 有人打牌了
	 */
	private somonePlayedCard(vo:CardVO):void
	{
		var dele:PlayerBaseRender = this.deleObj[vo.position];
		dele.addVoToPlayedCardList(vo);
		var hvo:HeroVO = dele.data;
		
		//dele.refreshPlayedCard();
		//dele.refreshOwnCard();   //如果刷新坐标有问题，刷新整个render即可
		dele.refreshRender();

		//return;
		
		var sp:egret.Point = this.model.getInitP(vo.position);
		if(hvo.roleId == GlobalDefine.herovo.roleId){
			this.tweenStartX = this.ownTweenStartX;
			this.tweenStartY = this.ownTweenStartY;
		}else{
			this.tweenStartX = sp.x;
			this.tweenStartY = sp.y;
		}
		this.startPlayCard(vo);

	}
	/**
	 * 有人摸牌了
	 */
	private someoneGetCard(vo:CardVO):void
	{
		var dele:PlayerBaseRender = this.deleObj[vo.position];
		dele.addVoToOwnCardList(vo);
		dele.refreshOwnCard();	 //如果刷新坐标有问题，刷新整个render即可
		this.dirUI.playCardDirection(this.model.currentIndex);

		this.tickTool.stopTick();
		this.tickTool.startTick(15000);

		this.updateLeftCard();
	}

	private updateLeftCard():void
	{
		this.label_leftCard.textFlow = CommonTool.replaceStrBackColor('剩余  <font color="#fff000">{0}</font>  张牌',this.model.leftCard);
	}

	private refreshPlayerCard():void
	{
		this.pizi.data = this.model.card_pizi;
		this.pizi.setScale(0.63,0.63);
		this.laizi.data = this.model.card_laizi;
		this.laizi.setScale(0.63,0.63);

		this.updateRoom();

		if(GlobalDefine.gameState == 1){
			this.state.state = 'game';
		}

		var render:PlayerBaseRender;
		for (var key in this.model.playerData) {
			let hvo:HeroVO = this.model.playerData[key];
			render = this.deleObj[hvo.dir];
			render.refreshRender();
			if(!hvo.ready){
				render.hideReadyHand();
			}
		}
	}


	/*private testPlayCard(evt:any):void
	{
		this.dirUI.playCardDirection(this.tempDirIndex);
		let cvo:CardVO = new CardVO;
		cvo.position = this.tempDirIndex;
		cvo.style = Math.ceil(Math.random()*4);
		let max:number = cvo.style <= 3 ? 9 : 7;
		cvo.type = Math.ceil(Math.random()*max);
		cvo.id = this.idIndex ++;
		var sp:egret.Point = this.model.getInitP(cvo.position);
		this.tweenStartX = sp.x;
		this.tweenStartY = sp.y;
		this.startPlayCard(cvo);
	}*/

	private startPlayCard(cvo:CardVO):void
	{
		let tt:number = cvo.style == 4 ? cvo.type+1-10 : cvo.type;
		AsySoundManager.instance.loadSound(`s${cvo.style}_${tt}`);

		var dele:PlayerBaseRender = this.deleObj[cvo.position];
		var hvo:HeroVO = dele.data;
		this.tweenCardVO = cvo;


		//需要屏蔽--------------------------
		var list:any[] = hvo.ownCardList;
			var temp:CardVO;
			for (var key in list) {
				temp = list[key];
				if(temp.id == cvo.id){
					list.splice(list.indexOf(temp),1);
					hvo.playedCardList.push(temp);
					break;
				}
			}
			dele.refreshPlayedCard();
		//测试，然后屏蔽----------------------------

		this.aimP = dele.setNowPlayedCardHide(this.tweenCardVO.id);
		this.tweening = true;
		this.doTween();
		dele.refreshOwnCard(); //会让readyCard重新被addchild到pagelist中
	}

	private readyCardRender:CardRender;
	private tweenCardVO:CardVO;
	private tweenStartX:number;
	private tweenStartY:number;
	private aimP:egret.Point;
	private tweening:boolean;
	private touchBeginHandler(evt:egret.TouchEvent):void
	{
		if(this.tweening) return;
		this.readyCardRender = evt.target as CardRender;
		if(GlobalDefine.herovo.roleId != this.model.whoPlayedCard && this.model.currentIndex == this.model.mainDir && this.readyCardRender instanceof CardRender && this.readyCardRender.data.position == this.model.mainDir){
			LayerManage._instance.panelLayer.addChild(this.readyCardRender);
			this.readyCardRender.updateXY(evt.stageX-40,evt.stageY-60);
		}else{
			this.readyCardRender = null;
		}
	}
	private touchMoveHandler(evt:egret.TouchEvent):void
	{
		if(this.readyCardRender){
			this.readyCardRender.updateXY(evt.stageX-40,evt.stageY-60);
		}
	}
	/**
	 * 自己出的牌
	 */
	private ownTweenStartX:number;
	private ownTweenStartY:number;
	public touchEndHandler(evt:egret.TouchEvent):void
	{
		if(!this.readyCardRender){
			return;
		}
		var vo:CardVO = this.readyCardRender.data;
		let mainDele:PlayerBaseRender = this.deleObj[vo.position];
		if(this.readyCardRender.y < 500) //出牌了
		{
			//send(); 向服务器请求后出牌  这里为模拟
			this.ownTweenStartX = this.readyCardRender.x+30;
			this.ownTweenStartY = this.readyCardRender.y+10;
			SendOperate.instance.requestPlayCard(vo.id);
			//this.startPlayCard(vo);
			/*var hvo:HeroVO = this.mainDele.data;
			var list:any[] = hvo.ownCardList;
			var temp:CardVO;
			for (var key in list) {
				temp = list[key];
				if(temp.id == vo.id){
					list.splice(list.indexOf(temp),1);
					hvo.playedCardList.push(temp);
					break;
				}
			}
			this.tweenCardVO = temp;
			//this.mainDele.refreshPlayedCard();
			this.aimP = this.mainDele.setNowPlayedCardHide(this.tweenCardVO.id);
			this.tweenStartX = this.readyCardRender.x+30;
			this.tweenStartY = this.readyCardRender.y+10;
			this.tweening = true;
			this.doTween();
			this.mainDele.refreshOwnCard(); //会让readyCard重新被addchild到pagelist中*/
		}
		else
		{ //没有完全出牌，返回牌面
			//DisplayUtil.removeDisplay(this.readyCard);
			mainDele.refreshOwnCard();
		}
		this.readyCardRender = null;
		
	}

	private doTween():void
	{
		var tc:TweenCard = new TweenCard();
		tc.data = this.tweenCardVO;	
		LayerManage.instance.panelLayer.addChild(tc);
		tc.updateXY(this.tweenStartX,this.tweenStartY);
		tc.scaleX = 0.65;
		tc.scaleY = 0.65;
		let t1:number = this.getDuration(this.tweenStartX,this.tweenStartY,597,317)+10;
		let t2:number = this.getDuration(597,317,this.aimP.x,this.aimP.y)+10;
		egret.Tween.get(tc)
			.to({x:597,y:317},t1)
			.to({scaleX:1.2,scaleY:1.2},500)
			.to({x:this.aimP.x,y:this.aimP.y,scaleX:0.65,scaleY:0.65},t2)
			.call(this.tweenEnd,this,[tc])
	}
	private tweenEnd(tc:TweenCard):void
	{
		this.tweening = false;
		egret.Tween.removeTweens(tc);
		DisplayUtil.removeDisplay(tc);
		let dele:PlayerBaseRender = this.deleObj[this.tweenCardVO.position];
		dele.setNowPlayedCardShow(this.tweenCardVO.id);
	}

	private getDuration(sx:number,sy:number,ex:number,ey:number):number
	{
		var gx:number = ex - sx;
		var gy:number = ey - sy;
		return Math.abs(Math.sqrt(gx*gx+gy*gy));
	}

	/**
	 * 这玩意每次都是发所有人数据,每次先把现有的render移除掉
	 */
	private updatePlayer():void
	{
		let m:number = this.model.mainDir;
		var msg:Object = this.model.getHeromsg();
		var vo:HeroVO;
		let pbr:PlayerBaseRender;

		this.pizi.data = null;
		this.laizi.data = null;

		for (var key in this.deleObj) {
			pbr = this.deleObj[key];
			DisplayUtil.removeDisplay(pbr);
			pbr = null;
			delete this.deleObj[key];
		}
		
		for (var key in msg) {
			vo = msg[key];
			if(this.deleObj[vo.dir]){
				continue;
			}
			if(vo.roleId == GlobalDefine.herovo.roleId){
				pbr = new PlayerUnderRender();
				this.addChild(pbr);
				pbr.updateXY(30,436);
				pbr.data = vo;	
				this.dirUI.setDirection(vo.dir);
			}
			else if(this.model.checkIsRight(m,vo.dir)){
				pbr = new PlayerRightRender();
				this.addChild(pbr);
				pbr.updateXY(1050,107);
				pbr.data = vo;	
			}
			else if(this.model.checkIsTop(m,vo.dir))
			{
				pbr = new PlayerTopRender();
				this.addChild(pbr);
				pbr.updateXY(302,61);
				pbr.data = vo;	
			}
			else if(this.model.checkIsLeft(m,vo.dir))
			{
				pbr = new PlayerLeftRender();
				this.addChild(pbr);
				pbr.updateXY(17,85);
				pbr.data = vo;
			}
			this.deleObj[vo.dir] = pbr;
		}

		this.state.state = 'wait';

	}
}