define(function(require, exports, module) {
    J = jQuery;
    require('bootstrap');
    require('jqueryform');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    $("#selectAll").click(function(){
        if($("input[type='checkbox']:not(:checked)").length>0) {
            $("input[type='checkbox']").prop('checked',true);
        }else {
            $("input[type='checkbox']").prop('checked',false);
        }
    });

    /*加载答案，查看答案内容*/
    $(document).on('click', '.answer-show', function (){
        var id = $(this).attr("data-id");
        var _this=this;
        $.get("/system/answer/findId", { id: id },
            function(data){
                if(data.code >= 0){
                    $(_this).parent().prev().children().children(".direct-chat-text").html(data.data.content);
                }else{
                    layer.msg('该信息已删除或参数错误', {icon: 2});
                    return false;
                }
            });
    });

    $(document).on('click', '.answer-delete', function (){
        var id = $(this).attr("data-id");
        layer.confirm('您是确定删除本条答案？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/answer/del?"+Math.random(),
                data: {"id":id},
                dataType: "json",
                type :  "POST",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(ret){
                    if (ret.code >= 0) {
                        layer.msg("删除成功！", {icon: 1});
                        window.location.reload();
                        return false;
                    } else {
                        layer.msg(ret.message, {icon: 5});
                        return false;
                    }
                }
            });
        }, function(){
        });
    });

    /*答案审核*/
    $(document).on('click', '.answer-status', function (){
        var id = $(this).attr("data-id");
        $("#id").val("");
        $("#status0").prop( "checked", true );
        $.get("/system/answer/findId", { id: id },
            function(data){
                if(data.code >= 0){
                    $("#id").val(id);
                    $("#status"+data.data.status).prop( "checked", true );
                    return false;
                }else{
                    layer.msg('该信息已删除或参数错误', {icon: 2});
                    return false;
                }
            });
    });

    $(document).on('click', '.answer-status-btn', function (){
        var id=$("#id").val();
        var status=$('input:radio[name="status"]:checked').val();
        $.ajax({
            url: "/system/answer/answer-status?"+Math.random(),
            data: {"id":id,"status":status},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(ret){
                if (ret.code >= 0) {
                    //弹出提示2秒后刷新页面
                    layer.msg(ret.message,{icon: 1, time: 2000},function(){
                        window.location.reload();
                    });
                } else {
                    layer.msg(ret.message, {icon: 5});
                    return false;
                }
            }
        });
        return false;
    });

    $(document).on('click', '.question-delete', function (){
        var id = $(this).attr("data-id");
        var title = $(this).attr("data-title");
        layer.confirm('您是确定删除《'+title+'》？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/question/del?"+Math.random(),
                data: {"id":id},
                dataType: "json",
                type :  "POST",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(ret){
                    if (ret.code >= 0) {
                        layer.msg("删除成功！", {icon: 1});
                        window.location.reload();
                        return false;
                    } else {
                        layer.msg(ret.message, {icon: 5});
                        return false;
                    }
                }
            });
        }, function(){

        });
    });

    $(document).on('click', '.article-delete', function (){
        var id = $(this).attr("data-id");
        var title = $(this).attr("data-title");
        layer.confirm('您是确定删除《'+title+'》？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/article/del?"+Math.random(),
                data: {"id":id},
                dataType: "json",
                type :  "POST",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(ret){
                    if (ret.code >= 0) {
                        layer.msg("删除成功！", {icon: 1});
                        window.location.reload();
                        return false;
                    } else {
                        layer.msg(ret.message, {icon: 5});
                        return false;
                    }
                }
            });
        }, function(){
        });
    });

    $(document).on('click', '#indexAllshare', function (){
        $.ajax({
            url: "/system/share/index_all_share?"+Math.random(),
            data: {},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(ret){
                if (ret.code >= 0) {
                    layer.msg("已全部索引！", {icon: 1});
                    window.location.reload();
                    return false;
                } else {
                    layer.msg(ret.message, {icon: 5});
                    return false;
                }
            }
        });
    });

    $(document).on('click', '#indexAllarticle', function (){
        $.ajax({
            url: "/system/article/index_all_article?"+Math.random(),
            data: {},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(ret){
                if (ret.code >= 0) {
                    layer.msg("已全部索引！", {icon: 1});
                    window.location.reload();
                    return false;
                } else {
                    layer.msg(ret.message, {icon: 5});
                    return false;
                }
            }
        });
    });

    $(document).on('click', '#indexAllquestion', function (){
        $.ajax({
            url: "/system/question/index_all_question?"+Math.random(),
            data: {},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(ret){
                if (ret.code >= 0) {
                    layer.msg("已全部索引！", {icon: 1});
                    window.location.reload();
                    return false;
                } else {
                    layer.msg(ret.message, {icon: 5});
                    return false;
                }
            }
        });
    });

    /*问题审核*/
    $(document).on('click', '.info-status', function (){
        var id = $(this).attr("data-id");
        $("#id").val("");
        $("#status0").prop( "checked", true );
        $("#recommend").val(0);
        $.get("/system/question/findId", { id: id },
            function(data){
                if(data.code >= 0){
                    $("#id").val(id);
                    $("#status"+data.data.status).prop( "checked", true );
                    $("#recommend").val(data.data.recommend);
                }else{
                    layer.msg('该信息已删除或参数错误', {icon: 2});
                }
            });
    });

    $(document).on('click', '.status-btn', function (){
        var id=$("#id").val();
        var status=$('input:radio[name="status"]:checked').val();
        var recommend=$("#recommend option:selected").val();
        $.ajax({
            url: "/system/question/question-status?"+Math.random(),
            data: {"id":id,"status":status,"recommend":recommend},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(ret){
                if (ret.code >= 0) {
                    //弹出提示2秒后刷新页面
                    layer.msg(ret.message,{icon: 1, time: 2000},function(){
                        window.location.reload();
                    });
                } else {
                    layer.msg(ret.message, {icon: 5});
                    return false;
                }
            }
        });
        return false;
    });

    /* 文章审核 */
    $(document).on('click', '.article-status', function (){
        var id = $(this).attr("data-id");
        $("#id").val("");
        $("#status0").prop( "checked", true );
        $("#recommend").val(0);
        $.get("/system/article/findId", { id: id },
            function(data){
                if(data.code >= 0){
                    $("#id").val(id);
                    $("#status"+data.data.status).prop( "checked", true );
                    $("#recommend").val(data.data.recommend);
                }else{
                    layer.msg('该信息已删除或参数错误', {icon: 2});
                }
            });
    });

    $(document).on('click', '.article-status-btn', function (){
        var id=$("#id").val();
        var status=$('input:radio[name="status"]:checked').val();
        var recommend=$("#recommend option:selected").val();
        $.ajax({
            url: "/system/article/article-status?"+Math.random(),
            data: {"id":id,"status":status,"recommend":recommend},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(ret){
                if (ret.code >= 0) {
                    //弹出提示2秒后刷新页面
                    layer.msg(ret.message,{icon: 1, time: 2000},function(){
                        window.location.reload();
                    });
                } else {
                    layer.msg(ret.message, {icon: 5});
                    return false;
                }
            }
        });
        return false;
    });

    $(document).on('click', '.share-delete', function (){
        var id = $(this).attr("data-id");
        layer.confirm('您是确定删除本条答案？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/share/del?"+Math.random(),
                data: {"id":id},
                dataType: "json",
                type :  "POST",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(ret){
                    if (ret.code >= 0) {
                        layer.msg("删除成功！", {icon: 1});
                        window.location.reload();
                        return false;
                    } else {
                        layer.msg(ret.message, {icon: 5});
                        return false;
                    }
                }
            });
        }, function(){
        });
    });

    /* 分享审核 */
    $(document).on('click', '.share-status', function (){
        var id = $(this).attr("data-id");
        $("#id").val("");
        $("#status0").prop( "checked", true );
        $("#recommend").val(0);
        $.get("/system/share/findId", { id: id },
            function(data){
                if(data.code >= 0){
                    $("#id").val(id);
                    $("#status"+data.data.status).prop( "checked", true );
                    $("#recommend").val(data.data.recommend);
                }else{
                    layer.msg('该信息已删除或参数错误', {icon: 2});
                }
            });
    });

    $(document).on('click', '.share-status-btn', function (){
        var id=$("#id").val();
        var status=$('input:radio[name="status"]:checked').val();
        var recommend=$("#recommend option:selected").val();
        $.ajax({
            url: "/system/share/share-status?"+Math.random(),
            data: {"id":id,"status":status,"recommend":recommend},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(ret){
                if (ret.code >= 0) {
                    //弹出提示2秒后刷新页面
                    layer.msg(ret.message,{icon: 1, time: 2000},function(){
                        window.location.reload();
                    });
                } else {
                    layer.msg(ret.message, {icon: 5});
                    return false;
                }
            }
        });
        return false;
    });

    $(document).on('click', '.clean-index', function (){
        var display =$(this).parent().next().css('display');
        if(display != 'none'){
            $(this).parent().next().find(".overlay").show();
        }
        var _this=this;
        $.ajax({
            url: "/system/indexes/delete_all?"+Math.random(),
            data: {},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(ret){
                if (ret.code >= 0) {
                    //弹出提示2秒后刷新页面
                    layer.msg(ret.message,{icon: 1, time: 2000},function(){
                        $(_this).parent().next().find(".overlay").hide();
                    });
                } else {
                    layer.msg(ret.message, {icon: 5});
                    return false;
                }
            }
        });
        return false;
    });
});