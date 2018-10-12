var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TickTool = (function () {
    function TickTool(label, type) {
        if (type === void 0) { type = 1; }
        this.label = label;
        this.type = type;
        this.record = -1;
    }
    /**
     * time：倒计时时间间隔 /毫秒
     */
    TickTool.prototype.setOverTime = function (time) {
        this.overTime = GlobalDefine.serverInfo.serverTime + time;
    };
    TickTool.prototype.startTick = function () {
        if (!this.runing) {
            this.record = egret.setInterval(this.tick, this, 1000);
            this.runing = true;
            this.tick();
        }
    };
    TickTool.prototype.tick = function () {
        var countDownTime = Math.ceil(this.overTime - GlobalDefine.serverInfo.serverTime);
        if (this.type == 1) {
            this.label.text = TimeUtil.getCountDownTime(countDownTime);
        }
        if (countDownTime < 0) {
            this.stopTick();
            if (this.backFun != null) {
                this.label.text = '';
                this.backFun.call(this.funThis);
            }
        }
    };
    TickTool.prototype.stopTick = function () {
        if (this.runing) {
            egret.clearInterval(this.record);
            this.runing = false;
        }
    };
    return TickTool;
}());
__reflect(TickTool.prototype, "TickTool");
//# sourceMappingURL=TickTool.js.map