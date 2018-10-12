package com.lingyu.noark.asm;

import java.util.Date;

import com.lingyu.noark.data.entity.Item;

public class ItemCloneHelper {
	AttributeCloneHelper h = new AttributeCloneHelper();

	public Item clone(Item source) {
		Item item = new Item();

		if (source.getAddTime() != null) {
			item.setAddTime(new Date(source.getAddTime().getTime()));
		}
		return item;
	}
}
