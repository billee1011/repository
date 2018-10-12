<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<div class="table_area" style="overflow: auto;">
    <div class="table_mask" id="cont_table_area" style="width: 100%;">
         <c:choose>
		    <c:when test="${recordsCount le 0}">
			  <div class="table_blank">
				<p class="text">还没有补偿记录</p>
		      </div>
			</c:when>
			<c:otherwise>
			   <div>
					<table id="redeemMailRecordList" style="height: 600px;">
					  <thead style="height: 31px">
						<tr>
							<th data-options="field:'id',width:calColumnWidth(0.04)">ID</th>
						    <th data-options="field:'addTime',width:calColumnWidth(0.09),formatter:function(value,row,index){
								var date = new Date(value);
								return date.bfFormatter();
							}">时间</th>
							<th data-options="field:'adminName',width:calColumnWidth(0.06)">管理员</th>
							<th data-options="field:'ip',width:calColumnWidth(0.06)">IP</th>
							<th data-options="field:'displayareas',width:calColumnWidth(0.1),formatter:function(value,row,index){
								if(row['allArea']){
								    return '所有服务器';
								}
							    return row['areas'];
							}">区服</th>
							<th data-options="field:'allArea',hidden:true">是否全服玩家</th>
							<th data-options="field:'areas',hidden:true">所选区服</th>
							<th data-options="field:'isAll',hidden:true">是否所有玩家</th>
							<th data-options="field:'players',width:calColumnWidth(0.1),formatter:function(value,row,index){
							     if(row.all){
							         return '全部玩家';
							     }else{
							     	return value;
							     }
							}">目标玩家</th>
							<th data-options="field:'redeemMsg',width:calColumnWidth(0.15)">邮件内容</th>
						</tr>
					  </thead>
					  <tbody>
					  </tbody>
					</table>
			    </div>
			</c:otherwise>
		</c:choose>
    </div>
</div>
<script type="text/javascript">
$(function(){
	   commonServerPagination('#redeemMailRecordList', undefined, "${ctx}/pss/redeem/redeemmailrecord.do");
});
</script>
