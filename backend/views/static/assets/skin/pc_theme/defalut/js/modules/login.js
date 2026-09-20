define(function(require, exports, module) {
	J = jQuery;
    require('autovalidate');
    require('jqueryform');
    require('bootstrap');
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

    //===========================================注册开始=================================================

    //========================手机注册=================================
/*
    $('#addMobileCode').on("click",function(event){
        var time=$("#verifytime").val();
        var username=$("#phoneNumber").val();
        if(!/^[1][3,4,5,6,7,8][0-9]{9}$/.test(username)){
            return false;
        }
        if(time=="" || time==0){
            var count = 60;
            var countdown = setInterval(CountDown, 1000);
            function CountDown() {
                $("#verifytime").val(count);
                $("#addMobileCode").html(count + " 秒后");
                if (count == 0) {
                    $("#addMobileCode").html("获取验证码");
                    clearInterval(countdown);
                }
                count--;
            }
        }
    });
*/

    //注册的登录名
    $("#phoneNumber").on("blur", function(){
        var username=$("#phoneNumber").val();
        if(username.length>0){
            if(/^[1][3,4,5,6,7,8][0-9]{9}$/.test(username)){
                $(this).closest('div').removeClass('has-error').addClass('has-success');
            }else{
                $(this).closest('div').removeClass('has-success').addClass('has-error');
            }
        }else{
            $(this).closest('div').removeClass('has-success').addClass('has-error');
        }
    });

    $("#addMobileCode").on("click", function(){
        var username=$("#phoneNumber").val();
        if(username.length>0){
            if(!/^[1][3,4,5,6,7,8][0-9]{9}$/.test(username)){
                $('#myMobileModal').modal('hide');
                $("#phoneNumber").closest('div').removeClass('has-success').addClass('has-error');
                $("#phoneNumber").focus();
                return false;
            }else{
                $("#phoneNumber").closest('div').removeClass('has-error').addClass('has-success');
            }
        }else{
            $('#myMobileModal').modal('hide');
            $("#phoneNumber").closest('div').removeClass('has-success').addClass('has-error');
            $("#phoneNumber").focus();
            return false;
        }
        new_src = '/captcha/default?'+Math.random();
        $("#reloadCaptcha").find("img").attr("src",new_src);
        $(".captcha").val("");
    });

    $(".mobilecod-btn").on("click", function(){
        var username=$("#phoneNumber").val();
        var captcha=$(".captcha").val();

        $.ajax({
            url: '/ucenter/mobilecode',
            data: {'username': username,'captcha': captcha},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(data){
                if(data.code==0){
                    $('#myMobileModal').modal('hide');
                    layer.msg('验证码已发送您的手机上，请查收！', {icon: 1});
                    return false;
                }else{
                    layer.msg(data.message, {icon: 2});
                    new_src = '/captcha/default?'+Math.random();
                    $("#reloadCaptcha").find("img").attr("src",new_src);
                    $(".captcha").val("");
                }
            }
        });
    });

    //手机用户注册提交
    $("#register-btn").on("click", function(){
        var phoneNumber=$("#phoneNumber").val();
        var mobilecode=$(".mobilecode").val();
        var password=$("#password").val();
        var password2=$("#password2").val();
        var invite=$("#invite").val();
        if(phoneNumber.length>0){
            if(!/^[1][3,4,5,6,7,8][0-9]{9}$/.test(phoneNumber)){
                $("#phoneNumber").closest('div').removeClass('has-success').addClass('has-error');
                $("#phoneNumber").focus();
                return false;
            }else{
                $("#phoneNumber").closest('div').removeClass('has-error').addClass('has-success');
            }
        }else{
            $("#phoneNumber").closest('div').removeClass('has-success').addClass('has-error');
            $("#phoneNumber").focus();
            return false;
        }

        if(mobilecode.length>0){
            if(!/^[0-9]{6}$/.test(mobilecode)){
                $(".mobilecode").closest('div').removeClass('has-success').addClass('has-error');
                $(".mobilecode").focus();
                return false;
            }else{
                $(".mobilecode").closest('div').removeClass('has-error').addClass('has-success');
            }
        }else{
            $(".mobilecode").closest('div').removeClass('has-success').addClass('has-error');
            $(".mobilecode").focus();
            return false;
        }

        if(password.length>=6){
            if(!/^[0-9a-zA-Z_~!@#$%^&*()_+]{6,20}$/.test(password)){
                $("#password").closest('div').removeClass('has-success').addClass('has-error');
                $("#password").focus();
                return false;
            }else{
                $("#password").closest('div').removeClass('has-error').addClass('has-success');
            }
        }else{
            $("#password").closest('div').removeClass('has-success').addClass('has-error');
            $("#password").focus();
            return false;
        }

        if(password!=password2){
            $("#password").closest('div').removeClass('has-success').addClass('has-error');
            $("#password").focus();
            return false;
        }

        jQuery.ajax({
            type:"POST",
            url: "/ucenter/addMobileUser",
            data: {"phoneNumber":phoneNumber,"mobilecode":mobilecode,"password":password,"password2":password2,"invite":invite},
            dataType:"json",
            cache : false,
            async: false, //ajax方法外可以调用变量
            success : function(data) {
                if (data.code==0) {
                    window.location.href = "/ucenter/account";
                    return false;
                }else{
                    layer.msg(data.message, {icon: 2});
                    return false;
                }
            }
        });
        return false;
    });

    $("#register-btn").keydown(function(e){
        var e = e || event,
            keycode = e.which || e.keyCode;
        if (keycode==13) {
            $("#mobileregister-btn").trigger("click");
        }
    });
    //===========================================注册结束=================================================

    $(".js-trun").click(function() {
        var $loginbox = $(this).closest(".login-box");
        if($loginbox.hasClass("register")){
            $loginbox.siblings().addClass("login-delay");
            $(".login-box").removeClass("active");
            return;
        }
        $(".login-box").addClass('active').removeClass("login-delay");
    });

    $("#password-reset-form").ajaxForm({
        dataType : 'json',
        success : function(data) {
            if(data.code==0){
                $('#password-reset input[type=submit]', this).attr('disabled', 'disabled');
                $("#reset_password").css("display","none");
                $("#reset_result").css("display","inline");
                //window.location.href = "/member/login";
                setInterval(window.location.href = "/login",30000);
                return false;
            }else{
                layer.msg(data.message, {icon: 2});
                return false;
            }
        }
    });

    $("#reset-btn").on("click", function(){
        var uid=$("#uid").val();
        var password=$("#password").val();
        var password1=$("#password1").val();
        var rememberMe=$("#rememberMe").val();
        var code=$("#code").val();

        if(password.length>=6){
            if(!/^[0-9a-zA-Z_~!@#$%^&*()_+]{6,20}$/.test(password)){
                $("#password").closest('div').removeClass('has-success').addClass('has-error');
                $("#password").focus();
                return false;
            }else{
                $("#password").closest('div').removeClass('has-error').addClass('has-success');
            }
        }else{
            $("#password").closest('div').removeClass('has-success').addClass('has-error');
            $("#password").focus();
            return false;
        }

        if(password!=password1){
            $("#password1").closest('div').removeClass('has-success').addClass('has-error');
            $("#password1").focus();
            return false;
        }else{
            $("#password1").closest('div').removeClass('has-error').addClass('has-success');
        }


        $.ajax({
            url: '/ucenter/reset.json',
            data: {'uid': uid,'password': password,'password1': password1,'code': code,'rememberMe':rememberMe},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(data){
                if(data.code==0){
                    //弹出提示5秒后刷新页面
                    layer.msg(data.message,{icon:1,time:5000},function(){
                        window.location.href = "/login";
                    });
                    return false;
                }else{
                    layer.msg(data.message, {icon: 2});
                }
            }
        });
    });

});