/**
 * Created by egret on 2016/1/26.
 */
class Toast extends egret.DisplayObjectContainer{
    
    public static init(txtrToastBg:egret.Texture ):void{
      //  console.log( "Toast.init", txtrToastBg );
        this._txtrToastBg = txtrToastBg;
    }
    
    public end:boolean;
    public aimY:number=0;

    private static _txtrToastBg:egret.Texture;
    private static _cont:egret.Sprite;
    
    constructor ( msg:string){
        super();
        
        console.log( "Toast:", msg );
        let w:number = GlobalDefine.stageW;
        let h:number = GlobalDefine.stageH;
        var bg:egret.Bitmap = new egret.Bitmap( Toast._txtrToastBg );
        this.addChild( bg );
        
        var tx:egret.TextField = new egret.TextField;
        tx.multiline = true;
        tx.size = 30;
        tx.bold = true;
        tx.textColor = 0xFFFFFF;
        tx.stroke = 2;
        tx.strokeColor = 0;
        tx.text = msg;
        tx.fontFamily = "微软雅黑";
        tx.textAlign = egret.HorizontalAlign.CENTER;
        tx.width = w * .84;
        tx.x = ( Toast._txtrToastBg.textureWidth - tx.width ) / 2;
        tx.y = 6;
        this.addChild( tx );
        
        bg.height = 12 + tx.height;

      //  this.anchorOffsetX = this.width * .5;
        //this.anchorOffsetY = this.height * .5;
        this.x = w * .5 - bg.width*.5 ;
        this.y = h * .618;
        
        this.alpha = 1;
}
}