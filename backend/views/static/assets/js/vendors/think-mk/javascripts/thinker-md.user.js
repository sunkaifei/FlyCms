// Source: public/javascripts/user/index.js
/**
 * Created by ling on 2015/3/31.
 */
$("textarea[data-provide='markdown']").markdown({
    language: 'zh',
    fullscreen: {
        enable: true
    },
    resize: 'vertical',
    localStorage: 'md',
    imgurl: '/cors/upload/ajaxUpload',
    base64url: '/cors/upload/base64Upload'
});
