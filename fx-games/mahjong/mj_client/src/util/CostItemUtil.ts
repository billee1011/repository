/**
 * 道具消耗用这个
 */
class CostItemUtil extends egret.DisplayObjectContainer{
	private gslot:NoRareSlot;
	private label_value:eui.Label;
	private size:number;
	/**
	 * 
	 *
	 */
	public constructor(iconW:number=35,fontSize:number=20,fontColor:number=0,fontStroke:number=0) {
		super();
		this.size = iconW;
		this.gslot = new NoRareSlot(iconW,iconW);
		this.addChild(this.gslot);
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

	}

	public set data(ivo:ItemVO)
	{
		this.gslot.data = ivo;
	}

	public set value(v:number)
	{
		this.label_value.text = v.toString();
		this.label_value.y = this.size - this.label_value.height-5;
	}

	public updateXY(xx:number,yy:number):void
	{
		this.x = xx;
		this.y = yy;
	}

}