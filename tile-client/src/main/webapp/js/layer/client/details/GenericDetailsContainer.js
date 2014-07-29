/*
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


define(function (require) {
    "use strict";



    var Class = require('../../../class'),
        GenericDetailsContainer;



    GenericDetailsContainer = Class.extend({
        ClassName: "GenericDetailsContainer",

        init: function() {
            this.$container = null;
        },


        create: function( closeCallback ) {

            var html = '';

            html += '<div class="details-on-demand">';

            // top half
            html += '<div class="details-on-demand-half">';
            html +=     '<div class="details-on-demand-title></div>';
            html +=     '<div class="details-on-demand-content"></div>';
            html += '</div>';

            // bottom half
            html += '<div class="details-on-demand-half">';
            html +=     '<div class="details-on-demand-title></div>';
            html +=     '<div class="details-on-demand-content"></div>';
            html += '</div>';

            // close button
            html += '<div class="details-close-button"></div>';
            html += '</div>';

            this.$container = $(html).draggable().resizable({
                minHeight: 513,
                minWidth: 257
            });
            this.$container.find('.details-close-button').click( closeCallback );

            return this.$container;
        },


        destroy : function() {
            this.$container.remove();
        }

    });

    return GenericDetailsContainer;

});