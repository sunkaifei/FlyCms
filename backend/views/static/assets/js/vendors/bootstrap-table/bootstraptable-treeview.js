/**
 * created by lise 
 * @date 2018-01-18
 */
(function ($) {
    'use strict';

    var sprintf = function (str) {
        var args = arguments,
            flag = true,
            i = 1;

        str = str.replace(/%s/g, function () {
            var arg = args[i++];

            if (typeof arg === 'undefined') {
                flag = false;
                return '';
            }
            return arg;
        });
        return flag ? str : '';
    };

    var getFieldIndex = function (columns, field) {
        var index = -1;

        $.each(columns, function (i, column) {
            if (column.field === field) {
                index = i;
                return false;
            }
            return true;
        });
        return index;
    };
    var escapeHTML = function (text) {
        if (typeof text === 'string') {
            return text
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;')
                .replace(/`/g, '&#x60;');
        }
        return text;
    };

    var calculateObjectValue = function (self, name, args, defaultValue) {
        var func = name;

        if (typeof name === 'string') {
            var names = name.split('.');

            if (names.length > 1) {
                func = window;
                $.each(names, function (i, f) {
                    func = func[f];
                });
            } else {
                func = window[name];
            }
        }
        if (typeof func === 'object') {
            return func;
        }
        if (typeof func === 'function') {
            return func.apply(self, args);
        }
        if (!func && typeof name === 'string' && sprintf.apply(this, [name].concat(args))) {
            return sprintf.apply(this, [name].concat(args));
        }
        return defaultValue;
    };

    var getItemField = function (item, field) {
        var value = item;

        if (typeof field !== 'string' || item.hasOwnProperty(field)) {
            return item[field];
        }
        var props = field.split('.');
        for (var p in props) {
            value = value[props[p]];
        }
        return value;
    };

    var getParent = function (node, source, field) {
        var data = [];
        var items = $.grep(source, function (item, index) {
            return node.parentId == item[field];
        });
        $.each(items, function (index, item) {
            data.splice(0, 0, item);
            var child = getParent(item, source, field);
            $.each(child, function (i, n) {
                data.splice(0, 0, n);
            });
        });
        return data;
    };

    var getChild = function (node, source, field) {
        var items = $.grep(source, function (item, index) {
            return item.parentId == node[field];
        });
        return items;
    };

    var getAllChild = function (node, source, field) {
        var data = [];
        var g=function(child){
            $.each(child, function (i, n) {
                data.push(n);
                var subChild = getChild(n, source, field);
                if(subChild!=null && subChild.length>0){
                    g(subChild);
                }
            });
        }
        var child = getChild(node, source, field);
        g(child);
        return data;
    };

    //调用bootstrapTable组件的构造器得到对象
    var BootstrapTable = $.fn.bootstrapTable.Constructor,
        _initData = BootstrapTable.prototype.initData,
        _initPagination = BootstrapTable.prototype.initPagination;

    //重写bootstrapTable的initData方法
    BootstrapTable.prototype.initData = function () {
        _initData.apply(this, Array.prototype.slice.apply(arguments));
        var that = this;
        //初始化数据，添加level,isLeaf 属性
        if (that.options.treeView && this.data.length > 0) {
            var rows = [],levelStep=1;
            
            var roots = $.grep(this.data, function (row, index) {
                return row.parentId == null;
            });
            var g=function(child){
                var childLevel=that.options.treeRootLevel+levelStep;
                $.each(child, function (i, n) {
                    n.level=childLevel;
                    if (that.options.treeCollapseAll) {
                        n.hidden = true;
                    }
                    var subChild = getChild(n, that.data, that.options.treeId);
                    if(subChild==null || subChild.length==0){
                        n.isLeaf=true;
                    }
                    rows.push(n);
                    if(subChild!=null && subChild.length>0){
                        levelStep++;
                        g(subChild);
                    }else{
                        levelStep=1;
                    }
                });
            }
            $.each(roots, function (index, item) {
                item.level=that.options.treeRootLevel;
                var child = getChild(item, that.data, that.options.treeId);
                if(child==null || child.length==0){
                    item.isLeaf=true;
                }
                rows.push(item);
                g(child);
            });
            that.options.data = that.data = rows;
        }
    };

    //重写bootstrapTable的initPagination方法
    BootstrapTable.prototype.initPagination = function () {
        //理论情况下，treegrid是不支持分页的，所以默认分页参数为false
        if(this.options.treeView){
            this.options.pagination = false;
        }
         //调用“父类”的“虚方法”
        _initPagination.apply(this, Array.prototype.slice.apply(arguments));
    };

    //重写bootstrapTable的initRow方法
    
    BootstrapTable.prototype.initRow = function(item, i, data, parentDom) {
        var that=this,
            key,
            html = [],
            style = {},
            csses = [],
            data_ = '',
            attributes = {},
            htmlAttributes = [];

        if ($.inArray(item, this.hiddenRows) > -1) {
            return;
        }

        style = calculateObjectValue(this.options, this.options.rowStyle, [item, i], style);

        if (style && style.css) {
            for (key in style.css) {
                csses.push(key + ': ' + style.css[key]);
            }
        }

        attributes = calculateObjectValue(this.options,
            this.options.rowAttributes, [item, i], attributes);

        if (attributes) {
            for (key in attributes) {
                htmlAttributes.push(sprintf('%s="%s"', key, escapeHTML(attributes[key])));
            }
        }

        if (item._data && !$.isEmptyObject(item._data)) {
            $.each(item._data, function(k, v) {
                // ignore data-index
                if (k === 'index') {
                    return;
                }
                data_ += sprintf(' data-%s="%s"', k, v);
            });
        }

        html.push('<tr',
            sprintf(' %s', htmlAttributes.join(' ')),
            sprintf(' id="%s"', $.isArray(item) ? undefined : item._id),
            sprintf(' class="%s"', style.classes || ($.isArray(item) ? undefined : item._class)),
            sprintf(' data-index="%s"', i),
            sprintf(' data-uniqueid="%s"', item[this.options.uniqueId]),
            sprintf('%s', data_),
            '>'
        );

        if (this.options.cardView) {
            html.push(sprintf('<td colspan="%s"><div class="card-views">', this.header.fields.length));
        }

        if (!this.options.cardView && this.options.detailView) {
            html.push('<td>',
                '<a class="detail-icon" href="#">',
                sprintf('<i class="%s %s"></i>', this.options.iconsPrefix, this.options.icons.detailOpen),
                '</a>',
                '</td>');
        }

        $.each(this.header.fields, function(j, field) {
            var text = '',
                value_ = getItemField(item, field, that.options.escape),
                value = '',
                type = '',
                cellStyle = {},
                id_ = '',
                class_ = that.header.classes[j],
                data_ = '',
                rowspan_ = '',
                colspan_ = '',
                title_ = '',
                column = that.columns[j];

            if (that.fromHtml && typeof value_ === 'undefined') {
                return;
            }

            if (!column.visible) {
                return;
            }

            if (that.options.cardView && (!column.cardVisible)) {
                return;
            }

            if (column.escape) {
                value_ = escapeHTML(value_);
            }

            style = sprintf('style="%s"', csses.concat(that.header.styles[j]).join('; '));

            // handle td's id and class
            if (item['_' + field + '_id']) {
                id_ = sprintf(' id="%s"', item['_' + field + '_id']);
            }
            if (item['_' + field + '_class']) {
                class_ = sprintf(' class="%s"', item['_' + field + '_class']);
            }
            if (item['_' + field + '_rowspan']) {
                rowspan_ = sprintf(' rowspan="%s"', item['_' + field + '_rowspan']);
            }
            if (item['_' + field + '_colspan']) {
                colspan_ = sprintf(' colspan="%s"', item['_' + field + '_colspan']);
            }
            if (item['_' + field + '_title']) {
                title_ = sprintf(' title="%s"', item['_' + field + '_title']);
            }
            cellStyle = calculateObjectValue(that.header,
                that.header.cellStyles[j], [value_, item, i, field], cellStyle);
            if (cellStyle.classes) {
                class_ = sprintf(' class="%s"', cellStyle.classes);
            }
            if (cellStyle.css) {
                var csses_ = [];
                for (var key in cellStyle.css) {
                    csses_.push(key + ': ' + cellStyle.css[key]);
                }
                style = sprintf('style="%s"', csses_.concat(that.header.styles[j]).join('; '));
            }

            value = calculateObjectValue(column,
                that.header.formatters[j], [value_, item, i], value_);

            if (item['_' + field + '_data'] && !$.isEmptyObject(item['_' + field + '_data'])) {
                $.each(item['_' + field + '_data'], function(k, v) {
                    // ignore data-index
                    if (k === 'index') {
                        return;
                    }
                    data_ += sprintf(' data-%s="%s"', k, v);
                });
            }

            if (column.checkbox || column.radio) {
                type = column.checkbox ? 'checkbox' : type;
                type = column.radio ? 'radio' : type;

                text = [sprintf(that.options.cardView ?
                        '<div class="card-view %s">' : '<td class="bs-checkbox %s">', column['class'] || ''),
                    '<input' +
                    sprintf(' data-index="%s"', i) +
                    sprintf(' name="%s"', that.options.selectItemName) +
                    sprintf(' type="%s"', type) +
                    sprintf(' value="%s"', item[that.options.idField]) +
                    sprintf(' checked="%s"', value === true ||
                        (value_ || value && value.checked)  ||
                        (item && item.checked)? 'checked' : undefined) +
                    sprintf(' disabled="%s"', !column.checkboxEnabled ||
                        (value && value.disabled) ? 'disabled' : undefined) +
                    ' />',
                    that.header.formatters[j] && typeof value === 'string' ? value : '',
                    that.options.cardView ? '</div>' : '</td>'
                ].join('');

                item[that.header.stateField] = value === true || (value && value.checked);
            } else {
                value = typeof value === 'undefined' || value === null ?
                    that.options.undefinedText : value;
                    //渲染tree展开图标，下面text中添加了indent和icon。
                    var indent, icon;
                    if (that.options.treeView && column.field == that.options.treeField) {
                        var indent = item.level == that.options.treeRootLevel ? '' : sprintf('<span style="margin-left: %spx;"></span>', (item.level - that.options.treeRootLevel) * 15);
                        var child = $.grep(data, function (d, i) {
                            return d.parentId == item[that.options.treeId] && !d.hidden;
                        });
                        icon = sprintf('<span class="tree-icon %s" style="cursor: pointer; margin: 0px 5px;"></span>', child.length > 0 ? that.options.expandIcon : that.options.collapseIcon);
                        if(item.isLeaf){
                            icon = sprintf('<span class="tree-icon %s" style="cursor: pointer; margin: 0px 5px;"></span>', that.options.leafIcon);
                        }
                    }
                    //end
                text = that.options.cardView ? ['<div class="card-view">',
                    that.options.showHeader ? sprintf('<span class="title" %s>%s</span>', style,
                        getPropertyFromOther(that.columns, 'field', 'title', field)) : '',
                    sprintf('<span class="value">%s</span>', value),
                    '</div>'
                ].join('') : [sprintf('<td%s %s %s %s %s %s %s>',
                        id_, class_, style, data_, rowspan_, colspan_, title_),
                        indent,icon,
                    value,
                    '</td>'
                ].join('');

                // Hide empty data on Card view when smartDisplay is set to true.
                if (that.options.cardView && that.options.smartDisplay && value === '') {
                    // Should set a placeholder for event binding correct fieldIndex
                    text = '<div class="card-view"></div>';
                }
            }

            html.push(text);
        });

        if (this.options.cardView) {
            html.push('</div></td>');
        }
        html.push('</tr>');

        return html.join(' ');
    };
    //重写bootstrapTable的initBody方法
    BootstrapTable.prototype.initBody = function (fixedScroll) {
        var that = this,
            html = [],
            data = this.getData();

        this.trigger('pre-body', data);

        this.$body = this.$el.find('>tbody');
        if (!this.$body.length) {
            this.$body = $('<tbody></tbody>').appendTo(this.$el);
        }

        //Fix #389 Bootstrap-table-flatJSON is not working

        if (!this.options.pagination || this.options.sidePagination === 'server') {
            this.pageFrom = 1;
            this.pageTo = data.length;
        }

        var trFragments = $(document.createDocumentFragment());
        var hasTr;

        for (var i = this.pageFrom - 1; i < this.pageTo; i++) {
            var item = data[i];
            if (item.hidden) continue;//hidden属性,当前行不渲染
            var tr = this.initRow(item, i, data, trFragments);
            hasTr = hasTr || !!tr;
            if (tr&&tr!==true) {
                trFragments.append(tr);
            }
        }

        // show no records
        if (!hasTr) {
            trFragments.append('<tr class="no-records-found">' +
                sprintf('<td colspan="%s">%s</td>',
                this.$header.find('th').length,
                this.options.formatNoMatches()) +
                '</tr>');
        }

        this.$body.html(trFragments);

        if (!fixedScroll) {
            this.scrollTo(0);
        }

        // click to select by column
        this.$body.find('> tr[data-index] > td').off('click dblclick').on('click dblclick', function (e) {
            var $td = $(this),
                $tr = $td.parent(),
                item = that.data[$tr.data('index')],
                index = $td[0].cellIndex,
                fields = that.getVisibleFields(),
                field = fields[that.options.detailView && !that.options.cardView ? index - 1 : index],
                column = that.columns[getFieldIndex(that.columns, field)],
                value = getItemField(item, field, that.options.escape);

            if ($td.find('.detail-icon').length) {
                return;
            }

            that.trigger(e.type === 'click' ? 'click-cell' : 'dbl-click-cell', field, value, item, $td);
            that.trigger(e.type === 'click' ? 'click-row' : 'dbl-click-row', item, $tr, field);

            // if click to select - then trigger the checkbox/radio click
            if (e.type === 'click' && that.options.clickToSelect && column.clickToSelect) {
                var $selectItem = $tr.find(sprintf('[name="%s"]', that.options.selectItemName));
                if ($selectItem.length) {
                    $selectItem[0].click(); // #144: .trigger('click') bug
                }
            }
        });

        this.$body.find('> tr[data-index] > td > .detail-icon').off('click').on('click', function () {
            var $this = $(this),
                $tr = $this.parent().parent(),
                index = $tr.data('index'),
                row = data[index]; // Fix #980 Detail view, when searching, returns wrong row

            // remove and update
            if ($tr.next().is('tr.detail-view')) {
                $this.find('i').attr('class', sprintf('%s %s', that.options.iconsPrefix, that.options.icons.detailOpen));
                that.trigger('collapse-row', index, row);
                $tr.next().remove();
            } else {
                $this.find('i').attr('class', sprintf('%s %s', that.options.iconsPrefix, that.options.icons.detailClose));
                $tr.after(sprintf('<tr class="detail-view"><td colspan="%s"></td></tr>', $tr.find('td').length));
                var $element = $tr.next().find('td');
                var content = calculateObjectValue(that.options, that.options.detailFormatter, [index, row, $element], '');
                if($element.length === 1) {
                    $element.append(content);
                }
                that.trigger('expand-row', index, row, $element);
            }
            that.resetView();
            return false;
        });
        //treeicon点击事件
        this.$body.find('> tr[data-index] > td > .tree-icon').off('click').on('click', function (e) {
            e.stopPropagation();
            var $this = $(this),
                $tr = $this.parent().parent(),
                index = $tr.data('index'),
                row = data[index];
            var icon = $(this);
            if(icon.hasClass(that.options.expandIcon)){
                //展开状态
                icon.removeClass(that.options.expandIcon).addClass(that.options.collapseIcon);
                var child = getAllChild(data[index], data, that.options.treeId);
                $.each(child, function (i, c) {
                    $.each(that.data, function (index, item) {
                        if (item[that.options.treeId] == c[that.options.treeId]) {
                            item.hidden = true;
                            return;
                        }
                    });
                });
            }else{
                icon.removeClass(that.options.collapseIcon).addClass(that.options.expandIcon);
                var child = getChild(data[index], data, that.options.treeId);
                $.each(child, function (i, c) {
                    $.each(that.data, function (index, item) {
                        if (item[that.options.treeId] == c[that.options.treeId]) {
                            item.hidden = false;
                            return;
                        }
                    });
                });
            }
            that.options.data = that.data;
            that.initBody(true);
        });
        //end

        this.$selectItem = this.$body.find(sprintf('[name="%s"]', this.options.selectItemName));
        this.$selectItem.off('click').on('click', function (event) {
            event.stopImmediatePropagation();

            var $this = $(this),
                checked = $this.prop('checked'),
                row = that.data[$this.data('index')];

            if (that.options.maintainSelected && $(this).is(':radio')) {
                $.each(that.options.data, function (i, row) {
                    row[that.header.stateField] = false;
                });
            }

            row[that.header.stateField] = checked;

            if (that.options.singleSelect) {
                that.$selectItem.not(this).each(function () {
                    that.data[$(this).data('index')][that.header.stateField] = false;
                });
                that.$selectItem.filter(':checked').not(this).prop('checked', false);
            }
            var child = getAllChild(row, that.options.data, that.options.treeId);
            $.each(child, function (i, c) {
                $.each(that.data, function (index, item) {
                    if (item[that.options.treeId] == c[that.options.treeId]) {
                        item.checked = checked ? true : false;
                        that.trigger(checked ? 'check' : 'uncheck', item, this);
                        return;
                    }
                });
            });
            that.options.data = that.data;
            that.initBody(true);
            that.updateSelected();
            that.trigger(checked ? 'check' : 'uncheck', row, $this);
        });

        $.each(this.header.events, function (i, events) {
            if (!events) {
                return;
            }
            // fix bug, if events is defined with namespace
            if (typeof events === 'string') {
                events = calculateObjectValue(null, events);
            }

            var field = that.header.fields[i],
                fieldIndex = $.inArray(field, that.getVisibleFields());

            if (that.options.detailView && !that.options.cardView) {
                fieldIndex += 1;
            }

            for (var key in events) {
                that.$body.find('>tr:not(.no-records-found)').each(function () {
                    var $tr = $(this),
                        $td = $tr.find(that.options.cardView ? '.card-view' : 'td').eq(fieldIndex),
                        index = key.indexOf(' '),
                        name = key.substring(0, index),
                        el = key.substring(index + 1),
                        func = events[key];

                    $td.find(el).off(name).on(name, function (e) {
                        var index = $tr.data('index'),
                            row = that.data[index],
                            value = row[field];

                        func.apply(this, [e, value, row, index]);
                    });
                });
            }
        });

        this.updateSelected();
        this.resetView();

        this.trigger('post-body', data);
    };

    
    /**
     * 展开所有树节点
     */
    BootstrapTable.prototype.expandAllTree = function ()
    {
        var that=this;
        var roots = $.grep(this.data, function (row, index) {
            return row.parentId == null;
        });
        $.each(roots, function (index, item) {
            var child = getAllChild(item, that.options.data, that.options.treeId);
            $.each(child, function (i, n) {
                n.hidden=false;
            });
        });
        that.initBody(true);

    }
    /**
     * 闭合所有树节点
     */
    BootstrapTable.prototype.collapseAllTree = function ()
    {
        var that=this;
        var roots = $.grep(this.data, function (row, index) {
            return row.parentId == null;
        });
        $.each(roots, function (index, item) {
            var child = getAllChild(item, that.options.data, that.options.treeId);
            $.each(child, function (i, n) {
                n.hidden=true;
            });
        });
        that.initBody(true);

    }

    //给组件增加默认参数列表
    $.extend($.fn.bootstrapTable.defaults, {
        treeView: false,//treeView视图
        treeField: "id",//treeView视图字段
        treeId: "id",
        treeRootLevel: 1,//根节点序号
        treeCollapseAll: true,//是否全部展开，默认不展开
        collapseIcon: "glyphicon glyphicon-chevron-right",//折叠样式
        expandIcon: "glyphicon glyphicon-chevron-down",//展开样式
        leafIcon:"glyphicon glyphicon-leaf"//叶子节点样式
    });
    $.fn.bootstrapTable.methods.push('expandAllTree', 'collapseAllTree');
})(jQuery);