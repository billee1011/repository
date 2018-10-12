class PlayerUnderRender extends PlayerBaseRender{
	public constructor() {
		super();
	}
	private horNum:number;
	protected childInit():void
	{
		this.horNum = 13;
		this.ownCardList = new HPagelist(CardRender,83,125,false);
		this.ownCardList.updateXY(23,141);
		this.addChild(this.ownCardList);

		this.duiList = new HPagelist(DuiCardRender,55,84,false);
		this.duiList.updateXY(30,177)
		this.addChild(this.duiList);

		this.playedList = new HPagelist(PlayedCardRender,36,43,true,this.horNum);
		this.playedList.layoutDirY = -1;
		this.playedList.updateXY(346,68);
		this.addChild(this.playedList);

		this.head.readyPic.x = 125;
		this.head.readyPic.y = 53;

		this.opEffectItem.updateXY(575,80);

		this.head.updateXY(0,-15)
	}
	protected layout():void
	{
		this.refreshOwnCardPosition();
	}

	protected refreshOwnCardPosition():void
	{
		if(this.duiList){
			this.ownCardList.x = this.vo.duiCardList ? this.vo.duiCardList.length*55 + 43 : 23;
		}else{
			this.ownCardList.x = 23;
		}
	}
}