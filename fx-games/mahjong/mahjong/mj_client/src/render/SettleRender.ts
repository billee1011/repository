class SettleRender extends RenderBase{
	public constructor() {
		super('resource/UI_exml/SettleItem.exml');
	}
	private label_name:eui.Label;
	private label_style:eui.Label;
	private label_id:eui.Label;
	private model:MjModel;
	private list:HPagelist;
	private label_win:AniLabel;
	private label_lose:AniLabel;
	 protected uiLoadComplete(evt:eui.UIEvent=null):void
   	 {

		let bg:AsyImage = new AsyImage();
		this.addChildAt(bg,0);
		bg.setUrl(PathDefine.UI_IMAGE+'settle_bar.png');

		this.list = new HPagelist(SettleCardRender,36,84,false);
		this.list.updateXY(118,45)
		this.addChild(this.list);
        //override
		this.model = TFacade.getProxy(MjModel.NAME);

		this.label_win = new AniLabel('mj_settleWinFont_fnt',-2);
		this.addChild(this.label_win);
		
		this.label_lose = new AniLabel('mj_settleLoseFont_fnt',-2);
		this.addChild(this.label_lose);
   	 }

    protected dataChanged(): void{
		var vo:SettleVO = this.data;
		var hvo:HeroVO = this.model.playerData[vo.roleId];
		if(hvo){
			this.label_name.text = hvo.name;
			this.label_id.text = hvo.roleId.toString();;
		}
		this.list.displayList(vo.cardList);


		var temp:AniLabel;
		this.label_lose.visible = vo.curScore < 0;
		this.label_win.visible = vo.curScore >= 0;
		var s:string;
		if(vo.curScore >= 0){
			temp = this.label_win;
			s = '+';
		}else{
			temp = this.label_lose;
			s = '-';
		}
		temp.text = s + Math.abs(vo.curScore);
		temp.x = 776
		temp.y = 35;
     }

}