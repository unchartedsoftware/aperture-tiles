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
 * This module provides a layout for a labeled layer control);
 */
define(function (require) {
    "use strict";



    var Class = require('../class'),
        LayerControl;



    LayerControl = Class.extend({
        /**
         * Construct a layer control.
         *
         * @param id Unique identifier for this control set
         */
        init: function (id) {
            // create a table to represent the single row that is the layer control
            this.$table = $('<table id="' + id + '.layer-control"></table>');
            this.$row = $('<tr></tr>');
            this.$table.append( this.$row );
            this.controlsById = {};
        },

        /**
         * Add a control to the set.
         *
         * @param id Unique identifier for this control
         * @param control The control
         */
        addControl: function (id, control) {
            var $controlCell = $('<td></td>');
            this.$row.append($controlCell);
            $controlCell.append(control);
            //this.$table.append($controlCell);
            this.controlsById[id] = $controlCell;
        },

        /**
         * Remove a label, control pair from the set.
         *
         * @param id Unique identifier for the control entry to remove
         */
        removeControl: function (id) {
            this.$table.remove(this.controlsById[id]);
        },

        /**
         * Get this layerControl's DOM element.
         */
        getElement: function () {
            return this.$table;
        }

    });

    return LayerControl;
});
