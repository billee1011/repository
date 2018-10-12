package com.cai.common.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 金币商城
 */
public class MoneyShopModel implements Serializable {

    private int id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 状态(1-正常 0-失效)
     */
    private int state;

    /**
     * 游戏类型 0-表示所有
     */
    private int gameType;

    /**
     * 金币money数量
     */
    private int money;

    /**
     * 赠送金币数量
     */
    private int sendMoney;

    /**
     * 兑换需要的(闲逸豆)
     */
    private int price;

    /**
     * 排序方式(大的排前面)
     */
    private int displayOrder;

    private Date createTime;

    private String remark;

    /**
     * ICON
     */
    private String icon;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getSendMoney() {
        return sendMoney;
    }

    public void setSendMoney(int sendMoney) {
        this.sendMoney = sendMoney;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * 商品状态是否正常
     * @return
     */
    public boolean isStatusEffect() {
        return state==1;
    }

}
