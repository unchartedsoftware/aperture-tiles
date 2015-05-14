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

( function() {

    "use strict";

    var Renderer = require('./Renderer'),
        RendererUtil = require('./RendererUtil'),
        injectCss,
        getLabelWidth,
        capitalize;

    injectCss = function( spec ) {
        var i;
        if ( spec.text.themes ) {
            for ( i=0; i<spec.text.themes.length; i++ ) {
                spec.text.themes[i].injectTheme({
                    selector: ".node-label"
                });
            }
        }
    };

    /**
     * Returns the pixel width of the label
     */
     getLabelWidth = function( str, fontSize ) {
        var $temp,
            width;
        $temp = $('<div class="node-label" style="font-size:'+fontSize+'px; padding-left:5px; padding-right:5px;">'+str+'</div>');
        $('body').append( $temp );
        width = $temp.outerWidth();
        $temp.remove();
        return width;
    };

    /**
     * Capitalizes the given word.
     */
    capitalize = function( str ) {
        return str.replace(/(?:^|,|\s)\S/g, function( a ) {
            return a.toUpperCase();
        });
    };

    /**
     * Instantiate a GraphLabelRenderer object.
     * @class GraphLabelRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders a set of graph labels.
     *
     * @param spec {Object} The specification object.
     * <pre>
     * {
     *     text: {
     *         xKey   {String|Function} - The attribute for the x coordinate.
     *         yKey   {String|Function} - The attribute for the y coordinate.
     *         themes {Array}  - The array of RenderThemes to be attached to this component.
     *     }
     * }
     * </pre>
     */
    function GraphLabelRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.values[0].value[0].communities";
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    GraphLabelRenderer.prototype = Object.create( Renderer.prototype );

    /**
     * Implementation specific rendering function.
     * @memberof GraphLabelRenderer
     * @private
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    GraphLabelRenderer.prototype.render = function( data ) {

        var GRAPH_COORD_RANGE = 256,
            text = this.spec.text,
            levelMinMax = this.parent.getLevelMinMax(),
            communities = RendererUtil.getAttributeValue( data, this.spec.rootKey ),
            scale = Math.pow( 2, this.parent.map.getZoom() ),
            range =  GRAPH_COORD_RANGE / scale,
            labelIndex = ( text.labelIndex !== undefined ) ? text.labelIndex : 0,
            community,
            html = "",
            count,
            fontSize,
            split,
            label,
            width,
            minimumCount,
            maximumCount,
            percent,
            percentLabel,
            hierLevel,
            parentIDarray = [],
            entries = [],
            x, y, i;



        // get graph hierarchy level for this zoom level
        // assumes same hierarchy level for all tiles at a given zoom level
        hierLevel = levelMinMax.maximum.communities[0].hierLevel;

        // if hierLevel = 0, normalize label attributes by community degree
        // else normalize label attributes by num internal nodes
        if ( hierLevel === 0 ) {
            minimumCount =  levelMinMax.minimum.communities[0].degree;
            maximumCount =  levelMinMax.maximum.communities[0].degree;
        } else {
            minimumCount =  levelMinMax.minimum.communities[0].numNodes;
            maximumCount =  levelMinMax.maximum.communities[0].numNodes;
        }

        for ( i=0; i<communities.length; i++ ) {

            community = communities[i];

            // capitalize label array, split by comma
            split = community.metadata.split(",");
            // don't draw labels for isolated communities
            // and don't render if label string is empty
            // only draw one label per parent community per tile
            // only draw up to 5 labels per tile
            if ( community.degree === 0 ||
                 split[ labelIndex ] === "" ||
                 parentIDarray.length >= 5 ||
                 parentIDarray.indexOf( community.parentID ) !== -1 ) {
                // Skip this label entry
                continue;
            }
            else {
                // add this parent ID to the list, and draw the label (below
                parentIDarray.push( community.parentID );
            }

            // add to entries only if there is a legible label
            entries.push( community );

            // get label position
            x = ( community[ text.xKey ] % range ) * scale;
            y = ( community[ text.yKey ] % range ) * scale;

            // capitalize label
            label = capitalize( split[ labelIndex ].toLowerCase() );

            // get font scale based on hierarchy level
            count = ( hierLevel === 0 ) ? community.degree : community.numNodes;
            fontSize = RendererUtil.getFontSize(
                count, minimumCount, maximumCount, { type:'log' } );

            // calc percent label
            percent = RendererUtil.transformValue( count, minimumCount, maximumCount, 'log' );
            percentLabel = Math.round( ( percent*100 ) / 10 ) * 10;

            // calc width for centering
            width = getLabelWidth( label, fontSize );
            width = Math.min( width, 200 );

            html += '<div class="node-label node-label-'+percentLabel+'" style="'
                + 'left:'+x+'px;'
                + 'bottom:'+y+'px;'
                + 'font-size:' + fontSize + 'px;'
                + 'line-height:' + fontSize + 'px;'
                + 'width:' + width + 'px;'
                + 'margin-left:' + (-width/2) + 'px;'
                + 'margin-top:' + (-fontSize/2) + 'px;'
                + 'height:' + fontSize + 'px;'
                + 'z-index:' + Math.floor( fontSize ) + ';'
                + '">'+label+'</div>';

        }
        return {
            html: html,
            entries: entries
        };
    };

    module.exports = GraphLabelRenderer;
}());
