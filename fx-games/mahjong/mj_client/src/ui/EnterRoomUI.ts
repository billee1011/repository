class EnterRoomUI extends UIBase{
	public static NAME:string = 'EnterRoomUI';
	public constructor() {
		super('resource/UI_exml/EnterRoom.exml','enterRoom');

		this.isAloneShow = true;
		this.closeOther = false;
	}
	private label_num:eui.Label;
	private numDatas:any[];
	private btn_close:eui.Button;
	private model:MjModel;
	protected bindEvent():void
	{
		this.model = TFacade.getProxy(MjModel.NAME);
	}

	protected uiLoadComplete():void
	{
		let img:eui.Image;

		img = new eui.Image;
		img.source = PathDefine.UI_IMAGE+'frameBarShort.png';
		this.addChild(img);
		img.x = 150;
		img.y = 15;

		img = new eui.Image();
		img.source = this.getTexture('enterRoomTitle');
		this.addChild(img);
		img.x = 274;
		img.y = 19;

		let i:number = 0;
		for(i=1; i<=12; i++)
		{
			img = new eui.Image();
			if(i <= 9){
				img.source = this.getTexture('btn_num_'+i);
				img.name = i.toString();
			}
			else if(i == 10){
				img.source = this.getTexture('btn_reset');
				img.name = '100'//重置
			}
			else if(i == 11){
				img.source = this.getTexture('btn_num_0');
				img.name = '0';
			}
			else {
				img.source = this.getTexture('btn_del');
				img.name = '90'; //删除
			}
			img.x = (i-1)%3 * 193 + 66;
			img.y = Math.floor((i-1)/3) * 65 + 159;
			this.addChild(img);
			img.addEventListener(egret.TouchEvent.TOUCH_TAP,this.operateHandler,this);
			
		}
		this.numDatas = [];
		this.btn_close.addEventListener(egret.TouchEvent.TOUCH_TAP,this.hide,this);

	}
	private operateHandler(evt:egret.TouchEvent):void
	{
		let img:eui.Image = evt.currentTarget as eui.Image;
		let value:number = parseInt(img.name);
		if(value >= 0 && value <= 9){
			if(this.numDatas.length >= 6){
				return;
			}
			this.numDatas.push(value);
		}
		else if(value == 90){ //删除
			this.numDatas.pop();
		}else if(value == 100){
			this.numDatas.length = 0;
		}
		else{
			return;
		}
		this.updateLabel();
	}
	private updateLabel():void
	{
		let label:eui.Label;
		let value:string = this.numDatas.join('');
		this.label_num.text = value;
		if(this.numDatas.length >= 6){
			this.checkPassword(value);
		}
	}
	private checkPassword(v:string):void
	{
		var me:number = parseInt(v);
		SendOperate.instance.requestEnterRoom(me);
		//this.hide();
	}

	private getTexture(name:string):egret.Texture
	{
		return SheetManage.getTextureFromSheet(name,'mj_enterroom_json');
	}

	public sleep():void
	{
		this.numDatas.length = 0;
		this.updateLabel();
	}
}