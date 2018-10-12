<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<link href="${pageContext.request.contextPath}/css/jquery-ui-1.10.4.custom.min.css" rel="stylesheet" media="screen">
<link href="${pageContext.request.contextPath}/css/pqgrid.min.css" rel="stylesheet" media="screen">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/themes/Office/pqgrid.css" />
<% request.setAttribute("ctx", request.getContextPath()); %>

<style>

</style>
<script type="text/javascript">

</script>
<div class="title_area">
	<h2>操作日志</h2>
</div>
<form action="${ctx}/mm/operationLog/list.do" id="searchLogForm" method="get">
<div class="search">
    <span class="tip">账号名：</span><input type="text" id="userName"  name="userName" class="input" maxlength="20" value="${userName}"/>
    <span class="tip">查询起始时间：</span><input id="startTime" name="startTime" value="${startTime}" class="input" maxlength="20" readonly="readonly" onfocus="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss'})"/>
    <span class="tip">查询结束时间：</span><input id="endTime" name="endTime" value="${endTime}" class="input" maxlength="20" readonly="readonly" onfocus="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss'})"/>
    <button id="searchLog" class="btn btn_submit not_close btn_blue" type="Submit" style="margin-left:20px;">查询</button>
    <span id="search_error" class="tip_error"></span>
</div>
<div class="bd" style="padding-top: 10px;">
	<div class="bd_title">
	</div>

	<div class="table_area" style="overflow: auto;">
		<div class="table_mask" id="cont_table_area" style="width: 100%;">
			<div class="pq-grid bd_content_table"  style="overflow: auto; height: 560px;">
			<table >
				<thead style="height: 31px">
					<tr>
					<th style="width:30px;"><div class="op"><a href="javascript:void(0);" class="btn_select_16" style="margin-left: 5px"><span class="visually_hidden">checkbox</span></a> </div></th>
					<th style="width:7%"><div class="op"><span class="table_title">ID</span></div></th>
					<th style="width:7%"><div class="op"><span class="table_title">Type</span></div></th>
					<th style="width:7%"><div class="op"><span class="table_title">UserId</span></div></th>
					<th style="width:10%"><div class="op"><span class="table_title">UserName</span></div></th>
					<th style="width:14%"><div class="op"><span class="table_title">Fun</span></div></th>
					<th style="width:auto"><div class="op"><span class="table_title">Value</span></div></th>
					<th style="width:10%"><div class="op"><span class="table_title">AddTime</span></div></th>
					<th style="width:10%"><div class="op"><span class="table_title">lastLoginIp</span></div></th>
					</tr>
				</thead>
				<tbody>
					<c:forEach items="${list}" var="operationLog" varStatus="status">
					<tr>
						<td class="line_checkbox"><a href="javascript:void(0);" onclick="return false;" class="btn_select_16"><span class="visually_hidden">checkbox</span></a></td>
						<td class="name"><div class="name_text">
								<span title="" class="text textoverflow">${operationLog.id}</span>
							</div></td>
						<td><div class="name_text">
								<span title="" class="text textoverflow">${operationLog.type}</span>
							</div></td>
						<td><div class="name_text">
								<span title="" class="text textoverflow">${operationLog.userId}</span>
							</div></td>
						<td><div class="name_text">
								<span title="" class="text textoverflow">${operationLog.userName}</span>
							</div></td>
						<td><div class="name_text">
								<span title="" class="text textoverflow">${operationLog.fun}</span>
							</div></td>
						<td><div class="name_text">
								<span title="" class="text textoverflow">${operationLog.value}</span>
							</div></td>
						<td><div class="name_text">
								<span title="" class="text textoverflow">${operationLog.addTime}</span>
							</div></td>
						<td><div class="name_text">
								<span title="" class="text textoverflow">${operationLog.lastLoginIp}</span>
							</div></td>
					</tr>
					</c:forEach>
				</tbody>
				<tfoot>
				<tr><td colspan="8" align="center">
					<div class="pq-grid-bottom ui-corner-bottom">	
					<div class="pq-grid-footer pq-pager">&nbsp;
						<!-- firstPage -->
						<button id="firstPage" type="button" title="First Page" class="ui-button ui-widget ui-corner-all ui-button-icon-only"
							style="background:url(${ctx}/css/images/page-first-gray.gif); width:21px; height:21px; ">
							<span class="ui-button-icon-primary pq-page-first"></span>
							<span class="ui-button-text"></span>
						</button>
						<!-- previousPage -->
						<button id="previousPage" type="button" title="Previous Page" class="ui-button ui-widget ui-corner-all ui-button-icon-only"
							style="background:url(${ctx}/css/images/page-prev-gray.gif); width:21px; height:21px; ">
							<span class="ui-button-icon-primary pq-page-prev"></span>
							<span class="ui-button-text"></span>
						</button>
						<span class="pq-separator"></span>
						<span class="pq-pageholder"><span>Page</span>
						<input id="pageNo" name="pageNo" type="text" value="${pageNo}" tabindex="0">
						<span>of</span><span class="total">${pageNum}</span></span><input id="pageNum" type="hidden" value="${pageNum}">
						<span class="pq-separator"></span>
						<!-- nextPage -->
						<button id="nextPage" type="button" title="Next Page" class="ui-button ui-widget ui-corner-all ui-button-icon-only"
							style="background:url(${ctx}/css/images/page-next-gray.gif); width:21px; height:21px; ">
							<span class="ui-button-icon-primary pq-page-next"></span>
							<span class="ui-button-text"></span>
						</button>
						<!-- lastPage -->
						<button id="lastPage" type="button" title="Last Page" class="ui-button ui-widget ui-corner-all ui-button-icon-only"
							style="background:url(${ctx}/css/images/page-last-gray.gif); width:21px; height:21px; ">
							<span class="ui-button-icon-primary pq-page-last"></span>
							<span class="ui-button-text"></span>
						</button>
						<span class="pq-separator"></span>
						<span>Records per page:</span>
						<select id="pageSize" name="pageSize" style="padding:0;" >
							<option value="10">10</option>
							<option value="20">20</option>
							<option value="30">30</option>
							<option value="40">40</option>
							<option value="50">50</option>
							<option value="100">100</option>
						</select>
						<span class="pq-separator"></span>
						<!-- refresh -->
						<button id="refreshPage" type="button" title="Refresh" class="ui-button ui-widget ui-corner-all ui-button-icon-only"
							style="background:url(${ctx}/css/images/Refresh.gif); width:21px; height:21px;display: none; ">
							<span class="ui-button-text"></span>
						</button>
					</div></div>
				<td></tr>
				</tfoot>
			</table>
        </div>
    </div>
