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

            this.$container = $('#'+this.id);
            this.$container.addClass('overlay-container');
            this.$header = $('<div id="' + this.id + '-header" class="overlay-header">'+spec.header+'</div>');
            this.$content = $('<div id="' + this.id + '-content" class="overlay-content">'+spec.content+'</div>');

            this.$container.append(this.$header);
            this.$container.append(this.$content);
            this.activeWidth = this.$container.width();
            this.inactiveWidth = this.$header.width();
            this.active = spec.active || false;

            this.$header.click( function(e){

                var deltaWidth = that.activeWidth - that.inactiveWidth;

                if (that.active) {
                    // close
                    that.$content.animate({
                            height: 'toggle'
                        }, {
                        complete: function() {
                            that.$header.animate({
                                 width: "-="+deltaWidth
                            });
                        }
                    });

                } else {
                    // open
                    that.$header.animate({
                            width: "+="+deltaWidth
                        },{
                        complete: function() {
                            that.$content.animate({
                                height: 'toggle'
                            });
                        }
                    });
                }

                that.active = !that.active;
            });

            if (!this.active) {
                // trigger close and skip animation;
                //this.active  = !this.active;
                that.$content.animate({height: 'toggle'});
                this.$content.finish();
            }

            return this.$content;
        },


        getHeaderElement: function() {
            return this.$header;
        },


        getContentElement: function() {
            return this.$container;
        },

        getContainerElement: function() {
            return this.$content;
        }


    });

    return OverlayButton;
});
