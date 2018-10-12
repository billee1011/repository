/**
 * 元宝等虚拟货币用这个
 */
class CostUtil extends egret.DisplayObjectContainer{
	private image_icon:eui.Image;
	private label_value:eui.Label;
	private size:number;
	/**
	 * type 0:元宝 1：金币 2:寻宝陨石
	 *
	 */
	public constructor(type:number=0,iconW:number=25,fontSize:number=20,fontColor:number=0,fontStroke:number=0) {
		super();
		this.size = iconW;
		this.image_icon = new eui.Image();
		this.addChild(this.image_icon);
		this.image_icon.width = this.image_icon.height = this.size;

		this.label_value = new eui.Label();
		this.addChild(this.label_value);
		this.label_value.size = fontSize;
		this.label_value.x = iconW + 2;
		if(fontColor){
			this.label_value.textColor = fontColor;
		}
		if(fontStroke){
			CommonTool.addStroke(this.label_value,1,fontStroke);
		}
		

		 ///this.image_icon.source = SheetManage.getCurrencyTexture(type);
	}

	public set type(value:number)
	{
		//this.image_icon.source = SheetManage.getCurrencyTexture(value);
	}

	public set value(v:number)
	{
		this.label_value.text = v.toString();
		this.label_value.y = this.size - this.label_value.height;
	}

	public updateXY(xx:number,yy:number):void
	{
		this.x = xx;
		this.y = yy;
	}

}