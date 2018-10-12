class MultiCountSelectUtil {

	private btn_add:eui.Button;
	private btn_addTen:eui.Button;
	private btn_cut:eui.Button;
	private btn_cutTen:eui.Button;
	private _opCount:number;
	private label_count:eui.Label;
	private min:number=0;
	private max:number=0;
	public constructor(add:any,addTen:any,cut:any,cutTen:any,label:any) {
		this.btn_add = add;
		this.btn_addTen = addTen;
		this.btn_cut = cut;
		this.btn_cutTen = cutTen;
		this.label_count = label;
		this.opCount = 1;

		this.btn_add.addEventListener(egret.TouchEvent.TOUCH_TAP,this.clickHandler,this);
		this.btn_addTen.addEventListener(egret.TouchEvent.TOUCH_TAP,this.clickHandler,this);
		this.btn_cut.addEventListener(egret.TouchEvent.TOUCH_TAP,this.clickHandler,this);
		this.btn_cutTen.addEventListener(egret.TouchEvent.TOUCH_TAP,this.clickHandler,this);
	}

	public setMinMax(min:number,max:number)
	{
		this.min = min;
		this.max = max;
		this.opCount = 1;
	}
	

	private clickHandler(evt:egret.TouchEvent):void
	{
		switch (evt.currentTarget) {
			case this.btn_add:
				this.opCount ++;
				break;
			case this.btn_addTen:
				this.opCount += 10;
				break;
			case this.btn_cut:
				this.opCount --;
				break;
			case this.btn_cutTen:
				this.opCount -= 10;
				break;
			default:
				break;
		}
	}

	public get opCount():number
	{
		return this._opCount;
	}

	public set opCount(vv:number)
	{
		vv = Math.max(vv,this.min);
		vv = Math.min(vv,this.max);
		this._opCount = vv;
		this.label_count.text = this._opCount.toString();
	}
}