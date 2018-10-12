package com.lingyu.msg.http;
import java.util.ArrayList;
import java.util.List;
public class QueryDTO
{
	private List<FieldDTO> list = new ArrayList<FieldDTO>();
	public List<FieldDTO> getList()
	{
		return list;
	}
	public void setList(List<FieldDTO> list)
	{
		this.list = list;
	}
	public void add(FieldDTO dto)
	{
		list.add(dto);
	}
}
