package com.lingyu.admin.controller.pss;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.manager.GameAreaManager;
import com.lingyu.admin.manager.MailTemplateManager;
import com.lingyu.admin.manager.PrivilegeManager;
import com.lingyu.admin.manager.RedeemManager;
import com.lingyu.admin.privilege.Privilege;
import com.lingyu.admin.privilege.PrivilegeConstant;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.DisplayGameAreaListVo;
import com.lingyu.admin.vo.RetCode;
import com.lingyu.common.entity.MailTemplate;
import com.lingyu.common.entity.RedeemMailRecord;
import com.lingyu.common.entity.RedeemRecord;
import com.lingyu.common.entity.User;
import com.lingyu.msg.http.Redeem_S2C_Msg;
/**
 * 玩家补偿
 * @author Wang Shuguang
 */
@Controller
@RequestMapping(value = "/pss/redeem")
public class RedeemController {
	private static final Logger logger = LogManager.getLogger(RedeemController.class);
	
	private MailTemplateManager mailTemplateManager;
	
	private RedeemManager redeemManager;
	
	private GameAreaManager gameAreaManager;
	
	private PrivilegeManager privilegeManager;
	
	public void initialize() {
		mailTemplateManager = AdminServerContext.getBean(MailTemplateManager.class);
		redeemManager = AdminServerContext.getBean(RedeemManager.class);
		gameAreaManager = AdminServerContext.getBean(GameAreaManager.class);
		privilegeManager = AdminServerContext.getBean(PrivilegeManager.class);
	}
	/** 客服系统玩家补偿管理主页面UI */
	@Privilege(value=PrivilegeConstant.MENU_PLAY_RECOUP)
	@RequestMapping(value = "/index.do", method = RequestMethod.GET)
	public void toIndex(Model model){
		
		Collection<MailTemplate> mailTemplates = mailTemplateManager.getMailTemplateList();
		model.addAttribute("mailTemplateList", JSON.toJSONString(mailTemplates));
//		model.addAttribute("itemTemplates", JSON.toJSONString(redeemManager.getItems()));
	}
	
