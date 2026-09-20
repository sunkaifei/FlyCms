define(function(require, exports, module) {
	J = jQuery;
    require('autovalidate');
    require('jqueryform');
	require('layer');
	layer.config({
	    path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
	});

	//===========================================登录开始=================================================
    /*验证码重新加载*/
    $("#reloadCaptcha").click(function(){
        new_src = '/captcha/default?'+Math.random();
        $(this).find("img").attr("src",new_src);
    });

    $("#myloginForm").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                location.href = ret.url;
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

	$("#myloginForm").keydown(function(e){
		 var e = e || event,
		 keycode = e.which || e.keyCode;
		 if (keycode==13) {
		  $(".log_btn").trigger("click");
		 }
	});
	//===========================================登陆结束=================================================

});