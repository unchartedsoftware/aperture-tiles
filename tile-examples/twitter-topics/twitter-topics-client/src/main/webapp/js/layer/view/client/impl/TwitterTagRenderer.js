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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines a intermediary class for the twitter demo, contains
 * common functionality shared across each of the render layers
 */
define(function (require) {
    "use strict";



    var ApertureRenderer = require('../ApertureRenderer'),
        TwitterTagRenderer;



    TwitterTagRenderer = ApertureRenderer.extend({
        ClassName: "TwitterTagRenderer",

        /**
         * Constructs a twitter tag render layer object
         * @param id the id string for the render layer
         */
        init: function( map ) {
            this._super( map );
            this.Y_SPACING = 10;
            this.MAX_NUM_VALUES = 10;   // default, over-ride this based on the renderer

            this.HORIZONTAL_BUFFER = 14;
            this.VERTICAL_BUFFER = 24;

            this.BLACK_COLOUR = '#000000';
            this.DARK_GREY_COLOUR = '#222222';
            this.GREY_COLOUR = '#666666';
            this.LIGHT_GREY_COLOUR = '#999999';
            this.WHITE_COLOUR = '#FFFFFF';
            this.BLUE_COLOUR = '#09CFFF';
            this.DARK_BLUE_COLOUR  = '#069CCC';
            this.PURPLE_COLOUR = '#D33CFF';
            this.DARK_PURPLE_COLOUR = '#A009CC';
            this.YELLOW_COLOUR = '#F5F56F';

            this.FILTER_WORDS = [/s+h+i+t+/, /f+u+c+k+/, /n+i+g+g+/];
        },


        /**
         * Returns the number of values in the bin, capped by the MAX_NUM_VALUES constant
         * @param data the aperturejs node data object
         */
        getCount: function(data) {
            if (data.bin.value === undefined || !$.isArray(data.bin.value) ) {
                return 0;
            }
            return (data.bin.value.length > this.MAX_NUM_VALUES) ? this.MAX_NUM_VALUES : data.bin.value.length;
        },


        isTileTranslated: function( tilekey ) {
            var translationState = this.clientState['translate-' + tilekey];
            if ( translationState === "" ) {
                return false;
            } else {
                return translationState;
            }
        },


        toggleTileTranslation: function( tilekey ) {
            var translationState = this.clientState['translate-' + tilekey];
            if ( !translationState ) {
                this.clientState['translate-' + tilekey] = true;
            } else {
                delete this.clientState['translate-' + tilekey];
            }
        },


        getMonth: function(data) {

            var month = new Date( data.bin.value[0].endTimeSecs * 1000 ).getMonth();

            switch(month) {
                case 0: return "Jan";
                case 1: return "Feb";
                case 2: return "Mar";
                case 3: return "Apr";
                case 4: return "May";
                case 5: return "Jun";
                case 6: return "Jul";
                case 7: return "Aug";
                case 8: return "Sep";
                case 9: return "Oct";
                case 10: return "Nov";
                default: return "Dec";
            }
        },


        getLastWeekOfMonth: function(data) {

            var lastDay = this.getLastDayOfMonth(data),
                i,
                week = [];

            function numToDay(num) {
                switch (num) {
                    case 0: return "Su";
                    case 1: return "Mo";
                    case 2: return "Tu";
                    case 3: return "We";
                    case 4: return "Th";
                    case 5: return "Fr";
                    case 6: 
                    default: 
                        return "Sa";
                }
            } 

            for (i=0; i<7; i++) {
                week.push( numToDay( (lastDay + i) % 7 ) );
            }

            return week
        },


        getLastDayOfMonth: function(data) {
            return new Date( data.bin.value[0].endTimeSecs * 1000 ).getDay();
        },

        
        getTotalDaysInMonth: function(data) {
            return new Date( data.bin.value[0].endTimeSecs * 1000 ).getDate();
        },


        /**
         * Returns a y offset for a given index
         * @param data the aperturejs node data object
         * @param index the layer element index
         */
        getYOffset: function(data, index) {
            return -this.Y_SPACING * (((this.getCount(data) - 1) / 2) - index);
        },


        /**
         * Returns true if the current tag in the respective tile is hovered over
         * @param tag the twitter data tag string
         * @param tilekey the tilekey of the respective tile
         */
        isHovered: function (tag, tilekey) {
            var hoverTilekey = this.clientState.hoverState.tilekey,
                hoverTag = this.clientState.hoverState.tag;

            return hoverTag === tag && hoverTilekey === tilekey;

        },


        /**
         * Returns true if the current tag in the respective tile is clicked
         * @param tag the twitter data tag string
         * @param tilekey the tilekey of the respective tile
         */
        isClicked: function (tag, tilekey) {
            var clickTilekey = this.clientState.clickState.tilekey,
                clickTag = this.clientState.clickState.tag;

            return clickTag === tag && clickTilekey === tilekey;

        },


        /**
         * Returns true if the current tag in the respective tile is hovered on or clicked
         * @param tag the twitter data tag string
         * @param tilekey the tilekey of the respective tile
         */
        isHoveredOrClicked: function (tag, tilekey) {
            return this.isHovered(tag, tilekey) || this.isClicked(tag, tilekey);
        },


        /**
         * Returns true if the criteria for whether the element should be greyed out is satisfied
         * @param tag the twitter data tag string
         * @param tilekey the tilekey of the respective tile
         */
        shouldBeGreyedOut: function (tag, tilekey) {

            var hoverTilekey = this.clientState.hoverState.tilekey,
                hoverTag = this.clientState.hoverState.tag,
                clickTilekey = this.clientState.clickState.tilekey,
                clickTag = this.clientState.clickState.tag;

            if ( // nothing is hovered or clicked on
                 (clickTilekey === '' && hoverTilekey === '') ||
                 // current tag is hovered on
                 (hoverTag === tag && hoverTilekey === tilekey )) {
                return false
            } else if (clickTag !== undefined && clickTag !== tag) {
                return true;
            }
            return false;
        },


        getTopic: function (data, index) {
            return (this.isTileTranslated(data.tilekey)) ? data.bin.value[index].topicEnglish : data.bin.value[index].topic;                                   
        },

        /**
         * Returns true if the current tag is hovered over or clicked anywhere
         * @param tag the twitter data tag string
         */
        matchingTagIsSelected: function (tag, tilekey) {
            return ((this.clientState.hoverState.tilekey === tilekey &&
                     this.clientState.hoverState.tag === tag) ||
                     this.clientState.clickState.tag === tag)
        },


        createTranslateLabel: function () {

            var that = this,
                isHoveredOn = false;

            this.translateLabel = this.nodeLayer.addLayer(aperture.LabelLayer);

            this.translateLabel.map('visible').from(function() {
                return that.visibility &&
                       that.clientState.activeCarouselTile === this.tilekey;
            });

            this.translateLabel.map('fill').from( function() {

                if (that.isTileTranslated(this.tilekey)) {
                    return that.WHITE_COLOUR;
                }
                return that.LIGHT_GREY_COLOUR;
            });

            this.translateLabel.map('cursor').asValue('pointer');

            this.translateLabel.on('click', function(event) {
                that.toggleTileTranslation(event.data.tilekey);
                that.nodeLayer.all().where(event.data).redraw();
                return true; // swallow event
            });

            this.translateLabel.on('mousemove', function(event) {
                isHoveredOn = true;
                that.nodeLayer.all().where(event.data).redraw();
                return true; // swallow event
            });

            this.translateLabel.on('mouseout', function(event) {
                isHoveredOn = false;
                that.nodeLayer.all().where(event.data).redraw();
            });

            this.translateLabel.map('label-count').asValue(1);

            this.translateLabel.map('text').asValue('translate');

            this.translateLabel.map('font-size').from(function () {
                var FONT_SIZE = 16;
                if (isHoveredOn) {
                    return FONT_SIZE + 2;
                }
                return FONT_SIZE;

            });
            this.translateLabel.map('offset-x').asValue(this.X_CENTRE_OFFSET + this.TILE_SIZE / 3);
            this.translateLabel.map('offset-y').asValue(this.Y_CENTRE_OFFSET - 100);          
            this.translateLabel.map('text-anchor').asValue('left');
            this.translateLabel.map('text-anchor-y').asValue('start');
            this.translateLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.translateLabel.map('font-outline-width').asValue(3);
            this.translateLabel.map('opacity').from( function() {
                return that.opacity;
            })
        },

        /**
         * Filters any word in the filtered words list, replacing all inner letters with *
         * @param text the text to be filtered
         */
        filterText: function (text) {
            var splitStr = text.split(' '),
                i, j, k, index,
                replacement,
                filteredStr = '';

            String.prototype.regexIndexOf = function(regex) {
                var indexOf = this.substring(0).search(regex);
                return (indexOf >= 0) ? (indexOf + (0)) : indexOf;
            }

            function decodeHTML(s){
                var str, temp= document.createElement('p');
                temp.innerHTML= s;
                str= temp.textContent || temp.innerText;
                temp=null;
                return str;
            }

            // for each word
            for (i=0; i< splitStr.length; i++) {
                // for each filter word
                for (j=0; j<this.FILTER_WORDS.length; j++) {

                    do {
                        index = splitStr[i].toLowerCase().regexIndexOf(this.FILTER_WORDS[j]);
                        if ( index !== -1) {
                            // if it exists, replace inner letters with '*'
                            replacement = splitStr[i].substr(0, index+1);
                            for (k=index+1; k<splitStr[i].length-1; k++) {
                                replacement += '*';
                            }
                            replacement += splitStr[i].substr(index+splitStr[i].length-1, splitStr[i].length-1);
                            splitStr[i] = replacement;
                        }
                    // make sure every instance is censored
                    } while ( index !== -1);
                }
                filteredStr += splitStr[i];
                if ( i+1 < splitStr.length ) {
                    filteredStr += ' ';
                }
            }

            return decodeHTML(filteredStr);
        }

    });

    return TwitterTagRenderer;
});
