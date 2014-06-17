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

    return {

        /*
            Return the count of node entries, clamped at MAX_COUNT
        */
        getTagCount : function( values, max ) {
            var MAX_COUNT = max || 5;
            return Math.min( values.length, MAX_COUNT );
        },

        /*
            Return the count of recent tweets entries, clamped at MAX_COUNT
        */
        getTweetCount : function( data ) {
            return data.recent.length;
        },

        /*
            Return the relative percentages of positive, neutral, and negative tweets
        */
        getSentimentPercentages : function( value ) {
            return {
                positive : this.getSentimentPercentage( value, 'positive'),
                neutral : this.getSentimentPercentage( value, 'neutral'),
                negative : this.getSentimentPercentage( value, 'negative')
            };
        },

        /*
            Return the relative percentages of positive, neutral, or negative tweets
        */
        getSentimentPercentage : function( value, sentiment ) {
            return ( value[sentiment] / value.count ) || 0;
        },

        /*
            Return the relative percentages of positive, neutral, and negative tweets
        */
        getSentimentPercentagesByTime : function( value ) {

            var NUM_HOURS_IN_DAY = 24,
                percentages = {
                    positive: [],
                    neutral: [],
                    negative: []
                },
                count,
                i;

            for (i=0; i<NUM_HOURS_IN_DAY; i++) {
                count = value.countByTime[i];
                percentages.positive.push( ( value.positiveByTime[i] / count ) || 0 );
                percentages.neutral.push( ( value.neutralByTime[i] / count ) || 0 );
                percentages.negative.push( ( value.negativeByTime[i] / count ) || 0 );
            }
            return percentages;
        },

        /*
            Return the relative percentages of positive, neutral, and negative tweets
        */
        getSentimentPercentageByTime : function( value, index ) {

            var percentages = {},
                count = value.countByTime[index];

            percentages.positive = ( value.positiveByTime[index] / count ) || 0;
            percentages.neutral = ( value.neutralByTime[index] / count ) || 0;
            percentages.negative = ( value.negativeByTime[index] / count ) || 0;

            return percentages;
        },

        /*
            Returns the total count of all tweets in a node
        */
        getTotalCount : function( values ) {
            var i,
                sum = 0,
                n = this.getTagCount( values );
            for (i=0; i<n; i++) {
                sum += values[i].count;
            }
            return sum;
        },

        /*
            Returns the percentage of tweets in a node for the respective tag
        */
        getTotalCountPercentage : function( values, index ) {
            return ( values[index].count / this.getTotalCount( values ) ) || 0;
        },


        getCountByTimePercentage: function( value, hour ) {
            var countByTime = value.countByTime[hour];
            return ( countByTime / value.count ) || 0;
        },


        getMaxCountByTimePercentage: function( value ) {
            var NUM_HOURS_IN_DAY = 24,
                i,
                percent,
                maxPercent = 0,
                count = value.count;

            for (i=0; i<NUM_HOURS_IN_DAY; i++) {
                percent = ( value.countByTime[i] / count ) || 0;
                maxPercent = Math.max( percent, maxPercent );
            }
            return maxPercent;
        },



        /*
            Returns a font size based on the percentage of tweets relative to the total count
        */
        getFontSize : function( values, index, minFontSize, maxFontSize ) {
            var fontRange = maxFontSize - minFontSize,
                sum = this.getTotalCount( values, index ),
                percentage = this.getTotalCountPercentage( values, index ),
                scale = Math.log( sum ),
                size = ( percentage * fontRange * scale ) + ( minFontSize * percentage );
            return Math.min( Math.max( size, minFontSize), maxFontSize );
        },


        /*
            Returns a trimmed string based on character limit
        */
        trimLabelText : function( str, charCount ) {
            var MAX_LABEL_CHAR_COUNT = charCount || 9;
            if (str.length > MAX_LABEL_CHAR_COUNT) {
                str = str.substr( 0, MAX_LABEL_CHAR_COUNT ) + "...";
            }
            return str;
        },
        
        /*
            Returns a string of the format "Month ##, year:" from a unix timestamp
        */
        getDay : function( timestamp ) {
        
            function getMonth( date ) {
                var month = date.getMonth();
                switch(month) {
                    case 0: return "January";
                    case 1: return "February";
                    case 2: return "March";
                    case 3: return "April";
                    case 4: return "May";
                    case 5: return "June";
                    case 6: return "July";
                    case 7: return "August";
                    case 8: return "September";
                    case 9: return "October";
                    case 10: return "November";
                    default: return "December";
                }
            }
            var date = new Date( timestamp ),
                month = getMonth( date ),
                year =  date.getFullYear(),
                day = date.getDate();

            return month + " " + day + ", " + year + ":";
        },

        /*
            Returns a string of the format "HH:MM:SS xm" from a unix timestamp
        */
        getTime : function( timestamp ) {

            function padZero( num ) {
                return ("0" + num).slice(-2);
            }
            var date = new Date( timestamp ),
                hours = date.getHours(),
                minutes = padZero( date.getMinutes() ),
                seconds = padZero( date.getSeconds() ),
                suffix = (hours >= 12) ? 'pm' : 'am';
            // ensure hour is correct
            hours = ( hours === 0 || hours === 12 ) ? 12 : hours % 12;
            return hours + ':' + minutes + ':' + seconds + " " + suffix;
        },

        /*
            Buckets recent tweets by day
        */
        getRecentTweetsByDay : function( tagData ) {
            // bucket tweets by day
            var days = {},
                count = this.getTweetCount( tagData ),
                time, day, recent, i;
            for (i=0; i<count; i++) {
                recent = tagData.recent[i];
                time = recent.time;
                day = this.getDay( time );
                days[day] = days[day] || [];
                days[day].push({
                    tweet: recent.tweet,
                    time: this.getTime( time )
                });
            }
            return days;
        },

        /*
            Centre map between tile and details on demand pane
        */
        centreForDetails: function( map, data ) {
            var viewportPixel = map.getViewportPixelFromCoord( data.longitude, data.latitude ),
                panCoord = map.getCoordFromViewportPixel( viewportPixel.x + map.getTileSize(),
                                                          viewportPixel.y + map.getTileSize() );
            map.panToCoord( panCoord.x, panCoord.y );
        },


        blendSentimentColours: function( positivePercent, neutralPercent, negativePercent ) {
            var BLUE_COLOUR = '#09CFFF',
                PURPLE_COLOUR = '#D33CFF',
                negWeight, negRGB,
                neuWeight, neuRGB,
                posWeight, posRGB,
                finalRGB = {};

            function hexToRgb(hex) {
                 var bigint;
                 if (hex[0] === '#') {
                     hex = hex.substr(1,6);
                 }
                 bigint = parseInt(hex, 16);
                 return {
                     r: (bigint >> 16) & 255,
                     g: (bigint >> 8) & 255,
                     b: bigint & 255
                 };
            }

            function rgbToHex(r, g, b) {
                function componentToHex(c) {
                    var hex = c.toString(16);
                    return (hex.length === 1) ? "0" + hex : hex;
                }
                return "#" + componentToHex( Math.floor(r)) +
                             componentToHex( Math.floor(g)) +
                             componentToHex( Math.floor(b));
            }

            if ( positivePercent === 0 || negativePercent ===0 ) {
                return '#ffffff';
            }

            negRGB = hexToRgb(PURPLE_COLOUR);
            neuRGB = { r: 255, g: 255, b: 255 };
            posRGB = hexToRgb(BLUE_COLOUR);

            posWeight = positivePercent;
            neuWeight = neutralPercent;
            negWeight = negativePercent;

            finalRGB.r = (negRGB.r * negWeight) + (posRGB.r * posWeight) + (neuRGB.r * neuWeight);
            finalRGB.g = (negRGB.g * negWeight) + (posRGB.g * posWeight) + (neuRGB.g * neuWeight);
            finalRGB.b = (negRGB.b * negWeight) + (posRGB.b * posWeight) + (neuRGB.b * neuWeight);
            return rgbToHex( finalRGB.r, finalRGB.g, finalRGB.b );
        }

    };

});