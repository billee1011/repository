<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%request.setAttribute("ctx", request.getContextPath());%>
<!-- 后台管理主页面 -->
<div class="manage_content mod_cdb">
	<!-- 左边导航开始 -->
	<div class="mod_slidenav" id="slide_nav">
		<h3><div><em>后台管理</em></div></h3>
		<div class="slidenav_area">
			<ul>
				<li>
                	<a href="${ctx}/mm/platform/list.do">平台管理<i class="ico ico_arrowright"></i></a>
            	</li>
            	<li>
                	<a href="${ctx}/mm/user/list.do">账号管理<i class="ico ico_arrowright"></i></a>
            	</li>
            	<li>
                	<a href="${ctx}/mm/role/list.do">权限管理<i class="ico ico_arrowright"></i></a>
            	</li>
            	<li>
                	<a href="${ctx}/mm/operationLog/list.do?isSearch=false">操作日志<i class="ico ico_arrowright"></i></a>
            	</li>
        	</ul>
    	</div>
	</div>
	<!-- 左边导航结束啦 -->
	<div class="mod_slidenav slidenav_min" id="slide_nav_min" style="display:none;">
		<a href="javascript:void(0);" id="btn_show_left_nav" class="slidenav_btn_op" title="点击展开"></a>
	</div>
    
    <!--运营系统主页面-->
    <div class="mod_content" id="mod_content_wrapper">
        <a class="btn_op" href="javascript:void(0);" id="btn_hide_left_nav" title="点击收起"><span class="visually_hidden">openclose</span></a>
		<div class="content_main" style="height: 800px">
			<div class="title_area">
				<h2>后台管理</h2>
			</div>
        </div>
    </div>
</div>
<script type="text/javascript">
$(function(){
	// AJAX去加载页面, 显示加载图片，加载目标页面，隐藏加载图片
	$("div.slidenav_area ul li a").click(function(){
		if($(this).hasClass('parentmenu')){
			if(!$(this).parent().hasClass("selected")){
				$(this).parent().parent().find(".selected")	.removeClass("selected");
			}
			$(this).parent().toggleClass("selected");
			//$(this).parent().addClass("selected");
		}else{
			$(this).parent().parent().find(".selected")	.removeClass("selected");
			$(this).parent().addClass("selected");
			$(".content_main").load($(this).attr('href'));
		}
		return false;
	});
	
	$("#btn_show_left_nav").click(function(){
		$('div[id=slide_nav]').show();
		$('div[id=slide_nav_min]').hide();
		$('#btn_hide_left_nav').show();
		$('div[id=mod_content_wrapper]').css("margin-left", 210);
	});
	$('#btn_hide_left_nav').click(function(){
		$(this).hide();
		$('div[id=slide_nav]').hide();
		$('div[id=slide_nav_min]').show();
		$('div[id=mod_content_wrapper]').css("margin-left", 12);
	});
});
</script>