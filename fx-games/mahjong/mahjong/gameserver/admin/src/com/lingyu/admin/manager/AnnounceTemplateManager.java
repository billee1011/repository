package com.lingyu.admin.manager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.dao.AnnounceTemplateDao;
import com.lingyu.common.entity.AnnoucenceTemplate;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class AnnounceTemplateManager {
	@Autowired
	private AnnounceTemplateDao announceTemplateDao;
	
	public List<AnnoucenceTemplate> queryAll(){
		return announceTemplateDao.getAll();
	}
	
	public void delete(int id){
		announceTemplateDao.delete(id);
	}
	
	public void insert(String title,String content){
		announceTemplateDao.insert(title, content);
	}
	
	public void update(int id, String title,String content){
		announceTemplateDao.update(id, title, content);
	}
}