	/** 客服系统玩家补偿管理创建邮件模板 */
	@ResponseBody
	@RequestMapping(value = "/createmailtemplate.do", method = RequestMethod.POST)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_RECOUP)
	public MailTemplate createMailTemplate(@RequestParam("title") String title, @RequestParam("content") String content){
		MailTemplate mailTemplate = new MailTemplate();
		mailTemplate.setTitle(title);
		mailTemplate.setContent(content);
		String retCode = mailTemplateManager.createMailTemplate(mailTemplate);
		if(!ErrorCode.EC_OK.equals(retCode)){
			mailTemplate.setId(-1);
			mailTemplate.setTitle(retCode);
		}
		return mailTemplate;
	}
	
	/** 客服系统玩家补偿管理修改邮件模板 */
	@ResponseBody
	@RequestMapping(value = "/modifymailtemplate.do", method = RequestMethod.POST)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_RECOUP)
	public MailTemplate modifyMailTemplate(@RequestParam("id") int id, @RequestParam("title") String title, @RequestParam("content") String content){
		MailTemplate mailTemplate = mailTemplateManager.getMailTemplate(id);
		if(mailTemplate == null){
			mailTemplate = new MailTemplate();
			mailTemplate.setId(-2);
		}else{
			mailTemplate.setTitle(title);
			mailTemplate.setContent(content);
			String retCode = mailTemplateManager.updateMailTemplate(mailTemplate);
			if(!ErrorCode.EC_OK.equals(retCode)){
				mailTemplate = new MailTemplate();
				mailTemplate.setId(-1);
				mailTemplate.setTitle(retCode);
			}
		}
		return mailTemplate;
	}
	
	/** 客服系统玩家补偿管理删除邮件模板 */
	@ResponseBody
	@RequestMapping(value = "/deletemailtemplate.do", method = RequestMethod.POST)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_RECOUP)
	public MailTemplate deleteMailTemplate(@RequestParam("id") int id){
		MailTemplate mailTemplate = mailTemplateManager.getMailTemplate(id);
		String retCode = mailTemplateManager.removeMailTemplate(id);
		if(!ErrorCode.EC_OK.equals(retCode)){
			mailTemplate = new MailTemplate();
			mailTemplate.setId(-1);
			mailTemplate.setTitle(retCode);
		}
		return mailTemplate;
	}
	
	/** 客服系统玩家邮件+补偿 */
	@ResponseBody
	@RequestMapping(value = "/redeem.do", method = RequestMethod.POST)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_RECOUP)
	public Redeem_S2C_Msg redeem(@RequestParam("mailTitle") String mailTitle, @RequestParam("mailContent") String mailContent, 
			@RequestParam("selectRoleType") int selectRoleType, @RequestParam(value = "roleArray", required=false) String roleArray,
			@RequestParam(value = "money", required=false) Integer money, @RequestParam(value = "diamond", required=false) Integer diamond, 
			@RequestParam(value = "itemArray") String itemArray, @RequestParam(value = "all", required=false) boolean allAreas,
			@RequestParam(value = "areaList", required=false) List<Integer> areaList,@RequestParam(value = "redeemType", required=false) String redeemType){
		List<Integer> orignalAreaList = areaList;
		Redeem_S2C_Msg ret = new Redeem_S2C_Msg();
		ret.setRetCode(RetCode.ASYN_SUCCESS.getCode());
		if(selectRoleType == 0 && StringUtils.isEmpty(roleArray)){
			ret.setRetCode(com.lingyu.common.core.ErrorCode.EC_FAILED);
		}else if((!allAreas) && CollectionUtils.isEmpty(areaList)){ //没有选服
			ret.setRetCode(com.lingyu.common.core.ErrorCode.EC_FAILED);
		}else{
			if(!allAreas){
				areaList = gameAreaManager.filterGameAreaIds(SessionUtil.getCurrentUser().getLastPid(), areaList);
			}
			Object[] retObjs = redeemManager.redeemMultiAreas(mailTitle, mailContent, selectRoleType, roleArray, money, diamond, itemArray, allAreas, areaList, orignalAreaList,redeemType);
			Object retObj=retObjs[0];
			int insertReocrdId=(int) retObjs[1];
			if(retObj != null && retObj instanceof Redeem_S2C_Msg){
				ret = (Redeem_S2C_Msg) retObj;
				//更新record记录
				redeemManager.updateRecord(redeemType, insertReocrdId, ret.getMessages());
			}
		}
		
		User user = SessionUtil.getCurrentUser();
		if(user.getPrivilegeList().contains(PrivilegeConstant.MENU_PLAY_RECOUP_CHECK)){
			ret.setHasCheckPrivilege(true);
		}
		return ret;
	}
	
	
	/** 客服系统玩家补偿 */
	@ResponseBody
	@RequestMapping(value = "/redeemMail.do", method = RequestMethod.POST)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_SENDMAIL)
	public Redeem_S2C_Msg redeemMail(@RequestParam("mailTitle") String mailTitle, @RequestParam("mailContent") String mailContent, 
			@RequestParam("selectRoleType") int selectRoleType, @RequestParam(value = "roleArray", required=false) String roleArray, @RequestParam(value = "all", required=false) boolean allAreas,
			@RequestParam(value = "areaList", required=false) List<Integer> areaList,@RequestParam(value = "redeemType", required=false) String redeemType){
		List<Integer> orignalAreaList = areaList;
		Redeem_S2C_Msg ret = new Redeem_S2C_Msg();
		ret.setRetCode(RetCode.ASYN_SUCCESS.getCode());
		if(selectRoleType == 0 && StringUtils.isEmpty(roleArray)){
			ret.setRetCode(com.lingyu.common.core.ErrorCode.EC_FAILED);
		}else if((!allAreas) && CollectionUtils.isEmpty(areaList)){ //没有选服
			ret.setRetCode(com.lingyu.common.core.ErrorCode.EC_FAILED);
		}else{
			if(!allAreas){
				areaList = gameAreaManager.filterGameAreaIds(SessionUtil.getCurrentUser().getLastPid(), areaList);
			}
			Object[] retObjs = redeemManager.redeemMultiAreas(mailTitle, mailContent, selectRoleType, roleArray, 0, 0, null, allAreas, areaList, orignalAreaList,redeemType);
			Object retObj=retObjs[0];
			int insertReocrdId=(int) retObjs[1];
			if(retObj != null && retObj instanceof Redeem_S2C_Msg){
				ret = (Redeem_S2C_Msg) retObj;
				//更新record记录
				redeemManager.updateRecord(redeemType, insertReocrdId, ret.getMessages());
			}
		}
		
		User user = SessionUtil.getCurrentUser();
		if(user.getPrivilegeList().contains(PrivilegeConstant.MENU_PLAY_RECOUP_CHECK)){
			ret.setHasCheckPrivilege(true);
		}
		return ret;
	}
	
	
	@ResponseBody
	@RequestMapping(value = "/checkredeem.do", method = RequestMethod.POST)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_RECOUP_CHECK)
	public Redeem_S2C_Msg checkRedeem(@RequestParam("id") int id, @RequestParam("accepted") boolean accepted){
		Redeem_S2C_Msg ret = redeemManager.checkRedeem(id, accepted);
		return ret;
	}
	
	@RequestMapping(value = "/redeemrecord.do", method = RequestMethod.GET)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_RECOUP)
	public void redeemRecord(Model model){
		model.addAttribute("recordsCount", redeemManager.getRecordsCount());
		User user = SessionUtil.getCurrentUser();
		boolean checkRight = user.getPrivilegeList().contains(PrivilegeConstant.MENU_PLAY_RECOUP_CHECK);
		
		model.addAttribute("checkRight", checkRight);
	}
	
	@ResponseBody
	@RequestMapping(value = "/redeemrecord.do", method = RequestMethod.POST)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_RECOUP)
	public DisplayGameAreaListVo<RedeemRecord> redeemRecord(@RequestParam("page") int page, @RequestParam("rows") int rows){
		
		int totalCount = redeemManager.getRecordsCount();
		List<RedeemRecord> records = null;
		if(totalCount > 0){
			records = redeemManager.getRedeemRecords(page, rows);
		}else{
			records = Collections.emptyList();
		}
		DisplayGameAreaListVo<RedeemRecord> result = new DisplayGameAreaListVo<RedeemRecord>();
		result.setTotal(totalCount);
		result.setRows(records);
		return result;
	}
	
	@RequestMapping(value = "/redeemopt.do", method = RequestMethod.GET)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_RECOUP)
	public void redeemOpt(){
	}
	
	@RequestMapping(value = "/sendMail.do", method = RequestMethod.GET)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_SENDMAIL)
	public void sendMail(){
	}
	
	@ResponseBody
	@RequestMapping(value = "/redeemmailrecord.do", method = RequestMethod.POST)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_SENDMAIL)
	public DisplayGameAreaListVo<RedeemMailRecord> redeemmailrecord(@RequestParam("page") int page, @RequestParam("rows") int rows){
		
		int totalCount = redeemManager.getMailRecordsCount();
		List<RedeemMailRecord> records = null;
		if(totalCount > 0){
			records = redeemManager.getMailRedeemRecords(page, rows);
		}else{
			records = Collections.emptyList();
		}
		DisplayGameAreaListVo<RedeemMailRecord> result = new DisplayGameAreaListVo<RedeemMailRecord>();
		result.setTotal(totalCount);
		result.setRows(records);
		return result;
	}
	
	@RequestMapping(value = "/redeemmailrecord.do", method = RequestMethod.GET)
	@Privilege(value=PrivilegeConstant.MENU_PLAY_SENDMAIL)
	public void redeemmailrecord(Model model){
		model.addAttribute("recordsCount", redeemManager.getMailRecordsCount());
	}
}
