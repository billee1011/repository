package com.cai.service;

import org.springframework.stereotype.Service;

import com.cai.handler.client.CityReportHandler;

@Service("taskService")
public class TaskService  {
	public void taskJob(){
		try{
			CityReportHandler.taskInDb();
			CityReportHandler.clearCityReportModel();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public void taskJobByHours(){
		try{
			CityReportHandler.taskInDbByHours();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
