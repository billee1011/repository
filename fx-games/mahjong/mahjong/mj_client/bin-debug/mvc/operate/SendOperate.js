var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var SendOperate = (function () {
    function SendOperate() {
        this.socket = GlobalDefine.socket;
    }
    Object.defineProperty(SendOperate, "instance", {
        get: function () {
            if (!this._instance) {
                this._instance = new SendOperate();
            }
            return this._instance;
        },
        enumerable: true,
        configurable: true
    });
    /**
     * 10003  登陆游戏
        C -> S
        [loginType	Int	登录类型	1：微信登录，2: 游客登录
        pid	string	平台ID
        uid	string		客户端有缓存，就传userId过来，没有缓存传""
        machingId	string	设备码
        code	string		只用作微信登陆并且没有userId没有缓存，游客登陆传""
        ]
     *
     */
    SendOperate.prototype.requestLogin = function () {
        var t = GlobalDefine.shebeima ? GlobalDefine.shebeima : 'fd' + Math.floor(Math.random() * 100000);
        this.socket.sendData(10003, [2, '', '', t, '']);
    };
    /**
     * (10007)创建房间
        c->s
            [
            jushu, 局数
            costType, 1：房主付费 2：AA付费
            ]
        s->c
            失败=[0,errorcode]
            成功=[1,roomNum]
     */
    SendOperate.prototype.requestCreateRoom = function (ju, costType) {
        this.socket.sendData(10007, [ju, costType]);
    };
    /**
     * 10008 加入房间
        c->s
            roomNum
     */
    SendOperate.prototype.requestEnterRoom = function (roomId) {
        this.socket.sendData(10008, roomId);
    };
    SendOperate.prototype.requestPlayCard = function (id) {
        this.socket.sendData(10011, id);
    };
    /**
     * 碰=1  暗杠=2  明杠=3  胡=4  自摸=5
     */
    SendOperate.prototype.requestOperate = function (type) {
        this.socket.sendData(10013, type);
    };
    /**
     * 吃
     */
    SendOperate.prototype.requestEatCard = function (arr) {
        this.socket.sendData(10022, arr);
    };
    SendOperate.prototype.requestJiesanRoom = function () {
        this.socket.sendData(10014);
    };
    SendOperate.prototype.requestExitRoom = function () {
        this.socket.sendData(10015);
    };
    SendOperate.prototype.requestStartAgainGame = function () {
        this.socket.sendData(10017);
    };
    return SendOperate;
}());
__reflect(SendOperate.prototype, "SendOperate");
//# sourceMappingURL=SendOperate.js.map