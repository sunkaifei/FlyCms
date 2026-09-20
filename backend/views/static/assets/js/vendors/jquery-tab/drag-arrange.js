/**
 * drag-shift
 * Copyright (c) 2014 Vishal Kumar
 * Licensed under the MIT License.
 */
'use strict';
(function (factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define(['jquery'], factory);
  } else {
    // Browser globals
    factory(jQuery);
  }
}(function ($) {
  var IS_TOUCH_DEVICE = ('ontouchstart' in document.documentElement);
  /**
   * mouse move threshold (in px) until drag action starts
   * @type {Number}
   */
  var DRAG_THRESHOLD = 5;
  /**
   * to generate event namespace
   * @type {Number}
   */
  var counter = 0;

  /**
   * Javascript events for touch device/desktop
   * @return {Object}
   */
  var dragEvents = (function () {
    if (IS_TOUCH_DEVICE) {
      return {
        START: 'touchstart',
        MOVE: 'touchmove',
        END: 'touchend'
      };
    }
    else {
      return {
        START: 'mousedown',
        MOVE: 'mousemove',
        END: 'mouseup'
      };
    }
  }());

  $.fn.arrangeable = function(options) {
    var dragging = false;
    var $clone;
    var dragElement;
    var originalClientX, originalClientY; // client(X|Y) position before drag starts
    var $elements;                        // list of elements to shift between
    var touchDown = false;
    var leftOffset, topOffset;
    var eventNamespace;

    if (typeof options === "string") {
      // check if want to destroy drag-arrange
      if (options === 'destroy') {
        if (this.eq(0).data('drag-arrange-destroy')) {
          this.eq(0).data('drag-arrange-destroy')();
        }

        return this;
      }
    }

    options = $.extend({
      "dragEndEvent": "drag.end.arrangeable"
    }, options);

    var dragEndEvent = options["dragEndEvent"];

    $elements = this;
    eventNamespace = getEventNamespace();

    this.each(function() {
      // bindings to trigger drag on element
      var dragSelector = options.dragSelector;
      var self = this;
      var $this = $(this);

      if (dragSelector) {
        $this.on(dragEvents.START + eventNamespace, dragSelector, dragStartHandler);
      } else {
        $this.on(dragEvents.START + eventNamespace, dragStartHandler);
      }

      function dragStartHandler(e) {
        // a mouse down/touchstart event, but still drag doesn't start till threshold reaches
        // stopPropagation is compulsory, otherwise touchmove fires only once (android < 4 issue)
        e.stopPropagation();
        touchDown = true;
        originalClientX = e.clientX || e.originalEvent.touches[0].clientX;
        originalClientY = e.clientY || e.originalEvent.touches[0].clientY;
        dragElement = self;
      }
    });

    // bind mouse-move/touchmove on document
    // (as it is not compulsory that event will trigger on dragging element)
    $(document).on(dragEvents.MOVE + eventNamespace, dragMoveHandler)
      .on(dragEvents.END + eventNamespace, dragEndHandler);

    function dragMoveHandler(e) {
      if (!touchDown) { return; }

      var $dragElement = $(dragElement);
      var dragDistanceX = (e.clientX  || e.originalEvent.touches[0].clientX) - originalClientX;
      var dragDistanceY = (e.clientY || e.originalEvent.touches[0].clientY) - originalClientY;

      if (dragging) {
        e.stopPropagation();

        $clone.css({
          left: leftOffset + dragDistanceX,
          top: topOffset + dragDistanceY
        });

        shiftHoveredElement($clone, $dragElement, $elements);

      // check for drag threshold (drag has not started yet)
      } else if (Math.abs(dragDistanceX) > DRAG_THRESHOLD ||
          Math.abs(dragDistanceY) > DRAG_THRESHOLD) {
        $clone = clone($dragElement);

        // initialize left offset and top offset
        // will be used in successive calls of this function
        leftOffset = dragElement.offsetLeft - parseInt($dragElement.css('margin-left')) - 
          parseInt($dragElement.css('padding-left'));
        topOffset = dragElement.offsetTop - parseInt($dragElement.css('margin-top')) - 
          parseInt($dragElement.css('padding-top'));

        // put cloned element just above the dragged element
        // and move it instead of original element
        $clone.css({
          left: leftOffset,
          top: topOffset
        });
        $dragElement.parent().append($clone);

        // hide original dragged element
        $dragElement.css('visibility', 'hidden');

        dragging = true;
      }
    }

    function dragEndHandler(e) {
      if (dragging) {
        // remove the cloned dragged element and
        // show original element back
        e.stopPropagation();
        dragging = false;
        $clone.remove();
        dragElement.style.visibility = 'visible';

        $(dragElement).parent().trigger(dragEndEvent, [$(dragElement)]);
      }

      touchDown = false;
    }

    function destroy() {
      $elements.each(function() {
        // bindings to trigger drag on element
        var dragSelector = options.dragSelector;
        var $this = $(this);

        if (dragSelector) {
          $this.off(dragEvents.START + eventNamespace, dragSelector);
        } else {
          $this.off(dragEvents.START + eventNamespace);
        }
      });

      $(document).off(dragEvents.MOVE + eventNamespace)
        .off(dragEvents.END + eventNamespace);

      // remove data
      $elements.eq(0).data('drag-arrange-destroy', null);

      // clear variables
      $elements = null;
      dragMoveHandler = null;
      dragEndHandler = null;
    }

    this.eq(0).data('drag-arrange-destroy', destroy);
  };

  function clone($element) {
    var $clone = $element.clone();

    $clone.css({
      position: 'absolute',
      width: $element.width(),
      height: $element.height(),
      'z-index': 100000 // very high value to prevent it to hide below other element(s)
    });

    return $clone;
  }

  /**
   * find the element on which the dragged element is hovering
   * @return {DOM Object} hovered element
   */
  function getHoveredElement($clone, $dragElement, $movableElements) {
    var cloneOffset = $clone.offset();
    var cloneWidth = $clone.width();
    var cloneHeight = $clone.height();
    var cloneLeftPosition = cloneOffset.left;
    var cloneRightPosition = cloneOffset.left + cloneWidth;
    var cloneTopPosition = cloneOffset.top;
    var cloneBottomPosition = cloneOffset.top + cloneHeight;
    var $currentElement;
    var horizontalMidPosition, verticalMidPosition;
    var offset, overlappingX, overlappingY, inRange;

    for (var i = 0; i < $movableElements.length; i++) {
      $currentElement = $movableElements.eq(i);

      if ($currentElement[0] === $dragElement[0]) { continue; }

      offset = $currentElement.offset();

      // current element width and draggable element(clone) width or height can be different
      horizontalMidPosition = offset.left + 0.5 * $currentElement.width();
      verticalMidPosition = offset.top + 0.5 * $currentElement.height();

      // check if this element position is overlapping with dragged element
      overlappingX = (horizontalMidPosition < cloneRightPosition) &&
        (horizontalMidPosition > cloneLeftPosition);

      overlappingY = (verticalMidPosition < cloneBottomPosition) &&
        (verticalMidPosition > cloneTopPosition);

      inRange = overlappingX && overlappingY;

      if (inRange) {
        return $currentElement[0];
      }
    }
  }

  function shiftHoveredElement($clone, $dragElement, $movableElements) {
    var hoveredElement = getHoveredElement($clone, $dragElement, $movableElements);

    if (hoveredElement !== $dragElement[0]) {
      // shift all other elements to make space for the dragged element
      var hoveredElementIndex = $movableElements.index(hoveredElement);
      var dragElementIndex = $movableElements.index($dragElement);
      if (hoveredElementIndex < dragElementIndex) {
        $(hoveredElement).before($dragElement);
      } else {
        $(hoveredElement).after($dragElement);
      }

      // since elements order have changed, need to change order in jQuery Object too
      shiftElementPosition($movableElements, dragElementIndex, hoveredElementIndex);
    }
  }

  function shiftElementPosition(arr, fromIndex, toIndex) {
    var temp = arr.splice(fromIndex, 1)[0];
    return arr.splice(toIndex, 0, temp);
  }

  function getEventNamespace() {
    counter += 1;

    return '.drag-arrange-' + counter;
  }

}));
