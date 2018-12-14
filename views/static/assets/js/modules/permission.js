define(function(require, exports, module) {
    J = jQuery;
    require('jqueryform');
    require('bootstrapSwitch');
    require('masonry');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });


    $('.controller-box').masonry({
        // options
        itemSelector: '.controller-box-item'
    });

    $(".data-update").on("click", function(){
        $.ajax({
            url: '/system/admin/permission_sync',
            data: {},
            dataType: "json",
            type :  "get",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(data){
                if(data.code==0){
                    layer.msg(data.message, {icon: 1});
                    return false;
                }else{
                    layer.msg(data.message, {icon: 2});
                }
            }
        });
    });

    $(document).on('click', '.data-delete', function (){
        var id = $(this).attr("data-id");
        var title = $(this).attr("data-title");
        layer.confirm('您是确定删除《'+title+'》？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/admin/permission_del?"+Math.random(),
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
                url: "/system/admin/group_del?"+Math.random(),
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

    $("#selectAll").click(function(){
        if($("input[type='checkbox']:not(:checked)").length>0) {
            $("input[type='checkbox']").prop('checked',true);
        }else {
            $("input[type='checkbox']").prop('checked',false);
        }
    });


    $("#group_add").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg("添加成功", { shift: -1 }, function () {
                    location.href = "/system/admin/group_list";
                });
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    $("#group_edit").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg("修改成功", { shift: -1 }, function () {
                    location.href = "/system/admin/group_list";
                });
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    $("#permission_edit").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg("修改成功", { shift: -1 }, function () {
                    location.href = "/system/admin/permission_list";
                });
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    $('.group-switch').bootstrapSwitch({
        size: "mini",
        onSwitchChange:function(){
            var groupId = $(this).attr("data-group-id");
            var permissionId = $(this).attr("data-permission-id");
            $.ajax({
                url: "/system/admin/group_markpermissions?"+Math.random(),
                data: {"groupId":groupId,"permissionId":permissionId},
                dataType: "json",
                type :  "POST",
                cache : false,
                async: false,
                error : function(i, g, h) {
                    layer.msg('发送错误', {icon: 2});
                },
                success: function(ret){
                    if (ret.message == "删除成功") {
                        $('.group-switch').bootstrapSwitch('state',false);
                        return false;
                    }
                    if (ret.message == "添加成功") {
                        $('.group-switch').bootstrapSwitch('state',true);
                        return false;
                    }
                    if (ret.message == "超级管理员组权限不能修改") {
                        $('.group-switch').bootstrapSwitch('state',true);
                        layer.msg('超级管理员组权限不能修改', {icon: 2});
                        return false;
                    }
                }
            });
        }
    });
});