class RoleHeadRender extends RenderBase{
	public constructor() {
		super('resource/UI_exml/RoleItem.exml');
	}
	private label_name:eui.Label;
	private bg:AsyImage;
	private img_headBg:eui.Image;
	public readyPic:eui.Image;
	private label_id:eui.Label;
	protected uiLoadComplete():void
    {
		this.bg = new AsyImage(81,79);
		this.addChild(this.bg);
		this.bg.updateXY(25,11);
		this.addChild(this.label_name);

		 this.readyPic = new eui.Image();
	  this.readyPic.source = SheetManage.getTextureFromCenter('ready_sign');
	  this.addChild(this.readyPic);

	}
	private vo:HeroVO;
	protected dataChanged(): void
	{
		this.vo = this.data;
		this.bg.setUrl(PathDefine.UI_IMAGE+this.vo.head+'.png');
		this.label_name.text = this.vo.name;
		this.label_id.text = this.vo.roleId.toString();
	}
}