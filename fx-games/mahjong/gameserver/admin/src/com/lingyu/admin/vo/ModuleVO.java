package com.lingyu.admin.vo;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
public class ModuleVO
{
	private int code;
	private String name;
	private List<MenuVO> menuDTOList = new ArrayList<MenuVO>();
	public int getCode()
	{
		return code;
	}
	public void setCode(int code)
	{
		this.code = code;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public List<MenuVO> getMenuDTOList()
	{
		return menuDTOList;
	}
	public void addMenuDTO(MenuVO menuDTO)
	{
		this.menuDTOList.add(menuDTO);
	}
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
	}
}
