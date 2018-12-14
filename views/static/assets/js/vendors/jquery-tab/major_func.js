var Major = {

/* ****************************** 单选 ********************************* */
	// 单选输出
	Show2 : function(){
		var output='',flag,output2='';
		for (var i in m_a){
			if(i.substring(2)=='00'){
				output+='<li onclick="Major.SubLayer2(\''+i+'\')">'+m_a[i]+'</li>';
			}
		}
		$('#drag').width('670px');
		$('#MajorList').html('<ul>'+output+'</ul>');
		// 鼠标悬停变色
		$('#MajorAlpha li').hover(function(){$(this).addClass('over')},function(){$(this).removeClass('over')});
		// 点击弹出子菜单
		$('#MajorList li').click(function(e){$("#sublist").css({top:e.pageY-4,left:e.pageX-4}).hover(function(){$(this).show()},function(){$(this).hide()})})
	},
	// 子职位 悬浮菜单
	SubLayer2 : function(id){
		var output='',width;
		var myid=id.substr(0,2);
		var len=0;
		for (var i in m_a){
			if(i.substr(0,2)==myid){
				if(i.substr(2)=='00'){
					output+='<h4 onclick="Major.Chk2(\''+id+'\')"><a href="javascript:">'+m_a[id]+'</a></h4>';
				}else{
					output+='<li><a href="javascript:" onclick="Major.Chk2(\''+i+'\')">'+m_a[i]+'</a></li>';
					len++;
				}
			}
		}
		width=len>10?440:220;
		output='<div id="sub_funtype" class="radio"><ul style="width:'+width+'px">'+output+'</ul></div>';
		$("#sublist").html(output).show();
	},
	Chk2 : function(id){
		$('#btn_MajorID_2').val(m_a[id]);
		$('#MajorID_2').val(id);
		$("#sublist").empty().hide();
		boxAlpha();
	}
}
// 单选
function majorSelect_2(){
	var dragHtml ='<div id="MajorAlpha">';			//职能类别
		dragHtml+='		<div id="MajorList"></div>';	//职能类别列表
		dragHtml+='</div>';
	$('#drag_h').html('<b>请选择专业</b><span onclick="boxAlpha()">关闭</span>');
	$('#drag_con').html(dragHtml);
	Major.Show2();
	boxAlpha();
	draglayer();
}