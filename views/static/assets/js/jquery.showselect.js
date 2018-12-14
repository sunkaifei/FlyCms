/*******************************************************************************
**                  Jquery通用 [二维数组] 无限级联动、赋值联动                **
**                             jquery.getselect.js                            **
**                               2012-7-27 无聊的周五                         **
**                             TonY-c  QQ:20011011                            **
** 说明:                                                                      **
** 要求数据格式:二维数组                                                      **
** [[id, name, parentid],[id, name, parentid]]                                **
** 调用方法:                                                                  **
** var arr = [[1, "根级", 0],[2, "一级", 1],[3, "二级", 2]]                   **
** 1.$("#Select").showselect(0, arr);                                         **
** 2.$("#Select").showselect(arr);//参数id不传默认为0 ;                       **
** 3.赋值方式：id直接传入即可！                                               **
**   $("#Select").showselect(2, arr);//自动选择2级联动 根级 -> 一级 -> 请选择 **
**   $("#Select").showselect(3, arr);//自动选择3级联动 根级 -> 一级 -> 二级   **
*******************************************************************************/

(function ($) {
    $.fn.extend({
        showselect: function (id, data) {
            if ($.isArray(id)) return this.getselect(0, id);
            if (!$.isArray(data)) {
                alert("select 数据获取失败!");
                return false;
            }
            if (id == 0) return this.getselect(data);
            var arr = new Array();
            arr.length = 0;
            var pid = id;
            var doi = -1;
            while (pid != doi) {
                doi = pid;
                for (var i = 0; i < data.length; i++) {
                    if (data[i][0] == pid) {
                        pid = data[i][2];
                        arr.push([pid, data[i][0]]);
                    }
                }
            }
            for (var i = 0; i < arr.length; i++) {
                this.getselect(arr[arr.length - 1 - i][0], data, arr[arr.length - 1 - i][1]);
                if (i == arr.length - 1) this.getselect(arr[arr.length - 1 - i][1], data);
            }
        },
        getselect: function (pid, data, selectedid) {
            if (typeof pid == "undefined") pid = 0;
            if (typeof selectedid == "undefined") selectedid = 0;
            if ($.isArray(pid)) return this.getselect(0, pid);
            var arr = new Array();
            arr.length = 0;
            for (var i = 0; i < data.length; i++)
                if (data[i][2] == pid)
                    arr.push("<option value=\"" + data[i][0] + "\"" + (data[i][0] == selectedid ? " selected=\"selected\"" : "") + ">" + data[i][1] + "</option>");
            $(this).append(arr.length > 0 ? this.toselect(arr.toString()) : "");
            this.bindchange(this, data);
        },
        toselect: function (strOption) {
            return "<select id=\"" + $(this).attr("id") + "select" + ($(this).find("select").length + 1) + "\"><option value=\"0\">请选择</option>" + strOption + "</select>";
        },
        bindchange: function (t, d) {
            $(this).find("select").unbind("change").change(function () {
                $(t).find("select:gt(" + $(this).index() + ")").remove();
                if ($(this).val() == 0) return false;
                var sel = t.getselect($(this).val(), d);
                $(this).after(sel);
                t.bindchange(t, d);
                //为不同控件text赋值
                $("input[id$='" + $(t).attr("id") + "_txtSelectValue']").val($(t).find("select:last").val());
            });
        }
    });
})(jQuery);