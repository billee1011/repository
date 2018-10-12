<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<% request.setAttribute("ctx", request.getContextPath()); %>
<!--[if IE]><script type="text/javascript">alert('<spring:message code="can.not.use.ie.msg" text="嘿嘿，我们的游戏管理平台不支持IE浏览器,请换个浏览器吧，感谢您的支持~"/>')</script><![endif]-->
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title><spring:message code="index.ui.title" text="乐游科技管理平台"/></title>
		
	<link href="${ctx }/favicon.ico" rel="bookmark" type="image/x-icon" /> 
	<link href="${ctx }/favicon.ico" rel="icon" type="image/x-icon" /> 
	<link href="${ctx }/favicon.ico" rel="shortcut icon" type="image/x-icon" /> 
		
	<style type="text/css"> .j_tip { display: none } #tip_box_wrapper { display: none } #login_box_wrapper { display: } </style>
	<link href="${ctx }/css/global/g_min.css" rel="stylesheet" media="screen">
	<link href="${ctx }/css/login/login.css" rel="stylesheet" media="screen">
	<link href="${ctx }/css/login/h_qr_login_1.css" rel="stylesheet" type="text/css">
	<script src="${ctx }/js/jquery-2.1.1.min.js" type="text/javascript"></script>
	<script src="${ctx }/js/jquery.md5.js" type="text/javascript"></script>
	<script type="text/javascript">
		if($('#homePagePassed').html() == 1){
			window.location = '${ctx}'; //加载过center了  跳转到首页
		}
	</script>
