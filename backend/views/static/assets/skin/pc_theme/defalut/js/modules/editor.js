(function ($) {
    $.fn.CascadingSelect = function (options) {
        //默认参数设置
        var settings = {
            url: "/ucenter/article/category_child", //请求路径
            data: "0",    //初始值(字符串格式)
            split: ",",    //分割符
            cssName: "form-control category-select",  //样式名称
            val: "id",    //<option value="id">name</option>
            text: "name",   //<option value="id">name</option>
            hiddenName: "categoryId" //隐藏域的name属性的值
        }
        //合并参数
        if (options)
            $.extend(settings, options);
        //链式原则
        return this.each(function () {
            init($(this), settings.data);
            /*
            初始化
            @param container 容器对象
            @param data   初始值
            */
            function init(container, data) {
                //创建隐藏域对象,并赋初始值
                var _input = $("<input type='hidden' name='" + settings.hiddenName + "' />").appendTo(container).val(settings.data);
                var arr = data.split(settings.split);
                for (var i = 0; i < arr.length; i++) {
                    //创建下拉框
                    createSelect(container, arr[i], arr[i + 1] || -1);
                }
            }
            /*
            创建下拉框
            @param container 容器对象
            @param parentid  父ID号
            @param id   自身ID号
            */
            function createSelect(container, parentid, id) {
                //创建select对象，并将select对象放入container内
                var _select = $("<select></select>").appendTo(container).addClass(settings.cssName);
                //如果parentid为空，则_parentid值为0
                var _parentid = parentid || 0;
                //发送AJAX请求,返回的data必须为json格式
                $.getJSON(settings.url, { parentId: _parentid }, function (data) {
                    //添加子节点<option>
                    addOptions(container, _select, data).val(id || -1)

                });
            }

            /*
            为下拉框添加<option>子节点
            @param container 容器对象
            @param select  下拉框对象
            @param data   子节点数据(要求数据为json格式)
            */
            function addOptions(container, select, data) {
                select.append($('<option value="-1">=请选择=</option>'));
                for (var i = 0; i < data.length; i++) {
                    select.append($('<option value="' + data[i][settings.val] + '">' + data[i][settings.text] + '</option>'));
                }
                //为select绑定change事件
                select.bind("change", function () { _onchange(container, $(this), $(this).val()) });
                return select;
            }
            /*
            select的change事件函数
            @param container 容器对象
            @param select  下拉框对象
            @param id   当前下拉框的值
            */
            function _onchange(container, select, id) {
                var nextAll = select.nextAll("select");
                //如果当前select对象的值是空或-1（即：==请选择==），则将其后面的select对象全部移除
                if (!id || id == "-1") {
                    nextAll.remove();
                }
                $.getJSON(settings.url, { parentId: id }, function (data) {
                    if (data.length > 0) {
                        var _html = $("<select class='" + settings.cssName + "'></select>");
                        var _select = addOptions(container, _html, data);
                        //判断当前select对象后面是否跟有select对象
                        if (nextAll.length < 1) {
                            select.after(_select); //没有则直接添加
                        } else {
                            nextAll.remove(); //有则先移除再添加
                            select.after(_select);
                        }
                    }
                    else {
                        nextAll.remove(); //没有子项则后面的select全部移除
                    }
                    saveVal(container); //进行数据保存，此方法必须放在回调函数里面
                });
                //saveVal(container); //如果放在这里，则会出现bug

            }
            /*
            将选择的值保存在隐藏域中，用于表单提交保存
            @param container 容器对象
            */
            function saveVal(container) {
                var arr = new Array();
                arr.push(0); //为数组arr添加元素0，父节点从0开始，所以添加0
                $("select", container).each(function () {
                    if ($(this).val() > 0) {
                        arr.push($(this).val()); //获取container下每个select对象的值，并添加到数组arr
                    }
                });
                //为隐藏域对象赋值
                $("input[name='" + settings.hiddenName + "']", container).val(arr.join(settings.split));
            }

        });
    }
})(jQuery);

define(function(require, exports, module) {

    var plugins = require('plugins');
    J = jQuery;
    require('layer');
    require('jqueryform');
    require('prettify');
    require('kindeditor');
    //require('kindeditorCN');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    var categoryId=$("#categoryId").val();
    $("#category").CascadingSelect({data:categoryId});

    if($("#txt_content").length > 0) {
        // 关闭过滤模式，保留所有标签
        KindEditor.options.filterMode = false;
        KindEditor.ready(function(K) {
            K.create('#txt_content', {
                uploadJson : '/ucenter/kindEditorUpload',
                cssPath :[ '/assets/js/vendors/prettify/prettify-default.css'],
                resizeType:0,
                filterMode : false,
                allowFileManager : true,
                afterBlur: function(){this.sync();}
            });
            prettyPrint();
        });

    }

    $('#tags').tagit({
        singleField: true,
        singleFieldNode: $('#fieldTags'),
        tagLimit: 5
    });

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