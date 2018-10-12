package com.lingyu.noark.data.entity;

import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.Table;

@Entity(fetch = FeatchType.START)
@Table(name = "role")
public class Role {
	@Id
	@Column(name = "id")
	private long id;

	@Column(name = "name")
	private String name;
}
