<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<div class="title_area">
	<h2>平台管理系统</h2>
</div>

<div class="condition_area">
	<div class="mod_select_down">
		<a href="javascript:void(0);" onclick="return false;" id="search_type_btn" class="btn_white_2 btn_select_outline"><span>所有类型</span><i class="ico"></i></a>
		<ul class="down_list btn_select_outline" id="search_type_list" style="display: none;"><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">所有类型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">高性能版-超微A型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">高性能版-超微B型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">高性能版-微型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">高性能版-小型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">高性能版-A型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">高性能版-B型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">高性能版-C型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">高性能版-大型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">标准版-微型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">标准版-小型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">标准版-标准型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">标准版-大型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">单机版-微型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">单机版-小型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">单机版-A型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">单机版-B型</a></li><li><a href="javascript:void(0);" onclick="return false;" _search_type="1">单机版-C型</a></li><li class="last"><a href="javascript:void(0);" onclick="return false;" _search_type="1">单机版-大型</a></li></ul>
	</div>
	<div class="mod_search ">
		<span class="text_area"><textarea id="search_input" class="">请输入访问地址或名称(换行分隔)</textarea></span>
		<button type="submit" id="btn_search"><span class="visually_hidden">搜索</span></button>
		<div class="qz_bubble warn_bubble" style="display: none;">
			<i class="ico ico_arrow"></i>
			<div class="bubble_i">
				<i class="ico ico_warn_17"></i>请输入正确的服务器名称
			</div>
		</div>
	</div>
	<div id="returnList" class="btn_area" style="display:none;">
		<a href="javascript:void(0);" onclick="return false;" class="btn btn_gray_v2"><span>返回列表</span></a>
	</div>
	<div class="mod_page_v2" id="pager"><span class="page_text">1/1 页</span></div>
</div>
                
