define(function(require, exports, module) {
    var plugins = require('plugins');
    J = jQuery;
	require('layer');
	require('jqueryform');
    require('prettify');
	layer.config({
	    path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
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