define(function(require, exports, module) {
	J = jQuery;
    require('jqueryform');
    require('bootstrapSwitch');
    require('masonry');
	require('layer');
	layer.config({
	    path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
	});
	var timenow = new Date().getTime();

    $('.controller-box').masonry({
        // options
        itemSelector: '.controller-box-item'
    });

    $(".data-update").on("click", function(){
        $.ajax({
            url: '/system/user/permission_sync',
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


    $('.group-switch').bootstrapSwitch({
        size: "mini",
        onSwitchChange:function(){
            var groupId = $(this).attr("data-group-id");
            var permissionId = $(this).attr("data-permission-id");
            $.ajax({
                url: "/system/user/markpermissions?"+Math.random(),
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

    $("#group_add").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg("添加成功", { shift: -1 }, function () {
                    location.href = "/system/user/group_list";
                });
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    //更新信息
    $("#update").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg("更新成功", { shift: -1 }, function () {
                    location.href = ret.url;
                });
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    //地区选择下拉菜单
    $('#province').change(function () {
        $('#city option:gt(0)').remove();
        $('#area option:gt(0)').remove();
        var parentId=$(this).val();
        $.ajax({
            type: "post",
            dataType:"json",
            url: "/areas/area_child",
            data: {"parentId":parentId},
            success: function (result) {
                var strocity = '';
                var datas = eval(result);
                $.each(datas, function(i,val){
                    strocity += "<option value='"+val.areaId+"' >"+val.areaName+"</option>";
                });
                $('#city').append(strocity);
            }
        })
    });

    //县级下来菜单
    $('#city').change(function () {
        $('#area option:gt(0)').remove();
        var parentId=$(this).val();
        $.ajax({
            type: "post",
            dataType:"json",
            url: "/areas/area_child",
            data: {"parentId":parentId},
            success: function (result) {
                var stroarea = '';
                var datas = eval(result);
                $.each(datas, function(i,val){
                    stroarea += "<option value='"+val.areaId+"' >"+val.areaName+"</option>";
                });
                $('#area').append(stroarea);
            }
        })
    });

    //删除用户信息
    $(document).on('click', '.user-delete', function (){
        var id = $(this).attr("data-id");
        var title = $(this).attr("data-title");
        layer.confirm('您是确定删除《'+title+'》？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/user/del?"+Math.random(),
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

    $("#admin_add").ajaxForm({
        dataType: "json"
        , beforeSubmit: function(formData, jqForm, options) {}
        , success: function(ret) {
            if (ret.code == 0) {
                layer.msg(ret.message, { shift: -1 }, function () {
                    location.href = "/system/user/admin_list";
                });
            } else {
                layer.msg(ret.message, {icon: 2});
            }
        }
        , error: function(ret) {alert(ret.message);}
        , complete: function(ret) {} 	      // 无论是 success 还是 error，最终都会被回调
    });

    //删除用户信息
    $(document).on('click', '.admin-delete', function (){
        var id = $(this).attr("data-id");
        var title = $(this).attr("data-title");
        layer.confirm('您是确定删除《'+title+'》管理员？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/user/delAdmin?"+Math.random(),
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

    $(document).on('click', '.role-delete', function (){
        var id = $(this).attr("data-id");
        var title = $(this).attr("data-title");
        layer.confirm('您是确定删除《'+title+'》？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/user/permission_del?"+Math.random(),
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

    //修改定时任务状态
    $('.job-switch').bootstrapSwitch({
        onText:'启用',
        offText:'停用' ,
        onColor:"success",
        offColor:"danger",
        size:"normal",
        onSwitchChange:function(event,state){
            //if(state==true){
                var id = $(this).attr("data-job-id");
                var _this=this;
                $.ajax({
                    url: "/system/job/update_status?"+Math.random(),
                    data: {"id":id},
                    dataType: "json",
                    type :  "POST",
                    cache : false,
                    async: false,
                    error : function(i, g, h) {
                        layer.msg('发送错误', {icon: 2});
                    },
                    success: function(ret){
                        if (ret.message == "0") {
                            //$(_this).bootstrapSwitch('state',false);
                            return false;
                        }
                        if (ret.message == "1") {
                            //$(_this).bootstrapSwitch('state',true);
                            return false;
                        }
                    }
                });
           // }
        }
    });

    $(document).on('click', '.job-delete', function (){
        var id = $(this).attr("data-id");
        layer.confirm('您是确定删除该任务？删除后将无法恢复！', {
            btn: ['确定','取消'] //按钮
        }, function(){
            $.ajax({
                url: "/system/job/delete?"+Math.random(),
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