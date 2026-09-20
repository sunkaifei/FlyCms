define(function(require, exports, module) {
    $("body").append( '<div class="modal fade login" id="loginModal">\n' +
    '    <div class="modal-dialog login animated">\n' +
    '        <div class="modal-content">\n' +
    '            <div class="modal-header">\n' +
    '                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>\n' +
    '                <h4 class="modal-title">用户登录</h4>\n' +
    '            </div>\n' +
    '            <div class="modal-body">\n' +
    '                <div class="box">\n' +
    '                    <div class="content">\n' +
    '                        <div class="error"></div>\n' +
    '                        <div class="form loginBox">\n' +
    '                            <form method="post" action="/ucenter/ajaxlogin" accept-charset="UTF-8" onsubmit="return false;">\n' +
    '                                <div class="form-group ">\n' +
    '                                    <input type="text" class="form-control" name="username" id="username" value="" required placeholder="登录名、邮箱或者手机号">\n' +
    '                                </div>\n' +
    '                                <div class="form-group">\n' +
    '                                    <input type="password" class="form-control" name="password" id="password" required placeholder="不少于 6 位">\n' +
    '                                </div>\n' +
    '                                <div class="form-group">\n' +
    '                                    <div class="input-group">\n' +
    '                                        <input type="text" id="code" name="code" class="form-control" autocomplete="new-text" placeholder="右侧验证码">\n' +
    '                                        <div class="input-group-addon"><a href="javascript:void(0);" id="reloadCaptcha"><img src="/captcha/default"></a></a></div>\n' +
    '                                    </div>\n' +
    '                                </div>\n' +
    '                                <div class="form-group">\n' +
    '                                    <button type="submit" id="register-btn" class="btn btn-default btn-login">登录</button>\n' +
    '                                </div>\n' +
    '                            </form>\n' +
    '                        </div>\n' +
    '                    </div>\n' +
    '                </div>\n' +
    '                <div class="box">\n' +
    '                    <div class="content registerBox" style="display:none;">\n' +
    '                        <div class="form">\n' +
    '                            <form method="post" action="/register" accept-charset="UTF-8" onsubmit="return false;">\n' +
    '                                <div class="form-group">\n' +
    '                                    <input id="phoneNumber" class="form-control" type="text" placeholder="手机号码" name="text">\n' +
    '                                </div>\n' +
    '                                <div class="form-group">\n' +
    '                                    <div class="input-group">\n' +
    '                                        <input type="text" name="mobilecode" class="form-control mobilecode" autocomplete="new-text" placeholder="手机验证码">\n' +
    '                                        <input type="hidden" id="verifytime" value="" />\n' +
    '                                        <div class="input-group-addon" id="addMobileCode" data-toggle="modal" data-target="#myMobileModal">获取验证码</div>\n' +
    '                                    </div>\n' +
    '                                </div>\n' +
    '                                <div class="form-group">\n' +
    '                                    <input id="regPassword" class="form-control" type="password" placeholder="登录密码" name="password">\n' +
    '                                </div>\n' +
    '                                <div class="form-group">\n' +
    '                                    <input id="regPassword2" class="form-control" type="password" placeholder="确认密码" name="password2">\n' +
    '                                </div>\n' +
    '                                <div class="form-group">\n' +
    '                                    <input class="btn btn-default btn-register" type="submit" value="提交注册" name="commit">\n' +
    '                                </div>\n' +
    '                            </form>\n' +
    '                        </div>\n' +
    '                    </div>\n' +
    '                </div>\n' +
    '            </div>\n' +
    '            <div class="modal-footer">\n' +
    '                <div class="forgot login-footer">\n' +
    '                     <span>没有 <a href="javascript:;" class="showRegister">注册账号</a> ?</span>\n' +
    '                </div>\n' +
    '                <div class="forgot register-footer" style="display:none">\n' +
    '                    <span>已经有账号?</span>\n' +
    '                    <a href="javascript: ;" class="showLogin">登录</a>\n' +
    '                </div>\n' +
    '            </div>\n' +
    '        </div>\n' +
    '    </div>\n' +
    '</div>\n' +
    '\n' +
    '<div class="modal fade bs-example-modal-sm" id="myMobileModal" tabindex="-1" role="dialog" aria-labelledby="mySmallModalLabel">\n' +
    '    <div class="modal-dialog modal-sm" role="document">\n' +
    '        <div class="modal-content">\n' +
    '            <div class="modal-header">\n' +
    '                <button type="button" class="close" data-dismiss="modal" aria-label="Close">\n' +
    '                    <span aria-hidden="true">&times;</span>\n' +
    '                </button>\n' +
    '                <h4 class="modal-title" id="myModalLabel">获取手机验证码</h4>\n' +
    '            </div>\n' +
    '            <div class="modal-body">\n' +
    '                <div class="input-group">\n' +
    '                    <input type="text" class="form-control input-lg regCaptcha" id="exampleInputAmount" placeholder="右侧验证码">\n' +
    '                    <div class="input-group-addon" style="padding: 0px;background-color: #fff;"><a href="javascript:void(0);" id="regCaptcha"><img src=""></a></div>\n' +
    '                </div>\n' +
    '            </div>\n' +
    '            <div class="modal-footer">\n' +
    '                <button type="button" class="btn btn-default" data-dismiss="modal">关闭</button>\n' +
    '                <button type="button" class="btn btn-primary mobilecod-btn">免费获取</button>\n' +
    '            </div>\n' +
    '        </div>\n' +
    '    </div>\n' +
    '</div>');
});