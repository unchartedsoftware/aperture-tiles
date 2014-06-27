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
         * @param spec the specification object
         *
         *      {
         *          id : the element id
         *          header: the header html
         *          content: the content html
         *      }
         *
         */
        init: function ( spec ) {

            var that = this,
                openOverlay,
                closeOverlay;

            this.id = spec.id;

            this.$container = $('#'+this.id);
            this.$container.addClass('overlay-container');
            this.$header = $('<div id="' + this.id + '-header" class="overlay-header" title>'+spec.header+'</div>');
            this.$content = $('<div id="' + this.id + '-content" class="overlay-content">'+spec.content+'</div>');

            this.$container.append(this.$header);
            this.$container.append(this.$content);

            openOverlay = function () {

                var deltaWidth;

                // measure elements
                that.inactiveWidth = that.$header.outerWidth();
                that.activeWidth = that.$content.outerWidth();

                deltaWidth = that.activeWidth - that.inactiveWidth;

                 // disable click until animation is complete
                that.$header.off('click');
                that.$header.animate({
                        // open header
                        width: "+="+deltaWidth
                    },
                    {
                        complete: function() {
                            // open content
                            that.$content.animate({
                                height: 'toggle'
                            },
                            {
                                complete: function() {
                                    // re-enable click, but switch to close callback
                                    that.$header.click( closeOverlay );
                                }
                            });
                        }
                    });
            };

            closeOverlay = function() {

                var deltaWidth = that.activeWidth - that.inactiveWidth;
                // disable click until animation is complete
                that.$header.off('click');
                that.$content.animate({
                        height: 'toggle'
                    },
                    {
                        complete: function() {
                            that.$header.animate({
                                 width: "-="+deltaWidth
                            },
                            {
                                complete: function() {
                                    // re-enable click, but switch to open callback
                                    that.$header.click( openOverlay );
                                }
                            });
                        }
                    });
            };

            this.$header.click( openOverlay );

            // begin with content closed, skip animation;
        	this.$content.animate({height: 'toggle'});
            this.$content.finish();
        },


        getHeaderElement: function() {
            return this.$header;
        },


        getContentElement: function() {
            return this.$content;
        },

        getContainerElement: function() {
            return this.$container;
        }


    });

    return OverlayButton;
});
