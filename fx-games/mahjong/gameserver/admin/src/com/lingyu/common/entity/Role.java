package com.lingyu.common.entity;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.alibaba.fastjson.JSON;
import com.lingyu.admin.vo.RoleVO;

@Entity
@Table(name = "role")
public class Role {
	/**
	 * 主键id
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	private int id;
	/**
	 * 角色名
	 */
	@Column(name = "name")
	private String name;
	/**
	 * 角色描述
	 */
	@Column(name = "description")
	private String description;
	/**
	 * 拥有的资源权限
	 */
	@Column(name = "privileges")
	private String privileges;

	// private transient List<Integer> privilegeList= new ArrayList<>();
	private transient Map<Integer, Integer> privilegeMap = new HashMap<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isAuthorize(int code) {
		return privilegeMap.containsKey(code);
	}

	public void serialize() {
		privileges = JSON.toJSONString(privilegeMap.values());
	}

	public void deserialize() {
		List<Integer> list = JSON.parseArray(privileges, Integer.class);
		for (int code : list) {
			privilegeMap.put(code, code);
		}
	}

	public Collection<Integer> getPrivilegeList() {
		return privilegeMap.values();
	}

	public void setPrivilegeList(List<Integer> privilegeList) {
		privilegeMap.clear();
		if(privilegeList != null){
			for (int code : privilegeList) {
				privilegeMap.put(code, code);
			}
		}

	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
	}

	public RoleVO toVO() {
		RoleVO ret = new RoleVO();
		ret.setId(id);
		ret.setName(name);
		ret.setDescription(description);
		return ret;
	}
}
