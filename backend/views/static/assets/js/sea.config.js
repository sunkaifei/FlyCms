seajs.config({
	alias: {
        'jquery': 'jquery.min',
        '$': 'jquery.min',
        'jquery.migrate': 'jquery-migrate-1.2.1.min',
        'plugins': 'plugins',
        'jqueryform': 'jquery.form.min',

        /* modules */
        'adminlogin': 'modules/login',
        'login': 'modules/login',
        'editor': 'modules/editor',
        'permission': 'modules/permission',
        'user': 'modules/user',
        'siterelated': 'modules/siterelated',
        'form': 'modules/form',
        'content': 'modules/content',
        'config': 'modules/config',
        'tools': 'modules/tools',
        'share': 'modules/share',
        
        /* vendors */
        'bootstrap': 'vendors/bootstrap/js/bootstrap.min',
        'bootstraponhover': 'vendors/bootstrap/js/bootstrap-dropdown-on-hover',
        'bootstrapValidator': 'vendors/bootstrapValidator/bootstrapValidator',
        'bootstrapSwitch': 'vendors/bootstrap-switch/js/bootstrap-switch.min',
        'kindeditor': 'vendors/kindeditor/kindeditor-all-min',
        'kindeditorCN': 'vendors/kindeditor/lang/zh-CN',
        'slick': 'vendors/slide/slick',
        'datetimepicker': 'vendors/bootstrap-datetimepicker/js/bootstrap-datetimepicker.min',
        'datetimepicker.zh-CN': 'vendors/bootstrap-datetimepicker/js/locales/bootstrap-datetimepicker.zh-CN',
        'hover': 'vendors/bootstrap/js/bootstrap-hover-dropdown.min',
        'baguetteBox': 'vendors/baguette/baguetteBox.min',
        'cropper': 'vendors/cropper/cropper.min',
        'layer': 'vendors/layer/layer',
        'laydate': 'vendors/laydate/laydate',
        'pace': 'vendors/pace/pace.min',
        'jqueryform': 'vendors/from/jquery.form.min',
        'jquerypjax': 'vendors/jquery_pjax/jquery.pjax',
        'dmuploader': 'vendors/uploader/dmuploader',
        'jcrop': 'vendors/jcrop/jquery.jcrop.min',
        'validate': 'vendors/validate/jquery-validate',
        'autovalidate': 'vendors/autovalidate/validate',
        'lazyload': 'vendors/lazyload/jquery.lazyload',
        'knob': 'vendors/jquery-knob/js/jquery.knob',
        'wangEditor': '../../js/vendors/wangEditor/wangEditor.min',
        'zhcn': 'vendors/umeditor/lang/zh-cn/zh-cn',
        'masonry': 'vendors/masonry/masonry.pkgd.min',
        'handlebars': 'vendors/waterfall/handlebars',
        'waterfall': 'vendors/waterfall/waterfall.min',
        'fileinput': 'vendors/jquery-Upload/js/fileinput',
        'fileinput_locale_zh': 'vendors/jquery-Upload/js/fileinput_locale_zh',
        'uploadPreview': 'vendors/uploadPreview/uploadPreview',
        'drag': 'vendors/jquery-tab/drag-arrange',
        'major_arr': 'vendors/jquery-tab/major_arr',
        'major_func': 'vendors/jquery-tab/major_func',
        'nationality_arr': 'vendors/jquery-tab/nationality_arr',
        'nationality_func': 'vendors/jquery-tab/nationality_func',
        'jquery-ui': 'vendors/jquery-tab/jquery-ui.min',
        'bootstraptable': 'vendors/bootstrap-table/bootstrap-table',
        'treeview': 'vendors/bootstrap-table/bootstrap-treeview',
        'webuploader': 'vendors/webuploader/webuploader.min',
        'infinitescroll': 'vendors/masonry/jquery.infinitescroll',
        'layout-latest': 'vendors/jquery_layout/jquery.layout-latest.min',
        'ztreecore': 'vendors/ztree/jquery.ztree.core.min',
        'ztreeexcheck': 'vendors/ztree/jquery.ztree.excheck-3.5.min',
        'ztreeexedit': 'vendors/ztree/jquery.ztree.exedit.min',
        'ztreeexhide': 'vendors/ztree/jquery.ztree.exhide.min',
        /* Select搜索 */
        'searchableSelect': 'vendors/searchableSelect/jquery.searchableSelect',
        /* ajax搜索当前元素 */
        'hideseek': 'vendors/ajaxsearch/jquery.hideseek.min',
        'sticky': 'vendors/ajaxsearch/waypoints-sticky.min',
        'waypoints': 'vendors/ajaxsearch/waypoints.min'
    },

    // 预加载项
    preload: [this.JSON ? '' : 'json', 'jquery'],

        // 路径配置
    paths: {
        'vendors': '../vendors',
    },

    // 变量配置
    vars: {
        'locale': 'zh-cn'
    },

    charset: 'utf-8',

    debug: false
});


var __SEAJS_FILE_VERSION = '?v=1.3';

//seajs.on('fetch', function(data) {
//if (!data.uri) {
//	return ;
//}
//
//if (data.uri.indexOf(app.mainScript) > 0) {
//	return ;
//}
//
//if (/\:\/\/.*?\/assets\/libs\/[^(common)]/.test(data.uri)) {
//    return ;
//}
//
//data.requestUri = data.uri + __SEAJS_FILE_VERSION;
//
//});
//
//seajs.on('define', function(data) {
//if (data.uri.lastIndexOf(__SEAJS_FILE_VERSION) > 0) {
//    data.uri = data.uri.replace(__SEAJS_FILE_VERSION, '');
//}
//});