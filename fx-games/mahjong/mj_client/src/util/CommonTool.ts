
class CommonTool
{
    public constructor(){

    }
    public static htmlP:egret.HtmlTextParser = new egret.HtmlTextParser();
    /**
     * keepNum 保留几位小数
     */
    public static getPercentNum(value:number,keepNum:number=0):string
    {
        var k:number = Math.pow(10,keepNum)
        var t:number = value * k * 100;
        t = Math.round(t);
        return t/k + '%';
    }
    /**
     * 保留小数
     */
    public static getKeepNum(value:number,keepNum:number=0):number
    {
        var k:number = Math.pow(10,keepNum)
        var t:number = value * k;
        t = Math.round(t);
        return t/k;
    }
    /**
     * 大于万显示 xx万
     */
    public static getMoneyShow(value:number):string
    {
        var vv:number = Math.floor(value/10000);
        if(vv > 0){
            return vv + '万';
        }
        return value.toString();
    }


    public static addStroke(tx:egret.TextField,width:number=2,color:number=0):void
    {
        tx.stroke = width;
        tx.strokeColor = color;
    }


    public static setColorText(label:eui.Label,ivo:ItemVO):void
    {
        label.textColor = ColorDefine.rareColr[ivo.quality];
        label.text = ivo.name;
    }
    public static getColorTextByRare(content:string,quality:number):string
    {
        var c:string = ColorDefine.rareHtmlColor[quality];
        return `<font color=${c}>${content}</font>`;
    }
    /**
     * color: htmlColor
     */
    public static getColorText(content:string,color:string):string
    {
        return `<font color=${color}>${content}</font>`;
    }
   
    /**
     * 格式 'xxxx{0},xxx{1}',333,666
     */
    public static replaceStr(...arg):string
    {
        var s:string = arg.shift();
        for (var key in arg)
        {
            var value:any = arg[key];
            s = s.replace(/\{\d+\}/,value);
        }
        return s;
    }
    /**
     * 返回html 的 'xxxx{0},xxx{1}',333,666
     */
    public static replaceStrBackColor(...arg):any
    {
        var s:string = arg.shift();
        for (var key in arg)
        {
            var value:any = arg[key];
            s = s.replace(/\{\d+\}/,value);
        }
        return CommonTool.htmlP.parser(s);
    }

    /**
     * 跳转UI
     */
    public static skipToUI(ui:string):void
    {
        if(!ui) return;
        var list:Array<any> = ui.split(':');
        if(list.length == 1){
            TFacade.toggleUI(list[0]);
        }else{
            TFacade.toggleUI(list[0]).execute(list[1]);
        }
    }


    public static grayFilter:egret.ColorMatrixFilter = new egret.ColorMatrixFilter([
                                                                                    0.3,0.6,0,0,0,
                                                                                    0.3,0.6,0,0,0,
                                                                                    0.3,0.6,0,0,0,
                                                                                    0,0,0,1,0
                                                                                    ]
    );
}