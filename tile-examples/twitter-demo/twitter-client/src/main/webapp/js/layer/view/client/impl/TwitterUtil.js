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
        getTagCount : function( value, max ) {
            var MAX_COUNT = max || 5;
            return Math.min( value.length, MAX_COUNT );
        },

        /*
            Return the count of recent tweets entries, clamped at MAX_COUNT
        */
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


        getCountByTimePercentage: function( value, index, hour ) {
            var val = value[index],
                countByTime = val.countByTime[hour];
            return ( countByTime / val.count ) || 0;
        },


        getMaxCountByTimePercentage: function( value, index ) {
            var NUM_HOURS_IN_DAY = 24,
                val = value[index],
                i,
                percent,
                maxPercent = 0,
                count = val.count;

            for (i=0; i<NUM_HOURS_IN_DAY; i++) {
                percent = ( val.countByTime[i] / count ) || 0;
                maxPercent = Math.max( percent, maxPercent );
            }
            return maxPercent;
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
                day = date.getDay();

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

        /*
            Used to inject classes when nodes are loaded
        */
        injectClickStateClasses: function( $elem, tag, selectedTag ) {
            // if user has clicked a tag entry, ensure newly created nodes are styled accordingly
            if ( selectedTag ) {
                if ( selectedTag !== tag ) {
                    $elem.addClass('greyed');
                } else {
                    $elem.addClass('clicked');
                }
            }
        },

        createTweetSummaries: function() {
            return $('<div class="sentiment-summaries">'
                + '<div class="positive-summaries"></div>'
                + '<div class="neutral-summaries"></div>'
                + '<div class="negative-summaries"></div>'
                + '</div>');
        },



        setMouseEventCallbacks: function( map, $element, $summaries, data, index, callback, DetailsOnDemand ) {

            var that = this,
                value = data.bin.value[index];

            // set summaries text
            $element.mouseover( function( event ) {
                $summaries.find(".positive-summaries").text( "+" +value.positive );
                $summaries.find(".neutral-summaries").text( value.neutral );
                $summaries.find(".negative-summaries").text("-" + value.negative );
            });

            // clear summaries text
            $element.mouseout( function( event ) {
                $summaries.find(".positive-summaries").text( "" );
                $summaries.find(".neutral-summaries").text( "" );
                $summaries.find(".negative-summaries").text( "" );
                $element.off('click');
            });

            // moving mouse disables click event
            $element.mousemove( function( event ) {
                 $element.off('click');
            });

            // mouse down enables click event
            $element.mousedown( function( event ) {

                // set click handler
                $element.click( function( event ) {
                    // call given callback
                    $.proxy( callback, this )( event );
                    // create details on demand
                    that.createDetailsOnDemand( map, data, index, DetailsOnDemand );
                    // prevent event from going further
                    event.stopPropagation();
                 });
            });

        },


        /*
            Create details on demand
        */
        createDetailsOnDemand: function( map, data, index, DetailsOnDemand ) {

            var pos = map.getMapPixelFromCoord( data.longitude, data.latitude ),
                $details;

            this.centreForDetails( map, data );

            $details = DetailsOnDemand.create( pos.x + 256, map.getMapHeight() - pos.y, data.bin.value[index] );

            map.enableEventToMapPropagation( $details, ['onmousemove', 'onmouseup'] );
            map.getRootElement().append( $details );
        },

        destroyDetailsOnDemand: function( DetailsOnDemand ) {

            DetailsOnDemand.destroy();
        }



        

    };

});