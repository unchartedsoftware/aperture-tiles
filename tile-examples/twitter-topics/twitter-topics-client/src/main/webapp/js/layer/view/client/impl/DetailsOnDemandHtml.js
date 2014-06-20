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



    var TwitterUtil = require('./TwitterUtil');



    return {

        createAxisByType: function( value, type ) {

            var MONTH_INCS = 5,
                WEEK_INCS = 7,
                DAY_INCS = 5,
                NUM_INCS = {
                    month : MONTH_INCS,
                    week : WEEK_INCS,
                    day : DAY_INCS
                },
                month,
                dayInc,
                html = "",
                labels = [],
                i;

            function createMarkersHtml( numIncs ) {
                var totalIncs = (numIncs*2)-1,
                    width = 100/(totalIncs-1),
                    majorOrMinor = 'major',
                    html = "",
                    i;

                for (i=0; i<totalIncs; i++) {
                    if (i < totalIncs-1) {
                        html +=  '<div class="details-axis-marker details-'+majorOrMinor+'-marker" style="width:calc('+width+'% - 1px);"></div>';
                    } else {
                        html +=  '<div class="details-axis-marker details-last-major-marker"></div>';
                    }
                    majorOrMinor = (majorOrMinor === 'major') ? 'minor' : 'major';
                }

                return html;
            }

            function createLabelsHtml( numIncs, labels ) {
                var width = 100/numIncs,
                    html = "",
                    i;
                for (i=0; i<numIncs; i++) {
                    html += '<div class="details-axis-label" style="width:'+width+'%;">'+labels[i]+'</div>';
                }
                return html;
            }


            html +=     '<div class="details-axis-markers">';
            html +=         createMarkersHtml( NUM_INCS[type] );
            html +=     '</div>';

            switch (type) {

                case 'month':

                    month = TwitterUtil.getMonth( value );
                    dayInc = TwitterUtil.getTotalDaysInMonth( value ) / (MONTH_INCS-1);

                    for (i=0; i<MONTH_INCS; i++) {
                        labels.push( (i === 0) ? month + " 1" : month + " " + Math.round( dayInc*i ) );
                    }
                    break;

                case 'week':

                    for (i=0; i<WEEK_INCS+1; i++) {
                        labels.push( TwitterUtil.getLastWeekOfMonth( value )[i % WEEK_INCS] );
                    }
                    break;

                case 'day':

                    for (i=0; i<WEEK_INCS+1; i++) {
                        switch (i) {
                            case 1: labels.push( "6am" );   break;
                            case 2: labels.push( "12pm" );  break;
                            case 3: labels.push( "6pm" );   break;
                            default: labels.push( "12am" ); break;
                        }
                    }
                    break;
            }

            html +=     '<div class="details-axis-labels">';
            html +=         createLabelsHtml( NUM_INCS[type], labels );
            html +=     '</div>';

            return html;
        },


        createBarsHtml: function( value, type ) {

            var html = "",
                maxPercentage,
                relativePercent,
                visibility,
                barCount,
                barWidth,
                incType,
                i;

            switch (type) {

                case 'month':

                    barCount = TwitterUtil.getTotalDaysInMonth( value );
                    incType = 'Daily';
                    break;

                case 'week':

                    barCount = 28;
                    incType = 'Per6hrs';
                    break;

                case 'day':

                    barCount = 24;
                    incType = 'PerHour';
                    break;
            }

            barWidth = 100 / barCount;
            maxPercentage = TwitterUtil.getMaxPercentageByType( value, incType );
            for (i=0; i<barCount; i++ ) {
                relativePercent = ( TwitterUtil.getPercentageByType( value, i, incType ) / maxPercentage ) * 100;
                visibility = (relativePercent > 0) ? 'visible' : 'hidden';
                html += '<div class="details-chart-bar" style="visibility:'+visibility+'; width:'+barWidth+'%;">';
                html += '<div class="details-chart-bar-fill" style="height:'+relativePercent+'%;"></div>';
                html += '</div>';
            }

            return html;

        },


        createChartByType: function( value, type, title ) {

            var html = '';

            html +=     '<div class="details-on-demand-sub-chart">';
            html +=         '<div class="details-chart-title-label">'+title+'</div>';
            html +=         '<div class="details-chart-content">';
            // create bars
            html +=             '<div class="details-chart-bars">';
            html +=                 this.createBarsHtml( value, type );
            html +=             '</div>';
            // create axis
            html +=             '<div class="details-chart-axis">';
            html +=                 this.createAxisByType( value, type );
            html +=             '</div>';
            html +=         '</div>';
            html +=     '</div>';

            return html;
        },

        create: function( position, value, topic, closeCallback ) {

            var html = '',
                day, tweetsByDay, key, lightOrDark,
                $details,
                i;

            html += '<div class="details-on-demand" style="left:'+position.x+'px; top:'+position.y+'px;">';

            // top half
            html += '<div class="details-on-demand-half">';
            html +=     '<div class="details-on-demand-title large-title">'+TwitterUtil.trimLabelText( topic, 12 )+'</div>';
            html +=     '<div class="details-on-demand-chart">';
            html +=         this.createChartByType( value, "month", "Last Month" );    // last month
            html +=         this.createChartByType( value, "week", "Last Week" );      // last week
            html +=         this.createChartByType( value, "day", "Last 24 hours" );   // last day
            html +=     '</div>';
            html += '</div>';


            // bottom half
            html += '<div class="details-on-demand-half">';
            // most recent tweets
            html +=     '<div class="details-on-demand-title small-title">Most Recent</div>';
            html +=     '<div class="details-on-demand-recent-tweets">';

            // bucket tweets by day
            tweetsByDay = TwitterUtil.getRecentTweetsByDay( value );

            for ( key in tweetsByDay ) {
                if( tweetsByDay.hasOwnProperty( key ) ) {
                    day = tweetsByDay[key];
                    lightOrDark = 'light';
                    html += '<div class="details-on-demand-tweet-day">'+key+'</div>';
                    for (i=0; i<day.length; i++) {
                        html += '<div class="details-on-demand-tweet tweet-'+lightOrDark+'">';
                        html +=     '<div class="details-on-demand-tweet-text">'+day[i].tweet.substring(1, day[i].tweet.length - 2)+'</div>';
                        html +=     '<div class="details-on-demand-tweet-time">'+day[i].time+'</div>';
                        html += '</div>';
                        lightOrDark = (lightOrDark === 'light') ? 'dark' : 'light';
                    }
                }
            }

            html +=     '</div>';
            html += '</div>';

            html += '<div class="details-on-demand-close-button"></div>';
            html += '</div>';

            this.destroy(); // destroy any previous DoD

            // create element
            $details = $(html).draggable().resizable({
                minHeight: 513,
                minWidth: 257,
                handles: 'se'
            });
            $details.find('.details-on-demand-close-button').click( closeCallback );
            return $details;

        },


        destroy : function() {
            $('.details-on-demand').remove();
        }

    };

});