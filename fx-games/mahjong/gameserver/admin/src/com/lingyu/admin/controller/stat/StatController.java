package com.lingyu.admin.controller.stat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 统计系统Controller
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Controller
@RequestMapping(value = "/stat")
public class StatController {
	private static final Logger logger = LogManager.getLogger(StatController.class);

	/** 统计系统主页面UI */
	@RequestMapping(value = "/index.do", method = RequestMethod.GET)
	public void indexUI() {
	}
}
