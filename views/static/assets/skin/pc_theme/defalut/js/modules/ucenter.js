define(function(require, exports, module) {
    J = jQuery;
    require('jqueryform');
    require('bootstrap');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    $("#addMobileCode").on("click", function(){
        var username=$("#mobile").val();
        if(username.length>0){
            if(!/^[1][3,4,5,7,8][0-9]{9}$/.test(username)){
                $('#myMobileModal').modal('hide');
                $("#mobile").closest('div').removeClass('has-success').addClass('has-error');
                $("#mobile").focus();
                return false;
            }else{
                $("#mobile").closest('div').removeClass('has-error').addClass('has-success');
            }
        }else{
            $('#myMobileModal').modal('hide');
            $("#mobile").closest('div').removeClass('has-success').addClass('has-error');
            $("#mobile").focus();
            return false;
        }
        new_src = '/captcha/default?'+Math.random();
        $("#reloadCaptcha").find("img").attr("src",new_src);
        $(".captcha").val("");
    });

    $(".safecode-btn").on("click", function(){
        var mobile=$("#mobile").val();
        var captcha=$(".captcha").val();

        $.ajax({
            url: '/ucenter/safemobilecode',
            data: {'mobile': mobile,'captcha': captcha},
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

    $('#verify_code').on("click",function(event){
        var time=$("#verifytime").val();
        if($("#userEmail").val()==""){
            $("#userEmail").closest('div').removeClass('has-success').addClass('has-error');
            return false;
        }
        if(time=="" || time==0){
            var count = 60;
            var countdown = setInterval(CountDown, 1000);
            function CountDown() {
                $("#verifytime").val(count);
                $("#verify_code").html(count + " 秒后");
                if (count == 0) {
                    $("#verify_code").html("发送验证码");
                    clearInterval(countdown);
                }
                count--;
            }
        }
    });
    $("#verify_code").on("click",function(event){
        var time=$("#verifytime").val();
        var userEmail = $("#userEmail").val();
        if(time>0){
            layer.msg(time+'后再试', {icon: 2});
            return false;
        }
        if(userEmail.length>0){
            if(/^([a-zA-Z0-9]+[_|\_|\.]?)*[a-zA-Z0-9]+@([a-zA-Z0-9]+[_|\_|\.]?)*[a-zA-Z0-9]+\.[a-zA-Z]{2,3}$/.test(userEmail)){
                $("#userEmail").closest('div').removeClass('has-error').addClass('has-success');
            }else{
                $("#userEmail").closest('div').removeClass('has-success').addClass('has-error');
            }
        }else{
            $("#userEmail").closest('div').removeClass('has-success').addClass('has-error');
        }
        $.ajax({
            url: "/ucenter/safe_email_code?"+Math.random(),
            data: {"userEmail":userEmail},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(ret){
                if (ret.code >= 0) {
                    return false;
                } else {
                    layer.msg(ret.message, {icon: 5});
                    return false;
                }
            }
        });
        return false;
    });

});