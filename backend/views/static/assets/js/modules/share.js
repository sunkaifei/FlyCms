(function ($) {
    var categoryUrl = null;
    if($("#categoryselect").length > 0) {
        categoryUrl = $("#categoryselect").attr("data-category-url");
    }
    $.fn.CascadingSelect = function (options) {
        //默认参数设置
        var settings = {
            url: categoryUrl, //请求路径
            data: "0",    //初始值(字符串格式)
            split: ",",    //分割符
            cssName: "form-control w-auto valid-text mr-10",  //样式名称
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
    J = jQuery;
    require('jqueryform');
    require('bootstrap');
    require('bootstraptable');
    require('ztreecore');
    require('ztreeexcheck');
    require('ztreeexedit');
    require('ztreeexhide');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    if($("#category").length > 0) {
        var category = $("#category").val();
    }else{
        var category = 0;
    }
    $("#categoryselect").CascadingSelect({data:category});

    var allcategory = null;
    if($(".tree-box").length > 0) {
        allcategory = $(".tree-box").attr("data-category-url");
        $(function() {
            // 授权树初始化
            var setting = {
                async: {
                    enable: true,
                    url: allcategory,
                    autoParam: ["ID"],
                    contentType: "application/json",
                    type: "get",
                    dataFilter: filter
                },
                view: {
                    addHoverDom: addHoverDom,
                    removeHoverDom: removeHoverDom,
                    showIcon: false
                },
                edit:{
                    enable: true,
                    drag:{
                        autoExpandTrigger: true,
                        prev: dropPrev,
                        inner: dropInner,
                        next: dropNext
                    },
                    editNameSelectAll: true,
                    showRemoveBtn: showRemoveBtn,
                    removeTitle: "删除节点",
                    renameTitle: "编辑节点名称",
                    showRenameBtn: true
                },
                data: {
                    simpleData: {
                        enable: true,
                        idKey: "id",
                        pIdKey: "pId",
                        nameKey: "name",
                        url: "url"
                    }
                },
                callback: {
                    beforeDrag: beforeDrag,
                    beforeDrop: beforeDrop,
                    beforeRemove: beforeRemove,
                    onDrop: onDrop,
                    onRemove: onRemove, //移除事件
                    onRename: onRename, //修改事件
                    onClick: zTreeOnClick
                }
            };

            $.ajax({
                url:allcategory,
                type: "get",
                dataType: "json",
                success:function(data){
                    //树形菜单初始化
                    zTreeObj = $.fn.zTree.init($("#functionTree"), setting,data);
                },
                error:function(xhr){
                    alert(xhr.status);
                }
            });

        });
    }

    function filter(treeId, parentNode, childNodes) {
        if (!childNodes) return null;
        for (var i = 0, l = childNodes.length; i < l; i++) {
            childNodes[i].name = childNodes[i].name.replace(/\.n/g, '.');
        }
        return childNodes;
    }

    function zTreeOnClick(e, treeId, treeNode) {
        //alert(treeNode.tId + ", " + treeNode.name);
        location.href = "/system/share/category_edit?id="+treeNode.id;
    };

    /** 是否显示删除按钮 */
    function showRemoveBtn(treeId, treeNode){
        // 根节点不显示删除按钮
        if(treeNode.level == 0){
            return false;
        }else{
            return true;
        }
    }
    function beforeRemove(treeId, treeNode) {
        var zTree = $.fn.zTree.getZTreeObj("functionTree");
        zTree.selectNode(treeNode);
        return confirm("确认删除 分类 -- " + treeNode.name + " 吗？");
    }
    function beforeRemove(treeId, treeNode){
        if(treeNode.pId==0){
            //如果删除的是根节点，也提示无法删除
            alert("根节点无法删除！")
            return false;
            //返回false 就会使前端的节点还保持存在，
            // 	如果是true,则会在前端界面删除，但数据库中还会有，刷新一下即可再次出现
        }
        if(treeNode.isParent){
            layer.msg("该分类下有子节点，无法删除", {icon: 2});
            return false;
        }
        if(confirm("是否删除？")){
            // 询问是否删除，若删除，则利用Ajax 技术异步删除，同是返回json格式数据，告知前台是否删除成功！
            $.post('/system/share/category_delete?id='+treeNode.id,function(data){
                // 从数据库中删除
                if(data.success){
                    // data  为 json 格式数据
                    zTree.removeNode(treeNode);
                    // z-Tree 的api ，从视角上 删除
                    alert(data.message);
                    // 要在后台删除成功后再进行视角上删除，这样就真正意义实现了删除。
                }else{
                    alert(data.message);
                    return false;
                }
            });
        }
        return ;
    }

    var log, className = "dark", curDragNodes, autoExpandNode;
    var dragId ; //拖拽节点的父节点的id
    /** 拖拽前执行 */
    function beforeDrag(treeId, treeNodes) {
        className = (className === "dark" ? "":"dark");
        //showLog("[ "+getTime()+" beforeDrag ]&nbsp;&nbsp;&nbsp;&nbsp; drag: " + treeNodes.length + " nodes." );
        for (var i=0,l=treeNodes.length; i<l; i++) {
            dragId = treeNodes[i].pId;
            if (treeNodes[i].drag === false) {
                curDragNodes = null;
                return false;
            } else if (treeNodes[i].parentTId && treeNodes[i].getParentNode().childDrag === false) {
                curDragNodes = null;
                return false;
            }
        }
        curDragNodes = treeNodes;
        return true;
    }
    function beforeDragOpen(treeId, treeNode) {
        autoExpandNode = treeNode;
        return true;
    }

    //用于捕获节点拖拽操作结束之前的事件回调函数，并且根据返回值确定是否允许此拖拽操作
    //默认值 null
    function beforeDrop(treeId, treeNodes, targetNode, moveType) {
        return targetNode ? targetNode.drop !== false : true;
    }
    //用于捕获节点拖拽操作结束的事件回调函数  默认值： null
    function onDrop(event, treeId, treeNodes, targetNode,moveType) {
        //拖拽成功时，修改被拖拽节点的pid
        var level=treeNodes[0].level,pid=0;

        if(level>0){
            pid=targetNode.id;
        }
        $.ajax({
            type:'post',
            url: '/system/share/category_drags?id='+treeNodes[0].id+'&pId='+pid,
            dataType: "text",
            async: false,
            success: function (data) {
            },
            error: function (msg) {
            }
        });
    }

    function beforeRename(treeId, treeNode, newName) {
        if (newName.length == 0) {
            alert("分类名称不能为空!");
            return false;
        }
        if(newName.length < 2){
            alert("分类名称不能少于2个字符！");
            return false;
        }
        return true;
    }
    function onRename(e, treeId, treeNode, isCancel) {
        //需要对名字做判定的，可以来这里写~~
        $.post('/system/share/ztree_category_edit?id='+treeNode.id+'&name='+treeNode.name),function (results) {
            if(results.code == 0){
                alert(results.message);
            }else{
                layer.msg(results.message, {icon: 2});
            }
        }
    }
    function onRemove(e, treeId, treeNode) {
        var node = treeNode[0];
        if(node.isParent){
            layer.msg("该分类下有子节点，无法删除", {icon: 2});
            return false;
        }
        $.post('/system/share/category_delete?id='+treeNode.id);
    }

    var newCount = 1;
    function addHoverDom(treeId, treeNode) {
        var sObj = $("#" + treeNode.tId + "_span");
        if (treeNode.editNameFlag || $("#addBtn_"+treeNode.tId).length>0) return;
        var addStr = "<span class='button add' id='addBtn_" + treeNode.tId
            + "' title='增加分类' onfocus='this.blur();'></span>";
        sObj.after(addStr);
        var btn = $("#addBtn_"+treeNode.tId);
        if (btn) btn.bind("click", function(){
            var zTree = $.fn.zTree.getZTreeObj("functionTree");
            //将新节点添加到数据库中  
            var name='NewNode' + (newCount++);
            $.post('/system/share/ztree_category_save?pid='+treeNode.id+'&name='+name,function (results) {
                if(results.code == 0){
                    zTree.addNodes(treeNode, { id: results.data.id, pId: treeNode.id, name: results.data.name});
                    var node = zTree.getNodeByParam("id", results.data, null); //根据新的id找到新添加的节点  
                    zTree.selectNode(node); //让新添加的节点处于选中状态
                }else{
                    layer.msg("分类名“ "+name+ " ”已存在！", {icon: 2});
                }
            });
        });
    };
    function removeHoverDom(treeId, treeNode) {
        $("#addBtn_"+treeNode.tId).unbind().remove();
    };

    function dropPrev(treeId, nodes, targetNode) {
        var pNode = targetNode.getParentNode();
        if (pNode && pNode.dropInner === false) {
            return false;
        } else {
            for (var i=0,l=curDragNodes.length; i<l; i++) {
                var curPNode = curDragNodes[i].getParentNode();
                if (curPNode && curPNode !== targetNode.getParentNode() && curPNode.childOuter === false) {
                    return false;
                }
            }
        }
        return true;
    }
    function dropInner(treeId, nodes, targetNode) {
        if (targetNode && targetNode.dropInner === false) {
            return false;
        } else {
            for (var i=0,l=curDragNodes.length; i<l; i++) {
                if (!targetNode && curDragNodes[i].dropRoot === false) {
                    return false;
                } else if (curDragNodes[i].parentTId && curDragNodes[i].getParentNode() !== targetNode && curDragNodes[i].getParentNode().childOuter === false) {
                    return false;
                }
            }
        }
        return true;
    }
    function dropNext(treeId, nodes, targetNode) {
        var pNode = targetNode.getParentNode();
        if (pNode && pNode.dropInner === false) {
            return false;
        } else {
            for (var i=0,l=curDragNodes.length; i<l; i++) {
                var curPNode = curDragNodes[i].getParentNode();
                if (curPNode && curPNode !== targetNode.getParentNode() && curPNode.childOuter === false) {
                    return false;
                }
            }
        }
        return true;
    }
});