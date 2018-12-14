define(function(require, exports, module) {
    J = jQuery;
    require('jqueryform');
    require('laydate');
    require('moment');
    require('cropper');
    require('bootstrap');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    $("#choiceAvatarBtn").click(function() {
        $("#newAvatarFile").click();
    });

    if($("#newAvatarImg").length > 0) {
        var newAvatarImg = $("#newAvatarImg");
    }
    $("#newAvatarFile").change(function() {
        $("#newAvatarImg").attr("src", "");
        var reader = new FileReader();
        reader.onload = function(e) {
            $("#newAvatarImg").attr("src", e.target.result).removeClass('hidden');
        };
        reader.readAsDataURL($(this)[0].files[0]);

        //cropper load image
        setTimeout(function() {
                newAvatarImg.cropper('destroy');
                var $previews = $(".preview");
                newAvatarImg.cropper({
                    ready: function(e) {
                        var $clone = $(this).clone().removeClass('cropper-hidden');
                        $clone.css({
                            display: 'block',
                            width: '100%',
                            minWidth: 0,
                            minHeight: 0,
                            maxWidth: 'none',
                            maxHeight: 'none'
                        });
                        $previews.css({
                            width: '100%',
                            overflow: 'hidden'
                        }).html($clone);
                    },
                    viewMode: 1,
                    aspectRatio: 1,
                    scalable: false,
                    cropBoxResizable: false,
                    crop: function(e) {
                        var imageData = $(this).cropper('getImageData');
                        var previewAspectRatio = e.width / e.height;
                        $previews.each(function() {
                            var $preview = $(this);
                            var previewWidth = $preview.width();
                            var previewHeight = previewWidth / previewAspectRatio;
                            var imageScaledRatio = e.width / previewWidth;
                            $preview.height(previewHeight).find('img').css({
                                width: imageData.naturalWidth / imageScaledRatio,
                                height: imageData.naturalHeight / imageScaledRatio,
                                marginLeft: -e.x / imageScaledRatio,
                                marginTop: -e.y / imageScaledRatio
                            });
                        });
                    }
                });
            },
            200);
    });

    $(document).on('click', '#confirmAvatarBtn', function (){
        if (!$("#newAvatarFile").val()) {
            alert("请先选择图片");
        } else {
            var avatarBase64 = newAvatarImg.cropper('getCroppedCanvas', {
                width: 150,
                height: 150
            }).toDataURL();
            $.ajax({
                url: '/ucenter/avatar.json',
                async: false,
                cache: false,
                method: 'post',
                dataType: 'json',
                data: {
                    avatar: avatarBase64
                },
                success: function(data) {
                    if (data.code >= 0) {
                        layer.msg("头像修改成功！", {icon: 1});
                        var new_src = data.url+'?v='+Math.random();
                        $("#user_avatar_image,.avatar-32").attr("src",new_src);
                        $('#avatar_modal').modal('hide');
                    } else {
                        layer.msg(data.message, {icon: 5});
                    }
                }
            });
        }
    });

    $(".password-btn").on("click", function(){
        var oldPassword=$("#old_password").val();
        var password=$("#password").val();
        var confirmPassword=$("#password_confirmation").val();
        var captcha=$("#captcha").val();
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

        if(confirmPassword!=password){
            $("#password_confirmation").closest('div').removeClass('has-success').addClass('has-error');
        }else{
            $("#password_confirmation").closest('div').removeClass('has-error').addClass('has-success');
        }

        $.ajax({
            url: '/ucenter/password_update',
            data: {'old_password': oldPassword,'password': password,'password_confirmation':confirmPassword,'captcha':captcha},
            dataType: "json",
            type :  "POST",
            cache : false,
            async: false,
            error : function(i, g, h) {
                layer.msg('发送错误', {icon: 2});
            },
            success: function(data){
                new_src = '/captcha/default?'+Math.random();
                $("#reloadCaptcha").find("img").attr("src",new_src);
                $(".captcha").val("");
                if(data.code==0){
                    //弹出提示5秒后刷新页面
                    layer.msg(data.message,{icon:1},function(){
                        parent.location.reload(); // 父页面刷新
                        var index = parent.layer.getFrameIndex(window.name); //获取窗口索引
                        parent.layer.close(index);
                    });
                    return false;
                }else{
                    layer.msg(data.message, {icon: 2});
                }
            }
        });
    });

    //生日
    laydate.render({
        elem: '#birthday' //指定元素
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

    $(".profile-sub").on("click", function(){
        var nickName=$("#nickName").val();
        var signature=$("#signature").val();
        var sex = $('input[name="sex"]:checked').val();
        var birthday=$("#birthday").val();
        var province = $("#province option:selected").val();
        var city = $("#city option:selected").val();
        var work=$("#work").val();
        var description=$("#setting-description").val();
        var userId=$("#userId").val();
        $.ajax({
            url: '/ucenter/account_update',
            data: {'nickName': nickName,'signature':signature,'sex': sex,'birthday': birthday,'province': province,'city': city,'work': work,'description': description,'userId': userId},
            dataType: "json",
            type :  "POST",
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
});