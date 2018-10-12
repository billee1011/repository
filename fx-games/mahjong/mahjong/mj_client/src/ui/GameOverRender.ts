class GameOverRender extends RenderBase{
	public constructor() {
		super('resource/UI_exml/GameOverItem.exml');
	}
	private list:HPagelist;
	private label_name:eui.Label;
	private label_id:eui.Label;
	private label_win:AniLabel;
	private label_lose:AniLabel;
	protected uiLoadComplete(evt:eui.UIEvent=null):void
    {
        let bg:AsyImage = new AsyImage();
		this.addChildAt(bg,0);
		bg.setUrl(PathDefine.UI_IMAGE+'settle_renderBg.png');

		this.list = new HPagelist(OverLabelRender,180,33);
		this.list.updateXY(35,122);
		this.addChild(this.list);

		this.label_win = new AniLabel('mj_settleWinFont_fnt',-2);
		this.addChild(this.label_win);
		
		this.label_lose = new AniLabel('mj_settleLoseFont_fnt',-2);
		this.addChild(this.label_lose);
    }

    protected dataChanged(): void{
		let vo:FinalSettleVO = this.data;
		this.label_id.text = vo.roleid.toString();
		this.label_name.text = vo.name;
		this.list.displayList(vo.countList);
		var temp:AniLabel;
		this.label_lose.visible = vo.score < 0;
		this.label_win.visible = vo.score >= 0;
		var s:string;
		if(vo.score >= 0){
			temp = this.label_win;
			s = '+';
		}else{
			temp = this.label_lose;
			s = '-';
		}
		temp.text = s + Math.abs(vo.score);
		temp.x = 110 - temp.width*.5;
		temp.y = 338;
    }
}