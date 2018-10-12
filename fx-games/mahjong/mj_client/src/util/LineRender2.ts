/**
 * 属性+值
 */
class LineRender2 extends RenderBase{
	protected icon:AsyImage;
	protected label_name:eui.Label;
	protected label_value:eui.Label;
	public constructor(hasIcon:boolean=false) {
		super('');

		this.label_name = new eui.Label();
		this.label_name.size = 16;
		this.label_name.fontFamily = '微软雅黑';
		this.addChild(this.label_name);
		this.label_name.x = 0;
		this.label_name.y =6;
		this.label_name.textColor = 0xB68173;
		if(hasIcon){
			this.icon = new AsyImage(30,30)
			this.addChild(this.icon);
			this.label_name.x = 32;
		}

		this.label_value = new eui.Label();
		this.label_value.size = 16;
		this.label_name.fontFamily = '微软雅黑';
		this.label_value.textColor = 0xB68173;
		this.addChild(this.label_value);
		this.label_value.y = 6;


	}
	/**               
	 * data [ [[id,name,type,value]...]
	 */
	protected dataChanged(): void{
		var list:Array<any> = this.data;
		this.label_name.text = list[1]+':';
		this.label_value.text = list[3];	
		this.label_value.x = this.label_name.x + this.label_name.width+4;
     }
}