<div class="bd" style="padding-top: 10px;">
	<div class="bd_title">
		<div class="bd_title_btns">
			<a id="btn_rename" href="javascript:void(0);" onclick="return false;" class="btn btn_disable_v2" title="请选择一个实例"><span>改名</span></a>
			<a id="btn_back" href="javascript:void(0);" style="display: none;" onclick="return false;" class="btn btn_disable_v2" title="请选择一个或多个实例"><span>退还</span></a>
			<a id="btn_project" href="javascript:void(0);" onclick="return false;" class="btn btn_disable_v2" title="请选择一个或多个实例" style="display: none;"><span>分配至项目</span></a>
		</div>
		<div class="bd_title_op">
			<a id="btn_refresh" href="javascript:void(0);" onclick="return false;" class="ico ico_refresh" title="刷新当前页面"><span class="visually_hidden">刷新</span></a>
			<a id="btn_setting" href="javascript:void(0);" onclick="return false;" class="ico ico_setting" title="设置列表显示字段"><span class="visually_hidden">设置</span></a>
		</div>
	</div>
                            
	<div class="table_area" style="overflow: auto;">
		<div class="bd_title_table">                
			<table>
				<colgroup>
						<col class="col1">
						<col class="col2">
						<col class="col3">
						<col class="col4">
						<col class="col5">
					</colgroup>
				<thead>
				<tr>
				<th style="width: 200px;"><div class="op"><a href="javascript:void(0);" onclick="return false;" class="btn_select_16"><span class="visually_hidden">checkbox</span></a></div></th>
				<th style="width: 301px;"><div class="op"><i class="ico ico_stick_left"></i><a href="javascript:void(0);" onclick="qcloudTable.sort(this);return false;" title="名称" _id="cdb_name" _status="down" class="op_rank textoverflow"><span class="table_title">名称</span><i class="ico ico_trangledown" style="display:none;"></i></a><i class="ico ico_stick_right" _resize_ico="2"></i></div></th>
				<th style="width: 301px;"><div class="op"><a href="javascript:void(0);" onclick="qcloudTable.sort(this);return false;" title="访问地址" _id="cdb_addr" _status="down" class="op_rank textoverflow"><span class="table_title">访问地址</span></a><i class="ico ico_stick_right" _resize_ico="3"></i></div></th>
				<th style="width: 301px;"><div class="op"><a href="javascript:void(0);" onclick="qcloudTable.sort(this);return false;" title="实例类型" _id="cdb_type" _status="down" class="op_rank textoverflow"><span class="table_title">实例类型</span></a><i class="ico ico_stick_right" _resize_ico="4"></i></div></th>
				<th style="width: 310px;"><div class="op"><a href="javascript:void(0);" onclick="qcloudTable.sort(this);return false;" title="操作" _id="operation" _status="down" class="op_rank textoverflow"><span class="table_title">操作</span></a></div></th>
				</tr>
				</thead>
			</table>
		</div>
		<div class="table_mask" id="cont_table_area" style="height: 536px; width: 100%;">
			<div class="table_blank" style="display: none;">
				<p class="text">您还没实例， <a class="link" href="http://manage.qcloud.com/shoppingcart/shop.php">立即创建</a></p>
			</div>
			<div class="bd_content_table">
				<table>
					<colgroup>
						<col class="col1">
						<col class="col2">
						<col class="col3">
						<col class="col4">
						<col class="col5">
					</colgroup>
					<tbody>
						<tr seq="0">
							<td class="line_checkbox" style="width: 99px;"><a
								href="javascript:void(0);" onclick="return false;"
								class="btn_select_16"><span class="visually_hidden">checkbox</span></a></td>
							<td class="name" style="width: 194px;"><div class="name_text">
									<span title="" class="text textoverflow" style="width: 174px;">cdb70235</span><a
										class="ico ico_edit_20 j_btn_edit" href="javascript:void(0);"
										onclick="return false;" title="编辑" style="display: none;"></a>
									<div class="edit_pad"
										style="min-height: 90px; height: auto; display: none;">
										<input type="text" class="input_style" maxlength="60"
											onkeydown="qcloud.cdb.rename.num(this);"
											onkeyup="qcloud.cdb.rename.num(this);"
											onblur="qcloud.cdb.rename.num(this,1);"
											onfocus="qcloud.cdb.rename.num(this,2);" value="test"
											placeholder="60个字以内中英文,数字,连接符-">
										<p>
											<span class="tip_word">还能输入<span class="num"></span>个字符
											</span><span class="op"><a
												class="btn btn_blue j_rename_submit"
												href="javascript:void(0);" onclick="return false;"><span>保存</span></a><a
												class="link j_rename_cancel" href="javascript:void(0);"
												onclick="return false;">取消</a></span>
										</p>
									</div>
								</div></td>
							<td style="width: 200px;"><div class="name_text">
									<span title="" class="text textoverflow" style="width: 180px;">10.66.100.75:3306</span>
								</div></td>
							<td style="width: 200px;"><div class="name_text">
									<span title="" class="text textoverflow" style="width: 180px;">高性能版-微型</span>
								</div></td>
							<td style="width: 200px;"><div class="name_text">
									<span title="" _charset="05a8e0f8-c92e-11e3-a203-3cd92b056b40"
										class="text textoverflow" style="width: 180px;">utf8</span>
								</div></td>
							<td style="width: 214px;"><div class="name_text">
									<span class="text text_link"><a
										href="javascript:void(0);"
										onclick="qcloud.cdb.phpAdmin(0);return false;" class="link">phpMyAdmin</a><a
										href="javascript:void(0);"
										onclick="qcloud.cdb.showDbCfgBox(0);return false;"
										class="link">配置</a><a href="javascript:void(0);"
										onclick="qcloud.cdb.showXFeebox(0);return false;" class="link">续费</a></span>
								</div></td>
						</tr>
					</tbody>
				</table>
			</div>
        </div>
    </div>
</div>


<html>
    <head>
        <title>列表</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link href="../css/css.css" type="text/css" rel="stylesheet"/>
    </head>
    <body>
    <a href="<c:url value='/mm/platform/create.do'/>">新增</a><br/>
    <table border="1" class="gridtable">
        <tr>
         				<th>PID</th>
                        <th>平台</th>
                        <th>创建时间</th>
                        <th>&nbsp;</th>
        </tr>
        <c:forEach items="${platformList}" var="platform" varStatus="status">
        <tr>
                        <td>${platform.id}</td>
                        <td>${platform.name }</td>
                         <td>${platform.addTime}</td>
                        <td><a href="<c:url value='/mm/platform/delete.do?id=${platform.id}'/>">删除</a>|<a href="<c:url value='/mm/platform/update.do?id=${platform.id}'/>">修改</a></td>
        </tr>
        </c:forEach>
    </table>
    </body>
</html>