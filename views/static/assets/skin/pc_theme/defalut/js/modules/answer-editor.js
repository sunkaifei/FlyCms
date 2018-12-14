define(function(require, exports, module) {
    J = jQuery;
    require('jqueryform');
    require('bootstrap');
    require('prettify');
    require('ckeditor');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    $(document).ready(function() {
        $("pre").addClass("prettyprint linenums");
        prettyPrint();
    });

    var editor;
    $("#answer-add,.answer-btn").on("click", function(){
        if(userid>0){
            if($(".answer-add").length < 1) {
                $(".question-content").before("<div class=\'answer-add\'><div  class=\'user\'><div class=\'author-avatar\'><a href=\'/people/"+userid+"\' target=\'_blank\'><img class=\'avatar-38 mr-10 hidden-xs\' src=\'"+smallAvatar+"\' alt=\'"+nickname+"\'></a></div><div class=\'author-content\'><div class=\'author-name\'>"+nickname+"</div><div class=\'author-nick\'>"+signature+"</div></div></div><div class=\'form-group mt-10\'><div style=\'position: relative;\'><div id=\'content\'></div></div></div><div class=\'row mt-20\'><div class=\'answer-footer\'><button type=\'submit\' class=\'btn btn-primary pull-right btn-answer\'>提交答案</button></div></div></div>");
                CKEDITOR.replace('content');
            }
        }else{
            $("#loginModal").modal();
        }
    });

    $(document).on('click', '.btn-answer', function (){
        if(userid>0) {
            var questionId = $(".question-id").attr("data-question-id");
            var content = CKEDITOR.instances.content.getData();
            $.ajax({
                type: "post",
                dataType: "json",
                url: "/ucenter/answer/add",
                data: {"questionId": questionId, "content": content},
                success: function (result) {
                    if (result.code >= 0) {
                        layer.msg("答案提交成功！", {icon: 1});
                        window.location.reload();
                        return false;
                    } else {
                        layer.msg(result.message, {icon: 5});
                        return false;
                    }
                }
            })
        }else{
            $("#loginModal").modal();
        }
    });

    $(".showRegister").on("click", function(){
        $(".registerBox").show();
        $(".login-footer").hide();
        $(".loginBox").hide();
        $(".register-footer").show();
        $(".modal-title").html("用户注册");
    });

    $(".showLogin").on("click", function(){
        $(".loginBox").show();
        $(".login-footer").show();
        $(".register-footer").hide();
        $(".registerBox").hide();
        $(".modal-title").html("用户登录");
    });

    $(".question-list-item").on("mouseenter", function () {
        var $this = $(this);
        $this.find(".right-close").show();
    }).on("mouseleave", function () {
        var $this = $(this);
        $this.find(".right-close").hide();
    });

    $(".all-content").on("click", function(){
        var display =$(".hidden-content").css('display');
        if(display == 'none'){
            $(".hidden-content").show();
            $(".shrink-content").show();
            $(".show-content").hide();
        }
    });

    $(".shrink-content").on("click", function(){
        var display =$(".show-content").css('display');
        if(display == 'none'){
            $(".show-content").show();
            $(".hidden-content").hide();
            $(".shrink-content").hide();
        }
    });

});