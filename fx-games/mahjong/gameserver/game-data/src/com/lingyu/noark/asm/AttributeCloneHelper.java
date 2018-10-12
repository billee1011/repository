package com.lingyu.noark.asm;

import com.lingyu.noark.data.entity.Attribute;

public class AttributeCloneHelper {
	public Attribute clone(Attribute source) {
		Attribute attribute = new Attribute();
		attribute.setId(source.getId());
		attribute.setItem(source.getItem());
		attribute.setName(source.getName());
		return attribute;
	}
}
