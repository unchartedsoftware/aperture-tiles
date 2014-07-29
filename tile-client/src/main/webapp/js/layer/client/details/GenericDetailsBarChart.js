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



    var Class = require('../../../class'),
        createAxisHtml,
        createBarsHtml,
        setBarHoverCallbacks,
        GenericDetailsBarChart;


    createAxisHtml = function( labels ) {

        var NUM_INCS = labels.length,
            MARKER_INCS = NUM_INCS*2,
            LABEL_INCS = NUM_INCS,
            MARKER_WIDTH = 100/(NUM_INCS*2),
            LABEL_WIDTH = 100/NUM_INCS,
            html = "";

        function createMarkersHtml() {

            var majorOrMinor = 'major',
                html = "", i;

            for (i=0; i<MARKER_INCS+1; i++) {

                if (i < MARKER_INCS) {
                    html += '<div class="details-axis-marker details-'+majorOrMinor+'-marker" style="width:calc('+MARKER_WIDTH+'% - 1px);"></div>';
                } else {
                    html += '<div class="details-axis-marker details-'+majorOrMinor+'-marker details-last-marker"></div>';
                }
                majorOrMinor = (majorOrMinor === 'major') ? 'minor' : 'major';
            }
            return html;
        }

        function createLabelsHtml() {

            var html = "", i;

            for (i=0; i<LABEL_INCS; i++) {

                if (i < LABEL_INCS-1) {
                    html += '<div class="details-axis-label" style="width:'+LABEL_WIDTH+'%;">'+labels[i]+'</div>';
                } else {
                    html += '<div class="details-axis-label">'+labels[i]+'</div>';
                }
            }
            return html;
        }

        html += '<div class="details-axis-markers">';
        html +=     createMarkersHtml();
        html += '</div>';
        html += '<div class="details-axis-labels">';
        html +=     createLabelsHtml();
        html += '</div>';

        return html;
    };


    createBarsHtml =  function( percentages, tooltips ) {

        var html = "",
            maxPercentage,
            relativePercentage,
            barWidth,
            i;

        function getMaximumPercent( percentages ) {
            var max = 0,
                i;
            for (i=0; i<percentages.length; i++) {
                max = Math.max( percentages, max );
            }
            return max;
        }

        barWidth = 100 / percentages.length;
        maxPercentage = getMaximumPercent( percentages );

        for (i=0; i<percentages.length; i++ ) {
            relativePercentage = ( percentages[i] / maxPercentage ) * 100;
            html += '<div class="details-chart-bar style="width:'+barWidth+'%;">';
            html +=     tooltips[i];
            html +=     '<div class="details-chart-bar-fill" style="height:'+relativePercentage+'%;"></div>';
            html += '</div>';
        }
        return html;
    };


    setBarHoverCallbacks = function( $chart, value ) {

        var $bars = $chart.find('.details-chart-bar');

        function createHoverLabel( event, $this ) {
            var $chart = $this.parent(),
                pos = $chart.offset(),
                x = event.clientX-pos.left,
                y = event.clientY-pos.top,
                $label = $('<div class="details-bar-hover" style="left:'+ x +'px; top:'+ y +'px;">'
                         +    '<div class="details-bar-hover-label">'+ $this.text() +'</div>'
                         + '</div>');
            // remove previous label if it exists
            $chart.find('.details-bar-hover').remove();
            // add new label
            $chart.append( $label );
            // reposition to be centred above cursor
            $label.css( {"margin-top": -$label.outerHeight()*1.5, "margin-left": -$label.outerWidth()/2 } );
        }

        $bars.mousemove( function( event ) {
            createHoverLabel( event, $(this) );
        });
        $bars.mouseout( function( event ) {
            $('.details-bar-hover').remove();
        });

    };


    GenericDetailsBarChart = Class.extend({
        ClassName: "GenericDetailsBarChart",

        init: function() {
            this.$chart = null;
        },


        create: function( title, labels, percentages, tooltips ) {

            var html = '';
            html += '<div class="details-chart">';
            html +=     '<div class="details-chart-title">'+title+'</div>';
            html +=     '<div class="details-chart-content">';
            // create bars
            html +=         '<div class="details-chart-bars">';
            html +=             createBarsHtml( percentages, tooltips );
            html +=         '</div>';
            // create axis
            html +=         '<div class="details-chart-axis">';
            html +=             createAxisHtml( labels );
            html +=         '</div>';
            html +=     '</div>';
            html += '</div>';

            this.$chart = $(html);
            setBarHoverCallbacks( this.$chart );
            return this.$chart;
        },


        destroy : function() {
            this.$chart.remove();
        }

     });

     return GenericDetailsBarChart;

});