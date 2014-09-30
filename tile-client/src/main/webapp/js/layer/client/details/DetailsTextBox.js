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



    var GenericDetailsElement = require('./GenericDetailsElement'),
        DetailsTextBox;



    DetailsTextBox = GenericDetailsElement.extend({
        ClassName: "DetailsTextBox",

        init: function( spec ) {
            this._super( spec );
            this.$textBox = null;
        },

        parseInputSpec: function( spec ) {
            spec.textKey = spec.textKey || "text";
            return spec;
        },

        createStyles: function() {
            return true;
        },

        create: function( value ) {

            var html = '',
                lightOrDark,
                entry,
                i;

            html += '<div class="details-text-box">';

            for ( i=0; i<value[this.spec.textKey].length; i++ ) {
                entry = value[i];
                lightOrDark = 'light';
                html += '<div class="text-entry-'+lightOrDark+'">'+entry+'</div>';
                lightOrDark = (lightOrDark === 'light') ? 'dark' : 'light';
            }
            html += '</div>';

            this.$textBox = $( html );

            return this.$textBox;
        },


        destroy : function() {
            if ( this.$textbox ) {
                this.$textBox.remove();
                this.$textbox = null;
            }
        }

    });

    return DetailsTextBox;

});