(function ($) {
    var defaultSettings = {
        format: 'yyyy-MM-dd',//默认日期格式
        callBack: function () { },//回掉函数
        maxDate: new Date(),//最大日期
        minDate: '1949-10-01',//最小日期
        defDate: '',//默认日期
        defIsShow: false,//是否在文本框中显示默认日期
        inputIsRead: false
    };
    $.fn.jHsDate = function (options) {
        options = $.extend({}, defaultSettings, options || {});
        var $this = this;
        if (options.inputIsRead) {
            $this.attr("readonly","readonly");
        }
        $this.attr("name", "jHsDateInput");
        if (options.maxDate == options.minDate) {
            alert("最大值最小值错误");
        }
        if (options.maxDate < options.defDate && options.defDate!='') {
            options.defDate = options.maxDate;
        }
        if (options.minDate > options.defDate && options.defDate != '') {
            options.defDate = options.minDate;
        }
        var html = "", yearHtml = "", monHtml = "", dayHtml = "", hourHtml = "", minuteHtml = "";
        var thisId = $this.attr("id");
        if (thisId == undefined) {
            alert('元素Id或元素不存在');
            return;
        }
        html = "<div id='" + thisId + "hsdate' name='jHsDate' style='box-shadow: 0 3px 5px rgba(221,221,221,0.5);'>";
        var formatTmp = options.format;
        var maxDateTmp = chromeDateToDate(options.maxDate);
        var minDateTmp = chromeDateToDate(options.minDate);
        var yearDateTmp = "";
        var clickType = "";
        if (formatTmp.indexOf("y") > -1) {
            yearHtml = setYear(maxDateTmp.split('-')[0], minDateTmp.split('-')[0]);
            clickType += "y"
        }
        if (formatTmp.indexOf("M") > -1) {
            monHtml = setMon();
            clickType += "M"
        }
        if (formatTmp.indexOf("d") > -1) {
            dayHtml = setDay(31);
            clickType += "d"
        }

        html += yearHtml + monHtml + dayHtml + hourHtml + minuteHtml;
        html += "</div>"
        $this.click(function () {
            if ($("#" + thisId + "hsdate").length > 0) {
                return false;
            }
            $('body').append(html);
            setDefDate(options.defDate, thisId + "hsdate");
            if (options.format.indexOf("h") > -1) {
                bindCkeckClick(clickType+"h")
            }
            //定位日期框
            var y = $this.offset().top;
            var x = $this.offset().left;
            var eleHeight = $this.outerHeight(false);
            var checkYear = "";
            $("#" + thisId + "hsdate").css({ "top": y + (eleHeight + 5), "left": x });
            $('.hs_year').click(function () {
                $('.yearHsDate> .hs_check').removeClass('hs_check');
                $(this).addClass('hs_check');
                //设置最大月份
                $('.monHsDate> .noCheck').removeClass('noCheck');
                checkYear = $(this).text();
                if (checkYear == new Date(maxDateTmp).getFullYear()) {
                    $('.monHsDate> .hs_check').removeClass('hs_check');
                    $("#" + thisId + "hsdate")
                        .children(".monHsDate")
                        .children(':gt(' + (new Date(maxDateTmp).getMonth()) + ')')
                        .addClass('noCheck');
                }
                //设置最小月份
                if (checkYear == new Date(minDateTmp).getFullYear()) {
                    $('.monHsDate> .hs_check').removeClass('hs_check');
                    $("#" + thisId + "hsdate")
                        .children(".monHsDate")
                        .children(':lt(' + (new Date(minDateTmp).getMonth()) + ')')
                        .addClass('noCheck');
                }
            });
            $('.hs_mon').click(function () {
                if (!$(this).hasClass('noCheck')) {
                    $('.monHsDate> .hs_check').removeClass('hs_check');
                    $(this).addClass('hs_check');
                    if (clickType.indexOf('d') > -1) {
                        showDay($(this).text());
                    }
                    //设置最大日
                    $('.dayHsDate> .noCheck').removeClass('noCheck');
                    if (checkYear == new Date(maxDateTmp).getFullYear() &&
                        $(this).text() == (new Date(maxDateTmp).getMonth() + 1) + "月") {
                        $('.dayHsDate> .hs_check').removeClass('hs_check');
                        $("#" + thisId + "hsdate")
                            .children(".dayHsDate")
                            .children(':gt(' + (new Date(maxDateTmp).getDate() - 1) + ')')
                            .addClass('noCheck');
                    }
                    //设置最小日

                    if (checkYear == new Date(minDateTmp).getFullYear() &&
                        $(this).text() == (new Date(maxDateTmp).getMonth() + 1) + "月") {
                        $('.dayHsDate> .hs_check').removeClass('hs_check');
                        $("#" + thisId + "hsdate")
                            .children(".dayHsDate")
                            .children(':lt(' + (new Date(minDateTmp).getDate() - 1) + ')')
                            .addClass('noCheck');
                        $('[class="hs_day"]').parent().scrollTop($('[class="hs_day"]').position().top);
                    }
                    bindCkeckClick(clickType);
                }
            });
        });
        //点击空白处关闭选择框
        $(document).click(function (e) {
            e = window.event || e; // 兼容IE7
            obj = $(e.srcElement || e.target);
            if (!$(obj).is("[name='jHsDateInput']") && !$(obj).is('[class^="hs_"]')) {
                $('[name="jHsDate"]').remove();
            }
        });
        if (options.defIsShow && options.defDate != '') {
            $this.val(DateFormat(options.defDate, options.format));
        }
        function bindCkeckClick(clickType) {
            switch (clickType) {
                case "y":
                    $('.hs_year').click(function () {
                        $($this).val($('[class="hs_year hs_check"]').text());
                        closeHsDate($("#" + thisId + "hsdate"));
                        options.callBack();
                    });
                    break;
                case "yM":
                    $('.hs_mon').click(function () {
                        var dateTmp = $('[class="hs_year hs_check"]').text() + "-" +
                            $('[class="hs_mon hs_check"]').text();
                        $($this).val(DateFormat(dateTmp.replace("月", ""), options.format));
                        closeHsDate($("#" + thisId + "hsdate"));
                        options.callBack();
                    });
                    break;
                case "yMd":
                    $('.hs_day').click(function () {
                        $('.dayHsDate>.hs_check').removeClass('hs_check');
                        $(this).addClass('hs_check');
                        var dateTmp = $('[class="hs_year hs_check"]').text() + "-" +
                            $('[class="hs_mon hs_check"]').text() + "-" +
                            $('[class="hs_day hs_check"]').text();
                        if (formatTmp.indexOf("h") > -1) {
                            hourHtml = setHour();
                            minuteHtml = setMinute();
                            $("#" + thisId + "hsdate").html(hourHtml + minuteHtml);
                            $("#" + thisId + "hsdate").children('.hourHsDate').children().click(function () {
                                dateTmp = dateTmp.split(' ')[0]
                                dateTmp += " " + $(this).text().replace("时", "");
                                $(this).parent().children().removeClass('hs_check');
                                $(this).addClass('hs_check');
                            });
                            $("#" + thisId + "hsdate").children('.minuteHsDate').children().click(function () {
                                dateTmp = dateTmp.split(':')[0]
                                dateTmp += ":" + $(this).text().replace("分", "");
                                $(this).parent().children().removeClass('hs_check');
                                $(this).addClass('hs_check');
                                $($this).val(DateFormat(dateTmp.replace("月", ""), options.format));
                                closeHsDate($("#" + thisId + "hsdate"));
                                options.callBack();
                            });

                        } else {
                            $($this).val(DateFormat(dateTmp.replace("月", ""), options.format));
                            closeHsDate($("#" + thisId + "hsdate"));
                            options.callBack();
                        }
                    });
                    break;
                case "h":
                    var dateTmp = "";
                    hourHtml = setHour();
                    minuteHtml = setMinute();
                    $("#" + thisId + "hsdate").html(hourHtml + minuteHtml);
                    $("#" + thisId + "hsdate").children('.hourHsDate').children().click(function () {
                        dateTmp = "";
                        dateTmp = $(this).text().replace("时", "");
                        $(this).parent().children().removeClass('hs_check');
                        $(this).addClass('hs_check');
                    });
                    $("#" + thisId + "hsdate").children('.minuteHsDate').children().click(function () {
                        dateTmp = dateTmp.split(':')[0]
                        dateTmp += ":" + $(this).text().replace("分", "");
                        $(this).parent().children().removeClass('hs_check');
                        $(this).addClass('hs_check');
                        $($this).val(dateTmp);
                        closeHsDate($("#" + thisId + "hsdate"));
                    });

                    break;
                default:
                    $('.hs_day').click(function () {
                        $('.dayHsDate>.hs_check').removeClass('hs_check');
                        $(this).addClass('hs_check');
                        var dateTmp = $('[class="hs_mon hs_check"]').text() + "-" +
                            $('[class="hs_day hs_check"]').text();
                        if (formatTmp.indexOf("h") > -1) {
                            hourHtml = setHour();
                            minuteHtml = setMinute();
                            $("#" + thisId + "hsdate").html(hourHtml + minuteHtml);
                            $("#" + thisId + "hsdate").children('.hourHsDate').children().click(function () {
                                dateTmp += " " + $(this).text().replace("时", "");
                                $(this).parent().children().removeClass('hs_check');
                                $(this).addClass('hs_check');
                            });
                            $("#" + thisId + "hsdate").children('.minuteHsDate').children().click(function () {
                                dateTmp = dateTmp.split(':')[0]
                                dateTmp += ":" + $(this).text().replace("分", "");
                                $(this).parent().children().removeClass('hs_check');
                                $(this).addClass('hs_check');
                                $($this).val(DateFormat(dateTmp.replace("月", ""), options.format));
                                closeHsDate($("#" + thisId + "hsdate"));
                            });

                        } else {
                            $($this).val(DateFormat(dateTmp.replace("月", ""), options.format));
                            closeHsDate($("#" + thisId + "hsdate"));
                        }
                    });
                    break;
            }
        }
        //显示天
        function showDay(chekMon) {
            if (isLeapYear($('.yearHsDate>.hs_check').text()) && chekMon == "2月") {
                dayHtml = setDay(29);
            } else if (chekMon == "1月" ||
                chekMon == "3月" || chekMon == "5月" ||
                chekMon == "7月" || chekMon == "8月" ||
                chekMon == "10月" || chekMon == "12月") {
                dayHtml = setDay(31);
            } else if (chekMon == "4月" || chekMon == "6月" ||
                chekMon == "9月" || chekMon == "11月") {
                dayHtml = setDay(30);
            } else if (!isLeapYear($('.yearHsDate>.hs_check').text()) && chekMon == "2月") {
                dayHtml = setDay(28);
            }
            $('.dayHsDate').remove();
            $('.monHsDate').after(dayHtml);
        }
        //设置默认值
        function setDefDate(defDate, dateid) {
            var d = new Date(defDate);
            var yeardef = d.getFullYear();
            var mondef = (d.getMonth() + 1) + "月";
            var daydef = d.getDate();
            $('#' + dateid).children('.yearHsDate').children('.hs_year').each(function () {
                if ($(this).text() == yeardef) {
                    $(this).addClass('hs_check');
                    $(this).parent().scrollTop($(this).position().top);
                    return false;
                }
            });
            $('#' + dateid).children('.monHsDate').children('.hs_mon').each(function () {
                if ($(this).text() == mondef) {
                    $(this).addClass('hs_check');
                    if (clickType.indexOf('d') > -1) {
                        showDay(mondef);
                    }
                    bindCkeckClick(clickType);
                    return false;
                }
            });

            $('#' + dateid).children('.dayHsDate').children('.hs_day').each(function () {
                if ($(this).text() == daydef) {
                    $(this).addClass('hs_check');
                    $(this).parent().scrollTop($(this).position().top);
                    return false;
                }
            });
        }
    };
    function setYear(maxYear, minYear) {
        var setYearHtml = "<ul class='yearHsDate'>";
        for (var i = maxYear; i >= minYear; i--) {
            setYearHtml += "<li class='hs_year' >" + i + "</li>";
        }
        setYearHtml += "</ul>";
        return setYearHtml;
    }
    function setMon() {
        var setMonHtml = "<ul class='monHsDate'>";
        for (var i = 1; i <= 12; i++) {
            setMonHtml += "<li class='hs_mon'>" + i + "月</li>"
        }
        setMonHtml += "</ul>"
        return setMonHtml;
    }
    function setHour() {
        var setHourHtml = "<ul class='hourHsDate'>";
        for (var i = 1; i <= 24; i++) {
            if (i <= 9) {
                setHourHtml += "<li class='hs_hour'>0" + i + "时</li>"
            } else {
                setHourHtml += "<li class='hs_hour'>" + i + "时</li>"
            }
        }
        setHourHtml += "</ul>"
        return setHourHtml;
    }
    function setDay(days) {
        var setDayHtml = "<ul class='dayHsDate'>";
        for (var i = 1; i <= days; i++) {
            setDayHtml += "<li class='hs_day' >" + i + "</li>";
        }
        setDayHtml += "</ul>";
        return setDayHtml;
    }
    function setMinute() {
        var setminuteHtml = "<ul class='minuteHsDate'>";
        for (var i = 0; i < 60; i++) {
            if (i <= 9) {
                setminuteHtml += "<li class='hs_minute'>0" + i + "分</li>"
            }
            else {
                setminuteHtml += "<li class='hs_minute'>" + i + "分</li>"
            }
        }
        setminuteHtml += "</ul>"
        return setminuteHtml;
    }
    //部分浏览器时间转换问题
    function chromeDateToDate(cdate) {
        var d = new Date(cdate);
        return d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate() + ' ' + d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds();
    }
    //计算闰年
    function isLeapYear(Year) {
        if (((Year % 4) == 0) && ((Year % 100) != 0) || ((Year % 400) == 0)) {
            return true;
        }
        else {
            return false;
        }
    }
    //日期格式化
    function DateFormat(now, mask) {
        var d = new Date(now);
        var zeroize = function (value, length) {
            if (!length) length = 2;
            value = String(value);
            for (var i = 0, zeros = ''; i < (length - value.length); i++) {
                zeros += '0';
            }
            return zeros + value;
        };

        return mask.replace(/"[^"]*"|'[^']*'|\b(?:d{1,4}|m{1,4}|yy(?:yy)?|([hHMstT])\1?|[lLZ])\b/g, function ($0) {
            switch ($0) {
                case 'd': return d.getDate();
                case 'dd': return zeroize(d.getDate());
                case 'ddd': return ['Sun', 'Mon', 'Tue', 'Wed', 'Thr', 'Fri', 'Sat'][d.getDay()];
                case 'dddd': return ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'][d.getDay()];
                case 'M': return d.getMonth() + 1;
                case 'MM': return zeroize(d.getMonth() + 1);
                case 'MMM': return ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'][d.getMonth()];
                case 'MMMM': return ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'][d.getMonth()];
                case 'yy': return String(d.getFullYear()).substr(2);
                case 'yyyy': return d.getFullYear();
                case 'h': return d.getHours() % 12 || 12;
                case 'hh': return zeroize(d.getHours() % 12 || 12);
                case 'H': return d.getHours();
                case 'HH': return zeroize(d.getHours());
                case 'm': return d.getMinutes();
                case 'mm': return zeroize(d.getMinutes());
                case 's': return d.getSeconds();
                case 'ss': return zeroize(d.getSeconds());
                case 'l': return zeroize(d.getMilliseconds(), 3);
                case 'L': var m = d.getMilliseconds();
                    if (m > 99) m = Math.round(m / 10);
                    return zeroize(m);
                case 'tt': return d.getHours() < 12 ? 'am' : 'pm';
                case 'TT': return d.getHours() < 12 ? 'AM' : 'PM';
                case 'Z': return d.toUTCString().match(/[A-Z]+$/);
                // Return quoted strings with the surrounding quotes removed
                default: return $0.substr(1, $0.length - 2);
            }
        });
    };
    function closeHsDate(ele) {
        $(ele).remove();
    }
})(jQuery);    