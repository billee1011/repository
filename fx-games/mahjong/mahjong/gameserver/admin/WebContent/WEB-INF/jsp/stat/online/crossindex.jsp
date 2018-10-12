<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<% request.setAttribute("ctx", request.getContextPath()); %>
<style>
.noborder{border:none;}
.selectDiv{
	float: left;
    width: 5%;
    padding: 5px;
    border: 1px solid #999;
    background-color: #eee;
    text-align:center;
    margin-left:5px;
    margin-top:5px;
    height:20px;
    cursor:pointer;
}
.selectDiv:hover{
   color: red;
}
.mainDivs{
	width:98%;
	margin-left:20px;
	height:120px;
	overflow:auto;
}
</style>
<div class="title_area" style="height: 85px">
	<h2>跨服区在线</h2>
	<form>
		<div class="r relative" id="datePicker">
			<a class="time" id="datePicker_a">
			<span><p>日期选择<b id="displayDateTip" style="font-weight:normal"> : 查询日期</b><br><font class="calendar" id="dateValue">2014-06-04</font></p></span></a>
			<div class="timetxt hide panone" id="datePanel" style="width: 278px; left: 0px; display: none;">
				<div id="dateBanner" class="timetop"> 
				</div>
				<p class="TimeDef" id="timeInputRoom">
					自定<input type="text" id="startTime" name="startTime" class="Timeinput">到 <input type="text" id="endTime" name="endTime" class="Timeinput mrnone">
				</p>
				<div class="opeDiv after">
					<small id="datamessage" style="display: none; margin-left: 30px;" class="l"></small>
					<a class="timecolse r Confirm" id="confirmBtn">确定</a>
					<a class="r Cancel" id="cancelBtn">取消</a>
				</div>
			</div>
		</div>
	</form>
</div>
<div class="area_title_line" style="margin-left:20px;height:30px;">
			<div class="mod_select_down" style="float:left;">
                   <a href="javascript:void(0);" onclick="return false;" id="search_type_btn2" class="btn_white_2 btn_select_outline" value="-1"><span>全部</span><i class="ico"></i></a>
                   <div id="search_type_list_div2" style="display:none">
                   <ul class="down_list btn_select_outline" id="search_type_list2">
						<li>
							<a href="javascript:void(0);" onclick="return false;" _search_type="1" value="-1">全部</a>
               			</li>
               			<li>
							<a href="javascript:void(0);" onclick="return false;" _search_type="1" value="0">全功能型</a>
               			</li>
               			<li>
							<a href="javascript:void(0);" onclick="return false;" _search_type="1" value="2">跨服世界BOSS打宝</a>
               			</li>
               			<li>
							<a href="javascript:void(0);" onclick="return false;" _search_type="1" value="3">国家玩法</a>
               			</li>
               			<li>
							<a href="javascript:void(0);" onclick="return false;" _search_type="1" value="4">非定向性的跨服服务器</a>
               			</li>
               			<li>
							<a href="javascript:void(0);" onclick="return false;" _search_type="1" value="5">跨服竞技场-勇气</a>
               			</li>
               			<li>
							<a href="javascript:void(0);" onclick="return false;" _search_type="1" value="6">跨服竞技场-荣誉</a>
               			</li>
               			<li>
							<a href="javascript:void(0);" onclick="return false;" _search_type="1" value="7">跨服竞技场-英雄</a>
               			</li>
               			<li>
							<a href="javascript:void(0);" onclick="return false;" _search_type="1" value="8">跨服竞技场-巅峰</a>
               			</li>
					</ul>
					</div>
     		</div>
     		<div style="float:left;margin-left:20px;margin-top:5px;">
     			<input type="radio" class="noborder" name="kuafu" value="0" />全选
     			<input type="radio" class="noborder" name="kuafu" value="1" checked="checked"/>多选
     			<input type="radio" class="noborder" name="kuafu" value="2" />单选
     			<input type="radio" class="noborder" name="kuafu" value="3" />汇总
     			<div id="serverNum" style="display:inline;margin-left:10px;"></div>
     		</div>
</div>
<div class="mainDivs" id="divs">
	<c:forEach var="server" items="${serverList}" varStatus="status">
	   <div class="selectDiv" onclick="divClick(this)">${server.worldId}</div>
	</c:forEach>
</div>
<div id="container" style="width:99%;height:500px;margin-left:20px;"></div>
<script>
var detailChart;
$(function(){
	initDatePicker({func:refreshInfo,days:0},'${serverTime}','${timezonerawoffset}',0,1);
	createDetail();
	selectItemList2(2,itemRequest);
	itemRequest(-1);
	$("input[name='kuafu']").bind("click",function(){
		selectRadio($(this).attr('value'));
	});
});

function selectRadio(value){
	switch(value){
		case '0':
			  $('#divs').show();
			  selectAllDiv();
			  removeOnlineNum('汇总');
		      break;
		case '1':
			  removeOnlineNum('汇总');
			  $('#divs').show();
			  break;
		case '2':
			  removeOnlineNum('汇总');
			  $('#divs').show();
			  break;
		case '3':
			  $('#divs').hide();
			  showDiejia();
			  break;
	}
	showCrossServerNum();
}

