/**
 * 创建房间界面使用
 */
class CRSelectBox extends egret.Sprite{
	private _select:boolean;
	public gou:eui.Image;
	public data:any;
	public static CHANGE:string = 'crbox_change';
	public constructor(text:string) {
		super();
		let di:eui.Image = new eui.Image();
		this.addChild(di);
		di.source = SheetManage.getTextureFromSheet('create_noselect','mj_createRoom_json');
		
		this.gou = new eui.Image();
		this.addChild(this.gou);
		this.gou.source = SheetManage.getTextureFromSheet('create_select','mj_createRoom_json');
		this.gou.visible  = false;

		let label:eui.Label = new eui.Label();
		label.size = 26;
		label.textColor = 0x8a6053;
		this.addChild(label);
		label.fontFamily = 'Microsoft YaHei';
		label.text = text;
		label.x = 42;
		label.y = 30 - label.textHeight;

		//this.addEventListener(egret.TouchEvent.TOUCH_TAP,this.touchHandler,this);
	}

	/*private touchHandler(evt:egret.TouchEvent):void
	{
		this.select = !this._select;
	}
*/
	public set select(b:boolean)
	{
		this._select = b;
		this.gou.visible = b;
	}
	

	public get select():boolean
	{
		return this._select;
	}
}
