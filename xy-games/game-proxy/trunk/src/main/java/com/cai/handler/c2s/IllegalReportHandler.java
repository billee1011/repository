/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.C2SCmd;
import com.cai.common.domain.ReportIllegalModel;
import com.cai.common.util.SpringService;
import com.cai.service.MongoDBService;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.ReportIllegal;

/**
 * 
 *
 * @author tang date: 2017年12月22日 上午11:17:00 <br/>
 */
@ICmd(code = C2SCmd.ILLEGAL_REPORT, desc = "举报信息上报")
public final class IllegalReportHandler extends IClientHandler<ReportIllegal> {

//	@Autowired
//	private JavaMailSenderImpl mailSender;
	@Override
	protected void execute(ReportIllegal req, Request topRequest, C2SSession session) throws Exception {
		boolean IsOK = false;
		IsOK = checkEmail(req.getEmail());
		if(IsOK){
			if(StringUtils.isBlank(req.getContent())){
				IsOK = false;
			}else{
				ReportIllegalModel model = new ReportIllegalModel();
				model.setContent(req.getContent());
				model.setCreate_time(new Date());
				model.setEmail(req.getEmail());
				model.setImgUrlList(com.alibaba.fastjson.JSON.toJSONString(req.getImgUrlListList()));
				model.setState(0);
				SpringService.getBean(MongoDBService.class).getMongoTemplate().save(model);
//				try{
//					StringBuffer reportContent = new StringBuffer();
//					reportContent.append("举报者游戏id:").append(req.getAccountId()).append("      \n");
//					reportContent.append("举报内容:\n").append(req.getContent()).append("      \n");
//					reportContent.append("图片链接:\n").append(com.alibaba.fastjson.JSON.toJSONString(req.getImgUrlListList()));
//					MimeMessage message = mailSender.createMimeMessage();
//					MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");//
//					helper.setSubject("闲逸游戏数据统计"); // 主題
//					helper.setFrom(req.getEmail()); // 寄件者
//					String[] users = new String[] { "xianyihuyu@163.com" };
//					helper.setTo(users); // 收件人
//					helper.setText(reportContent.toString(), true); // 內容(HTML)
//					mailSender.send(message);
//					logger.info("邮件发送成功");
//				}catch(Exception e){
//					logger.error("邮件发送失败",e);
//				}
			}
			
			
		}
		if(IsOK){
			session.send(MessageResponse.getMsgAllResponse("举报反馈成功!").build());
		}else{
			session.send(MessageResponse.getMsgAllResponse("举报反馈失败!").build());
		}
	}
	public static boolean checkEmail(String email){// 验证邮箱的正则表达式 
		return true;
	  }
//	   String format = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
//	   //p{Alpha}:内容是必选的，和字母字符[\p{Lower}\p{Upper}]等价。如：200896@163.com不是合法的。
//	   //w{2,15}: 2~15个[a-zA-Z_0-9]字符；w{}内容是必选的。 如：dyh@152.com是合法的。
//	   //[a-z0-9]{3,}：至少三个[a-z0-9]字符,[]内的是必选的；如：dyh200896@16.com是不合法的。
//	   //[.]:'.'号时必选的； 如：dyh200896@163com是不合法的。
//	   //p{Lower}{2,}小写字母，两个以上。如：dyh200896@163.c是不合法的。
//	   if (email.matches(format))
//	    { 
//	     return true;// 邮箱名合法，返回true 
//	    }
//	   else
//	    {
//	     return false;// 邮箱名不合法，返回false
//	    }
//	  } 
}
