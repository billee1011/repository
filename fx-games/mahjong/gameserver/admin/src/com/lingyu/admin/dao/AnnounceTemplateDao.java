package com.lingyu.admin.dao;

import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingyu.common.entity.AnnoucenceTemplate;
import com.lingyu.common.orm.SimpleHibernateTemplate;
@Service
public class AnnounceTemplateDao{

	private SimpleHibernateTemplate<AnnoucenceTemplate, Integer> template;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new SimpleHibernateTemplate<AnnoucenceTemplate, Integer>(sessionFactory, AnnoucenceTemplate.class);
	}
	
	public List<AnnoucenceTemplate> getAll() {
		List<AnnoucenceTemplate> ret = template.findAll();
		return ret;
	}
	
	public void insert(String title,String content){
		AnnoucenceTemplate entity = new AnnoucenceTemplate();
		entity.setContent(content);
		entity.setTitle(title);
		template.save(entity);
	}
	
	public void delete(int id){
		template.delete(id);
	}
	
	public void update(int id,String title,String content){
		AnnoucenceTemplate entity = template.get(id);
		entity.setContent(content);
		entity.setTitle(title);
		template.update(entity);
	}
}
