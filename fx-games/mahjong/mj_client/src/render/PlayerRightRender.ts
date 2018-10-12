class PlayerRightRender extends PlayerBaseRender{
	public constructor() {
		super();
	}
	private topNum:number;
	protected childInit():void
	{
		this.topNum = 12;
		this.ownCardList = new HPagelist(CardRender,24,30,true);
		this.ownCardList.updateXY(52,15);
		//this.ownCardList.downUp = true;
		this.addChild(this.ownCardList);

		this.duiList = new HPagelist(DuiCardRender,49,27,true);
		this.duiList.updateXY(38,448)
		this.addChild(this.duiList);

		this.playedList = new HPagelist(PlayedCardRender,49,27,false,this.topNum);
		this.playedList.layoutDirX = -1;
		this.playedList.downUp = true;
		this.playedList.updateXY(-31,81);
		this.addChild(this.playedList);

		this.head.readyPic.x = -38;
		this.head.readyPic.y = 62;

		this.opEffectItem.updateXY(-86,227);
		this.head.updateXY(90,44);
	}
	protected layout():void
	{
		
	/*	var t:number = Math.min(this.topNum,this.vo.playedCardList?this.vo.playedCardList.length:0)
		var len:number = t*27;
		this.playedList.y = 441-len;*/

		

		this.refreshDuiCardLocation();
	}

	public refreshOwnCard():void
	{
		if(this.ownCardList){
			this.ownCardList.displayList(this.vo.ownCardList);
			this.ownCardList.refreshOther();

			this.refreshOwnCardPosition();	
		}
	}

	protected refreshDuiCardLocation():void
	{
		let len:number = this.vo.duiCardList ? this.vo.duiCardList.length*27:0;
		this.duiList.y = 458 - len;

		this.refreshOwnCardPosition();
	}
	/**
	 * override
	 */
	protected refreshOwnCardPosition():void
	{
		let len:number = this.vo.ownCardList ? this.vo.ownCardList.length*30:0;
		this.ownCardList.y = this.duiList.y - 30 - len;
	}


	public addVoToOwnCardList(vo:CardVO):void
	{
		this.vo.ownCardList.unshift(vo);
	}
	public addVoToduiCardList(vo:CardVO):void
	{
		this.vo.duiCardList.unshift(vo);
	}

	public refreshPlayedCard():void
	{
		if(this.playedList){
			//var temp:any[] = this.vo.playedCardList.concat();
			//temp.reverse();
			this.playedList.displayList(this.vo.playedCardList);
			/*var t:number = Math.min(this.topNum,this.vo.playedCardList?this.vo.playedCardList.length:0)
			var len:number = t*27;
			this.playedList.y = 441-len;*/
		}
	}
	/***
	 * 
	 */
	/*public addVoToPlayedCardList(vo:CardVO):void
	{
		this.vo.playedCardList.push(vo);
	}*/
}