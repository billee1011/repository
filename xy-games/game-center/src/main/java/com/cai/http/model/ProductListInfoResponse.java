package com.cai.http.model;

import java.util.List;

/**
 * 获取商品列表接口 返回值 JSON 格式
 *
 */
public class ProductListInfoResponse {
	/**
	 * 成功失败  标记  必填
	 */
	private int result;
	private String appId;//游戏ID
	
	private List<Product> productList;
	
	public static class Product {
		//商品唯一ID
		private int shopId;
		//商品名称
		private String name;
		//商品类型(0-代理商品 1-普通商品)
		private int shop_type;
		//房卡数量
		private int gold;
		//赠送房卡数量
		private int send_gold;
		//商品出售价格(元)
		private int price;
		//排序方式(大的排前面)
	    private int display_order;
		public int getShopId() {
			return shopId;
		}
		public void setShopId(int shopId) {
			this.shopId = shopId;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getShop_type() {
			return shop_type;
		}
		public void setShop_type(int shop_type) {
			this.shop_type = shop_type;
		}
		public int getGold() {
			return gold;
		}
		public void setGold(int gold) {
			this.gold = gold;
		}
		public int getSend_gold() {
			return send_gold;
		}
		public void setSend_gold(int send_gold) {
			this.send_gold = send_gold;
		}
		public int getPrice() {
			return price;
		}
		public void setPrice(int price) {
			this.price = price;
		}
		public int getDisplay_order() {
			return display_order;
		}
		public void setDisplay_order(int display_order) {
			this.display_order = display_order;
		}
	}
	
	
	public int isResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public List<Product> getProductList() {
		return productList;
	}

	public void setProductList(List<Product> productList) {
		this.productList = productList;
	}
}
