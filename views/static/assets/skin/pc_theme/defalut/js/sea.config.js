seajs.config({
	alias: {
        'jquery': '/assets/js/jquery.min',
        '$': '/assets/js/jquery.min',
        'jquery.migrate': '/assets/js/jquery-migrate-1.2.1.min',
        'plugins': '/assets/js/plugins',
        'jqueryform': '/assets/js/jquery.form.min',

        /* modules */
        'main': 'modules/main',
        'index': 'modules/index',
        'ucenter': 'modules/ucenter',
        'login': 'modules/login',
        'editor': 'modules/editor',
        'post': 'modules/post',
        'answereditor': 'modules/answer-editor',
        'account': 'modules/account',
        'topic': 'modules/topic',
        'forgetpassword': 'modules/forget-password',
        'loginbox': 'modules/loginbox',
        
        /* vendors */
        'bootstrap': 'vendors/bootstrap/js/bootstrap.min',
        'bootstraponhover': 'vendors/bootstrap/js/bootstrap-dropdown-on-hover',
        'bootstrapValidator': 'vendors/bootstrapValidator/bootstrapValidator',
        'bootstrapSwitch': 'vendors/bootstrap-switch/js/bootstrap-switch.min',
        'ckeditor': 'vendors/ckeditor/ckeditor',
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
        'wangEditor': 'vendors/wangEditor/wangEditor.min',
        'kindeditor': 'vendors/kindeditor/kindeditor-all-min',
        'kindeditorCN': 'vendors/kindeditor/lang/zh-CN.js',
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
        'treetable': 'vendors/treetable/jquery.treetable',
        'webuploader': 'vendors/webuploader/webuploader.min',
        'infinitescroll': 'vendors/masonry/jquery.infinitescroll',
        'layout-latest': 'vendors/jquery_layout/jquery.layout-latest.min',
        'ztree': 'vendors/ztree/jquery.ztree.core-3.5.min',
        'jHsDate': 'vendors/jHsDate/js/jHsDate',
        'moment': 'vendors/momentjs/moment.min',
        'highlight': 'vendors/highlight/highlight.pack',
        'prettify': 'vendors/prettify/prettify',

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
        'vendors': '/assets/js/vendors',
    },

    // 变量配置
    vars: {
        'locale': 'zh-cn'
    },

    charset: 'utf-8',

    debug: false
});


var __SEAJS_FILE_VERSION = '?v=1.3';

