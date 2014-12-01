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

define( function() {
    "use strict";

    var DURATION = 300,
        getMaxContentHeight;

    getMaxContentHeight = function() {
        return  Math.floor( $(window).height() / 2 );
    };

    /**
     * Construct an overlay button.
     *
     * @param spec {Object} the specification object.
     *     id : the element id,
     *     header: the header html,
     *     content: the content html,
     *     verticalPosition: the vertical position, "bottom" or "top"
     *     horizontalPosition: the horizontal position, "left" or "right"
     */
    function OverlayButton( spec ) {
        var that = this,
            openOverlay,
            closeOverlay,
            vertical = spec.verticalPosition.toLowerCase() || 'bottom',
            horizontal = spec.horizontalPosition.toLowerCase() || 'right',
            quadrantId,
            $quadrantContainer;

        // get quadrant id
        quadrantId = vertical +'-'+ horizontal +'-overlay-quadrant';

        // get quadrant element
        $quadrantContainer = $( '#'+quadrantId );

        // if doesn't exist, create it
        if ( $quadrantContainer.length === 0 ) {
            $quadrantContainer = $('<div id="'+quadrantId+'"></div>' );
            $('body').append( $quadrantContainer );
        }

        this.id = spec.id;
        this.$container = $('<div id="'+this.id+'" style="float:'+ horizontal+'; clear:'+horizontal+';"></div>' );

        if ( vertical === "bottom" ) {
            $quadrantContainer.prepend( this.$container );
        } else {
            $quadrantContainer.append( this.$container );
        }

        this.$container.addClass('overlay-container');

        this.$header = $('<div id="' + this.id + '-header" class="' +this.id+ '-header overlay-header">'+spec.header+'</div>');
        this.$content = $('<div id="' + this.id + '-content" class="' +this.id+ '-content overlay-content">'+spec.content+'</div>');

        this.$container.append(this.$header);
        this.$container.append(this.$content);

        openOverlay = function () {

            var deltaWidth,
                maxHeight = getMaxContentHeight();

            // measure elements and calc delta width to re-size header
            that.inactiveWidth = that.$header.outerWidth();
            that.activeWidth = that.$content.outerWidth();
            deltaWidth = that.activeWidth - that.inactiveWidth;
            // set opacity to 0, to fade in from
            that.$content.children().css('opacity', 0);
            // set a maximum height
            that.$content.css( 'max-height', maxHeight );
             // disable click until animation is complete
            that.$header.off('click');

            that.$header.animate({
                    // open header
                    width: "+="+deltaWidth
                },
                {
                    complete: function() {
                        // open content
                        var contentHeight = parseInt( that.$content.css('height'), 10 );

                        that.$content.children().animate({
                            opacity: 1
                        }, DURATION );

                        if ( contentHeight >= maxHeight ) {
                            // set timeout to override jquery setting overflow to hidden during
                            // animation. This allows the scrollbar to appear instantly
                            setTimeout( function() {
                                that.$content.css('overflow','');
                            }, 1 );
                        }

                        that.$content.animate({
                            height: 'toggle'
                        },
                        {
                            complete: function() {
                                // re-enable click, but switch to close callback
                                that.$header.click( closeOverlay );
                            },
                            duration: DURATION
                        });
                    },
                    duration: DURATION
                });
        };

        closeOverlay = function() {

            var deltaWidth = that.activeWidth - that.inactiveWidth,
                contentHeight = parseInt( that.$content.css('height'), 10 ),
                maxHeight = getMaxContentHeight();

            // disable click until animation is complete
            that.$header.off('click');
            that.$content.children().css('opacity', 1);
            that.$content.children().animate({
                opacity: 0
            }, DURATION );

            if ( contentHeight >= maxHeight ) {
                // set timeout to override jquery setting overflow to hidden during
                // animation. This allows the scrollbar to appear instantly
                setTimeout( function() {
                    that.$content.css('overflow','');
                }, 1 );
            }

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
                            },
                            duration: DURATION
                        });
                    },
                    duration: DURATION
                });
        };

        this.$header.click( openOverlay );

        // begin with content closed, skip animation;
        this.$content.animate({height: 'toggle'});
        this.$content.finish();
    }

    /**
     * Returns the header DOM element.
     * @returns {jQuery|HTMLElement}
     */
    OverlayButton.prototype.getHeaderElement = function() {
        return this.$header;
    };

    /**
     * Returns the content DOM element.
     * @returns {jQuery|HTMLElement}
     */
    OverlayButton.prototype.getContentElement = function() {
        return this.$content;
    };

    /**
     * Returns the container DOM element.
     * @returns {jQuery|HTMLElement}
     */
    OverlayButton.prototypegetContainerElement = function() {
        return this.$container;
    }

    return OverlayButton;
});
