class PlayerLeftRender extends PlayerBaseRender{
	public constructor() {
		super();
	}
	private horNum:number;
	protected childInit():void
	{
		this.horNum = 12;
		this.ownCardList = new HPagelist(CardRender,24,30,true);
		this.ownCardList.updateXY(166,13);
		this.addChild(this.ownCardList);

		this.duiList = new HPagelist(DuiCardRender,49,27,true);
		this.duiList.updateXY(154,14)
		this.addChild(this.duiList);

		this.playedList = new HPagelist(PlayedCardRender,49,27,false,this.horNum);
		this.playedList.updateXY(222,90);
		this.addChild(this.playedList);

		this.head.updateXY(14,78);
		this.head.readyPic.x = 120;
		this.head.readyPic.y = 52;

		this.opEffectItem.updateXY(250,232);
	}
	protected layout():void
	{
		
		this.refreshOwnCardPosition();
	}

	protected refreshOwnCardPosition():void
	{
		if(this.duiList){
			this.ownCardList.y = this.vo.duiCardList ? this.vo.duiCardList.length*27 + 28 : 13;
		}else{
			this.ownCardList.y = 13;
		}
	}

}