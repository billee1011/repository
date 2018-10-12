class ColorDefine {
	public static rareYinshe:Object = {99:'weigoumai',0:'rare_hui',1:'rare_bai',2:'rare_lv',3:'rare_lan',4:'rare_zi',5:'rare_cheng',6:'rare_hong'};
	public static rareColr:Object = {0:0xbbbbbb,1:0xFFFFFF,2:0x00FF00,3:0x26aff5,4:0xb049ff,5:0xff8a00,6:0xFF0000};
	public static rareHtmlColor:Object = {0:'#bbbbbb',1:'#FFFFFF',2:'#00FF00',3:'#26aff5',4:'#b049ff',5:'#ff8a00',6:'#FF0000'};
	public static getRareIcon(rare:number):string
	{
		return this.rareYinshe[rare];
	}

	public static getColorByQuality(quailty:number):number
	{
		return this.rareColr[quailty];
	}
}