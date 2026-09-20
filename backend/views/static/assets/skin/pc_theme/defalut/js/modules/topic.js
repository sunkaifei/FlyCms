define(function(require, exports, module) {
    var plugins = require('plugins');
    J = jQuery;
    require('layer');
    require('ckeditor');
    require('jqueryform');

    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });
    if($("#txt_content").length > 0) {
        CKEDITOR.replace('txt_content');
    }

    $(".submit-btn").bind("click",function(){
        for (instance in CKEDITOR.instances){
            CKEDITOR.instances[instance].updateElement();
        }
    })

    $('#add_form').ajaxForm({
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
});