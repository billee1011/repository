class ToastManager {
	public static _instance:ToastManager;

	public static get instance():ToastManager
	{
		if(!this._instance){
			this._instance = new ToastManager();
		}
		return this._instance;
	}
	public constructor() {
		this._cont = LayerManage.instance.tipLayer;
	}
	public tipList:Array<any> = [];
	private _cont:egret.Sprite;
	public showTips(msg:string):void
	{
		let toast:Toast = new Toast( msg);
		this._cont.addChild( toast );
		this.tipList.push(toast);
		toast.end = false;
		//egret.log('----pre----',toast.hashCode);
		egret.Tween.get( toast )
            .to( { y:toast.y-100 }, 400/*, egret.Ease.quintOut*/ )
			.call(this.arriveDestination,this,[toast])
            /*.wait( 800 )
            .to( { alpha: 0 }, 100 , egret.Ease.quintIn ).call( ()=>{      
         	  DisplayUtil.removeDisplay(toast);
			  this.tipList.shift();
        } )*/;
	}
	private diY:number = GlobalDefine.stageH*.618-100;
	private arriveDestination(tt:Toast):void
	{
	//	egret.log('----back----',tt.hashCode);
		tt.end = true;
		let temp:number = this.diY;
		let toast:Toast;
		for(let i:number = this.tipList.length-1 ; i>=0; i--){
			toast = this.tipList[i];
			//egret.log('%%%',toast.hashCode,toast.end);
			if(!toast.end) continue;
			
			toast.y = temp;
			temp -= toast.height;
			//toast.end = false;
			//egret.log(toast.hashCode,'----------',temp,'toast.y=',toast.y);

		}
		//egret.log('-----',tt.aimY);
		egret.Tween.get( tt )
            .to( { alpha: 0}, 2200 , egret.Ease.quintIn ).call( ()=>{      
         	  DisplayUtil.removeDisplay(tt);
			  this.tipList.shift();
        } );
	}

	/*public static launch( msg:string ):void{
        if( this._cont ){
            var toast:Toast = new Toast( msg, this._cont.stage.stageWidth, this._cont.stage.stageHeight );
            this._cont.addChild( toast );
        }
    }*/
}