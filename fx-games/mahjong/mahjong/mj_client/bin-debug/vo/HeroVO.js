var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var HeroVO = (function () {
    function HeroVO() {
        this.gender = 1;
        this.name = '';
        this.head = '';
        this.diamond = 0;
        /**
         * 剩余牌的数量  （针对其他玩家不显示牌面）
         */
        //public leftCardLen:number = 0;
        /**
         * 对过的牌
         */
        this.duiCardList = [];
        /**
         * 打出去的牌
         */
        this.playedCardList = [];
        //this.leftCardLen = 13;
    }
    /**
     * [
            roleId,
            name,
            gender,
            diamond, // 钻石
            headimgUrl // 头像url地址 （游客登陆为""）
        ]
     */
    HeroVO.prototype.decode = function (data) {
        var i = 0;
        this.roleId = data[i++];
        this.name = data[i++];
        this.gender = data[i++];
        this.diamond = data[i++];
        this.head = data[i++];
    };
    HeroVO.prototype.decodeGameBase = function (arr) {
        this.dir = arr[0];
        this.roleId = arr[1];
        this.name = arr[2];
        this.ip = arr[3];
    };
    HeroVO.prototype.decodeTemp = function (name, ready, head, dir, leftCard, duiList, playedList) {
        this.name = name;
        this.ready = ready;
        this.head = head;
        this.dir = dir;
        //this.leftCardLen = leftCard;
        this.duiCardList = duiList;
        this.playedCardList = playedList;
    };
    return HeroVO;
}());
__reflect(HeroVO.prototype, "HeroVO");
//# sourceMappingURL=HeroVO.js.map