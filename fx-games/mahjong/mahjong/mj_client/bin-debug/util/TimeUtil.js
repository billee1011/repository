var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
/**
 * 解析时间
 */
var TimeUtil = (function () {
    function TimeUtil() {
    }
    /**
     * 单位/毫秒
     * 返回：xx天xx时xx分xx秒
     */
    TimeUtil.getCountDownTime = function (leftTime) {
        leftTime = Math.floor(leftTime / 1000);
        var day = Math.floor(leftTime / this.dayTime);
        var dayAfter = leftTime % this.dayTime;
        var hour = Math.floor(dayAfter / 3600);
        var hourAfter = leftTime % 3600;
        var minute = Math.floor(hourAfter / 60);
        var second = hourAfter % 60;
        var back = '';
        if (day > 0) {
            back += day < 10 ? '0' + day : day;
            back += this.Day;
        }
        if (hour > 0) {
            back += hour < 10 ? '0' + hour : hour;
            back += this.Hour;
        }
        if (minute > 0) {
            back += minute < 10 ? '0' + minute : minute;
            back += this.Minute;
        }
        if (second > 0) {
            back += second < 10 ? '0' + second : second;
            back += this.Second;
        }
        return back;
    };
    return TimeUtil;
}());
TimeUtil.Day = '天';
TimeUtil.Hour = '时';
TimeUtil.Minute = '分';
TimeUtil.Second = '秒';
TimeUtil.dayTime = 24 * 3600;
__reflect(TimeUtil.prototype, "TimeUtil");
//# sourceMappingURL=TimeUtil.js.map