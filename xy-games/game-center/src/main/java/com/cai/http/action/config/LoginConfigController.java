package com.cai.http.action.config;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.cai.common.util.PerformanceTimer;
import com.cai.http.FastJsonJsonView;
import com.google.common.collect.Maps;

//@Controller
//@RequestMapping("/config")
public class LoginConfigController {

	//@RequestMapping("/loginConfig")
	public ModelAndView loginConfig(HttpServletRequest request, Model model, @RequestParam(value = "clientVersion", required = false) String clientVersion, @RequestParam(value = "imei", required = false) String imei, @RequestParam(value = "channel", required = false) String channel) {

		PerformanceTimer timer = new PerformanceTimer();
		System.out.println("@@@@@@@@@@@@@@");
		Map<String, String> map = Maps.newHashMap();

		map.put("userList", "1");
		map.put("School", "22");
		map.put("Work", "t2");
		System.out.println(timer.getStr());
		
		
		
		return new ModelAndView(new MappingJackson2JsonView(),map);  

	}
	
	//@RequestMapping("/loginConfig2")
	public ModelAndView loginConfig2(HttpServletRequest request, Model model, @RequestParam(value = "clientVersion", required = false) String clientVersion, @RequestParam(value = "imei", required = false) String imei, @RequestParam(value = "channel", required = false) String channel) {

		PerformanceTimer timer = new PerformanceTimer();
		Map<String, String> map = Maps.newHashMap();

		map.put("userList", "1");
		map.put("School", "22");
		map.put("Work", "t2");
		System.out.println(timer.getStr());
		
		//map.put("code", "1");
		
		return new ModelAndView(new FastJsonJsonView(),map);  

	}
}