</head>
<body>
	<div class="head_v2">
		<div class="log_area">
			<div class="mod_inner">
				<p><span class="tip_word"><spring:message code="index.ui.head.tip.word" text="欢迎使用英雄领主游戏管理平台!"/></span></p>
				<ul class="op_area">
					<li><a href="${ctx }/changeLocal.do?locale=en_US" class="btn login_btn">English</a></li>
					<li><a href="${ctx }/changeLocal.do?locale=zh_CN" class="btn regist_btn">中文</a></li>
				</ul>
			</div>
		</div>
		<div class="nav_area">
			<div class="mod_inner">
				<h1 class="mod_logo" style="margin-left: -20px">
					<object type="application/x-shockwave-flash" id="logo" name="logo" data="${ctx }/images/logo.swf" width="300" height="70" style="visibility: visible;">
						<param name="menu" value="true"><param name="salign" value="T"><param name="scale" value="noscale"><param name="wmode" value="transparent"><param name="allowScriptAccess" value="always"><param name="align" value="middle"><param name="base" value="">
					</object>
				</h1>
			</div>
		</div>
	</div>

	<div class="body">
		<div class="bg_body"></div>
		<div class="login_wrapper">
			<div class="login_text login_text1"></div>
			<div class="login_text login_text2"></div>
			<div class="login_text login_text3"></div>
			<div class="mod_login_wrapper">
				<div id="login_box_wrapper">
					<div id="no_login_msg" class="mod_login_intro j_tip" style="display: none;">
						<p><spring:message code="index.ui.login.warning.msg" text="你已连续登录错误10次啦，系统已冻结了你账号，{0}小时后再来尝试吧!"/></p>
						<a href="javascript:void(0);" target="_blank" class="reg_link"><spring:message code="index.ui.security.center" text="安全中心"/>&gt;&gt;</a>
					</div>
					<div class="mod_login" id="loginbox" style="padding: 5px 0px; height: 305px; visibility: visible;">
						<div class="login_no_qlogin" id="login" style="border: 0px; display: block;">
							<div id="header" class="header">
								<div class="switch" id="switch"><a class="switch_btn_focus" id="switch_login" tabindex="8"><spring:message code="index.ui.login.title" text="帐号登录"/></a></div>
							</div>
						
							<!--二维码登录切换-->
							<div class="qrswitch" id="qrswitch" style="display: block;">
								<a class="qrswitch_logo" id="qrswitch_logo" href="javascript:void(0)" draggable="false" title="二维码登录"></a>
							</div>
							<!--二维码登录切换end-->
						
							<div class="web_qr_login" id="web_qr_login" style="display: block; height: 265px;">
								<!--为了动画-->
								<div class="web_qr_login_show" id="web_qr_login_show">
									<!--普通登录-->
									<div class="web_login" id="web_login">
										<div class="tips" id="tips">
											<div class="error_tips" id="error_tips" style="display: none;">
												<span class="error_logo" id="error_logo"></span><span class="err_m" id="err_m"></span>
											</div>
											<div class="operate_tips" id="operate_tips">
												<span class="operate_content"><spring:message code="index.ui.login.reminder" text="温馨提示:您的所有操作将会被记录 "/>&nbsp;<a class="tips_link" id="bind_account" href="javascript:void(0);"><spring:message code="index.ui.security.center" text="安全中心"/></a></span>
												<span class="down_row"></span>
											</div>
											<div class="loading_tips" id="loading_tips">
												<span id="loading_wording"><spring:message code="index.ui.login.logining" text="登录中..."/></span><img id="loading_img" src="${ctx }/images/index/load.gif" />
											</div>
										</div>
						
										<div class="login_form">
											<form id="loginform" autocomplete="off" name="loginform" action="${ctx }/login.do" method="post" target="_self" style="margin: 0px;">
												<!-- 外面放一个DIV点位，里面用来左右抖动 -->
												<div style="height: 55px">
													<div class="uinArea" id="uinArea">
														<label class="input_tips" id="uin_tips" for="u" style="display: block;"><spring:message code="index.ui.form.username" text="账号"/></label>
														<div class="inputOuter">
															<input type="text" class="inputstyle" id="u" name="u" value="" tabindex="1" maxlength="18"/> <a class="uin_del" id="uin_del" href="javascript:void(0);" style="display: none;"></a>
														</div>
													</div>
												</div>
												<div style="height: 55px">
													<div class="pwdArea" id="pwdArea">
														<label class="input_tips" id="pwd_tips" for="p" style="display: block;"><spring:message code="index.ui.form.password" text="密码"/></label>
														<div class="inputOuter">
															<input type="password" class="inputstyle password" id="p" name="p" value="" maxlength="16" tabindex="2" />
														</div>
														<div class="lock_tips" id="caps_lock_tips" style="display: none;">
															<span class="lock_tips_row"> </span><span>大写锁定已打开</span>
														</div>
													</div>
												</div>
												<div class="verifyArea" id="verifyArea" style="display: none;">
														<div class="verifyinputArea" id="verifyinputArea">
															<label class="input_tips" id="vc_tips" for="verifycode"><spring:message code="index.ui.form.verifycode" text="验证码"/></label>
															<div class="inputOuter">
																<input name="verifycode" type="text" class="inputstyle verifycode" id="verifycode" value="" maxlength="5" tabindex="3" />
															</div>
														</div>
													<div class="verifyimgArea" id="verifyimgArea">
														<img class="verifyimg" id="verifyimg" src="${ctx }/verifycode.do" title='<spring:message code="index.ui.form.verifyimg" text="看不清，换一张"/>'/>
														<a tabindex="4" href="javascript:void(0);" class="verifyimg_tips"><spring:message code="index.ui.form.verifyimg" text="看不清，换一张"/></a>
													</div>
												</div>
												<div class="submit" id="loginButtonArea">
													<a class="login_button" href="javascript:void(0);"> <input type="submit" tabindex="6" value='<spring:message code="index.ui.form.btnlogin" text="登录"/>' class="btn" id="login_button" /></a>
												</div>
											</form>
										</div>
										<div class="bottom" id="bottom_web" style="display: block;">
											<a href="javascript:void(0)" class="link" id="forgetpwd" target="_blank"><spring:message code="index.ui.form.forgetpwd" text="忘了密码？"/></a> <span class="dotted">|</span> <a class="link" id="feedback_web" href="javascript:void(0)" target="_blank"><spring:message code="index.ui.form.feedback" text="意见反馈"/></a>
										</div>
									</div>
									<!--普通登录end-->
								</div>
								<!--showArea end-->
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
	
	<div class="foot_v2">
		<div class="foot_title">
			<div class="inner">
				<ul class="nav_side">
					<li class=""><a href="javascript:void(0);" target="_blank" class="weibo"><span class="visually_hidden">收听微博</span></a></li>
				</ul>
			</div>
		</div>
		<div class="foot_content">
			<div class="inner">
				<div class="list_area">何苦做游戏...</div>
				<div class="footer_menu">
					<span class="en" >Copyright © 2013 - 2014 Lingyuwangluo. All Rights Reserved.<strong>灵娱网络 版权所有</strong></span>
				</div>
			</div>
		</div>
	</div>
