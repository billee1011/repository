var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var TSocket = (function (_super) {
    __extends(TSocket, _super);
    function TSocket() {
        var _this = _super.call(this) || this;
        _this.init();
        return _this;
    }
    TSocket.prototype.init = function () {
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
        this.socket.connect("192.168.1.49", 30052); // //
        // this.socket.connect("123.59.117.237",9999); // /外网
        //this.socket.connect("echo.websocket.org",80); // //
        // egret.log('aaaa');
    };
    /* public tryConnect(vo:ServerVO):void
     {
         this.socket.connect(vo.serverip, vo.port); // //192.168.0.104", 8088
     }*/
    TSocket.prototype.tryConnect = function (obj) {
        obj[0].socket.connect("192.168.1.49", 30052); // //192.168.0.104", 8088
    };
    /**
     * protocol 协议号
     */
    TSocket.prototype.sendData = function (protocol, data) {
        if (data === void 0) { data = null; }
        /*if(!GlobalDefine.WEB_SOCKE_COLLECTED){
            //Tnotice.instance.popUpTip('服务器断开连接');
            return;
        }*/
        if (!(data instanceof Array)) {
            data = [data];
        }
        //创建 ByteArray 对象
        var byte = new egret.ByteArray();
        var ss = data ? JSON.stringify(data) : '';
        var a;
        if (ss == '') {
            a = "{\"id\":" + protocol + "}";
        }
        else {
            a = "{\"id\":" + protocol + ",\"data\":" + ss + "}";
        }
        var temp = new egret.ByteArray();
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
    };
    TSocket.prototype.onSocketOpen = function () {
        this.trace("WebSocketOpen");
        Tnotice.instance.popUpTip('连接成功');
        GlobalDefine.WEB_SOCKE_COLLECTED = true;
        TFacade.facade.simpleDispatcher(ConstDefine.SERVER_CONNECT_SUCC);
        clearInterval(this.tryIndex);
        if (GlobalDefine.wxCode) {
            SendOperate.instance.requestLogin();
        }
        // this.sendData(1000);
        // this.sendData(111,null);
        // egret.setInterval(this.sentTest,this,10);
    };
    TSocket.prototype.sentTest = function () {
        // var a:number = 1000;
        // for()
        // this.sendData(1000,[1111111111111111,211111111111,311111111111111,41111111111,51111111111111])
    };
    TSocket.prototype.onSocketClose = function () {
        this.trace("WebSocketClose");
        Tnotice.instance.popUpTip('服务器断开连接');
        GlobalDefine.WEB_SOCKE_COLLECTED = false;
        clearInterval(this.tryIndex);
        this.tryIndex = setInterval(this.tryConnect, 1000, [this]);
    };
    TSocket.prototype.onSocketError = function () {
        this.trace("WebSocketError");
    };
    TSocket.prototype.onReceiveMessage = function (e) {
        var byte = new egret.ByteArray();
        this.socket.readBytes(byte);
        //包头
        byte.readInt();
        //先干掉长度
        byte.readInt(); ////
        var protocal = byte.readInt(); ///协议号///
        var str = byte.readUTFBytes(byte.bytesAvailable);
        var data = JSON.parse(str);
        var list = TFacade.facade.getProtocolFun(protocal);
        var temp;
        if (data.result == 1) {
            temp = data.data;
            temp.unshift(1);
        }
        else {
            temp = [0, data.code];
        }
        list[0].call(list[1], temp);
    };
    TSocket.prototype.simulateReceiveMsg = function (protocol, data) {
        var list = TFacade.facade.getProtocolFun(protocol);
        list[0].call(list[1], data);
    };
    TSocket.prototype.trace = function (msg) {
        console.log(msg);
    };
    return TSocket;
}(egret.EventDispatcher));
__reflect(TSocket.prototype, "TSocket");
//# sourceMappingURL=TSocket.js.map