function selectAllDiv(){
	$("#divs > div").each(function(){
		if($(this).css('background-color') == 'rgb(238, 238, 238)'){
			getCrossOnlineNum($(this).text());
			$(this).css('background-color','#778899');
		}
	});
}

function removeOnlyOne(worldId){
	//单选
	if($("input[name='kuafu']:checked").val() == '2'){
		$("#divs > div").each(function(){
			if($(this).css('background-color') != 'rgb(238, 238, 238)'){
				var tempId = $(this).text();
				if(worldId != tempId){
					$(this).css('background-color','#eee');
					removeOnlineNum($(this).text());
				}
			}
		});
	}
}

function itemRequest(data){
	removeAllOnlineNum();
	$.ajax({url:'${ctx}/stat/online/getworldid.do',data:{type:data},success:function(data) {
		if(data && data instanceof Array){
			$('#divs').empty();  
			for(var i=0;i<data.length;i++){
				$('#divs').append('<div class="selectDiv" onclick="divClick(this)">'+data[i]+'</div>');
			}
			loadDataProcess();
			showCrossServerNum();
		}
	}});
}

function showCrossServerNum(){
	$('#serverNum').text('跨服数:'+$('.mainDivs div').length);
}

function loadDataProcess(){
	if($("input[name='kuafu']:checked").val() == '3'){
		selectRadio('3');
	}
	if($("input[name='kuafu']:checked").val() == '0'){
		selectRadio('0');
	}
}
function divClick(e){
	var worldId = $(e).text();
	if($(e).css('background-color') != 'rgb(238, 238, 238)'){
		$(e).css('background-color','#eee');
		removeOnlineNum(worldId);
	}
	else{
		getCrossOnlineNum(worldId);
		$(e).css('background-color','#778899');
	}
	removeOnlyOne(worldId);
}

function showDiejia(){
	removeAllOnlineNum();
	$.ajax({url:'${ctx}/stat/online/getstatcrossnum.do',data:{type:$('#search_type_btn2').attr('value'),"startTime":$("#startTime").val(),"endTime":$("#endTime").val()},success:function(data) {
		if(data && data instanceof Array){
			addSeriesData(data,'汇总');
		}
	}});
}

function refreshInfo(){
	
}

function getCrossOnlineNum(worldId){
	$.ajax({url:'${ctx}/stat/online/getcrossnum.do',data:{worldId:worldId,"startTime":$("#startTime").val(),"endTime":$("#endTime").val()},success:function(data) {
		if(data && data instanceof Array){
			addSeriesData(data,worldId);
		}
	}});
}

function addSeriesData(data,name){
	var tempData = [];
	for(var i=0;i<data.length;i++){
		var date = new Date(getRealServerTime(data[i].addTime,'${timezonerawoffset}'));	
		tempData.push([Date.UTC(date.getFullYear(),date.getMonth(),date.getDate(),date.getHours(),date.getMinutes(),date.getSeconds()),data[i].num]);
	}
	detailChart.addSeries({data:tempData,name:name});
}

/** 删除一个series*/
function removeOnlineNum(worldId){
	var series = detailChart.series;
	var index = -1;
	for(var i=0;i<series.length;i++){
		if(series[i].name == worldId){
			index = i;
			break;
		}
	}
	if(index > -1){
		series[i].remove();
	}
}

function removeAllOnlineNum(){
	var series = detailChart.series;
	if(series && series.length > 0){
		series[0].remove();
		removeAllOnlineNum();
	}
}

function createDetail() {
    detailChart = $('#container').highcharts({
        chart: {
        	type:'area',
            reflow: false,
            marginRight: 20,
            style: {
                position: 'absolute'
            }
        },
        credits: {
            enabled: false
        },
        title: {
            text: '跨服在线人数统计图'
        },
        tooltip:{
			formatter:function(){
							return "跨服:"+this.series.name+" "+Highcharts.dateFormat("%Y-%m-%d %H:%M:%S",this.x)+"<br/>人数:"+this.y+"人";
						}
		},
        xAxis: {
            type: 'datetime',
            labels:{
            	formatter:function(){
            		return Highcharts.dateFormat('%H',this.value);
            	}
            }
        },
        yAxis: {
            title: {
                text: null
            },
            floor:0
        },
        legend: {
           	align: 'right',
           	verticalAlign:'top',
            x:-70,
            y:20,
            width:1400
        },
        plotOptions: {
            series: {
                marker: {
                    enabled: false
                },
                states: {
                    hover: {
                        enabled: false
                    }
                }
            },
            area: {
                fillColor: {
                    linearGradient: { x1: 0, y1: 2, x2: 0, y2: 0},
                    stops: [
                        [0, Highcharts.getOptions().colors[0]],
                        [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                    ]
                },
                lineWidth: 1,
                marker: {
                    enabled: false
                },
                shadow: false,
                states: {
                    hover: {
                        lineWidth: 1
                    }
                },
                threshold: null
            }
        }

    }).highcharts(); // return chart
}
</script>