</div>
</div>
</form>
<div id="mask_div" class="ui_mask" style="width: 1920px; height: 1746px; position: absolute; top: 0px; left: 0px; display: none; background-color: rgb(0, 0, 0); opacity: 0.6; z-index: 999; background-position: initial initial; background-repeat: initial initial;"></div>

<script>

	$(function() {
		$("#searchLogForm").submit(function(event) {
			// 停住原来的表单提交事件
			event.preventDefault();
			/// alert($("#isSearch").val());
			$("#pageNo").val("");
			$.ajax({
					type : "GET",
					url : $(this).attr('action'),
					data : $(this).serialize(),// 你的formId
					success : function(data) {
						$('.content_main').html(data);
					}
				});	
		});

		var startTime = "${startTime}".replace(" ", "%20"); // 置换时间参数中的空格
		if(!startTime){
			var date = new Date();
			date.setDate(date.getDate()-1);
			$("#startTime").val(date.Format('yyyy-MM-dd')+" 00:00:00");
			startTime = $("#startTime").val();
		}
		var endTime = "${endTime}".replace(" ", "%20"); // 置换时间参数中的空格
		if(!endTime){
			$("#endTime").val(new Date().Format('yyyy-MM-dd hh:mm:ss'));
			endTime = $("#endTime").val();
		}
		/// 第一页
		$("#firstPage").click(function(){
			if (isNaN(parseInt("${pageNo}"))){
				return;
			}
			if(parseInt("${pageNo}") <= 1){
				alert("This is the first page.");
				return;
			}
			$('.content_main').load("${ctx}/mm/operationLog/list.do?userName=${userName}&startTime=" + startTime + "&endTime=" + endTime + "&pageNo=1&pageSize=${pageSize}");
		});

		/// 上一页
		$("#previousPage").click(function(){
			if (isNaN(parseInt("${pageNo}"))){
				return;
			}
			if(parseInt("${pageNo}") <= 1){
				alert("This is the first page.");
				return;
			}
			var prevPageNum = parseInt("${pageNo}") - 1;
			$('.content_main').load("${ctx}/mm/operationLog/list.do?userName=${userName}&startTime=" + startTime + "&endTime=" + endTime + "&pageNo=" + prevPageNum + "&pageSize=${pageSize}");
		});

		/// 下一页
		$("#nextPage").click(function(){
			if (isNaN(parseInt("${pageNo}"))){
				return;
			}
			if(parseInt("${pageNo}") >= parseInt("${pageNum}")){
				alert("This is the last page.");
				return;
			}
			var nextPageNum = parseInt("${pageNo}") + 1; // 下一页页码
			$('.content_main').load("${ctx}/mm/operationLog/list.do?userName=${userName}&startTime=" + startTime + "&endTime=" + endTime + "&pageNo=" + nextPageNum + "&pageSize=${pageSize}");
		});

		/// 最好一页
		$("#lastPage").click(function(){
			if (isNaN(parseInt("${pageNo}"))){
				return;
			}
			if(parseInt("${pageNo}") >= parseInt("${pageNum}")){
				alert("This is the last page.");
				return;
			}
			$('.content_main').load("${ctx}/mm/operationLog/list.do?userName=${userName}&startTime=" + startTime + "&endTime=" + endTime + "&pageNo=${pageNum}&pageSize=${pageSize}");
		});

		/// 跳转到第N页
		$("#refreshPage").click(function(){
			var refreshPageNum = $("#pageNo").val();
			if (isNaN(refreshPageNum)) { 
				alert("Please enter the correct value."); 
				return;
			}
			refreshPageNum = parseInt(refreshPageNum);
			if(parseInt("${pageNo}") == refreshPageNum || refreshPageNum < 1){
				alert("The wrong page.");
				return;
			}
			if(refreshPageNum > parseInt("${pageNum}")){
				alert("The wrong page");
				return;
			}
			$('.content_main').load("${ctx}/mm/operationLog/list.do?userName=${userName}&startTime=" + startTime + "&endTime=" + endTime + "&pageNo=" + refreshPageNum + "&pageSize=${pageSize}");
		});

		/// 变更每页显示数据数量
		$("#pageSize").change(function(){
			if (isNaN(parseInt("${pageNo}"))){
				return;
			}
			var oldPageNo = parseInt("${pageNo}"); // 当前页码
			var oldPagesize = parseInt("${pageSize}"); // 每页显示数量old
			var newPagesize = parseInt($('#pageSize option:selected').val());  // 每页显示数量new
			var newPageNo = 1; // new当前页码
			if(newPagesize == oldPagesize){
				return;
			}
			if(newPagesize > oldPagesize){
				newPageNo = Math.ceil((oldPagesize*oldPageNo)/newPagesize); // 向上取整
			} else if(newPagesize < oldPagesize){
				newPageNo = Math.floor((oldPagesize*oldPageNo)/newPagesize); // 向下取整
			}
			$('.content_main').load("${ctx}/mm/operationLog/list.do?userName=${userName}&startTime=" + startTime + "&endTime=" + endTime + "&pageNo=" + newPageNo + "&pageSize=" + newPagesize);
		});

		/// pageSize控件 each方法; 给select控件赋值
		$('#pageSize option').each(function(){
        	if($(this).val() == "${pageSize}"){
        		$(this)[0].selected = true;
        		return false;
        	}
        });
	});
</script>