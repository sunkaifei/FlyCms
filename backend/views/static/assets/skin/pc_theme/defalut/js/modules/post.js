define(function(require, exports, module) {
    var plugins = require('plugins');
    J = jQuery;
	require('layer');
	require('jqueryform');
	
	layer.config({
	    path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
	});
	
	$('#tags').tagit({
        singleField: true,
        singleFieldNode: $('#fieldTags'),
        tagLimit: 5
    });
	
	$('#add_article_form').ajaxForm({
		dataType : 'json',
		success : function(data) {
			if (data.code==0) {
				$(this).attr("disabled", "disabled");
				layer.msg(data.message, {icon: 1});
				window.location.reload();
			}else{
				var message="";
				if(data.message==null){
					message="未提交成功！";
				}else{
					message=data.message;
				}
				layer.msg(message, {icon: 2});
				return false;
			}
		}
	});
	
	$('#update_article_form').ajaxForm({
		dataType : 'json',
		success : function(data) {
			if (data.code==0) {
				$(this).attr("disabled", "disabled");
				var datas = eval(data.data);
				if(datas.article_id > 0){
                    layer.msg(data.message, {icon: 1});
                    window.location.href = "/member/show/"+datas.article_id+"?uid="+datas.user_id;
    				return false;
                }
			}else{
				layer.msg(data.message, {icon: 2});
				return false;
			}
		}
	});
	
	$("#update_topic_form").on("click", function(){
		myeditor.updateElement();
	});
	
	$('#update_topic_form').ajaxForm({
		dataType : 'json',
		success : function(data) {
			if (data.code==0) {
				$(this).attr("disabled", "disabled");
				var datas = eval(data.data);
				if(datas.article_id > 0){
                    layer.msg(data.message, {icon: 1});
                    window.location.href = "/p/"+datas.article_id;
    				return false;
                }
			}else{
				layer.msg(data.message, {icon: 2});
				return false;
			}
		}
	});
});