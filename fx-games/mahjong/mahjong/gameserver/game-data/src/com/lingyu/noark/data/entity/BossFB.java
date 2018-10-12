package com.lingyu.noark.data.entity;

import java.util.ArrayList;
import java.util.List;

import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.Table;

@Entity
@Table(name = "boss_fb")
public class BossFB {
	@Id
	@Column(name = "id")
	private long id;

	@Column(name = "name")
	private List<FBInfo> infos = new ArrayList<>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<FBInfo> getInfos() {
		return infos;
	}

	public void setInfos(List<FBInfo> infos) {
		this.infos = infos;
	}

}
