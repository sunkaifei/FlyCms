(function(global) {
	
	/**
	 * Class intro
	 */
	var isMac = /Mac/.test(navigator.platform);

	var shortcuts = {
		'Cmd-B' : toggleBold,
		'Cmd-I' : toggleItalic,
		'Cmd-K' : drawLink,
		'Cmd-Alt-I' : drawImage,
		"Cmd-'" : toggleBlockquote,
		'Cmd-Alt-L' : toggleOrderedList,
		'Cmd-L' : toggleUnOrderedList
	};

	/**
	 * Fix shortcut. Mac use Command, others use Ctrl.
	 */
	function fixShortcut(name) {
		if (isMac) {
			name = name.replace('Ctrl', 'Cmd');
		} else {
			name = name.replace('Cmd', 'Ctrl');
		}
		return name;
	}

	/**
	 * Create icon element for toolbar.
	 */
	function createIcon(name, options) {
		options = options || {};
		var el = document.createElement('a');

		var shortcut = options.shortcut || shortcuts[name];
		if (shortcut) {
			shortcut = fixShortcut(shortcut);
			el.title = shortcut;
			el.title = el.title.replace('Cmd', '⌘');
			if (isMac) {
				el.title = el.title.replace('Alt', '⌥');
			}
		}

		el.className = options.className || 'fa fa-' + name;
		return el;
	}

	function createSep() {
		el = document.createElement('i');
		el.className = 'separator';
		el.innerHTML = '|';
		return el;
	}

	/**
	 * The state of CodeMirror at the given position.
	 */
	function getState(cm, pos) {
		pos = pos || cm.getCursor('start');
		var stat = cm.getTokenAt(pos);
		if (!stat.type)
			return {};

		var types = stat.type.split(' ');

		var ret = {}, data, text;
		for (var i = 0; i < types.length; i++) {
			data = types[i];
			if (data === 'strong') {
				ret.bold = true;
			} else if (data === 'variable-2') {
				text = cm.getLine(pos.line);
				if (/^\s*\d+\.\s/.test(text)) {
					ret['ordered-list'] = true;
				} else {
					ret['unordered-list'] = true;
				}
			} else if (data === 'atom') {
				ret.quote = true;
			} else if (data === 'em') {
				ret.italic = true;
			}
		}
		return ret;
	}

	/**
	 * Toggle full screen of the editor.
	 */
	function toggleFullScreen(editor) {
		var el = editor.codemirror.getWrapperElement();

		// https://developer.mozilla.org/en-US/docs/DOM/Using_fullscreen_mode
		var doc = document;
		var isFull = doc.fullScreen || doc.mozFullScreen
				|| doc.webkitFullScreen;
		var request = function() {
			if (el.requestFullScreen) {
				el.requestFullScreen();
			} else if (el.mozRequestFullScreen) {
				el.mozRequestFullScreen();
			} else if (el.webkitRequestFullScreen) {
				el.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
			}
		};
		var cancel = function() {
			if (doc.cancelFullScreen) {
				doc.cancelFullScreen();
			} else if (doc.mozCancelFullScreen) {
				doc.mozCancelFullScreen();
			} else if (doc.webkitCancelFullScreen) {
				doc.webkitCancelFullScreen();
			}
		};
		if (!isFull) {
			request();
		} else if (cancel) {
			cancel();
		}
	}

	/**
	 * Action for toggling bold.
	 */
	function toggleBold(editor) {
		var cm = editor.codemirror;
		var stat = getState(cm);

		var text;
		var start = '**';
		var end = '**';

		var startPoint = cm.getCursor('start');
		var endPoint = cm.getCursor('end');
		if (stat.bold) {
			text = cm.getLine(startPoint.line);
			start = text.slice(0, startPoint.ch);
			end = text.slice(startPoint.ch);

			start = start.replace(/^(.*)?(\*|\_){2}(\S+.*)?$/, '$1$3');
			end = end.replace(/^(.*\S+)?(\*|\_){2}(\s+.*)?$/, '$1$3');
			startPoint.ch -= 2;
			endPoint.ch -= 2;
			cm.setLine(startPoint.line, start + end);
		} else {
			text = cm.getSelection();
			cm.replaceSelection(start + text + end);

			startPoint.ch += 2;
			endPoint.ch += 2;
		}
		cm.setSelection(startPoint, endPoint);
		cm.focus();
	}

	/**
	 * Action for toggling italic.
	 */
	function toggleItalic(editor) {
		var cm = editor.codemirror;
		var stat = getState(cm);

		var text;
		var start = '*';
		var end = '*';

		var startPoint = cm.getCursor('start');
		var endPoint = cm.getCursor('end');
		if (stat.italic) {
			text = cm.getLine(startPoint.line);
			start = text.slice(0, startPoint.ch);
			end = text.slice(startPoint.ch);

			start = start.replace(/^(.*)?(\*|\_)(\S+.*)?$/, '$1$3');
			end = end.replace(/^(.*\S+)?(\*|\_)(\s+.*)?$/, '$1$3');
			startPoint.ch -= 1;
			endPoint.ch -= 1;
			cm.setLine(startPoint.line, start + end);
		} else {
			text = cm.getSelection();
			cm.replaceSelection(start + text + end);

			startPoint.ch += 1;
			endPoint.ch += 1;
		}
		cm.setSelection(startPoint, endPoint);
		cm.focus();
	}

	/**
	 * Action for toggling blockquote.
	 */
	function toggleBlockquote(editor) {
		var cm = editor.codemirror;
		_toggleLine(cm, 'quote');
	}

	/**
	 * Action for toggling ul.
	 */
	function toggleUnOrderedList(editor) {
		var cm = editor.codemirror;
		_toggleLine(cm, 'unordered-list');
	}

	/**
	 * Action for toggling ol.
	 */
	function toggleOrderedList(editor) {
		var cm = editor.codemirror;
		_toggleLine(cm, 'ordered-list');
	}

	/**
	 * Action for drawing a link.
	 */
	function drawLink(editor) {
		var cm = editor.codemirror;
		var stat = getState(cm);
		_replaceSelection(cm, stat.link, '[', '](http://)');
	}

	/**
	 * Action for drawing an img.
	 */
	function drawImage(editor) {
		var cm = editor.codemirror;
		var stat = getState(cm);
		_replaceSelection(cm, stat.image, '![', '](http://)');
	}

	/**
	 * Undo action.
	 */
	function undo(editor) {
		var cm = editor.codemirror;
		cm.undo();
		cm.focus();
	}

	/**
	 * Redo action.
	 */
	function redo(editor) {
		var cm = editor.codemirror;
		cm.redo();
		cm.focus();
	}

	/**
	 * Preview action.
	 */
	function togglePreview(editor) {
		var toolbar = editor.toolbar.preview;
		var parse = editor.constructor.markdown;
		var cm = editor.codemirror;
		var wrapper = cm.getWrapperElement();
		var preview = wrapper.lastChild;
		if (!/editor-preview/.test(preview.className)) {
			preview = document.createElement('div');
			preview.className = 'editor-preview';
			wrapper.appendChild(preview);
		}
		if (/editor-preview-active/.test(preview.className)) {
			preview.className = preview.className.replace(
					/\s*editor-preview-active\s*/g, '');
			toolbar.className = toolbar.className.replace(/\s*active\s*/g, '');
		} else {
			/*
			 * When the preview button is clicked for the first time, give some
			 * time for the transition from editor.css to fire and the view to
			 * slide from right to left, instead of just appearing.
			 */
			setTimeout(function() {
				preview.className += ' editor-preview-active'
			}, 1);
			toolbar.className += ' active';
		}
		var text = cm.getValue();
		preview.innerHTML = parse(text);
	}

	function _replaceSelection(cm, active, start, end) {
		var text;
		var startPoint = cm.getCursor('start');
		var endPoint = cm.getCursor('end');
		if (active) {
			text = cm.getLine(startPoint.line);
			start = text.slice(0, startPoint.ch);
			end = text.slice(startPoint.ch);
			cm.setLine(startPoint.line, start + end);
		} else {
			text = cm.getSelection();
			cm.replaceSelection(start + text + end);

			startPoint.ch += start.length;
			endPoint.ch += start.length;
		}
		cm.setSelection(startPoint, endPoint);
		cm.focus();
	}

	function _toggleLine(cm, name) {
		var stat = getState(cm);
		var startPoint = cm.getCursor('start');
		var endPoint = cm.getCursor('end');
		var repl = {
			quote : /^(\s*)\>\s+/,
			'unordered-list' : /^(\s*)(\*|\-|\+)\s+/,
			'ordered-list' : /^(\s*)\d+\.\s+/
		};
		var map = {
			quote : '> ',
			'unordered-list' : '* ',
			'ordered-list' : '1. '
		};
		for (var i = startPoint.line; i <= endPoint.line; i++) {
			(function(i) {
				var text = cm.getLine(i);
				if (stat[name]) {
					text = text.replace(repl[name], '$1');
				} else {
					text = map[name] + text;
				}
				cm.setLine(i, text);
			})(i);
		}
		cm.focus();
	}

	/* The right word count in respect for CJK. */
	function wordCount(data) {
		var pattern = /[a-zA-Z0-9_\u0392-\u03c9]+|[\u4E00-\u9FFF\u3400-\u4dbf\uf900-\ufaff\u3040-\u309f\uac00-\ud7af]+/g;
		var m = data.match(pattern);
		var count = 0;
		if (m === null)
			return count;
		for (var i = 0; i < m.length; i++) {
			if (m[i].charCodeAt(0) >= 0x4E00) {
				count += m[i].length;
			} else {
				count += 1;
			}
		}
		return count;
	}

	
	/**
	 * Class editor
	 */
	var toolbar = [
	    { name : 'bold', className : 'fa fa-bold', action : toggleBold },
	    { name : 'italic', className : 'fa fa-italic', action : toggleItalic }, 
	    '|',
	    { name : 'quote', className : 'fa fa-quote-left', action : toggleBlockquote },
	    { name : 'unordered-list', className : 'fa fa-list', action : toggleUnOrderedList },
	    { name : 'ordered-list', className : 'fa fa-list-ol', action : toggleOrderedList },
	    '|',
	    { name : 'link', className : 'fa fa-link', action : drawLink },
	    { name : 'image', className : 'fa fa-image', action : drawImage },
	    '|',
	    { name : 'info', className : 'fa fa-info', action : 'http://lab.lepture.com/editor/markdown' },
	    { name : 'preview', className : 'fa fa-eye', action : togglePreview },
	    { name : 'fullscreen', className : 'icon-fullscreen fa fa-arrows-alt', action : toggleFullScreen } ];

	/**
	 * Interface of Editor.
	 */
	function Editor(options) {
		options = options || {};

		if (options.element) {
			this.element = options.element;
		}

		options.toolbar = options.toolbar || Editor.toolbar;
		// you can customize toolbar with object
		// [{name: 'bold', shortcut: 'Ctrl-B', className: 'icon-bold'}]

		if (!options.hasOwnProperty('status')) {
			options.status = [ 'lines', 'words', 'cursor' ];
		}

		this.options = options;

		// If user has passed an element, it should auto rendered
		if (this.element) {
			this.render();
		}
	}

	/**
	 * Default toolbar elements.
	 */
	Editor.toolbar = toolbar;

	/**
	 * Default markdown render.
	 */
	Editor.markdown = function(text) {
		if (window.marked) {
			// use marked as markdown parser
			return marked(text);
		}
	};

	/**
	 * Render editor to the given element.
	 */
	Editor.prototype.render = function(el) {
		if (!el) {
			el = this.element || document.getElementsByTagName('textarea')[0];
		}

		if (this._rendered && this._rendered === el) {
			// Already rendered.
			return;
		}
		
		// wrapper element
		var markd = document.createElement('div');
		markd.className = 'editor-markd';
		el.parentNode.appendChild(markd);

		markd.appendChild(el);
		
		this.element = el;
		var options = this.options;
		
		var self = this;
		var keyMaps = {};

		for ( var key in shortcuts) {
			(function(key) {
				keyMaps[fixShortcut(key)] = function(cm) {
					shortcuts[key](self);
				};
			})(key);
		}

		keyMaps["Enter"] = "newlineAndIndentContinueMarkdownList";
		keyMaps['Tab'] = 'tabAndIndentContinueMarkdownList';
		keyMaps['Shift-Tab'] = 'shiftTabAndIndentContinueMarkdownList';

		this.codemirror = CodeMirror.fromTextArea(el, {
			mode : 'markdown',
			theme : 'paper',
			tabSize : '2',
			indentWithTabs : true,
			lineNumbers : false,
			autofocus : true,
			extraKeys : keyMaps
		});

		if (options.toolbar !== false) {
			this.createToolbar();
		}
		if (options.status !== false) {
			this.createStatusbar();
		}
		
		this.onChange();

		this._rendered = this.element;
	};
	
	Editor.prototype.onChange = function () {
		var opts = this.options;
		if (opts.hasOwnProperty('onChange')) {
			if (typeof opts.onChange === 'function') {
				var cm = this.codemirror;
				var parse = this.constructor.markdown;
				
				cm.on('update', function() {
					var html = parse(cm.getValue());
					opts.onChange.call(this, cm.getValue(), html);
				});
			}
		}
	};
	
	Editor.prototype.createToolbar = function(items) {
		items = items || this.options.toolbar;

		if (!items || items.length === 0) {
			return;
		}

		var bar = document.createElement('div');
		bar.className = 'editor-toolbar';

		var self = this;

		var el;
		self.toolbar = {};

		for (var i = 0; i < items.length; i++) {
			(function(item) {
				var el;
				if (item.name) {
					el = createIcon(item.name, item);
				} else if (item === '|') {
					el = createSep();
				} else {
					el = createIcon(item);
				}

				// bind events, special for info
				if (item.action) {
					if (typeof item.action === 'function') {
						el.onclick = function(e) {
							item.action(self);
						};
					} else if (typeof item.action === 'string') {
						el.href = item.action;
						el.target = '_blank';
					}
				}
				self.toolbar[item.name || item] = el;
				bar.appendChild(el);
			})(items[i]);
		}

		var cm = this.codemirror;
		cm.on('cursorActivity', function() {
			var stat = getState(cm);

			for ( var key in self.toolbar) {
				(function(key) {
					var el = self.toolbar[key];
					if (stat[key]) {
						el.className += ' active';
					} else {
						el.className = el.className
								.replace(/\s*active\s*/g, '');
					}
				})(key);
			}
		});

		var cmWrapper = cm.getWrapperElement();
		cmWrapper.parentNode.insertBefore(bar, cmWrapper);
		return bar;
	};

	Editor.prototype.createStatusbar = function(status) {
		status = status || this.options.status;

		if (!status || status.length === 0)
			return;

		var bar = document.createElement('div');
		bar.className = 'editor-statusbar';

		var pos, cm = this.codemirror;
		for (var i = 0; i < status.length; i++) {
			(function(name) {
				var el = document.createElement('span');
				el.className = name;
				if (name === 'words') {
					el.innerHTML = '0';
					cm.on('update', function() {
						el.innerHTML = wordCount(cm.getValue());
					});
				} else if (name === 'lines') {
					el.innerHTML = '0';
					cm.on('update', function() {
						el.innerHTML = cm.lineCount();
					});
				} else if (name === 'cursor') {
					el.innerHTML = '0:0';
					cm.on('cursorActivity', function() {
						pos = cm.getCursor();
						el.innerHTML = pos.line + ':' + pos.ch;
					});
				}
				bar.appendChild(el);
			})(status[i]);
		}
		var cmWrapper = this.codemirror.getWrapperElement();
		cmWrapper.parentNode.insertBefore(bar, cmWrapper.nextSibling);
		return bar;
	};

	/**
	 * Get or set the text content.
	 */
	Editor.prototype.value = function(val) {
		if (val) {
			this.codemirror.getDoc().setValue(val);
			return this;
		} else {
			return this.codemirror.getValue();
		}
	};

	/**
	 * Bind static methods for exports.
	 */
	Editor.toggleBold = toggleBold;
	Editor.toggleItalic = toggleItalic;
	Editor.toggleBlockquote = toggleBlockquote;
	Editor.toggleUnOrderedList = toggleUnOrderedList;
	Editor.toggleOrderedList = toggleOrderedList;
	Editor.drawLink = drawLink;
	Editor.drawImage = drawImage;
	Editor.undo = undo;
	Editor.redo = redo;
	Editor.togglePreview = togglePreview;
	Editor.toggleFullScreen = toggleFullScreen;

	/**
	 * Bind instance methods for exports.
	 */
	Editor.prototype.toggleBold = function() {
		toggleBold(this);
	};
	Editor.prototype.toggleItalic = function() {
		toggleItalic(this);
	};
	Editor.prototype.toggleBlockquote = function() {
		toggleBlockquote(this);
	};
	Editor.prototype.toggleUnOrderedList = function() {
		toggleUnOrderedList(this);
	};
	Editor.prototype.toggleOrderedList = function() {
		toggleOrderedList(this);
	};
	Editor.prototype.drawLink = function() {
		drawLink(this);
	};
	Editor.prototype.drawImage = function() {
		drawImage(this);
	};
	Editor.prototype.undo = function() {
		undo(this);
	};
	Editor.prototype.redo = function() {
		redo(this);
	};
	Editor.prototype.togglePreview = function() {
		togglePreview(this);
	};
	Editor.prototype.toggleFullScreen = function() {
		toggleFullScreen(this);
	};

	global.Editor = Editor;
})(this);