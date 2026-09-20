define(function(require, exports, module) {
    J = jQuery;
    require('jqueryform');
    require('bootstrap');
    require('bootstraponhover');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    $('.dropdown').bootstrapDropdownOnHover();

    /*验证码重新加载*/
    $("#reloadCaptcha").click(function(){
        new_src = '/captcha/default?'+Math.random();
        $(this).find("img").attr("src",new_src);
    });

    $(".userFollow-button").hover(function() {
        var follow=$(this).text();
        if(follow=="已关注"){
            $(this).html("取消关注");
        }
    }, function() {
        var follow=$(this).text();
        if(follow=="取消关注"){
            $(this).html("已关注");
        }
    });

    //关注用户
    $(".userFollow-button").on("click", function(){
        if(userid>0) {
            var id = $(this).attr("data-user-id");
            $.ajax({
                url: '/ucenter/user/follow',
                data: {"id":id},
                dataType: "json",
                type :  "post",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(data){
                    if(data.code==0){
                        //弹出提示2秒后刷新页面
                        layer.msg(data.message,{icon: 1, time: 2000},function(){
                            window.location.reload();
                        });
                        return false;
                    }else if(data.code==2) {
                        layer.msg(data.message,{icon: 5, time: 2000},function(){
                            window.location.reload();
                        });
                    }else{
                        layer.msg(data.message, {icon: 2});
                    }
                }
            });
        }else{
            $("#loginModal").modal();
        }
    });


    $(".article-digg").on("click", function(){
        if(userid>0) {
            var id = $(".article-content").attr("data-info-id");
            $.ajax({
                url: '/article/digg',
                data: {"id":id},
                dataType: "json",
                type :  "post",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(data){
                    if(data.code==0){
                        //弹出提示2秒后刷新页面
                        layer.msg(data.message,{icon: 1, time: 2000},function(){
                            window.location.reload();
                        });
                        return false;
                    }else if(data.code==2) {
                        layer.msg(data.message,{icon: 5, time: 2000},function(){
                            window.location.reload();
                        });
                    }else{
                        layer.msg(data.message, {icon: 2});
                    }
                }
            });
        }else{
            $("#loginModal").modal();
        }
    });

    $(".follow-button").on("click", function(){
        if(userid>0) {
            var questionId = $(".question-id").attr("data-question-id");
            $.ajax({
                url: '/question/follow',
                data: {"questionId":questionId},
                dataType: "json",
                type :  "post",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(data){
                    if(data.code==0){
                        //弹出提示2秒后刷新页面
                        layer.msg(data.message,{icon: 1, time: 2000},function(){
                            window.location.reload();
                        });
                        return false;
                    }else if(data.code==2) {
                        layer.msg(data.message,{icon: 5, time: 2000},function(){
                            window.location.reload();
                        });
                    }else{
                        layer.msg(data.message, {icon: 2});
                    }
                }
            });
        }else{
            $("#loginModal").modal();
        }
    });

    $(".ask-button").on("click", function(){
        window.location.href = "/question/add";
    });


    $('#update_form').ajaxForm({
        dataType : 'json',
        success : function(data) {
            if (data.code==0) {
                layer.msg(data.message, { shift: -1 }, function () {
                    location.href = data.url;
                });
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

    $(".follow-topic").on("click", function(){
        if(userid>0) {
            var id = $(".topic-card").attr("data-topic-id");
            $.ajax({
                url: '/topics/follow',
                data: {"id":id},
                dataType: "json",
                type :  "post",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(data){
                    if(data.code==0){
                        layer.msg(data.message,{icon: 1, time: 2000},function(){
                            window.location.reload();
                        });
                        return false;
                    }else if(data.code==2) {
                        layer.msg(data.message,{icon: 5, time: 2000},function(){
                            window.location.reload();
                        });
                    }else{
                        layer.msg(data.message, {icon: 2});
                    }
                }
            });
        }else{
            $("#loginModal").modal();
        }
    });

    $(".buy-share").on("click", function(){
        if(userid>0) {
            var id = $(this).attr("data-share-id");
            $.ajax({
                url: '/share/buy',
                data: {"id":id},
                dataType: "json",
                type :  "post",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(data){
                    if(data.code==0){
                        layer.msg(data.message, {icon: 1});
                        return false;
                    }else if(data.code==2) {
                        layer.msg(data.message, {icon: 5});
                    }else{
                        layer.msg(data.message, {icon: 2});
                    }
                }
            });
        }else{
            $("#loginModal").modal();
        }
    });

    $(".add-favorite").on("click", function(){
        if(userid>0) {
            var id = $(this).attr("data-info-id");
            var type = $(this).attr("data-type-id");
            $.ajax({
                url: '/ucenter/favorite/add',
                data: {"id":id,"type":type},
                dataType: "json",
                type :  "post",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(data){
                    if(data.code==0){
                        layer.msg(data.message, {icon: 1});
                        return false;
                    }else if(data.code==2) {
                        layer.msg(data.message, {icon: 5});
                    }else{
                        layer.msg(data.message, {icon: 2});
                    }
                }
            });
        }else{
            $("#loginModal").modal();
        }
    });

    $(".btn-login").on("click", function(){
        var username=$("#username").val();
        var password=$("#password").val();
        var code=$("#code").val();
        if(username.length>0){
            if(!/^([a-zA-Z0-9]+[_|\_|\.]?)*[a-zA-Z0-9]+@([a-zA-Z0-9]+[_|\_|\.]?)*[a-zA-Z0-9]+\.[a-zA-Z]{2,3}$/.test(username) && !/^[1][3,4,5,7,8][0-9]{9}$/.test(username)){
                $("#username").closest('div').removeClass('has-success').addClass('has-error');
                $("#username").focus();
                return false;
            }
        }else{
            $(this).removeClass('has-success').addClass('has-error');
            $("#username").focus();
            return false;
        }

        if(password.length>=6){
            if(!/^[0-9a-zA-Z_~!@#$%^&*()_+]{6,20}$/.test(password)){
                $("#password").closest('div').removeClass('has-success').addClass('has-error');
                $("#password").focus();
                return false;
            }
        }else{
            $("#password").closest('div').removeClass('has-success').addClass('has-error');
            $("#password").focus();
            return false;
        }
        if(code.length>0){
            if(!/^[a-zA-Z0-9]{4}$/.test(code)){
                $("#code").closest('div').removeClass('has-success').addClass('has-error');
                $("#code").focus();
                return false;
            }else{
                $("#code").closest('div').removeClass('has-error').addClass('has-success');
            }
        }else{
            $("#code").closest('div').removeClass('has-success').addClass('has-error');
            $("#code").focus();
            return false;
        }
        $.ajax({
            url: '/ucenter/ajaxlogin',
            data: {'username': username,'password': password,'code': code},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(data){
                if(data.code==0){
                    window.location.reload();
                    return false;
                }else{
                    layer.msg(data.message, {icon: 2});
                    return false;
                }
            }
        });
        return false;
    });

    $("#addMobileCode").on("click", function(){
        var username=$("#phoneNumber").val();
        if(username.length>0){
            if(!/^[1][3,4,5,7,8][0-9]{9}$/.test(username)){
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
        $("#regCaptcha").find("img").attr("src",new_src);
        $(".regCaptcha").val("");
    });

    $(".mobilecod-btn").on("click", function(){
        var username=$("#username").val();
        var captcha=$(".regCaptcha").val();

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
                    $("#regCaptcha").find("img").attr("src",new_src);
                    $(".regCaptcha").val("");
                }
            }
        });
    });

    //手机用户注册提交
    $(".btn-register").on("click", function(){
        var phoneNumber=$("#phoneNumber").val();
        var mobilecode=$(".mobilecode").val();
        var password=$("#regPassword").val();
        var password2=$("#regPassword2").val();
        var invite=$("#invite").val();
        if(phoneNumber.length>0){
            if(!/^[1][3,4,5,7,8][0-9]{9}$/.test(phoneNumber)){
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
                $("#regPassword").closest('div').removeClass('has-success').addClass('has-error');
                $("#regPassword").focus();
                return false;
            }else{
                $("#regPassword").closest('div').removeClass('has-error').addClass('has-success');
            }
        }else{
            $("#regPassword").closest('div').removeClass('has-success').addClass('has-error');
            $("#regPassword").focus();
            return false;
        }

        if(password!=password2){
            $("#regPassword").closest('div').removeClass('has-success').addClass('has-error');
            $("#regPassword").focus();
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

    if($(".question-id").length > 0) {
        $.get("/question/viewcount", { id: $(".question-id").attr("data-question-id")} );
    }


    if($(".article-content").length > 0) {
        $.get("/article/viewcount", { id: $(".article-content").attr("data-info-id")} );
    }

    if($(".share-content").length > 0) {
        $.get("/share/viewcount", { id: $(".share-content").attr("data-info-id")} );
    }
});