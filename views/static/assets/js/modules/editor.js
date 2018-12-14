define(function(require, exports, module) {

    require('kindeditor');

    //装载编辑器
    KindEditor.ready(function(K){
        K.create('#fly_footer_code',{"filterMode":false,afterBlur:function(){this.sync();}});
    });

    //装载编辑器
    KindEditor.ready(function(K){
        K.create('#content',{"filterMode":false,afterBlur:function(){this.sync();}});
    });
});