var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var MjModel = (function (_super) {
    __extends(MjModel, _super);
    function MjModel() {
        var _this = _super.call(this) || this;
        /**
         * 剩余多少张牌
         */
        _this.leftCard = 99;
        /***
         * 是否存在标签  超时不操作让其过
         */
        _this.signExist = false;
        _this.playerData = {};
        return _this;
        //	this.initTempData();
    }
    MjModel.prototype.getHeromsg = function () {
        //	return this.heroMsg; 测试数据
        return this.playerData;
    };
    /**
     * 相对主角是不是在右边
     */
    MjModel.prototype.checkIsRight = function (mainDir, curDir) {
        var temp = curDir - mainDir;
        return temp == 1 || temp == -3;
    };
    /**
     * 相对主角是不是在上边
     */
    MjModel.prototype.checkIsTop = function (mainDir, curDir) {
        var temp = Math.abs(curDir - mainDir);
        return temp == 2;
    };
    /**
     * 相对主角是不是在左边
     */
    MjModel.prototype.checkIsLeft = function (mainDir, curDir) {
        var temp = curDir - mainDir;
        return temp == 3 || temp == -1;
    };
    /**
     * return
     */
    MjModel.prototype.getIconPByPosition = function (p) {
        if (this.checkIsRight(this.mainDir, p))
            return 1;
        if (this.checkIsTop(this.mainDir, p))
            return 2;
        if (this.checkIsLeft(this.mainDir, p))
            return 3;
        return 4;
    };
    MjModel.prototype.getInitP = function (p) {
        var r = new egret.Point();
        if (this.checkIsRight(this.mainDir, p)) {
            r.x = 1108;
            r.y = 301;
        }
        if (this.checkIsTop(this.mainDir, p)) {
            r.x = 582;
            r.y = 61;
        }
        if (this.checkIsLeft(this.mainDir, p)) {
            r.x = 156;
            r.y = 286;
        }
        return r;
    };
    MjModel.prototype.getIconStyle = function (p) {
        return p == this.mainDir ? 'b' : 's';
    };
    MjModel.prototype.getOperateTexture = function (opValue) {
        switch (opValue) {
            case 1:
                return SheetManage.getTextureFromOperate('op_peng');
            case 2:
                return SheetManage.getTextureFromOperate('op_gang');
            case 3:
                return SheetManage.getTextureFromOperate('op_gang');
            case 4:
                return SheetManage.getTextureFromOperate('op_gang');
            case 5:
                return SheetManage.getTextureFromOperate('op_hu');
            case 6:
                return SheetManage.getTextureFromOperate('op_hu');
            case 7:
                return SheetManage.getTextureFromOperate('op_pass');
            case 8:
                return SheetManage.getTextureFromOperate('op_eat');
        }
        return null;
    };
    MjModel.prototype.sortCardList = function (temp) {
        /*temp.sort(this.sort1);
        temp.sort(this.sort2);*/
        temp.sort(this.sortMoreFun(["style", "type"], [0, 0]));
    };
    /*private sort1(c1:CardVO,c2:CardVO):number
    {
        if(c1.style < c2.style){
            return -1;
        }
        return 1;
    }
    private sort2(c1:CardVO,c2:CardVO):number
    {
        if(c1.style == c2.style){
            if(c1.type < c2.type) return -1;
            else return 1;
        }
        return 0;
    }*/
    MjModel.prototype.sortMoreFun = function (strarr, sortarr) {
        if (sortarr === void 0) { sortarr = null; }
        return function (obj1, obj2) {
            var temp;
            var valarr = [];
            var sorlen = 0;
            if (sortarr)
                sorlen = sortarr.length;
            var chanum;
            for (var b in strarr) {
                temp = parseInt(b);
                chanum = parseInt(obj1[strarr[b]]) - parseInt(obj2[strarr[b]]);
                if (chanum == 0) {
                    continue;
                }
                else {
                    if (sorlen > temp && sortarr[b] == 0) {
                        return chanum;
                    }
                    else {
                        return -chanum;
                    }
                }
            }
            return 0;
        };
    };
    return MjModel;
}(Proxy));
MjModel.NAME = 'MjModel';
MjModel.LOGIN_SUCC = 'LOGIN_SUCC';
/**
 * 输入房间秘密正确 进入房间
 */
MjModel.PASSWORD_RIGHT = 'PASSWORD_RIGHT';
MjModel.INIT_CARD_SUCC = 'INIT_CARD_SUCC';
/**
 * 加入房间 生成周围玩家
 */
MjModel.ENTER_ROOM_SUCC = 'ENTER_ROOM_SUCC';
/**
 * 有人出牌了
 */
MjModel.SOMEONE_PLAY_CARD = 'SOMEONE_PLAY_CARD';
/**
 * 有人摸牌了
 */
MjModel.SOMEONE_GET_CARD = 'SOMEONE_GET_CARD';
/**
 * 标签操作、碰、杠 等
 */
MjModel.SING_OPERATE_SUCC = 'SING_OPERATE_SUCC';
/**
 * 服务端提示有标签出现，此处用途：停掉计时器
 */
MjModel.SING_APPEAR = 'SING_APPEAR';
/**
 * 显示结算
 */
//public static SETTLE_SHOW:string = 'SETTLE_SUCC';
/**
 * 解散房间
 */
MjModel.JIESAN_ROOM_SUCC = 'JIESAN_ROOM_SUCC';
/**
 * 退出房间
 */
MjModel.EXIT_ROOM_SUCC = 'EXIT_ROOM_SUCC';
/**
 * 有人准备好了
 */
MjModel.SOMEONE_READY_GAME = 'SOMEONE_READ_GAME';
/**
 * 结算后点击开始游戏
 */
MjModel.GAME_START_AGAIN = 'GAME_START_AGAIN';
/***
 * 拉取了公告列表
 */
MjModel.GET_GONGGAO_LIST = 'GET_GONGGAO_LIST';
__reflect(MjModel.prototype, "MjModel");
//# sourceMappingURL=MjModel.js.map