</body>
<script>
var verifyCode = false;
$(function(){
	// 点击换个验证码...
	var verifyimgUrl = $("#verifyimg").attr("src"); $(".verifyimg_tips").click(function(){ $("#verifyimg").attr("src", verifyimgUrl + "?" + Math.random()); });
	// 控制输入框获得焦点时边上的效果变化
	$("input[type=text]").focus(function(){ $(this).parent().attr("class", "inputOuter_focus"); }).blur(function(){ $(this).parent().attr("class", "inputOuter"); });
	
	// 账号框一些边上的逻辑处理.
	$("#u").focus(function(){ $("#operate_tips").show(); $("#error_tips").hide(); }).blur(function(){ 
		$("#operate_tips").hide();
		// 发送AJAX请求去检查一个他要不要验证码
		var username = $(this).val();
		if ($.trim(username) != "") { 
			$.post("${ctx }/check.do", {"username" : username}, function(data) {
				if (data.errcode == "901") {
					showErrorMessage("<spring:message code='index.ui.login.err.msg.901' text='用户名不存在，请重新输入.'/>");
				} else if (data.errcode == "902"){
					showVerifyCode();
					showErrorMessage("<spring:message code='index.ui.login.err.msg.902' text='你已连续登录错误3次，请输入验证码.'/>");
				} else if (data.errcode == "903"){
					$("#no_login_msg").show();
					$("#verifyArea").show();
					$("#web_qr_login").height(385);
					showErrorMessage("<spring:message code='index.ui.login.err.msg.903' text='你已连续登录错误十次，请1小时后再登录.'/>");
				} else {// 正常就把验证码补上去
					$("#verifyArea").hide();
					$("#web_qr_login").height(256);
					$("#verifycode").val(data.verifycode);
				}
			});
		}
	}).focus().keyup(function(){ if ($.trim($(this).val()) == "") { $("#uin_del").hide(); $("#uin_tips").show(); } else { $("#uin_del").show(); $("#uin_tips").hide(); };});
	
	// 密码框边上的逻辑处理.
	$("#p").keyup(function(){ if ($.trim($(this).val()) == "") { $("#pwd_tips").show(); } else { $("#pwd_tips").hide(); };}).focus(function(){ $("#error_tips").hide(); });
	
	// 验证码
	$("#verifycode").keyup(function(){ if ($.trim($(this).val()) == "") { $("#vc_tips").show(); } else { $("#vc_tips").hide(); };}).focus(function(){ $("#error_tips").hide(); });
	
	// 账号后面的X号功能(清空前一个节点数据并获得焦点)
	$("#uin_del").click(function(){ $(this).prev().val("").focus(); });
	
	$("#loginform").submit(function(event) {
		// 停住原来的表单提交事件
		event.preventDefault();
		
		// // 取出表单原来的数据,检查登录参数
		var $form = $(this), url = $form.attr('action');
		var username = $form.find('input[name="u"]').val(), password = $form.find('input[name="p"]').val(), verifycode = $form.find('input[name="verifycode"]').val();
		if ($.trim(username) == "") { showErrorMessage("<spring:message code='index.ui.login.err.no.username' text='账号不能为空，请输入账号!'/>"); return false; }
		if ($.trim(password) == "") { showErrorMessage("<spring:message code='index.ui.login.err.no.password' text='密码不能为空，请输入密码!'/>"); return false; }
		if ($.trim(verifycode) == "") {
			if(verifyCode){
				showErrorMessage("<spring:message code='index.ui.login.err.no.verifycode' text='验证码不能为空，请输入验证码!'/>"); return false;
			}
		}
		
		// 显示登录中
		$("#loading_tips").show();
		//发送AJAX请求
		$.post(url, {"userName" : username, "password" : $.md5(password), "verifycode" : verifycode}).success(function(data) {
		  	if (data.id != 1) {
		  		// 账号错误，摇账号，密码错误，摇密码.
		  		if (data.id == "900"){
					//shake($("#verifyArea"));
					showErrorMessage("<spring:message code='index.ui.login.err.msg.900' text='验证码错误，请重新输入.'/>");
				} else if (data.id == "901") {
		  			shake($("#uinArea"));
		  			showErrorMessage("<spring:message code='index.ui.login.err.msg.901' text='账号不存在，请重新输入.'/>");
				} else if (data.id == "902"){
					shake($("#pwdArea"));
					showVerifyCode();
					showErrorMessage("<spring:message code='index.ui.login.err.msg.902' text='你已连续登录错误3次，请输入验证码.'/>");
				} else if (data.id == "903"){
					shake($("#pwdArea"));
					showErrorMessage("<spring:message code='index.ui.login.err.msg.903' text='你已连续登录错误十次，请1小时后再登录.'/>");
				} else if (data.id == "904"){
					shake($("#pwdArea"));
					showErrorMessage("<spring:message code='index.ui.login.err.msg.904' text='密码错误，请重新输入.'/>");
				} else if(data.id == "1014"){
					showErrorMessage("<spring:message code='index.ui.login.err.msg.1014' text='选择游戏区不存在.'/>");
				}
			}else{
				// 登录成功，直接跳转主页面
				parent.location.href = "${ctx }/center.do";
			}
		}).error(function() { 
			showErrorMessage("<spring:message code='index.ui.login.err.no.server' text='服务器离家出走了，请联系研发人员...'/>");
		});
	});
	
	function showVerifyCode(){
		verifyCode = true;
		$("#verifycode").val("");
		$("#verifyArea").show();
		$("#web_qr_login").height(385);
	}
	
	function showErrorMessage(message){
		// 隐藏掉登录中提示,输出并显示错误信息.
		$("#loading_tips").hide();
		$("#err_m").html(message);
		$("#error_tips").show();
	}
	
	// XXOO式的摇晃...
	function shake($panel){
	    //box_left = ($(window).width() -  $panel.width()) / 2;
	    box_left = 73;// 这里就这样写吧，不要动态去取了
	    $panel.css({'left': box_left, 'position':'absolute'});
	    for(var i=1; 4>=i; i++){
	        $panel.animate({left:box_left-(40-10*i)}, 50);
	        $panel.animate({left:box_left+2*(40-10*i)}, 50);
	    }
	}
});
</script>
</html>