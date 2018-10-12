package com.lingyu.admin.controller.os;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 运营管理Controller.
 * 
 * @author 小流氓<176543888@qq.com>
 */
@Controller
@RequestMapping(value = "/os")
public class OSController {
	private static final Logger logger = LogManager.getLogger(OSController.class);

	/** 运营管理主页面UI */
	@RequestMapping(value = "/index.do", method = RequestMethod.GET)
	public void indexUI() {
		logger.debug("运营管理主页面UI");
	}
}
