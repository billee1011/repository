var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var GameDecoder = (function (_super) {
    __extends(GameDecoder, _super);
    /**
     * //所有协议均有result，统一处理到data的第一位   不管是服务器推送还是请求后推送 如不用错误判断 则干掉第一位再操作
     */
    function GameDecoder() {
        var _this = _super.call(this) || this;
        _this.types = [10003, 10007, 10008, 10009, 10010, 10011, 10012, 10016, 10018,
            10013, 10014, 10015, 10017, 10019
        ];
        _this.model = TFacade.getProxy(MjModel.NAME);
        return _this;
    }
    /**
     *
     * 10003  登陆游戏
c->s
    loginType 登陆类型 1=微信登陆 2=游客登陆
    pid
    userId  客户端有缓存，就传userId过来，没有缓存传""
    machingId  机器的设备码 有唯一性
    code 只用作微信登陆并且没有userId没有缓存，游客登陆传""
s->c
    失败=[0,errorcode]
    成功=[
            1,
            nowTime, // 当前时间
            offset, // 当前时区
            [
                roleId,
                name,
                gender,
                diamond, // 钻石
                headimgUrl // 头像url地址 （游客登陆为""）
            ]
            userId,
            ip
        ]
     */
    GameDecoder.prototype.f_10003 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
        var hvo = GlobalDefine.herovo;
        hvo.decode(data[3]);
        GlobalDefine.userId = data[4];
        GlobalDefine.ip = data[5];
        GlobalDefine.serverInfo.serverTime = data[1];
        GlobalDefine.wxCode = 'aaaa';
        this.model.simpleDispatcher(MjModel.LOGIN_SUCC);
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
            成功=[1,roomNum,jushu]
     */
    GameDecoder.prototype.f_10007 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
        this.model.roomId = data[1];
        //自己创建的房间 直接怼进去
        var hvo = GlobalDefine.herovo;
        this.f_10008([1, data[1], data[2], [[1, hvo.roleId, hvo.name, hvo.ip]]]);
        //SendOperate.instance.requestEnterRoom(data[1]);
    };
    /**
     * (10008)加入房间
        c->s
            [
            roomNum
            ]
        s->c
            失败=[0,errorcode]
            成功=[1,
                roomNum,
                jushu
                    [
                    [index,id,name,ip]....
                    ]
                ]
     */
    GameDecoder.prototype.f_10008 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
        this.model.roomId = data[1];
        this.model.currentJu = 0;
        this.model.jushu = data[2];
        var vo;
        this.model.playerData = {};
        for (var key in data[3]) {
            var temp = data[3][key];
            vo = new HeroVO();
            vo.decodeGameBase(temp);
            vo.ready = true;
            this.model.playerData[vo.roleId] = vo;
            if (vo.roleId == GlobalDefine.herovo.roleId) {
                this.model.mainDir = vo.dir;
            }
        }
        TFacade.toggleUI(MainGameUI.NAME, 1);
        TFacade.toggleUI(EnterRoomUI.NAME, 0);
        this.model.simpleDispatcher(MjModel.ENTER_ROOM_SUCC);
    };
    /**
     *
     * (10009)给客户端推送初始化的牌
        s->c
                [
                  1,

            //痞子[
                        [
                        id, 牌的唯一id
                        color, 牌的花色
                        num, 牌的数值
                        used
                    ]
                    //癞子[
                        id, 牌的唯一id
                        color, 牌的花色
                        num, 牌的数值
                        used
                    ]
            ]
            
            [//以下才是手牌
                [
                    id, 牌的唯一id
                    color, 牌的花色
                    num, 牌的数值
                    used
                ]...
            ]
        ]
     */
    GameDecoder.prototype.f_10009 = function (data) {
        //data[0]  result
        this.model.card_pizi = new CardVO();
        this.model.card_pizi.decode(data[1][0]);
        this.model.card_laizi = new CardVO();
        this.model.card_laizi.decode(data[1][1]);
        this.model.leftCard = 84;
        this.model.currentJu++;
        var main = this.model.playerData[GlobalDefine.herovo.roleId];
        main.ownCardList = [];
        main.ready = false;
        var vo;
        var key;
        for (key in data[2]) {
            vo = new CardVO();
            var temp = data[2][key];
            vo.decode(temp);
            vo.position = main.dir;
            main.ownCardList.push(vo);
        }
        this.model.sortCardList(main.ownCardList);
        //初始化其它玩家的牌
        var hvo;
        for (key in this.model.playerData) {
            hvo = this.model.playerData[key];
            hvo.ready = false;
            if (hvo.roleId == main.roleId)
                continue;
            hvo.ownCardList = [];
            for (var i = 0; i < 13; i++) {
                vo = new CardVO();
                vo.position = hvo.dir;
                hvo.ownCardList.push(vo);
            }
        }
        GlobalDefine.gameState = 1;
        this.model.simpleDispatcher(MjModel.INIT_CARD_SUCC);
    };
    /**
     * 	(10010)客户端提示 碰，杠，胡 之类
         *  s->c
        数组大小不固定，如果有碰和暗杠，则会返回
        [
            1,
            2,
            ...
        ]
      碰=1  暗杠=2  明杠=3  过路杠=4  胡=5  自摸=6  过=7   吃=8
     */
    GameDecoder.prototype.f_10010 = function (data) {
        data.shift(); //所有协议均有result，统一处理到data的第一位
        TFacade.toggleUI(OperateUI.NAME, 1).execute('operate', data);
        this.model.simpleDispatcher(MjModel.SING_APPEAR);
        this.model.signExist = true;
    };
    /**
     *
     * 10011 打牌
        c->s
            paiId
        s->c
            失败=[0,errorcode]
            成功=[
                    1,
                    roleId,
                    roleIndex,
                    [
                        id, 牌的唯一id
                        color, 牌的花色
                        num, 牌的数值
                        used
                    ]
                ]
                如果有提示标签的话，会推送10010
                如果没有提示标签的话。会推送10012
     */
    GameDecoder.prototype.f_10011 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
        var vo = new CardVO();
        vo.position = data[2];
        vo.decode(data[3]);
        this.model.whoPlayedCard = data[1];
        this.model.playingCard = vo;
        this.model.gotCard = null;
        var hvo = this.model.getHeromsg()[data[1]];
        //let hvo:HeroVO = this.model.playerData[data[1]];//正式数据
        for (var key in hvo.ownCardList) {
            var temp_1 = hvo.ownCardList[key];
            temp_1.justGet = false;
        }
        if (hvo.roleId == GlobalDefine.herovo.roleId) {
            var list = hvo.ownCardList;
            var temp;
            for (var key in list) {
                temp = list[key];
                if (temp.id == vo.id) {
                    list.splice(list.indexOf(temp), 1); //只删除一个数据 可以在for循环里干
                    break;
                }
            }
        }
        else {
            hvo.ownCardList.length = 13 - hvo.duiCardList.length;
        }
        if (hvo.roleId == GlobalDefine.herovo.roleId) {
            this.model.sortCardList(hvo.ownCardList); //自己出过牌后 整理有序自己的牌面
        }
        this.model.simpleDispatcher(MjModel.SOMEONE_PLAY_CARD, vo);
    };
    /**
     * 10012 推送摸到的牌(都推送) 摸牌
        s->c
            [
                1,
                roleId,
                roleIndex,
                [
                    id, 牌的唯一id
                    color, 牌的花色
                    num, 牌的数值
                    used
                ]
            ]
     */
    GameDecoder.prototype.f_10012 = function (data) {
        var vo = new CardVO();
        vo.position = data[2];
        this.model.currentIndex = data[2];
        this.model.currentIndex = vo.position;
        this.model.whoPlayedCard = -1; //摸牌后必定能出牌
        vo.decode(data[3]);
        this.model.gotCard = vo;
        vo.justGet = true;
        this.model.leftCard--;
        if (this.model.leftCard < 0) {
            this.model.leftCard = 0;
        }
        this.model.simpleDispatcher(MjModel.SOMEONE_GET_CARD, vo);
    };
    /**
     * 10013 麻将标签操作   有人操作就推送
        c->s
            signType 标签类型
        s->c
            失败=[0,errorcode]
            成功=[
                    1,
                    roleId,
                    roleIndex,
                    signType,
                    [
                        [
                            id,
                            color,
                            num,
                            used
                        ]...
                    ]
                ]
        备：signType是胡牌类型的话，返回10016协议
     */
    GameDecoder.prototype.f_10013 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
        this.model.signExist = false;
        if (data[3] == 7) {
            return;
        }
        this.model.currentIndex = data[2];
        var deleteIds = [];
        var temps = [];
        var cvo;
        var key;
        for (key in data[4]) {
            cvo = new CardVO();
            cvo.position = data[2];
            cvo.decode(data[4][key]);
            temps.push(cvo);
            deleteIds.push(cvo.id);
        }
        if (temps.length > 0) {
            var xvo = this.model.playerData[this.model.whoPlayedCard];
            var xlist = xvo.playedCardList;
            for (var xt = 0; xt < xlist.length; xt++) {
                cvo = xlist[xt];
                if (cvo.id == this.model.playingCard.id) {
                    xlist.splice(xt, 1);
                    break;
                }
            }
        }
        var tt = [];
        var vo = this.model.playerData[data[1]];
        if (data[1] == GlobalDefine.herovo.roleId) {
            vo.duiCardList = vo.duiCardList.concat(temps); //碰牌追加
            for (key in vo.ownCardList) {
                cvo = vo.ownCardList[key];
                if (deleteIds.indexOf(cvo.id) != -1) {
                    //let index:number = vo.ownCardList.indexOf(cvo); ///不要在for循环里删数据啊
                    //vo.ownCardList.splice(index,1);
                    tt.push(cvo);
                }
            }
            if (tt.length > 0) {
                for (var j = 0; j < tt.length; j++) {
                    cvo = tt[j];
                    var index = vo.ownCardList.indexOf(cvo);
                    if (index != -1) {
                        vo.ownCardList.splice(index, 1);
                    }
                }
            }
        }
        else {
            vo.duiCardList = vo.duiCardList.concat(temps);
            vo.ownCardList.length = 13 - vo.duiCardList.length;
        }
        this.model.simpleDispatcher(MjModel.SING_OPERATE_SUCC, [vo.dir, data[3], this.model.playingCard.position]); ///抛事件、刷新对应玩家的牌面,顺便刷新出牌的那位
    };
    /**
     * 10014 游戏未开始房主解散房间
        c->s
            
        s->c
            失败=[0,errorcode]
            成功=[
                    1,
                    code,name,id
                ]
     */
    GameDecoder.prototype.f_10014 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
        Tnotice.instance.popUpTip(data[1]);
        this.model.simpleDispatcher(MjModel.JIESAN_ROOM_SUCC);
        TFacade.toggleUI(MainGameUI.NAME, 0);
    };
    /**
     * 10015 游戏未开始别人退出房间
        c->s
            
        s->c
            失败=[0,errorcode]
            成功=[
                    1
                ]
            注意：退出成功会给其他玩家刷新房间列表，走10008协议
     */
    GameDecoder.prototype.f_10015 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
        this.model.simpleDispatcher(MjModel.EXIT_ROOM_SUCC);
        TFacade.toggleUI(MainGameUI.NAME, 0);
    };
    /**
     (10016)每局的结算面板
        s->c
        [
            1
                [ 胡牌玩家列表，没人胡时为null
                winRoleId
                ...
                ],
            [
                roleId,
                jifen总积分,
                [玩家牌的列表
                    [
                        id,
                        color,
                        num,
                        used
                    ]...
                ]
            ]...
        ]
     */
    GameDecoder.prototype.f_10016 = function (data) {
        data.shift();
        var huList = data[0];
        this.model.settleData = {};
        var vo;
        for (var key in data[1]) {
            var arr = data[1][key];
            vo = new SettleVO();
            vo.decode(arr);
            this.model.settleData[vo.roleId] = vo;
        }
        vo = this.model.settleData[GlobalDefine.herovo.roleId];
        var ss;
        if (huList == null) {
            ss = 'ping';
        }
        else if (huList.indexOf(vo.roleId) != -1) {
            ss = 'win';
        }
        else {
            ss = 'lose';
        }
        TFacade.toggleUI(SettlementUI.NAME, 1).execute('go', ss);
        //this.model.simpleDispatcher(MjModel.SETTLE_SHOW);
    };
    /***
     * (10019)总战绩
        s->c
        失败=[0,errorcode]
        成功=[
            1,
            [
                index,roleId,name,jifen总积分,zimoCount,jiepaoCount,dianpaoCount,angangCount,minggangCount
            ]...
        ]
     */
    GameDecoder.prototype.f_10019 = function (data) {
        data.shift();
        this.model.overData = {};
        var vo;
        for (var key in data) {
            var arr = data[key];
            vo = new FinalSettleVO();
            vo.index = arr.shift();
            vo.roleid = arr.shift();
            vo.name = arr.shift();
            vo.score = arr.shift();
            vo.decode(arr);
            this.model.overData[vo.roleid] = vo;
        }
        GlobalDefine.gameState = 0;
        TFacade.toggleUI(GameOverUI.NAME, 1).execute('go');
    };
    /**
     * 10017 开始游戏
        c->s
        s->c
            失败=[0,errorcode]
            成功=[
                1,
                roleId
            ]
        如果4个人都点了开始游戏，会给客户端返回10009 ，10012 ，10010
     */
    GameDecoder.prototype.f_10017 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
        GlobalDefine.gameState = 0;
        var vo = this.model.playerData[data[1]];
        vo.ready = true;
        this.model.leftCard = 0;
        var tt;
        for (var key in this.model.playerData) {
            tt = this.model.playerData[key];
            tt.ownCardList = [];
            tt.duiCardList = [];
            tt.playedCardList = [];
        }
        this.model.simpleDispatcher(MjModel.GAME_START_AGAIN);
        this.model.simpleDispatcher(MjModel.SOMEONE_READY_GAME, vo.dir);
    };
    /**
     *
     * (10018)断线重连
        s->c
        失败=[0,errorcode]
        成功=[
            1,
            roomNum,
            alreadyJuShu, 已经打了的局数
                    totalJu,总局数
            sumIndex, 已经摸了多少张牌
            curIndex, 当前索引
            [
                index,玩家索引
                roleId,
                name,
                ip,
                jifen,
                [玩家自己牌的列表(其他玩家这里存放的是已经被用过的牌)
                    [
                        id,
                        color,
                        num,
                        used
                    ]...
                ]
                [打出来的牌
                    [
                        id,
                        color,
                        num,
                        used
                    ]...
                ]
            ]...
 ]
     */
    GameDecoder.prototype.f_10018 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
        this.model.roomId = data[1];
        this.model.currentJu = data[2];
        this.model.jushu = data[3];
        this.model.currentIndex = data[5];
        data = data.slice(6);
        var vo;
        var key;
        for (key in data) {
            var temp = data[key];
            var rid = temp[1];
            var hvo = this.model.playerData[rid];
            if (!hvo) {
                if (rid == GlobalDefine.herovo.roleId) {
                    hvo = GlobalDefine.herovo;
                }
                else {
                    hvo = new HeroVO();
                }
                hvo.dir = temp[0];
                hvo.roleId = rid;
                hvo.name = temp[2];
                hvo.ip = temp[3];
                hvo.jifen = temp[4];
                this.model.playerData[rid] = hvo;
            }
            hvo.ownCardList = [];
            hvo.duiCardList = [];
            hvo.playedCardList = [];
            var ownCardList = temp[5]; //手牌
            for (key in ownCardList) {
                var tt = ownCardList[key];
                vo = new CardVO();
                vo.position = hvo.dir;
                vo.decode(tt);
                if (vo.used) {
                    hvo.duiCardList.push(vo);
                }
                else {
                    hvo.ownCardList.push(vo);
                }
            }
            if (rid != GlobalDefine.herovo.roleId) {
                var ownLen = 13 - hvo.duiCardList.length;
                for (var i = 0; i < ownLen; i++) {
                    vo = new CardVO();
                    vo.position = hvo.dir;
                    hvo.ownCardList.push(vo);
                }
            }
            var playedCardList = temp[6]; //打出去的牌
            for (key in playedCardList) {
                var cc = ownCardList[key];
                vo = new CardVO();
                vo.decode(cc);
                hvo.playedCardList.push(vo);
            }
        }
        GlobalDefine.gameState = 1;
        TFacade.toggleUI(MainGameUI.NAME, 1);
        this.model.simpleDispatcher(MjModel.ENTER_ROOM_SUCC);
        this.model.simpleDispatcher(MjModel.INIT_CARD_SUCC);
    };
    return GameDecoder;
}(TDecoder));
__reflect(GameDecoder.prototype, "GameDecoder");
//# sourceMappingURL=GameDecoder.js.map