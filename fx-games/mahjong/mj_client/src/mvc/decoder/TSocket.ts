class TSocket extends egret.EventDispatcher{
	public constructor() {
		super();
		this.init();
	}
	private socket:egret.WebSocket;

	private init():void
	{
		  //创建 WebSocket 对象
        this.socket = new egret.WebSocket();
        //设置数据格式为二进制，默认为字符串
        this.socket.type = egret.WebSocket.TYPE_BINARY;
        //添加收到数据侦听，收到数据会调用此方法
        this.socket.addEventListener(egret.ProgressEvent.SOCKET_DATA, this.onReceiveMessage, this);
        //添加链接打开侦听，连接成功会调用此方法
        this.socket.addEventListener(egret.Event.CONNECT, this.onSocketOpen, this);
        //添加链接关闭侦听，手动关闭或者服务器关闭连接会调用此方法
        this.socket.addEventListener(egret.Event.CLOSE, this.onSocketClose, this);
        //添加异常侦听，出现异常会调用此方法
        this.socket.addEventListener(egret.IOErrorEvent.IO_ERROR, this.onSocketError, this);
        //连接服务器
       this.socket.connect("192.168.1.49",30052); // //
       // this.socket.connect("123.59.117.237",9999); // /外网
        //this.socket.connect("echo.websocket.org",80); // //
       // egret.log('aaaa');

	}
   /* public tryConnect(vo:ServerVO):void
    {
        this.socket.connect(vo.serverip, vo.port); // //192.168.0.104", 8088
    }*/
    public tryConnect(obj:any):void
    {
        obj[0].socket.connect("192.168.1.49",30052); // //192.168.0.104", 8088
    }
    /**
     * protocol 协议号
     */
	  public sendData(protocol:number,data:any=null):void {
        /*if(!GlobalDefine.WEB_SOCKE_COLLECTED){
            //Tnotice.instance.popUpTip('服务器断开连接');
            return;
        }*/
        if(!(data instanceof Array)){
            data = [data];
        }
        //创建 ByteArray 对象
       var byte:egret.ByteArray = new egret.ByteArray();
        var ss:string = data ? JSON.stringify(data) : '';
        var a:string;
        if( ss == ''){
            a = `{"id":${protocol}}`;
        }else{
            a = `{"id":${protocol},"data":${ss}}`;
        }

        var temp:egret.ByteArray = new egret.ByteArray();
        temp.writeUTFBytes(a);
        byte.writeInt(4165656);
        byte.writeInt(temp.length);
        byte.writeUTFBytes(a);
      /*  byte.position = 0;
        console.log(byte.readInt());
        console.log(byte.readInt());
        console.log(byte.readUTFBytes(byte.bytesAvailable));*/
        byte.position = 0;
        //发送数据
        this.socket.writeBytes(byte);
        this.socket.flush();
        
    }

    private onSocketOpen():void {
        this.trace("WebSocketOpen");
        Tnotice.instance.popUpTip('连接成功');
        GlobalDefine.WEB_SOCKE_COLLECTED = true;
        TFacade.facade.simpleDispatcher(ConstDefine.SERVER_CONNECT_SUCC);
        clearInterval(this.tryIndex);

        if(GlobalDefine.wxCode){
            SendOperate.instance.requestLogin();
        }

       // this.sendData(1000);
       // this.sendData(111,null);
      // egret.setInterval(this.sentTest,this,10);
    }
    private sentTest():void
    {
       // var a:number = 1000;
       // for()
      // this.sendData(1000,[1111111111111111,211111111111,311111111111111,41111111111,51111111111111])
    }
    private tryIndex:number;
    private onSocketClose():void {
        this.trace("WebSocketClose");
        Tnotice.instance.popUpTip('服务器断开连接');
        GlobalDefine.WEB_SOCKE_COLLECTED = false;
        clearInterval(this.tryIndex);
        this.tryIndex = setInterval(this.tryConnect,1000,[this]);
    }
    private onSocketError():void {
        this.trace("WebSocketError");
    }

    private onReceiveMessage(e:egret.Event):void {
        var byte:egret.ByteArray = new egret.ByteArray();

        this.socket.readBytes(byte);
        //包头
        byte.readInt();
        //先干掉长度
        byte.readInt();////
       let protocal:number = byte.readInt(); ///协议号///
        var str:string = byte.readUTFBytes(byte.bytesAvailable);
        var data:any = JSON.parse(str);
        var list:Array<any> = TFacade.facade.getProtocolFun(protocal);
        var temp:any;
        if(data.result == 1){ //成功
            temp = data.data;
            temp.unshift(1);
        }else{ //失败
            temp = [0,data.code];
        }
        list[0].call(list[1],temp)
    }

    public simulateReceiveMsg(protocol:number,data:any[]):void
    {
         var list:Array<any> = TFacade.facade.getProtocolFun(protocol);
        list[0].call(list[1],data)
    }


    private trace(msg:any):void {
        console.log(msg);
    }
}