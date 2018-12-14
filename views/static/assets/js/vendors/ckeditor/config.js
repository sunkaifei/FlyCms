/**
 * @license Copyright (c) 2003-2018, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see https://ckeditor.com/legal/ckeditor-oss-license
 */

CKEDITOR.editorConfig = function( config ) {
    config.language = 'zh-cn';
    config.height = 300;
    config.toolbarCanCollapse = true;
    // Define changes to default configuration here.
    // For complete reference see:
    // http://docs.ckeditor.com/#!/api/CKEDITOR.config

    // The toolbar groups arrangement, optimized for two toolbar rows.
    config.toolbar = 'Basic';

    config.toolbar_Basic =
        [
            ['Source','-','TextColor','Bold', 'Italic','FontSize', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink'],
            ['Outdent','Indent','Blockquote','Find','Replace','-','SelectAll','-','SpellChecker', 'Scayt'],
            ['TextColor','BGColor','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock'],
            ['Image','Syntaxhighlight']
        ];

    config.extraPlugins = 'clipboard,lineutils,widget,dialog,codesnippet';
    // Remove some buttons provided by the standard plugins, which are
    // not needed in the Standard(s) toolbar.
    config.removeButtons = 'Underline,Subscript,Superscript';

    // Set the most common block elements.
    config.format_tags = 'p;h1;h2;h3;pre';

    //从word中粘贴内容时是否移除格式
    config.pasteFromWordRemoveFontStyles = true;
    config.pasteFromWordRemoveStyles = true;

    // Simplify the dialog windows.
    config.removeDialogTabs = 'image:advanced;link:advanced;image:Link';

    // 图片上传配置
      /* config.filebrowserUploadUrl = '/ucenter/uploadImage';*/
       config.filebrowserImageUploadUrl = '/ucenter/uploadImage';
       /*config.filebrowserFlashUploadUrl = basePath + '/ckeditorUpload/uploadImage';*/
};

CKEDITOR.on('dialogDefinition', function(ev) {
    // Take the dialog name and its definition from the event data
    var dialogName = ev.data.name;
    var dialogDefinition = ev.data.definition;
    var editor = ev.editor;
    if (dialogName == 'image') {
        dialogDefinition.onOk = function(e) {
            var imageSrcUrl = e.sender.originalElement.$.src;
            var imgHtml = CKEDITOR.dom.element.createFromHtml("<img src='" + imageSrcUrl + "' alt=''/>");
            editor.insertElement(imgHtml);
        };
    }
});