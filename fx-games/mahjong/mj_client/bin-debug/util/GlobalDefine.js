var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var GlobalDefine = (function () {
    function GlobalDefine() {
    }
    GlobalDefine.charge = function () {
    };
    Object.defineProperty(GlobalDefine, "stageW", {
        get: function () {
            return this.stage.stageWidth;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(GlobalDefine, "stageH", {
        get: function () {
            return this.stage.stageHeight;
        },
        enumerable: true,
        configurable: true
    });
    return GlobalDefine;
}());
GlobalDefine.WEB_SOCKE_COLLECTED = false;
GlobalDefine.tickTemp = true;
GlobalDefine.serverInfo = new ServerInfoVO();
/**
 * 主角自己的信息 登录时赋值
 */
GlobalDefine.herovo = new HeroVO();
/**音效大小 */
GlobalDefine.playSoundVolume = 1;
/**
 * 0:游戏未开始
 * 1：游戏开始
 * 2：游戏开始了但在房间外面
 */
GlobalDefine.gameState = 0;
__reflect(GlobalDefine.prototype, "GlobalDefine");
//# sourceMappingURL=GlobalDefine.js.map