package com.lingyu.common.entity;


/**
 * 每个棋牌
 * @author wangning
 * @date 2016年12月6日 下午2:09:23
 */
public class ChessEvery {
	private int id; // 麻将的唯一标示 
	private int number;// 麻将的数值
    private int color;// 麻将的花色，1=筒，2=条， 3=万  4=风
    private boolean used; // 是否碰了或杠了
//    private boolean gang; // 是否杠了
    
    public ChessEvery (ChessEvery e){
    	this.number = e.getNumber();
    	this.color = e.getColor();
    }
    
    public ChessEvery (int num, int color, boolean used){
    	this.number = num;
    	this.color = color;
    	this.used = used;
    }
    
    public ChessEvery (int id, int num, int color, boolean used){
    	this.id = id;
    	this.number = num;
    	this.color = color;
    	this.used = used;
    }
    
    public ChessEvery (){

    }

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public boolean isUsed() {
		return used;
	}
	public void setUsed(boolean used) {
		this.used = used;
	}
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
