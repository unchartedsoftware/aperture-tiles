/*
 * Copyright (c) 2014 Oculus Info Inc.
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



    var Class = require('../class'),
        OverlayButton;



    OverlayButton = Class.extend({
        /**
         * Construct an overlay button
         * @param spec the ispecification object
         *
         *      {
         *          id : the element id
         *          active : whether or not it starts activated, default is false
         *          activeWidth : width of overlay when active, default from div .css
         *          inactiveWidth: width of overlay when inactive, default to '50%'
         *          text: text on overlay button
         *          css: css object to be passed to button
         *      }
         *
         */
        init: function ( spec ) {

            var that = this;

            this.id = spec.id;
            this.active = spec.active || false;


            // create header
            this.$header = $('<div id="' + spec.id + '" class="overlay-header"><h3>'+spec.text+'</h3></div>');
            this.$header.css(spec.css || "");
            $('body').append(this.$header);

            // create container
            this.$container = $('<div id="'+ spec.id + '-container" class="overlay-container"> </div>');
            this.$header.append(this.$container);

            this.$header.accordion({
                active: this.active,
                heightStyle: 'content',
                collapsible: true
            });

            this.activeWidth = spec.activeWidth || '50%';
            this.inactiveWidth = spec.inactiveWidth || this.$header.css('width');

            this.$header.click( function(e){

                var newWidth = that.active ? that.inactiveWidth : that.activeWidth,
                    maxWidth = that.$header.css('max-width');

                if (newWidth > maxWidth) {
                    newWidth = maxWidth;
                }

                // ensure click event is only processed on the actual accordion header
                if( $(".ui-accordion-header").is(e.target) ||
                    $(".ui-accordion-header-icon").is(e.target) ) {

                    // set overflow hidden while animating
                    that.$container.css('overflow-y', 'hidden');

                    that.$header.animate({
                        width: newWidth
                    }, {
                        complete: function() {
                            // on animation finish, allow scrollbar
                            that.$container.css('overflow-y', 'auto');
                        }
                    });
                    that.active = !that.active;
                }
            });

        },


        getHeader: function() {
            return this.$header;
        },


        getContainer: function() {
            return this.$container;
        },


        append: function( element ) {
            this.$container.append( element );
        }

    });

    return OverlayButton;
});
