define(function(require, exports, module) {
    var plugins = require('plugins');
    J = jQuery;
	require('layer');
	require('jqueryform');
    require('prettify');
	layer.config({
	    path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
	});

    $(".userFollow-button").on("click", function(){
        if(userid>0) {
            var id = $(this).parent().attr("data-user-id");
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

    $(".load-content").on("click", function(){
        var id = $(this).parent().parent().parent().attr("data-info-id");
        var type = $(this).parent().parent().parent().attr("data-info-type");
        var _this=this;
        if(type==0){
            $.get("/findQuestionById/"+id,
                function(data){
                    if(data.code >= 0){
                        $(_this).parent().parent().prepend("<div class='all-content'>"+data.data.content+"</div>");
                        $("pre").addClass("prettyprint linenums");
                        prettyPrint();
                    }else{
                        layer.msg('该信息已删除或参数错误', {icon: 2});
                        return false;
                    }
                });
        }else if(type==1){
            $.get("/findArticleById/"+id,
                function(data){
                    if(data.code >= 0){
                        $(_this).parent().parent().prepend("<div class='all-content'>"+data.data.content+"</div>");
                        $("pre").addClass("prettyprint linenums");
                        prettyPrint();
                    }else{
                        layer.msg('该信息已删除或参数错误', {icon: 2});
                        return false;
                    }
                });
        }else if(type==2){
            $.get("/findShareById/"+id,
                function(data){
                    if(data.code >= 0){
                        $(_this).parent().parent().prepend("<div class='all-content'>"+data.data.content+"</div>");
                        $("pre").addClass("prettyprint linenums");
                        prettyPrint();
                    }else{
                        layer.msg('该信息已删除或参数错误', {icon: 2});
                        return false;
                    }
                });
        }
        var display =$(this).parent().css('display');
        if(display != 'none'){
            $(this).parent().parent().next().children(".shrink-content").show();
            $(this).parent().hide();
        }
    });

    $(".shrink-content").on("click", function(){
        var display =$(this).css('display');
        if(display != 'none'){
            $(this).hide();
            $(this).parent().prev().children(".all-content").remove();
            $(this).parent().prev().children(".excerpt").show();
        }
    });
});