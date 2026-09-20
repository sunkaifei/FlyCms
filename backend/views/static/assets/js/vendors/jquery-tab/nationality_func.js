var nation = {
	// 国籍输出
	Show : function(){
		var output='',flag,output2='';
		for (var i in na){
			output+='<li onclick="nation.Chk(\''+i+'\')">'+na[i]+'</li>';
		}
		$('#drag').width('600px');
		$('#nationList').html('<ul>'+output+'</ul>');
		// 鼠标悬停变色
		$('#nationAlpha li').hover(function(){$(this).addClass('over')},function(){$(this).removeClass('over')});
	},
	Chk : function(id){
		$('#btn_nation').val(na[id]);
		$('#nation').val(id);
		boxAlpha();
	}
}

function nationSelect(){
	var dragHtml ='<div id="nationAlpha">';		//国籍
		dragHtml+='		<div id="nationList"></div>';//国籍列表
		dragHtml+='</div>';
	$('#drag_h').html('<h4 class="modal-title"  style="float: left;">选择计量单位</h4><button type="button" class="close"  style="float: right;"  onclick="boxAlpha()"><span aria-hidden="true">×</span></button>');
	$('#drag_con').html(dragHtml);

	nation.Show();
	boxAlpha();
	draglayer();
}