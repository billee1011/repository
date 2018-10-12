package com.lingyu.noark.data.entity;

import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.Table;

@Entity(fetch = FeatchType.START)
@Table(name = "server_info1123")
public class ServerInfo {
	@Id
	@Column(name = "id")
	private int id;

	@Column(name = "name")
	private String name;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "ServerInfo[id=" + id + ",name=" + name + "]";
	}
}
