var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var CodeDB = (function () {
    function CodeDB() {
    }
    CodeDB.prototype.decode = function (arr) {
        this._obj = {};
        for (var key in arr) {
            var temp = arr[key];
            this._obj[temp.code] = temp.des;
        }
    };
    CodeDB.prototype.getDes = function (code) {
        return this._obj[code];
    };
    return CodeDB;
}());
CodeDB.instance = new CodeDB();
__reflect(CodeDB.prototype, "CodeDB");
//# sourceMappingURL=CodeDB.js.map