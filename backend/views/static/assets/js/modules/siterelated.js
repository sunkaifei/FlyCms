define(function(require, exports, module) {
    J = jQuery;
    require('jqueryform');
    require('treetable');
    require('layer');
    layer.config({
        path: '/assets/js/vendors/layer/' //layer.js所在的目录，可以是绝对目录，也可以是相对目录
    });

    var option = {
        theme:'vsStyle',
        expandLevel : 2,
        beforeExpand : function($treeTable, id) {
            //判断id是否已经有了孩子节点，如果有了就不再加载，这样就可以起到缓存的作用
            if ($('.' + id, $treeTable).length) { return; }
            //这里的html可以是ajax请求
            var html = '<tr id="8" pId="6"><td>5.1</td><td>可以是ajax请求来的内容</td></tr>'
                + '<tr id="9" pId="6"><td>5.2</td><td>动态的内容</td></tr>';

            $treeTable.addChilds(html);
        },
        onSelect : function($treeTable, id) {
            window.console && console.log('onSelect:' + id);

        }

    };
    $('#treeTable').treeTable(option);

});