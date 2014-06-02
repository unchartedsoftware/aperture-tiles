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
 * This module defines the base class for a client render layer. Must be
 * inherited from for any functionality.
 */
define(function (require) {
    "use strict";

    return {

        /*
            Return the count of node entries, clamped at MAX_COUNT
        */
        getTagCount : function( value, max ) {
            var MAX_COUNT = max || 5;
            return Math.min( value.length, MAX_COUNT );
        },


        getTweetCount : function( data, max ) {
            var MAX_TWEETS = max || 10;
            return Math.min( data.recent.length, MAX_TWEETS );
        },

        /*
            Return the relative percentages of positive, neutral, and negative tweets
        */
        getSentimentPercentages : function( value, index ) {
            return {
                positive : ( value[index].positive / value[index].count )*100 || 0,
                neutral : ( value[index].neutral / value[index].count )*100 || 0,
                negative : ( value[index].negative / value[index].count )*100 || 0
            };
        },

        /*
            Returns the total count of all tweets in a node
        */
        getTotalCount : function( value, index ) {
            var i,
                sum = 0,
                n = this.getTagCount( value );
            for (i=0; i<n; i++) {
                sum += value[i].count;
            }
            return sum;
        },

        /*
            Returns the percentage of tweets in a node for the respective tag
        */
        getTotalCountPercentage : function( value, index ) {
            return ( value[index].count / this.getTotalCount( value, index ) ) || 0;
        },

        /*
            Returns a font size based on the percentage of tweets relative to the total count
        */
        getFontSize : function( value, index ) {
            var DOWNSCALE_OFFSET = 1.5,
                MAX_FONT_SIZE = 28 * DOWNSCALE_OFFSET,
                MIN_FONT_SIZE = 12 * DOWNSCALE_OFFSET,
                FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,
                sum = this.getTotalCount( value, index ),
                percentage = this.getTotalCountPercentage( value, index ),
                scale = Math.log( sum ),
                size = ( percentage * FONT_RANGE * scale ) + ( MIN_FONT_SIZE * percentage );
            return Math.min( Math.max( size, MIN_FONT_SIZE), MAX_FONT_SIZE );
        },


        /*
            Returns a trimmed string based on character limit
        */
        trimLabelText : function( str ) {
            var MAX_LABEL_CHAR_COUNT = 9;
            if (str.length > MAX_LABEL_CHAR_COUNT) {
                str = str.substr( 0, MAX_LABEL_CHAR_COUNT ) + "...";
            }
            return str;
        },
        
        
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
                day = date.getDay();

            return month + " " + day + ", " + year + ":";
        },

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
        }


        

    };

});