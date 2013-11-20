/**
 * Copyright (c) 2013 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module provides a table-based layout for labeled UI controls (e.g., sliders);
 */
define(['class'], function (Class) {
    "use strict";

    var LabeledControlSet;

    LabeledControlSet = Class.extend({
        /**
         * Construct a labeled control set.
         *
         * @param container The element into which this control set will be inserted, 
	 *                  specified in any way that will satisfy JQuery(...).
         * @param id Unique identifier for this control set
         */
        init: function (container, id) {
            this.rowsById = {};
            this.$table = $('<table id="labeled-control-set.' + id + '"></table>');
            this.$table.addClass('control-table');
            $(container).append(this.$table);
        },

        /**
         * Add a label, control pair to the set.
         *
         * @param id Unique identifier for this control
         * @param labelText Text to label the control
         * @param control The control
         */
        addControl: function (id, labelText, control) {
            var $row = $('<tr></tr>'),
                $labelCell = $('<td></td>'),
                $controlCell = $('<td></td>'),
                $label;

            this.$table.append($row);
            $row.append($labelCell);
            $row.append($controlCell);

            $label = $('<span>' + labelText + '</span>');
            $labelCell.append($label);
            $controlCell.append(control);

            this.rowsById[id] = $row;
        },

        /**
         * Remove a label, control pair from the set.
         *
         * @param id Unique identifier for the control entry to remove
         */
        removeControl: function (id) {
            this.$table.remove(this.rowsById[id]);
        }
    });

    return LabeledControlSet;
});
