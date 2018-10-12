<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<div class="pop_layer_main">
<div class="pop_layer_title annoucingListTitle">
   <h3>发布中的公告
   <span id="announcingList_error" class="tip_error"></span>
   </h3>
</div>
<div class="pop_layer_cont">
	<div class="table_area" style="overflow: auto;">
		<div class="table_mask" id="cont_table_area" style="width: 100%;">
		  <c:choose>
		    <c:when test="${empty announcingList}">
			  <div class="table_blank">
				<p class="text">没有发布中的公告</p>
		      </div>
			</c:when>
			<c:otherwise>
			<div>
			    <span id="announcingListError" class="tip_error"></span>
			</div>
				<div>
				  <table id="announcingListTable" style="width:900px;height:500px;border:1px solid #ccc;">
					<thead style="height: 31px">
						<tr>
						  
							<th style="width: 35px;" data-options="field:'check'"><div class="op"><a href="javascript:void(0);" class="btn_select_16" style="margin-left: 5px"><span class="visually_hidden">checkbox</span></a> </div></th>
							<th style="width:5%" data-options="field:'id'"><div class="op"><span class="table_title">公告ID</span></div></th>
							<th style="width:15%" data-options="field:'startTime'"><div class="op"><span class="table_title">开始时间</span></div></th>
							<th style="width:15%" data-options="field:'endTime'"><div class="op"><span class="table_title">结束时间</span></div></th>
							<th style="width:20%" data-options="field:'interval'"><div class="op"><span class="table_title">时间间隔</span></div></th>
							<th style="width:20%" data-options="field:'admin'"><div class="op"><span class="table_title">管理员</span></div></th>
							<th style="width:10%" data-options="field:'platform'"><div class="op"><span class="table_title">平台</span></div></th>
							<th style="width:20%" data-options="field:'areanames'"><div class="op"><span class="table_title">区服</span></div></th>
							<th style="width:5%" data-options="field:'pf'"><div class="op"><span class="table_title">渠道</span></div></th>
							<th style="width:210px;" data-options="field:'opt'"><div class="op"><span class="table_title">操作</span></div></th>
							 <th style="width:5%" data-options="field:'content'"><div class="op"><span class="table_title">公告内容</span></div></th>
						</tr>
					</thead>
						<tbody>
						 <c:forEach items="${announcingList}" var="announcing" varStatus="status">
							<tr>
								<td class="line_checkbox"><a href="javascript:void(0);" onclick="return false;" class="btn_select_16"><span class="visually_hidden">checkbox</span></a></td>
								<td>${announcing.id}</td>
								<td><fmt:formatDate value="${announcing.beginTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
								<td><fmt:formatDate value="${announcing.endTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
								<td>${announcing.interval}</td>
								<td>${announcing.userName}</td>
								<td>${announcing.platformName}</td>
								<td>${announcing.areaNames}</td>
								<td>${announcing.pf}</td>
								<td><div class="name_text">
										<span class="text text_link"><a href="javascript:;" onclick="toDeleteAnnounce('${ctx }/pss/announce/deleteAnnounce.do?id=${announcing.id}')">删除</a></span>
									</div></td>
								<td>${announcing.content }</td>
							</tr>
							</c:forEach>
						</tbody>
					</table>
				  </div>
				</c:otherwise>
			</c:choose>
        </div>
    </div>
    <div style="margin-bottom: 10px;"></div>
     <div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:$('#announcingList').hide();"><span>关闭</span></a></div>
</div>
</div>
<script type="text/javascript">
    
    $(function(){
    	commonPagination('#announcingListTable');
    });
</script>