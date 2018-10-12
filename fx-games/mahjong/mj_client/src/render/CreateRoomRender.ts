class CreateRoomRender extends RenderBase{
	public constructor() {
		super('');
	}
	
	protected uiLoadComplete():void
	{

	}
	/**
	 * data ['xxxxx',value]
	 */
	protected dataChanged(): void{
		var vs:any[] = this.data;
		this.ck = new CRSelectBox(vs[0]);
		this.addChild(this.ck);
		if(vs[2]){
			var img:eui.Image = new eui.Image;
			img.x = 96;
			img.y = -6;
			img.source = SheetManage.getTextureFromSheet('main_ka','mj_mainFrame_json');
			this.addChild(img);
		}
     }
	 private ck:CRSelectBox;
	 protected doChoose():void
	 {
		this.ck.gou.visible = this.choosed;
	 }

}