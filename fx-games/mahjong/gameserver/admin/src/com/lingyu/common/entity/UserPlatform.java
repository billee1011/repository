package com.lingyu.common.entity;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.lingyu.admin.vo.UserPlatformVO;
@Entity
@Table(name = "user_platform")
public class UserPlatform
{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	private int id;
	@Column(name = "user_id")
	private int userId;
	@Column(name = "pid")
	private String platformId;
	@Column(name = "name")
	private String name;
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getUserId()
	{
		return userId;
	}
	public void setUserId(int userId)
	{
		this.userId = userId;
	}
	public String getPlatformId()
	{
		return platformId;
	}
	public void setPlatformId(String platformId)
	{
		this.platformId = platformId;
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	public UserPlatformVO toVO(){
		UserPlatformVO ret=new UserPlatformVO();
		ret.setId(platformId);
		ret.setName(name);
		return ret;
	}
}
