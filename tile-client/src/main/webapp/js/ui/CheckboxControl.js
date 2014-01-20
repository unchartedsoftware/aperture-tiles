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
 * This module defines a generic standardized checkbox class.
 *
 */
define(function (require) {
    "use strict";



    var Class = require('../class'),
        CheckboxControl;



    CheckboxControl = Class.extend({
        ClassName: "CheckboxControl",
        /**
         * Construct a checkbox control.
         *
         * @param id Unique identifier for this slider
         * @param isChecked Whether or not the checkbox starts checked off
         * @param onClickCallback Callback function when clicked
         */
        init: function (id, isChecked ) {
            this.id = id;
            this.checkboxId = id+'.checkbox';
            this.checkbox = $('<input type="checkbox" id="' + this.checkboxId + '"checked>');
            this.checkbox.addClass("checkbox");
            this.checkedCallback   = null;
            this.uncheckedCallback = null;

            if ( isChecked === true ) {
                this.checkbox .prop('checked', true);
            }

            var that = this;
            this.checkbox.click( function () {
                if ( that.checkbox.prop('checked') ) {
                    that.checkedCallback(that);
                } else {
                    that.uncheckedCallback(that);
                }
            });
        },

        /**
         * Set the callback function to be called when box is being checked
         *
         * @param callback The function to be called.
         */
        setOnChecked: function (callback) {
            this.checkedCallback = callback;
        },

        /**
         * Set the callback function to be called when box is being unchecked
         *
         * @param callback The function to be called.
         */
        setOnUnchecked: function (callback) {
            this.uncheckedCallback = callback;
        },

        /**
         * Get the this checkbox's DOM element.
         */
        getElement: function () {
            return this.checkbox;
        }
    });

    return CheckboxControl;
});
