package com.lingyu.admin.controller.mm;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.manager.OperationLogManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.common.entity.OperationLog;

@Controller
@RequestMapping(value = "/mm/operationLog")
public class OperationLogController {
	private static final Logger logger = LogManager.getLogger(OperationLogController.class);
	private OperationLogManager operationLogManager;

	public void initialize() {
		operationLogManager = AdminServerContext.getBean(OperationLogManager.class);
	}

	/**
	 * 
	 * @param userName 操作者名称
	 * @param startTime 查询开始时间
	 * @param endTime 查询结束时间
	 * @param pageNo 查询页码
	 * @param pageSize 每页尺寸
	 * @return
	 */
	@Privilege(value = PrivilegeConstant.MENU_ROLE)
	@RequestMapping(value = "/list.do", method = { RequestMethod.GET })
	public void getList(Model model, @RequestParam(required = false) String isSearch, @RequestParam(required = false) String userName,
			@RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime,
			@RequestParam(required = false, defaultValue = "1") int pageNo, @RequestParam(required = false, defaultValue = "10") int pageSize) {
		if ("false".equals(isSearch)) {
			return;
		}

		if (pageSize < 1) {
			return;
		}

		Date startDate = null;
		Date endDate = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			if (!startTime.isEmpty()) {
				startDate = sdf.parse(startTime);
			}
			if (!endTime.isEmpty()) {
				endDate = sdf.parse(endTime);
			}
		} catch (ParseException e) {
			return;
		}
		List<OperationLog> list = operationLogManager.getLogList(userName, startDate, endDate, pageNo, pageSize);
		double logNum = operationLogManager.getLogNum(userName, startDate, endDate);
		model.addAttribute("list", list);
		model.addAttribute("userName", userName);
		model.addAttribute("startTime", startTime);
		model.addAttribute("endTime", endTime);
		model.addAttribute("pageNo", pageNo);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("pageNum", (int) Math.ceil(logNum / pageSize));
	}
}
