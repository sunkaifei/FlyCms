define(function(require, exports, module) {
    J = jQuery;
    require('jqueryform');
    require('bootstrapSwitch');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    $("#updateUserconfig").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg("更新成功", { shift: -1 }, function () {});
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    $(".update-sort").blur(function(){
        var id = $(this).attr("data-group-id");
        var sort = $(this).val();
        $.ajax({
            url: '/system/user/groupsort_update',
            data: {"id":id,"sort":sort},
            dataType: "json",
            type :  "post",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(data){
                if(data.code==0){
                    //layer.msg(data.message, {icon: 1});
                    return false;
                }else{
                    //layer.msg(data.message, {icon: 2});
                }
            }
        });
    });

    $("#testmail").on("click", function(){
        var testaddress=$("#test_address").val();
        $.ajax({
            url: '/system/site/testemail',
            data: {'test_address': testaddress},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(data){
                if(data.code==0){
                    layer.msg('已发送，请查收！', {icon: 1});
                    return false;
                }else{
                    layer.msg(data.message, {icon: 2});
                }
            }
        });
    });


    //规则开启关闭设置
    $('.rule-switch').bootstrapSwitch({
        size: "mini",
        onSwitchChange:function(){
            var id = $(this).attr("data-rule-id");
            $.ajax({
                url: "/system/score/rule_status?"+Math.random(),
                data: {"id":id},
                dataType: "json",
                type :  "POST",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(ret){
                    if (ret.message == "关闭") {
                        $('.rule-switch').bootstrapSwitch('state',false);
                        return false;
                    }
                    if (ret.message == "开启") {
                        $('.rule-switch').bootstrapSwitch('state',true);
                        return false;
                    }
                }
            });
        }
    });

    $("#score_form").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg("操作成功", { shift: -1 }, function () {
                    location.href = "/system/score/list_scorerule";
                });
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    $(document).on('click', '.score-delete', function (){
        var id = $(this).attr("data-id");
        var title = $(this).attr("data-title");
        layer.confirm('您是确定删除《'+title+'》？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/score/del?"+Math.random(),
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

    $(document).on('click', '.group-delete', function (){
        var id = $(this).attr("data-id");
        var title = $(this).attr("data-title");
        layer.confirm('您是确定删除《'+title+'》？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/user/group_del?"+Math.random(),
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
});