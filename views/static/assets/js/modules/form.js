define(function(require, exports, module) {
    J = jQuery;
    require('jqueryform');
    require('bootstrapSwitch');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    $(".updateForm").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg("更新成功", { shift: -1 }, function () {
                    window.location.reload();
                });
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    $(".addForm").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg("添加成功", { shift: -1 }, function () {
                    window.location.reload();
                });
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    //修改定时任务状态
    $('.job-switch').bootstrapSwitch({
        onText:'启用',
        offText:'停用' ,
        onColor:"success",
        offColor:"danger",
        size:"normal"
    });

    $('.info-switch').bootstrapSwitch({
        onText:'启用',
        offText:'停用' ,
        onColor:"success",
        offColor:"danger",
        size:"normal"
    });

    $(document).on('click', '.info-delete', function (){
        var id = $(this).attr("data-id");
        var url = $(this).attr("data-info-url");
        layer.confirm('您是确定删除本条信息？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: url+"?"+Math.random(),
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